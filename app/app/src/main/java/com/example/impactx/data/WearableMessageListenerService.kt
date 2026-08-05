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
import com.example.impactx.data.local.WearSyncEventEntity
import com.example.impactx.data.remote.ApiClient
import com.example.impactx.data.remote.SosRequest
import com.example.impactx.data.remote.StartTripRequest
import com.example.impactx.ui.screens.BLEState
import com.example.impactx.ui.screens.WearableManager
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WearableMessageListenerService : WearableListenerService() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // ── Secondary debounce guards (primary protection is Room idempotency) ───
    @Volatile private var lastImpactHandledMs: Long = 0
    private val IMPACT_COOLDOWN_MS = 15_000L

    override fun onMessageReceived(messageEvent: MessageEvent) {
        val path = messageEvent.path
        val rawData = String(messageEvent.data)
        val sourceNodeId = messageEvent.sourceNodeId

        Log.d("WearSync", "Mensaje recibido en path: $path | sourceNodeId: $sourceNodeId")

        when {
            path == "/telemetry"                                         -> handleTelemetry(rawData)
            path == "/impact-detected" || path == "/sos-triggered"      -> handleImpact(rawData, path, sourceNodeId)
            path == "/start-trip"                                        -> handleStartTrip(rawData, sourceNodeId)
            path == "/finish-trip"                                       -> handleFinishTrip(rawData, sourceNodeId)
            path == "/alarm-reset"                                       -> { /* watch cancelled alarm */ }
            else -> Log.w("WearSync", "Path desconocido: $path")
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private fun nowUtcString(): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())

    private fun pendingEvent(eventId: String, eventType: String): WearSyncEventEntity =
        WearSyncEventEntity().apply {
            this.eventId = eventId
            this.eventType = eventType
            this.status = "PENDING"
            this.createdAtUtc = nowUtcString()
            this.updatedAtUtc = nowUtcString()
            this.httpCode = 0
        }

    private suspend fun sendConfirmationToNode(
        targetNodeId: String,
        path: String,
        payload: JSONObject
    ) {
        try {
            Wearable.getMessageClient(applicationContext)
                .sendMessage(targetNodeId, path, payload.toString().toByteArray())
                .await()
        } catch (e: Exception) {
            Log.e("WearSync", "Error enviando confirmación a nodo $targetNodeId: ${e.message}")
        }
    }

    // ─── Telemetry ───────────────────────────────────────────────────────────
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

            Log.d("WearSync", "Telemetría actualizada: HR=$heartRate, G=$gForce, Batt=$batteryLevel%")
        } catch (e: Exception) {
            Log.e("WearSync", "Error parseando telemetría: ${e.message}")
        }
    }

    // ─── Impact / SOS ────────────────────────────────────────────────────────
    private fun handleImpact(rawData: String, path: String, sourceNodeId: String) {
        // Secondary debounce — protects against bursts if wearable sends multiple messages
        val now = System.currentTimeMillis()
        if (now - lastImpactHandledMs < IMPACT_COOLDOWN_MS) {
            Log.w("WearSync", "Impacto ignorado por debounce secundario (cooldown activo).")
            return
        }
        lastImpactHandledMs = now

        // Parse the eventId sent by the wearable. If none (legacy), generate one.
        val json = runCatching { JSONObject(rawData) }.getOrNull()
        val eventId = json?.optString("eventId", "")?.takeIf { it.isNotBlank() }
            ?: java.util.UUID.randomUUID().toString()
        val action = if (path == "/sos-triggered") "SOS" else "IMPACT_DETECTED"

        triggerEmergencyAutoLaunch(applicationContext)

        scope.launch {
            val db = AppDatabase.getDatabase(applicationContext)
            val dao = db.wearSyncEventDao()

            // Room idempotency check
            val existing = withContext(Dispatchers.IO) { dao.findByEventId(eventId) }
            if (existing != null && existing.status == "SUCCEEDED") {
                Log.w("WearSync", "[$action] eventId=$eventId ya fue procesado (SUCCEEDED). Ignorando.")
                return@launch
            }

            // Insert as PENDING (IGNORE if already exists)
            withContext(Dispatchers.IO) {
                dao.insertEventIfAbsent(pendingEvent(eventId, action))
            }

            try {
                val heartRate = if (WearableManager.realHeartRate > 0) WearableManager.realHeartRate else 75
                val gForce = 25.0
                val location = LocationHelper.getLastKnownLocation(applicationContext)
                val lat = location?.latitude ?: 0.0
                val lng = location?.longitude ?: 0.0
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

                Log.w("WearSync", "[$action] eventId=$eventId | G=$gForce | HR=$heartRate | GPS=$lat,$lng")

                // Save to SQLite accident_records
                withContext(Dispatchers.IO) {
                    db.accidentDao().insertAccident(
                        AccidentEntity(heartRate, gForce, timestamp, lat, lng, false)
                    )
                }

                // Mark event as SUCCEEDED in Room
                withContext(Dispatchers.IO) {
                    dao.updateStatus(eventId, "SUCCEEDED", 200, "", nowUtcString())
                }

                // If there's an active trip, automatically close it
                val prefs = applicationContext.getSharedPreferences("impactx_prefs", Context.MODE_PRIVATE)
                val tripId = prefs.getString("active_trip_id", null) ?: WearableManager.activeWearTripId
                if (tripId != null) {
                    try {
                        val api = ApiClient.getApiService(applicationContext)
                        val finishResp = api.finishTrip(tripId)
                        if (finishResp.isSuccessful) {
                            prefs.edit().remove("active_trip_id").apply()
                            WearableManager.activeWearTripId = null
                            Log.i("WearSync", "Viaje $tripId finalizado automáticamente por impacto/SOS.")
                        } else {
                            Log.e("WearSync", "Error al finalizar viaje $tripId. HTTP: ${finishResp.code()}")
                        }
                    } catch (ex: Exception) {
                        Log.e("WearSync", "Excepción al finalizar viaje: ${ex.message}")
                    }
                }

                WearableManager.triggerEmergencyNav = true

            } catch (e: Exception) {
                Log.e("WearSync", "Error procesando impacto eventId=$eventId: ${e.message}")
                withContext(Dispatchers.IO) {
                    dao.updateFailure(eventId, "FAILED", 0, e.message ?: "Error desconocido", nowUtcString())
                }
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
            context, 1001, launchIntent,
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
            Log.e("WearSync", "Error iniciando MainActivity: ${e.message}")
        }
    }

    // ─── Trip Start ──────────────────────────────────────────────────────────
    private fun handleStartTrip(rawData: String, sourceNodeId: String) {
        val json = runCatching { JSONObject(rawData) }.getOrNull() ?: JSONObject()
        val eventId = json.optString("eventId", "").takeIf { it.isNotBlank() }
            ?: java.util.UUID.randomUUID().toString()

        scope.launch {
            val db = AppDatabase.getDatabase(applicationContext)
            val dao = db.wearSyncEventDao()

            // Check if already SUCCEEDED — reply with cached tripId
            val existing = withContext(Dispatchers.IO) { dao.findByEventId(eventId) }
            if (existing != null && existing.status == "SUCCEEDED") {
                Log.i("WearSync", "[START_TRIP] eventId=$eventId ya procesado (SUCCEEDED). Devolviendo tripId cacheado.")
                val confirm = JSONObject().apply {
                    put("eventId", eventId)
                    put("success", true)
                    put("tripId", existing.backendEntityId ?: "")
                    put("status", "Activo")
                }
                sendConfirmationToNode(sourceNodeId, "/trip-confirmed", confirm)
                return@launch
            }

            // Check if PENDING (in-flight) — do not launch a duplicate call
            if (existing != null && existing.status == "PENDING") {
                Log.w("WearSync", "[START_TRIP] eventId=$eventId ya está PENDING. Ignorando duplicado.")
                return@launch
            }

            // Insert as PENDING (IGNORE if exists, double safety)
            withContext(Dispatchers.IO) {
                dao.insertEventIfAbsent(pendingEvent(eventId, "START_TRIP"))
            }

            Log.i("WearSync", "[START_TRIP] Mensaje recibido: START_TRIP | eventId=$eventId")

            try {
                val location = LocationHelper.getLastKnownLocation(applicationContext)
                val lat = location?.latitude
                val lng = location?.longitude
                val rutaOrigen = if (lat != null && lng != null) LocationHelper.formatLocation(lat, lng) else null

                Log.i("WearSync", "[START_TRIP] GPS origen: $rutaOrigen")

                val api = ApiClient.getApiService(applicationContext)
                Log.i("WearSync", "[START_TRIP] URL: POST /api/v1/trips/start")

                val response = api.startTrip(
                    StartTripRequest(
                        dispositivoId = android.provider.Settings.Secure.getString(
                            contentResolver,
                            android.provider.Settings.Secure.ANDROID_ID
                        ) ?: "watch-device",
                        proposito = "Viaje iniciado manualmente desde Galaxy Watch8",
                        rutaOrigen = rutaOrigen
                    )
                )

                val httpCode = response.code()
                Log.i("WearSync", "[START_TRIP] HTTP: $httpCode")

                when {
                    response.isSuccessful -> {
                        val trip = response.body()
                        val tripId = trip?.id?.takeIf { it.isNotBlank() }

                        if (tripId != null) {
                            // Confirm real success
                            WearableManager.activeWearTripId = tripId
                            val prefs = applicationContext.getSharedPreferences("impactx_prefs", Context.MODE_PRIVATE)
                            prefs.edit().putString("active_trip_id", tripId).apply()

                            withContext(Dispatchers.IO) {
                                dao.updateStatus(eventId, "SUCCEEDED", httpCode, tripId, nowUtcString())
                            }

                            Log.i("WearSync", "[START_TRIP] TripId: $tripId | Estado: ${trip.estado}")

                            val confirm = JSONObject().apply {
                                put("eventId", eventId)
                                put("success", true)
                                put("tripId", tripId)
                                put("status", trip.estado ?: "Activo")
                            }
                            sendConfirmationToNode(sourceNodeId, "/trip-confirmed", confirm)
                        } else {
                            // Backend returned 2xx but no tripId — verify via GET /active
                            Log.w("WearSync", "[START_TRIP] HTTP 2xx pero tripId ausente. Consultando GET /trips/active...")
                            val activeResp = runCatching { api.getActiveTrip() }.getOrNull()
                            val activeTrip = activeResp?.body()
                            val activeTripId = activeTrip?.id?.takeIf { it.isNotBlank() }

                            if (activeTripId != null) {
                                WearableManager.activeWearTripId = activeTripId
                                val prefs = applicationContext.getSharedPreferences("impactx_prefs", Context.MODE_PRIVATE)
                                prefs.edit().putString("active_trip_id", activeTripId).apply()

                                withContext(Dispatchers.IO) {
                                    dao.updateStatus(eventId, "SUCCEEDED", httpCode, activeTripId, nowUtcString())
                                }
                                Log.i("WearSync", "[START_TRIP] TripId (via GET /active): $activeTripId")

                                val confirm = JSONObject().apply {
                                    put("eventId", eventId)
                                    put("success", true)
                                    put("tripId", activeTripId)
                                    put("status", activeTrip.estado ?: "Activo")
                                }
                                sendConfirmationToNode(sourceNodeId, "/trip-confirmed", confirm)
                            } else {
                                // Cannot confirm — mark FAILED so it can be retried
                                withContext(Dispatchers.IO) {
                                    dao.updateFailure(eventId, "FAILED", httpCode, "Backend respondió 2xx sin tripId y GET /active no encontró viaje.", nowUtcString())
                                }
                                Log.e("WearSync", "[START_TRIP] No se pudo confirmar el viaje. eventId=$eventId marcado como FAILED.")
                                sendErrorConfirmation(sourceNodeId, eventId, httpCode, "No se pudo confirmar el viaje.")
                            }
                        }
                    }
                    httpCode == 409 -> {
                        // There's already an active trip — get it and return it
                        Log.w("WearSync", "[START_TRIP] HTTP 409 — Ya existe un viaje activo. Consultando GET /trips/active...")
                        val activeResp = runCatching { api.getActiveTrip() }.getOrNull()
                        val activeTrip = activeResp?.body()
                        val activeTripId = activeTrip?.id?.takeIf { it.isNotBlank() }

                        if (activeTripId != null) {
                            WearableManager.activeWearTripId = activeTripId
                            val prefs = applicationContext.getSharedPreferences("impactx_prefs", Context.MODE_PRIVATE)
                            prefs.edit().putString("active_trip_id", activeTripId).apply()

                            withContext(Dispatchers.IO) {
                                dao.updateStatus(eventId, "SUCCEEDED", 409, activeTripId, nowUtcString())
                            }
                            Log.i("WearSync", "[START_TRIP] Viaje activo existente recuperado. TripId: $activeTripId")

                            val confirm = JSONObject().apply {
                                put("eventId", eventId)
                                put("success", true)
                                put("tripId", activeTripId)
                                put("status", activeTrip.estado ?: "Activo")
                            }
                            sendConfirmationToNode(sourceNodeId, "/trip-confirmed", confirm)
                        } else {
                            withContext(Dispatchers.IO) {
                                dao.updateFailure(eventId, "FAILED", 409, "409 y no se encontró viaje activo.", nowUtcString())
                            }
                            sendErrorConfirmation(sourceNodeId, eventId, 409, "Ya hay un viaje activo pero no se pudo recuperar.")
                        }
                    }
                    httpCode == 401 -> {
                        withContext(Dispatchers.IO) {
                            dao.updateFailure(eventId, "FAILED", 401, "Token vencido o sesión inválida.", nowUtcString())
                        }
                        Log.e("WearSync", "[START_TRIP] HTTP 401 — sesión vencida. eventId=$eventId")
                        sendErrorConfirmation(sourceNodeId, eventId, 401, "Sesión vencida. Inicia sesión en el celular.")
                    }
                    httpCode == 400 -> {
                        withContext(Dispatchers.IO) {
                            dao.updateFailure(eventId, "FAILED", 400, "Body incorrecto o campo obligatorio ausente.", nowUtcString())
                        }
                        Log.e("WearSync", "[START_TRIP] HTTP 400 — body incorrecto. eventId=$eventId")
                        sendErrorConfirmation(sourceNodeId, eventId, 400, "No se pudo iniciar el viaje (400).")
                    }
                    else -> {
                        withContext(Dispatchers.IO) {
                            dao.updateFailure(eventId, "FAILED", httpCode, "Error del servidor HTTP $httpCode.", nowUtcString())
                        }
                        Log.e("WearSync", "[START_TRIP] HTTP $httpCode — error desconocido. eventId=$eventId")
                        sendErrorConfirmation(sourceNodeId, eventId, httpCode, "No se pudo iniciar el viaje (HTTP $httpCode).")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.IO) {
                    val dao2 = db.wearSyncEventDao()
                    dao2.updateFailure(eventId, "FAILED", 0, e.message ?: "Error de red", nowUtcString())
                }
                Log.e("WearSync", "[START_TRIP] Excepción: ${e.message} | eventId=$eventId")
                sendErrorConfirmation(sourceNodeId, eventId, 0, "Error de red. Reintenta desde el reloj.")
            }
        }
    }

    private suspend fun sendErrorConfirmation(
        targetNodeId: String,
        eventId: String,
        httpCode: Int,
        message: String
    ) {
        val error = JSONObject().apply {
            put("eventId", eventId)
            put("success", false)
            put("httpCode", httpCode)
            put("message", message)
        }
        sendConfirmationToNode(targetNodeId, "/trip-confirmed", error)
    }

    // ─── Trip Finish ─────────────────────────────────────────────────────────
    private fun handleFinishTrip(rawData: String, sourceNodeId: String) {
        val prefs = applicationContext.getSharedPreferences("impactx_prefs", Context.MODE_PRIVATE)
        val tripId = prefs.getString("active_trip_id", null) ?: WearableManager.activeWearTripId

        if (tripId == null) {
            Log.w("WearSync", "[FINISH_TRIP] No hay tripId activo para finalizar.")
            return
        }

        val json = runCatching { JSONObject(rawData) }.getOrNull() ?: JSONObject()
        val eventId = json.optString("eventId", "").takeIf { it.isNotBlank() }
            ?: java.util.UUID.randomUUID().toString()

        scope.launch {
            val db = AppDatabase.getDatabase(applicationContext)
            val dao = db.wearSyncEventDao()

            val existing = withContext(Dispatchers.IO) { dao.findByEventId(eventId) }
            if (existing != null && existing.status == "SUCCEEDED") {
                Log.i("WearSync", "[FINISH_TRIP] eventId=$eventId ya fue SUCCEEDED. Ignorando.")
                return@launch
            }

            withContext(Dispatchers.IO) {
                dao.insertEventIfAbsent(pendingEvent(eventId, "FINISH_TRIP"))
            }

            Log.i("WearSync", "[FINISH_TRIP] Finalizando tripId=$tripId | eventId=$eventId")

            try {
                val api = ApiClient.getApiService(applicationContext)
                val response = api.finishTrip(tripId)
                val httpCode = response.code()

                Log.i("WearSync", "[FINISH_TRIP] HTTP: $httpCode")

                if (response.isSuccessful) {
                    prefs.edit().remove("active_trip_id").apply()
                    WearableManager.activeWearTripId = null
                    withContext(Dispatchers.IO) {
                        dao.updateStatus(eventId, "SUCCEEDED", httpCode, tripId, nowUtcString())
                    }
                    Log.i("WearSync", "[FINISH_TRIP] Viaje $tripId finalizado correctamente.")
                    
                    val confirm = JSONObject().apply {
                        put("eventId", eventId)
                        put("success", true)
                        put("tripId", tripId)
                        put("status", "Finalizado")
                    }
                    sendConfirmationToNode(sourceNodeId, "/trip-confirmed", confirm)
                } else {
                    withContext(Dispatchers.IO) {
                        dao.updateFailure(eventId, "FAILED", httpCode, "Error finalizando viaje HTTP $httpCode", nowUtcString())
                    }
                    Log.e("WearSync", "[FINISH_TRIP] Error HTTP $httpCode finalizando viaje $tripId")
                    sendErrorConfirmation(sourceNodeId, eventId, httpCode, "No se pudo finalizar el viaje en el servidor.")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.IO) {
                    dao.updateFailure(eventId, "FAILED", 0, e.message ?: "Error de red", nowUtcString())
                }
                Log.e("WearSync", "[FINISH_TRIP] Excepción: ${e.message}")
                sendErrorConfirmation(sourceNodeId, eventId, 0, "Error de red al finalizar viaje.")
            }
        }
    }
}
