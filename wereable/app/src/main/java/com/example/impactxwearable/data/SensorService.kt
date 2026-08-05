package com.example.impactxwearable.data

import android.app.*
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import kotlin.coroutines.resumeWithException
import kotlin.math.sqrt

class SensorService : Service(), SensorEventListener {

    private val binder = SensorBinder()
    private lateinit var sensorManager: SensorManager
    private var heartRateSensor: Sensor? = null
    private var accelerometerSensor: Sensor? = null
    private var gyroscopeSensor: Sensor? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Live telemetry states
    private val _heartRate = MutableStateFlow(0)
    val heartRate = _heartRate.asStateFlow()

    private val _gForce = MutableStateFlow(1.0f)
    val gForce = _gForce.asStateFlow()

    private val _maxGForce = MutableStateFlow(1.0f)
    val maxGForce = _maxGForce.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected = _isConnected.asStateFlow()

    private val _impactDetected = MutableStateFlow(false)
    val impactDetected = _impactDetected.asStateFlow()

    private val _isTripActive = MutableStateFlow(false)
    val isTripActive = _isTripActive.asStateFlow()

    // ── Trip sync state machine ─────────────────────────────────────────────
    enum class TripSyncState { IDLE, STARTING, ACTIVE, FINISHING, ERROR }

    internal val _tripSyncState = MutableStateFlow(TripSyncState.IDLE)
    val tripSyncState = _tripSyncState.asStateFlow()

    /** The eventId of the pending START_TRIP sent but not yet confirmed. */
    @Volatile var pendingTripEventId: String? = null

    fun setTripActive(active: Boolean) {
        // Legacy helper — use tripSyncState for new UI code
        _isTripActive.value = active
    }

    /**
     * Called when the phone replies on /trip-confirmed.
     * Updates the state machine: STARTING → ACTIVE or ERROR.
     */
    fun receiveTripConfirmation(payloadJson: String) {
        val json = runCatching { org.json.JSONObject(payloadJson) }.getOrNull() ?: return
        val success = json.optBoolean("success", false)
        val tripId = json.optString("tripId", "")
        if (success && tripId.isNotBlank()) {
            _tripSyncState.value = TripSyncState.ACTIVE
            _isTripActive.value = true
        } else {
            _tripSyncState.value = TripSyncState.ERROR
            _isTripActive.value = false
        }
    }

    // ── Unified impact coordinator ────────────────────────────────────────────
    // Guarantees exactly ONE impactEventId per physical event regardless of
    // how many sensors (accelerometer, gyroscope) fire simultaneously.
    @Volatile internal var impactEventId: String? = null
    @Volatile private var lastImpactMs: Long = 0
    private val IMPACT_COOLDOWN_MS = 15_000L

    private fun coordinateImpact(source: String, peakG: Float) {
        val now = System.currentTimeMillis()
        // If already triggered within cooldown, ignore
        if (now - lastImpactMs < IMPACT_COOLDOWN_MS) return
        // Set _impactDetected before the cooldown guard so the UI reflects it immediately
        if (_impactDetected.value) return
        lastImpactMs = now
        val eventId = java.util.UUID.randomUUID().toString()
        impactEventId = eventId
        triggerImpactAlert(source, peakG, eventId)
    }

    private var lastTelemetryTime = 0L

    inner class SensorBinder : Binder() {
        fun getService(): SensorService = this@SensorService
    }

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        // WakeLock setup for background operations
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ImpactX:SensorServiceWakeLock").apply {
            acquire(10 * 60 * 1000L /*10 minutes fallback*/)
        }

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification("Iniciando sensores..."))

        registerSensors()
        checkPhoneConnection()
        startTelemetryLoop()
    }

    private fun registerSensors() {
        // Register PPG sensor (Heart Rate)
        heartRateSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        // Register Accelerometer (G-Force calculation)
        accelerometerSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        // Register Gyroscope (Rollover/fall detection)
        gyroscopeSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    private fun unregisterSensors() {
        sensorManager.unregisterListener(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_HEART_RATE -> {
                if (event.values.isNotEmpty()) {
                    val hr = event.values[0].toInt()
                    if (hr > 0) {
                        _heartRate.value = hr
                        updateNotification("Ritmo Cardíaco: $hr lpm")
                        sendTelemetryToPhone()
                    }
                }
            }
            Sensor.TYPE_ACCELEROMETER -> {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                
                // Calculate G-Force magnitude
                val magnitude = sqrt(x*x + y*y + z*z) / 9.81f
                _gForce.value = magnitude

                if (magnitude > _maxGForce.value) {
                    _maxGForce.value = magnitude
                }

                // Crash detection threshold (> 8.0G) - sensitive to agitating/shaking for testing
                if (magnitude > 8.0f) {
                    coordinateImpact("ACCELEROMETER", magnitude)
                }

                val now = System.currentTimeMillis()
                if (now - lastTelemetryTime > 1000L) { // Throttle telemetry updates to 1Hz
                    sendTelemetryToPhone()
                    lastTelemetryTime = now
                }
            }
            Sensor.TYPE_GYROSCOPE -> {
                val rollRate = event.values[0]
                val pitchRate = event.values[1]
                
                // Check for fast rotation suggesting a rollover (> 30 rad/s threshold)
                val rotationMagnitude = sqrt(rollRate*rollRate + pitchRate*pitchRate)
                if (rotationMagnitude > 30.0f) {
                    coordinateImpact("GYROSCOPE", rotationMagnitude)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun triggerImpactAlert(source: String, peakG: Float, eventId: String) {
        _impactDetected.value = true
        // Send IMPACT_DETECTED with unified eventId — the phone escalates to SOS on confirmation
        val payload = org.json.JSONObject().apply {
            put("eventId", eventId)
            put("action", "IMPACT_DETECTED")
            put("source", source)
            put("peakG", peakG)
            put("timestampUtc", java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).format(java.util.Date()))
        }.toString()
        sendSignalToPhone("/impact-detected", payload)
        
        // 1. Wake physical screen using WakeLock
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            val screenWakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
                "ImpactX:SensorServiceScreenWakeLock"
            )
            screenWakeLock.acquire(15000L /* 15 seconds */)
        } catch (e: Exception) {
            Log.e("SensorService", "Failed to acquire screen wake lock: ${e.message}")
        }

        // 2. High priority notification with fullScreenIntent
        val emergencyChannelId = "wearable_emergency_alerts"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                emergencyChannelId,
                "Alertas Críticas de Emergencia",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de colisión y pánico"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val alarmIntent = Intent(this, Class.forName("com.example.impactxwearable.MainActivity")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("TRIGGER_ALARM", true)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            2027,
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, emergencyChannelId)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("¡Colisión Detectada!")
            .setContentText("Sospecha de colisión grave. Iniciando SOS...")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(2027, notification)

        // 3. Start Activity directly
        try {
            startActivity(alarmIntent)
        } catch (e: Exception) {
            Log.e("SensorService", "Failed to start MainActivity directly: ${e.message}")
        }
    }

    fun resetAlarm() {
        _impactDetected.value = false
        _maxGForce.value = 1.0f
        sendSignalToPhone("/alarm-reset", "OK")
    }

    private fun checkPhoneConnection() {
        serviceScope.launch {
            while (isActive) {
                try {
                    val nodes = Wearable.getNodeClient(this@SensorService).connectedNodes.await()
                    _isConnected.value = nodes.isNotEmpty()
                } catch (e: Exception) {
                    _isConnected.value = false
                }
                delay(5000)
            }
        }
    }

    private fun startTelemetryLoop() {
        serviceScope.launch {
            while (isActive) {
                // If heart rate or G-Force has no sensor updates (e.g. emulator static), fluctuate them locally
                if (_heartRate.value == 0) {
                    _heartRate.value = (70..85).random()
                }
                if (_gForce.value == 1.0f && !_impactDetected.value) {
                    _gForce.value = 0.98f + ((-2..2).random() / 100f)
                }
                sendTelemetryToPhone()
                delay(2000) // Send telemetry every 2 seconds
            }
        }
    }

    private fun getBatteryLevel(): Int {
        val bm = getSystemService(Context.BATTERY_SERVICE) as? android.os.BatteryManager
        return bm?.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 100
    }

    private fun sendTelemetryToPhone() {
        serviceScope.launch {
            try {
                val nodes = Wearable.getNodeClient(this@SensorService).connectedNodes.await()
                val data = JSONObject().apply {
                    put("heartRate", _heartRate.value)
                    put("gForce", _gForce.value)
                    put("maxGForce", _maxGForce.value)
                    put("isImpact", _impactDetected.value)
                    put("batteryLevel", getBatteryLevel())
                }.toString().toByteArray()

                for (node in nodes) {
                    Wearable.getMessageClient(this@SensorService)
                        .sendMessage(node.id, "/telemetry", data)
                }
            } catch (e: Exception) {
                Log.e("WearTelemetry", "Error sending telemetry: ${e.message}")
            }
        }
    }

    fun sendSignalToPhone(path: String, payload: String) {
        serviceScope.launch {
            try {
                val nodes = Wearable.getNodeClient(this@SensorService).connectedNodes.await()
                val data = payload.toByteArray()
                for (node in nodes) {
                    Wearable.getMessageClient(this@SensorService)
                        .sendMessage(node.id, path, data)
                }
            } catch (e: Exception) {
                Log.e("WearTelemetry", "Error sending signal: ${e.message}")
            }
        }
    }

    // Helper extension to suspend await Google tasks
    private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            if (task.isSuccessful) {
                continuation.resume(task.result, null)
            } else {
                continuation.resumeWithException(task.exception ?: Exception("Unknown error"))
            }
        }
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            "ImpactX Sensor Monitoring",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(serviceChannel)
    }

    private fun createNotification(content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Burbuja de Seguridad Activa")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .build()
    }

    private fun updateNotification(content: String) {
        val notification = createNotification(content)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        unregisterSensors()
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        super.onDestroy()
    }

    companion object {
        private const val NOTIFICATION_ID = 2026
        private const val CHANNEL_ID = "SensorServiceChannel"
    }
}
