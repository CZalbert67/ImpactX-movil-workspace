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
import com.example.impactx.data.local.WearableLinkageEntity
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

    private fun pendingEvent(eventId: String, eventType: String, sourceNodeId: String? = null): WearSyncEventEntity =
        WearSyncEventEntity().apply {
            this.eventId = eventId
            this.sourceNodeId = sourceNodeId
            this.eventType = eventType
            this.status = "PENDING"
            this.createdAt = nowUtcString()
            this.updatedAt = nowUtcString()
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

    // Helper to resolve wearable device ID via Room database or backend GET api/v1/wearable
    private suspend fun resolveDispositivoId(sourceNodeId: String, db: AppDatabase, api: com.example.impactx.data.remote.ApiService): String? {
        val linkage = withContext(Dispatchers.IO) {
            db.wearableLinkageDao().getLinkageByNodeId(sourceNodeId)
        }
        if (linkage != null && linkage.backendDeviceId != null) {
            return linkage.backendDeviceId
        }

        try {
            val getResp = api.getWearable()
            if (getResp.isSuccessful && getResp.body() != null) {
                val body = getResp.body()!!
                val devId = body.dispositivoId
                withContext(Dispatchers.IO) {
                    db.wearableLinkageDao().insertLinkage(
                        WearableLinkageEntity(
                            sourceNodeId,
                            devId,
                            body.nombre,
                            body.modelo,
                            body.fabricante,
                            body.estado,
                            System.currentTimeMillis()
                        )
                    )
                }
                return devId
            }
        } catch (e: Exception) {
            Log.e("WearSync", "Error resolving dispositivoId from backend: ${e.message}")
        }
        return null
    }

    // ─── Trip Start ──────────────────────────────────────────────────────────
    private fun handleStartTrip(rawData: String, sourceNodeId: String) {
        val json = runCatching { JSONObject(rawData) }.getOrNull() ?: JSONObject()
        val eventId = json.optString("eventId", "").takeIf { it.isNotBlank() }
            ?: java.util.UUID.randomUUID().toString()

        scope.launch {
            val db = AppDatabase.getDatabase(applicationContext)
            val dao = db.wearSyncEventDao()

            val existing = withContext(Dispatchers.IO) { dao.findByEventId(eventId) }
            if (existing != null && existing.status == "SUCCEEDED") {
                Log.i("WearSync", "[START_TRIP] eventId=$eventId ya procesado (SUCCEEDED). Devolviendo tripId cacheado.")
                val confirm = JSONObject().apply {
                    put("eventId", eventId)
                    put("success", true)
                    put("tripId", existing.backendTripId ?: "")
                    put("status", "Activo")
                }
                sendConfirmationToNode(sourceNodeId, "/trip-confirmed", confirm)
                return@launch
            }

            if (existing != null && existing.status == "PENDING") {
                Log.w("WearSync", "[START_TRIP] eventId=$eventId ya está PENDING. Ignorando duplicado.")
                return@launch
            }

            withContext(Dispatchers.IO) {
                val event = WearSyncEventEntity().apply {
                    this.eventId = eventId
                    this.sourceNodeId = sourceNodeId
                    this.eventType = "START_TRIP"
                    this.status = "PENDING"
                    this.createdAt = nowUtcString()
                    this.updatedAt = nowUtcString()
                    this.httpCode = 0
                }
                dao.insertEventIfAbsent(event)
            }

            try {
                val api = ApiClient.getApiService(applicationContext)

                // Resolve dispositivoId
                val dispositivoId = resolveDispositivoId(sourceNodeId, db, api)
                if (dispositivoId == null) {
                    val errorMsg = "El Galaxy Watch8 no está vinculado correctamente. Vuelve a vincularlo desde la aplicación móvil."
                    Log.e("WearSync", "START_TRIP_FAILED eventId=$eventId HTTP=403 errorCode=WEARABLE_NOT_LINKED detail=$errorMsg")
                    withContext(Dispatchers.IO) {
                        dao.updateFailure(eventId, "FAILED", 403, errorMsg, nowUtcString())
                    }
                    sendErrorConfirmation(sourceNodeId, eventId, 403, errorMsg, "WEARABLE_NOT_LINKED")
                    return@launch
                }

                val safeDeviceId = if (dispositivoId.length > 10) dispositivoId.take(10) + "..." else dispositivoId
                Log.i("WearSync", "START_TRIP eventId=$eventId sourceNodeId=$sourceNodeId backendDeviceId=$safeDeviceId")

                val location = LocationHelper.getLastKnownLocation(applicationContext)
                val lat = location?.latitude
                val lng = location?.longitude
                val rutaOrigen = if (lat != null && lng != null) LocationHelper.formatLocation(lat, lng) else null

                val response = api.startTrip(
                    StartTripRequest(
                        dispositivoId = dispositivoId,
                        proposito = "Viaje iniciado manualmente desde Galaxy Watch8",
                        rutaOrigen = rutaOrigen
                    )
                )

                val httpCode = response.code()

                when {
                    response.isSuccessful -> {
                        val trip = response.body()
                        val tripId = trip?.id?.takeIf { it.isNotBlank() }

                        if (tripId != null) {
                            WearableManager.activeWearTripId = tripId
                            val prefs = applicationContext.getSharedPreferences("impactx_prefs", Context.MODE_PRIVATE)
                            prefs.edit().putString("active_trip_id", tripId).apply()

                            withContext(Dispatchers.IO) {
                                dao.updateStatus(eventId, "SUCCEEDED", httpCode, tripId, nowUtcString())
                            }

                            Log.i("WearSync", "POST /api/v1/trips/start eventId=$eventId HTTP=$httpCode tripId=$tripId")

                            val confirm = JSONObject().apply {
                                put("eventId", eventId)
                                put("success", true)
                                put("tripId", tripId)
                                put("status", trip.estado ?: "Activo")
                            }
                            sendConfirmationToNode(sourceNodeId, "/trip-confirmed", confirm)
                        } else {
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
                                Log.i("WearSync", "POST /api/v1/trips/start eventId=$eventId HTTP=$httpCode tripId=$activeTripId")

                                val confirm = JSONObject().apply {
                                    put("eventId", eventId)
                                    put("success", true)
                                    put("tripId", activeTripId)
                                    put("status", activeTrip.estado ?: "Activo")
                                }
                                sendConfirmationToNode(sourceNodeId, "/trip-confirmed", confirm)
                            } else {
                                withContext(Dispatchers.IO) {
                                    dao.updateFailure(eventId, "FAILED", httpCode, "Respuesta vacía del backend", nowUtcString())
                                }
                                sendErrorConfirmation(sourceNodeId, eventId, httpCode, "Respuesta vacía del backend.")
                            }
                        }
                    }
                    httpCode == 409 -> {
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
                            Log.i("WearSync", "POST /api/v1/trips/start eventId=$eventId HTTP=409 tripId=$activeTripId")

                            val confirm = JSONObject().apply {
                                put("eventId", eventId)
                                put("success", true)
                                put("tripId", activeTripId)
                                put("status", activeTrip.estado ?: "Activo")
                            }
                            sendConfirmationToNode(sourceNodeId, "/trip-confirmed", confirm)
                        } else {
                            withContext(Dispatchers.IO) {
                                dao.updateFailure(eventId, "FAILED", 409, "Conflicto de viaje sin viaje activo.", nowUtcString())
                            }
                            sendErrorConfirmation(sourceNodeId, eventId, 409, "Ya hay un viaje activo en el servidor.", "TRIP_CONFLICT")
                        }
                    }
                    httpCode == 401 -> {
                        val detail = "Sesión vencida. Inicia sesión en el celular."
                        Log.e("WearSync", "START_TRIP_FAILED eventId=$eventId HTTP=401 errorCode=UNAUTHORIZED detail=$detail")
                        withContext(Dispatchers.IO) {
                            dao.updateFailure(eventId, "FAILED", 401, detail, nowUtcString())
                        }
                        sendErrorConfirmation(sourceNodeId, eventId, 401, detail, "UNAUTHORIZED")
                    }
                    httpCode == 403 -> {
                        val errorBody = response.errorBody()?.string()
                        val detail = runCatching { JSONObject(errorBody).optString("detail") }.getOrNull() ?: "Acceso denegado."
                        Log.e("WearSync", "START_TRIP_FAILED eventId=$eventId HTTP=403 errorCode=FORBIDDEN detail=$detail")
                        withContext(Dispatchers.IO) {
                            dao.updateFailure(eventId, "FAILED", 403, detail, nowUtcString())
                        }
                        sendErrorConfirmation(sourceNodeId, eventId, 403, detail, "FORBIDDEN")
                    }
                    httpCode == 404 -> {
                        val detail = "Dispositivo o viaje no encontrado."
                        Log.e("WearSync", "START_TRIP_FAILED eventId=$eventId HTTP=404 errorCode=NOT_FOUND detail=$detail")
                        withContext(Dispatchers.IO) {
                            dao.updateFailure(eventId, "FAILED", 404, detail, nowUtcString())
                        }
                        sendErrorConfirmation(sourceNodeId, eventId, 404, detail, "NOT_FOUND")
                    }
                    else -> {
                        val errorBody = response.errorBody()?.string()
                        val detail = runCatching { JSONObject(errorBody).optString("detail") }.getOrNull() ?: "Error del servidor HTTP $httpCode"
                        Log.e("WearSync", "START_TRIP_FAILED eventId=$eventId HTTP=$httpCode errorCode=SERVER_ERROR detail=$detail")
                        withContext(Dispatchers.IO) {
                            dao.updateFailure(eventId, "FAILED", httpCode, detail, nowUtcString())
                        }
                        sendErrorConfirmation(sourceNodeId, eventId, httpCode, detail, "SERVER_ERROR")
                    }
                }
            } catch (e: Exception) {
                val detail = e.message ?: "Error de red"
                Log.e("WearSync", "START_TRIP_FAILED eventId=$eventId HTTP=0 errorCode=NETWORK_ERROR detail=$detail")
                withContext(Dispatchers.IO) {
                    dao.updateFailure(eventId, "FAILED", 0, detail, nowUtcString())
                }
                sendErrorConfirmation(sourceNodeId, eventId, 0, "Error de red. Reintenta desde el reloj.", "NETWORK_ERROR")
            }
        }
    }

    private suspend fun sendErrorConfirmation(
        targetNodeId: String,
        eventId: String,
        httpCode: Int,
        message: String,
        errorCode: String = "ERROR"
    ) {
        val error = JSONObject().apply {
            put("eventId", eventId)
            put("success", false)
            put("httpCode", httpCode)
            put("errorCode", errorCode)
            put("message", message)
        }
        sendConfirmationToNode(targetNodeId, "/trip-confirmed", error)
        sendConfirmationToNode(targetNodeId, "/trip-failed", error)
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
