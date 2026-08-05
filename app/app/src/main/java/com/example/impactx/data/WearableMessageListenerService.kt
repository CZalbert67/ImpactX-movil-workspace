package com.example.impactx.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.impactx.MainActivity
import com.example.impactx.data.local.AppDatabase
import com.example.impactx.data.local.AccidentEntity
import com.example.impactx.data.remote.ApiClient
import com.example.impactx.data.remote.SosRequest
import com.example.impactx.data.remote.StartTripRequest
import com.example.impactx.ui.screens.BLEState
import com.example.impactx.ui.screens.WearableManager
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class WearableMessageListenerService : WearableListenerService() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onMessageReceived(messageEvent: MessageEvent) {
        val path = messageEvent.path
        val rawData = String(messageEvent.data)
        Log.d("WearSync", "Received message on path: $path | data: $rawData")

        when {
            path == "/telemetry" -> handleTelemetry(rawData)
            path == "/impact-detected" || path == "/sos-triggered" -> handleImpact(rawData)
            path == "/start-trip" -> handleStartTrip(rawData)
            path == "/finish-trip" -> handleFinishTrip()
            path == "/alarm-reset" -> { /* watch cancelled the alarm - no action needed */ }
            else -> Log.w("WearSync", "Unknown path: $path")
        }
    }

    // ─── Telemetry ────────────────────────────────────────────────────────────
    private fun handleTelemetry(rawData: String) {
        try {
            val json = JSONObject(rawData)
            val heartRate = json.optInt("heartRate", 0)
            val gForce = json.optDouble("gForce", 1.0).toFloat()
            val batteryLevel = json.optInt("batteryLevel", 100)

            WearableManager.realHeartRate = heartRate
            WearableManager.realBatteryLevel = batteryLevel
            WearableManager.bleState = BLEState.CONNECTED_DASHBOARD
            WearableManager.isRealConnection = true
            WearableManager.connectedDeviceName = "Galaxy Watch (Wear OS)"

            Log.d("WearSync", "Telemetry updated: HR=$heartRate, G=$gForce, Batt=$batteryLevel%")
        } catch (e: Exception) {
            Log.e("WearSync", "Error parsing telemetry: ${e.message}")
        }
    }

    // ─── Impact / SOS ────────────────────────────────────────────────────────
    private fun handleImpact(rawData: String) {
        // Trigger screen wake and high-priority app launch immediately
        triggerEmergencyAutoLaunch(applicationContext)

        scope.launch {
            try {
                // Always record as 25.0 G for real-looking crash records as requested
                val gForce = 25.0
                val heartRate = if (WearableManager.realHeartRate > 0) WearableManager.realHeartRate else 75

                // Get GPS from phone
                val location = LocationHelper.getLastKnownLocation(applicationContext)
                val lat = location?.latitude ?: 0.0
                val lng = location?.longitude ?: 0.0

                val timestamp = java.text.SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss", 
                    java.util.Locale.getDefault()
                ).format(java.util.Date())

                Log.w("WearSync", "IMPACT received. Saving locally! G=$gForce, HR=$heartRate, GPS=$lat,$lng, Time=$timestamp")

                // Insert into SQLite database in background thread
                val db = AppDatabase.getDatabase(applicationContext)
                withContext(Dispatchers.IO) {
                    db.accidentDao().insertAccident(
                        AccidentEntity(
                            heartRate,
                            gForce,
                            timestamp,
                            lat,
                            lng,
                            false
                        )
                    )
                }

                // If there's an active trip, automatically finish it since an accident occurred
                val prefs = applicationContext.getSharedPreferences("impactx_prefs", Context.MODE_PRIVATE)
                val tripId = prefs.getString("active_trip_id", null) ?: WearableManager.activeWearTripId
                if (tripId != null) {
                    try {
                        val api = ApiClient.getApiService(applicationContext)
                        api.finishTrip(tripId)
                        prefs.edit().remove("active_trip_id").apply()
                        WearableManager.activeWearTripId = null
                        Log.i("WearSync", "Trip $tripId automatically finished on impact SOS.")
                    } catch (ex: Exception) {
                        Log.e("WearSync", "Failed to auto-finish trip: ${ex.message}")
                    }
                }

                // Signal UI to navigate to MandarDatosScreen
                WearableManager.triggerEmergencyNav = true

            } catch (e: Exception) {
                Log.e("WearSync", "Error handling impact: ${e.message}")
                // Still notify UI even if DB insert fails
                WearableManager.triggerEmergencyNav = true
            }
        }
    }

    private fun triggerEmergencyAutoLaunch(context: Context) {
        val channelId = "emergency_alerts"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Alertas Críticas de Colisión",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones prioritarias para incidentes y colisiones detectadas"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("TRIGGER_ALARM", true)
        } ?: Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("TRIGGER_ALARM", true)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            1001,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("¡Posible Colisión Detectada!")
            .setContentText("Abre la aplicación para verificar tu estado.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1001, notification)

        try {
            context.startActivity(launchIntent)
        } catch (e: Exception) {
            Log.e("WearSync", "Failed to start MainActivity directly: ${e.message}")
        }
    }

    // ─── Trip Start ──────────────────────────────────────────────────────────
    private fun handleStartTrip(rawData: String) {
        scope.launch {
            try {
                val location = LocationHelper.getLastKnownLocation(applicationContext)
                val lat = location?.latitude
                val lng = location?.longitude
                val rutaOrigen = if (lat != null && lng != null) {
                    LocationHelper.formatLocation(lat, lng)
                } else null

                Log.i("WearSync", "Starting trip from watch. Origin GPS: $rutaOrigen")

                val api = ApiClient.getApiService(applicationContext)
                val response = api.startTrip(
                    StartTripRequest(
                        dispositivoId = android.provider.Settings.Secure.getString(
                            contentResolver,
                            android.provider.Settings.Secure.ANDROID_ID
                        ) ?: "watch-device",
                        proposito = "Viaje iniciado desde el reloj",
                        rutaOrigen = rutaOrigen
                    )
                )

                if (response.isSuccessful) {
                    val trip = response.body()
                    WearableManager.activeWearTripId = trip?.id
                    
                    // Persist active trip ID
                    val prefs = applicationContext.getSharedPreferences("impactx_prefs", Context.MODE_PRIVATE)
                    prefs.edit().putString("active_trip_id", trip?.id).apply()
                    
                    Log.i("WearSync", "Trip started! TripId=${trip?.id}")
                } else {
                    Log.e("WearSync", "Trip start API error: ${response.code()} ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e("WearSync", "Error starting trip from watch: ${e.message}")
            }
        }
    }

    // ─── Trip Finish ─────────────────────────────────────────────────────────
    private fun handleFinishTrip() {
        val prefs = applicationContext.getSharedPreferences("impactx_prefs", Context.MODE_PRIVATE)
        val tripId = prefs.getString("active_trip_id", null) ?: WearableManager.activeWearTripId
        if (tripId == null) {
            Log.w("WearSync", "No active trip ID found to finish.")
            return
        }
        
        scope.launch {
            try {
                val api = ApiClient.getApiService(applicationContext)
                api.finishTrip(tripId)
                prefs.edit().remove("active_trip_id").apply()
                WearableManager.activeWearTripId = null
                Log.i("WearSync", "Trip $tripId finished from watch.")
            } catch (e: Exception) {
                Log.e("WearSync", "Error finishing trip: ${e.message}")
            }
        }
    }
}

