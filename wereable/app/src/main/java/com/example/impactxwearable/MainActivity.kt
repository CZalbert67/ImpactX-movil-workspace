package com.example.impactxwearable

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import com.example.impactxwearable.data.SensorService
import com.example.impactxwearable.theme.ImpactXWearableTheme
import com.example.impactxwearable.ui.WearAlertScreen
import com.example.impactxwearable.ui.WearHomeScreen

class MainActivity : ComponentActivity() {

    private var sensorService by mutableStateOf<SensorService?>(null)
    private var isServiceRunning by mutableStateOf(false)
    private var isBound by mutableStateOf(false)
    private var triggerAlarmState by mutableStateOf(false)

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as SensorService.SensorBinder
            sensorService = binder.getService()
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            sensorService = null
            isBound = false
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val sensorsGranted = permissions[Manifest.permission.BODY_SENSORS] ?: false
        val activityGranted = permissions[Manifest.permission.ACTIVITY_RECOGNITION] ?: false
        if (sensorsGranted && activityGranted) {
            Toast.makeText(this, "Permisos concedidos ✅", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Se requieren permisos para monitorear", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        checkAndRequestPermissions()

        triggerAlarmState = intent?.getBooleanExtra("TRIGGER_ALARM", false) ?: false

        // Prevent screen sleep and turn screen on for emergency alerts
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            ImpactXWearableTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    val service = sensorService

                    if (isServiceRunning && service != null) {
                        val heartRate by service.heartRate.collectAsState()
                        val gForce by service.gForce.collectAsState()
                        val maxGForce by service.maxGForce.collectAsState()
                        val isConnected by service.isConnected.collectAsState()
                        val impactDetected by service.impactDetected.collectAsState()
                        val isTripActive by service.isTripActive.collectAsState()
                        val tripSyncState by service.tripSyncState.collectAsState()

                        // ── Safe one-shot impact signal on TRIGGER_ALARM intent ──────────
                        // LaunchedEffect runs once per key change, NOT on every recomposition.
                        // After sending, we consume the flag to prevent resend after rotation.
                        LaunchedEffect(triggerAlarmState) {
                            if (triggerAlarmState && !impactDetected) {
                                val eventId = java.util.UUID.randomUUID().toString()
                                val payload = org.json.JSONObject().apply {
                                    put("eventId", eventId)
                                    put("action", "IMPACT_DETECTED")
                                    put("source", "INTENT")
                                }.toString()
                                service.sendSignalToPhone("/impact-detected", payload)
                                triggerAlarmState = false  // Consume flag immediately
                            }
                        }

                        if (impactDetected || triggerAlarmState) {
                            WearAlertScreen(
                                onCancel = {
                                    triggerAlarmState = false
                                    intent?.removeExtra("TRIGGER_ALARM")
                                    service.resetAlarm()
                                },
                                onTimeout = {
                                    // Escalate the SAME impactEventId to SOS — do NOT create a new event.
                                    // The phone already received IMPACT_DETECTED and is tracking it.
                                    val existingEventId = service.impactEventId
                                    val payload = org.json.JSONObject().apply {
                                        put("eventId", existingEventId ?: java.util.UUID.randomUUID().toString())
                                        put("action", "ESCALATE_TO_SOS")
                                    }.toString()
                                    service.sendSignalToPhone("/sos-triggered", payload)
                                }
                            )
                        } else {
                            WearHomeScreen(
                                isServiceRunning = true,
                                heartRate = heartRate,
                                gForce = gForce,
                                maxGForce = maxGForce,
                                isConnected = isConnected,
                                isTripActive = isTripActive,
                                tripSyncState = tripSyncState,
                                onToggleService = { toggleMonitoringService() },
                                onSimulateImpact = {
                                    // Send crash signal with current sensor data
                                    val payload = """{"gForce":${gForce},"heartRate":${heartRate}}"""
                                    service.sendSignalToPhone("/impact-detected", payload)
                                },
                                onStartTrip = {
                                    // Only send START_TRIP if in IDLE or ERROR state
                                    if (tripSyncState == SensorService.TripSyncState.IDLE ||
                                        tripSyncState == SensorService.TripSyncState.ERROR) {
                                        val eventId = java.util.UUID.randomUUID().toString()
                                        service.pendingTripEventId = eventId
                                        service._tripSyncState.value = SensorService.TripSyncState.STARTING
                                        val payload = org.json.JSONObject().apply {
                                            put("eventId", eventId)
                                            put("action", "START_TRIP")
                                        }.toString()
                                        service.sendSignalToPhone("/start-trip", payload)
                                        // Do NOT call setTripActive(true) here — wait for /trip-confirmed from phone
                                    }
                                },
                                onFinishTrip = {
                                    val eventId = java.util.UUID.randomUUID().toString()
                                    service._tripSyncState.value = SensorService.TripSyncState.FINISHING
                                    val payload = org.json.JSONObject().apply {
                                        put("eventId", eventId)
                                        put("action", "FINISH_TRIP")
                                    }.toString()
                                    service.sendSignalToPhone("/finish-trip", payload)
                                    service.setTripActive(false)
                                    service._tripSyncState.value = SensorService.TripSyncState.IDLE
                                    Toast.makeText(this, "Viaje finalizado", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    } else {
                        WearHomeScreen(
                            isServiceRunning = false,
                            heartRate = 0,
                            gForce = 1.0f,
                            maxGForce = 1.0f,
                            isConnected = false,
                            isTripActive = false,
                            onToggleService = { toggleMonitoringService() },
                            onSimulateImpact = {},
                            onStartTrip = {},
                            onFinishTrip = {}
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        triggerAlarmState = intent.getBooleanExtra("TRIGGER_ALARM", false)
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.BODY_SENSORS,
            Manifest.permission.ACTIVITY_RECOGNITION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            requestPermissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun toggleMonitoringService() {
        val intent = Intent(this, SensorService::class.java)
        if (isServiceRunning) {
            if (isBound) {
                unbindService(connection)
                isBound = false
            }
            stopService(intent)
            sensorService?.setTripActive(false)
            isServiceRunning = false
            sensorService = null
        } else {
            startForegroundService(intent)
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
            isServiceRunning = true
        }
    }

    override fun onDestroy() {
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
        super.onDestroy()
    }
}
