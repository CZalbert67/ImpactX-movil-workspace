package com.example.impactx.data

import android.util.Log
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

            WearableManager.realHeartRate = heartRate
            WearableManager.bleState = BLEState.CONNECTED_DASHBOARD
            WearableManager.isRealConnection = true
            WearableManager.connectedDeviceName = "Galaxy Watch (Wear OS)"

            Log.d("WearSync", "Telemetry updated: HR=$heartRate, G=$gForce")
        } catch (e: Exception) {
            Log.e("WearSync", "Error parsing telemetry: ${e.message}")
        }
    }

    // ─── Impact / SOS ────────────────────────────────────────────────────────
    private fun handleImpact(rawData: String) {
        scope.launch {
            try {
                val json = runCatching { JSONObject(rawData) }.getOrNull()
                val gForce = json?.optDouble("gForce", 4.5) ?: 4.5
                val heartRate = WearableManager.realHeartRate

                // Get GPS from phone
                val location = LocationHelper.getLastKnownLocation(applicationContext)
                val lat = location?.latitude ?: 0.0
                val lng = location?.longitude ?: 0.0

                Log.w("WearSync", "IMPACT detected! G=$gForce, HR=$heartRate, GPS=$lat,$lng")

                val api = ApiClient.getApiService(applicationContext)
                val response = api.sendSos(
                    SosRequest(
                        lat = lat,
                        lng = lng,
                        lugar = if (lat != 0.0) LocationHelper.formatLocation(lat, lng) else "Ubicación no disponible",
                        severidad = "severe",
                        canal = "wearable",
                        gForce = "%.2f".format(gForce),
                        frecuenciaCardiaca = heartRate.toString(),
                        modo = "automatico",
                        viajeId = WearableManager.activeWearTripId
                    )
                )

                if (response.isSuccessful) {
                    val alert = response.body()
                    WearableManager.lastCrashAlertId = alert?.id
                    Log.i("WearSync", "SOS sent! AlertId=${alert?.id}, contacts=${alert?.contactosNotificados}")
                } else {
                    Log.e("WearSync", "SOS API error: ${response.code()} ${response.message()}")
                }

                // Signal the UI to navigate to EmergencyChatScreen
                WearableManager.triggerEmergencyNav = true

            } catch (e: Exception) {
                Log.e("WearSync", "Error handling impact: ${e.message}")
                // Still open emergency screen even if API fails
                WearableManager.triggerEmergencyNav = true
            }
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
        val tripId = WearableManager.activeWearTripId ?: return
        scope.launch {
            try {
                val api = ApiClient.getApiService(applicationContext)
                api.finishTrip(tripId)
                WearableManager.activeWearTripId = null
                Log.i("WearSync", "Trip $tripId finished from watch.")
            } catch (e: Exception) {
                Log.e("WearSync", "Error finishing trip: ${e.message}")
            }
        }
    }
}
