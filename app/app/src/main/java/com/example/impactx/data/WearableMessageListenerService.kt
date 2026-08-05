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
import com.example.impactx.data.remote.PairConfirmRequest
import com.example.impactx.data.remote.PairWearableRequest
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

private const val TAG = "WearSync"

class WearableMessageListenerService : WearableListenerService() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // ── Secondary debounce guards (primary protection is Room idempotency) ───
    @Volatile private var lastImpactHandledMs: Long = 0
    private val IMPACT_COOLDOWN_MS = 15_000L

    override fun onMessageReceived(messageEvent: MessageEvent) {
        val path = messageEvent.path
        val rawData = String(messageEvent.data)
        val sourceNodeId = messageEvent.sourceNodeId

        Log.d(TAG, "MSG_RECEIVED path=$path sourceNodeId=$sourceNodeId")

        when (path) {
            "/telemetry"       -> handleTelemetry(rawData, sourceNodeId)
            "/device-info"     -> handleDeviceInfo(rawData, sourceNodeId)
            "/impact-detected",
            "/sos-triggered"   -> handleImpact(rawData, path, sourceNodeId)
            "/start-trip"      -> handleStartTrip(rawData, sourceNodeId)
            "/pause-trip"      -> handlePauseTrip(rawData, sourceNodeId)
            "/resume-trip"     -> handleResumeTrip(rawData, sourceNodeId)
            "/finish-trip"     -> handleFinishTrip(rawData, sourceNodeId)
            "/alarm-reset"     -> { /* watch cancelled alarm */ }
            else               -> Log.w(TAG, "PATH_UNKNOWN path=$path")
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private fun nowUtcString(): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())

    private fun pendingEvent(
        eventId: String,
        eventType: String,
        sourceNodeId: String? = null
    ): WearSyncEventEntity =
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
            Log.e(TAG, "ERROR_SEND_ACK path=$path nodeId=$targetNodeId msg=${e.message}")
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
        sendConfirmationToNode(targetNodeId, "/trip-failed", error)
    }

    // ─── /device-info handler ────────────────────────────────────────────────
    /**
     * Called when the wearable app starts or reconnects.
     * Resolves: sourceNodeId → installationId → backendDeviceId
     *
     * This is the authoritative pairing entry point. The pairing that used to
     * happen in WearableSyncScreen.connectToDevice() was wrong because it used
     * the Bluetooth MAC address as dispositivoId, which is unstable.
     */
    private fun handleDeviceInfo(rawData: String, sourceNodeId: String) {
        val json = runCatching { JSONObject(rawData) }.getOrNull() ?: return
        val installationId = json.optString("installationId", "").takeIf { it.isNotBlank() }
        val deviceName    = json.optString("deviceName", WearableContract.DEVICE_NAME)
        val reportedModel = json.optString("model", json.optString("hardwareModel", "SM-L330"))
        val hardwareModel = json.optString("hardwareModel", reportedModel)
        val model         = WearableContract.canonicalModel(reportedModel)
        val manufacturer  = WearableContract.MANUFACTURER
        val appVersion    = json.optString("appVersion", null)
        val osVersion     = json.optString("versionSistemaOperativo", null)

        if (installationId == null) {
            Log.w(TAG, "DEVICE_INFO_MISSING_INSTALLATIONID sourceNodeId=$sourceNodeId")
            return
        }

        Log.i(TAG, "DEVICE_INFO_RECEIVED sourceNodeId=$sourceNodeId " +
            "installationId=${installationId.take(8)}... hardwareModel=$hardwareModel contractModel=$model")

        scope.launch {
            val db = AppDatabase.getDatabase(applicationContext)
            val linkageDao = db.wearableLinkageDao()

            // Update lastSeenAt for this node
            withContext(Dispatchers.IO) {
                linkageDao.touchLastSeen(sourceNodeId, System.currentTimeMillis())
            }

            // 1. Check if we already have a linkage for this installationId
            val existingByInstallation = withContext(Dispatchers.IO) {
                linkageDao.getLinkageByInstallationId(installationId)
            }

            if (existingByInstallation != null &&
                !existingByInstallation.backendDeviceId.isNullOrBlank() &&
                WearableManager.backendLinked &&
                WearableManager.backendDeviceId == existingByInstallation.backendDeviceId) {
                // Device already linked. Update nodeId if it changed (e.g. after reboot).
                if (existingByInstallation.nodeId != sourceNodeId) {
                    Log.i(TAG, "NODE_ID_UPDATED installationId=${installationId.take(8)} "
                        + "oldNodeId=${existingByInstallation.nodeId} newNodeId=$sourceNodeId")
                    withContext(Dispatchers.IO) {
                        linkageDao.updateNodeIdForInstallation(
                            installationId, sourceNodeId, System.currentTimeMillis()
                        )
                    }
                }

                // Update WearableManager to reflect confirmed backend link
                val safeDeviceId = existingByInstallation.backendDeviceId!!
                updateWearableManagerLinked(sourceNodeId, safeDeviceId, deviceName)
                Log.i(TAG, "LINKAGE_CONFIRMED sourceNodeId=$sourceNodeId "
                    + "installationId=${installationId.take(8)} "
                    + "backendDeviceId=${safeDeviceId.take(8)}...")
                return@launch
            }

            // 2. Check if we have a linkage for this nodeId (maybe installationId was empty before)
            val existingByNode = withContext(Dispatchers.IO) {
                linkageDao.getLinkageByNodeId(sourceNodeId)
            }
            if (existingByNode != null &&
                !existingByNode.backendDeviceId.isNullOrBlank() &&
                WearableManager.backendLinked &&
                WearableManager.backendDeviceId == existingByNode.backendDeviceId) {
                // Update the installationId on existing record
                val updated = WearableLinkageEntity(
                    sourceNodeId,
                    installationId,
                    existingByNode.backendDeviceId,
                    existingByNode.nombre,
                    existingByNode.modelo,
                    existingByNode.fabricante,
                    existingByNode.estado,
                    existingByNode.linkedAt
                )
                withContext(Dispatchers.IO) { linkageDao.insertLinkage(updated) }
                updateWearableManagerLinked(sourceNodeId, existingByNode.backendDeviceId!!, deviceName)
                Log.i(TAG, "LINKAGE_UPDATED_INSTALLATION_ID nodeId=$sourceNodeId installationId=${installationId.take(8)}...")
                return@launch
            }

            // 3. No local linkage found — perform backend pairing
            Log.i(TAG, "PAIRING_START nodeId=$sourceNodeId installationId=${installationId.take(8)}...")
            try {
                val api = ApiClient.getApiService(applicationContext)

                // Step 1: GET /api/v1/wearable
                val getResp = api.getWearable()
                Log.i(TAG, "GET_WEARABLE HTTP=${getResp.code()}")

                when {
                    getResp.isSuccessful && getResp.body() != null -> {
                        // Backend already has a wearable for this user — save the linkage
                        val wearableDto = getResp.body()!!
                        val backendDeviceId = wearableDto.dispositivoId
                        if (backendDeviceId.isBlank() ||
                            !wearableDto.estado.equals("Vinculado", ignoreCase = true) ||
                            !WearableContract.isSupported(
                                wearableDto.fabricante,
                                wearableDto.modelo,
                                wearableDto.plataforma
                            )) {
                            val detail = "El wearable registrado no cumple el contrato Galaxy Watch8 vinculado."
                            WearableManager.backendLinked = false
                            WearableManager.backendDeviceId = null
                            WearableManager.pairingError = detail
                            Log.e(TAG, "PAIRING_ERROR_INVALID_BACKEND_WEARABLE " +
                                "estado=${wearableDto.estado} modelo=${wearableDto.modelo}")
                            sendConfirmationToNode(
                                sourceNodeId,
                                "/pairing-failed",
                                JSONObject().apply {
                                    put("success", false)
                                    put("errorCode", "INVALID_BACKEND_WEARABLE")
                                    put("message", detail)
                                }
                            )
                            return@launch
                        }
                        val linkage = WearableLinkageEntity(
                            sourceNodeId, installationId, backendDeviceId,
                            deviceName, model, manufacturer, "Vinculado",
                            System.currentTimeMillis()
                        )
                        withContext(Dispatchers.IO) { linkageDao.insertLinkage(linkage) }

                        // Verify it was saved
                        val verification = withContext(Dispatchers.IO) {
                            linkageDao.getLinkageByNodeId(sourceNodeId)
                        }
                        if (verification?.backendDeviceId == backendDeviceId) {
                            updateWearableManagerLinked(sourceNodeId, backendDeviceId, deviceName)
                            Log.i(TAG, "LINKAGE_SAVED nodeId=$sourceNodeId "
                                + "installationId=${installationId.take(8)} "
                                + "backendDeviceId=${backendDeviceId.take(8)}...")
                            // Notify wearable that pairing is confirmed
                            sendConfirmationToNode(
                                sourceNodeId, "/pairing-confirmed",
                                JSONObject().apply {
                                    put("success", true)
                                    put("backendDeviceId", backendDeviceId)
                                }
                            )
                        } else {
                            Log.e(TAG, "LINKAGE_VERIFICATION_FAILED nodeId=$sourceNodeId")
                        }
                    }
                    getResp.code() == 404 -> {
                        // No wearable registered — perform pair + confirm flow
                        val pairReq = PairWearableRequest(
                            // Use the stable installationId (not MAC!) as the external device identifier
                            dispositivoId = installationId,
                            nombre = deviceName,
                            modelo = model,
                            fabricante = manufacturer,
                            plataforma = "WearOS",
                            versionSistemaOperativo = osVersion,
                            appVersion = appVersion,
                            capacidadesSensores = listOf("HEART_RATE", "ACCELEROMETER", "GYROSCOPE")
                        )
                        Log.i(TAG, "PAIR HTTP=posting dispositivoId=${installationId.take(8)}...")
                        val pairResp = api.pairWearable(pairReq)
                        Log.i(TAG, "PAIR HTTP=${pairResp.code()}")

                        if (pairResp.isSuccessful && pairResp.body() != null) {
                            val token = pairResp.body()!!.token
                            val confirmResp = api.confirmPairWearable(PairConfirmRequest(token))
                            Log.i(TAG, "PAIR_CONFIRM HTTP=${confirmResp.code()}")

                            if (confirmResp.isSuccessful && confirmResp.body() != null) {
                                val backendDeviceId = confirmResp.body()!!.dispositivoId
                                if (backendDeviceId.isBlank()) {
                                    reportPairingFailure(
                                        sourceNodeId, confirmResp.code(), "EMPTY_DEVICE_ID",
                                        "El backend confirmó la vinculación sin devolver dispositivoId."
                                    )
                                    return@launch
                                }
                                val linkage = WearableLinkageEntity(
                                    sourceNodeId, installationId, backendDeviceId,
                                    deviceName, model, manufacturer, "Vinculado",
                                    System.currentTimeMillis()
                                )
                                withContext(Dispatchers.IO) { linkageDao.insertLinkage(linkage) }

                                // Verify save
                                val verification = withContext(Dispatchers.IO) {
                                    linkageDao.getLinkageByNodeId(sourceNodeId)
                                }
                                if (verification?.backendDeviceId == backendDeviceId) {
                                    updateWearableManagerLinked(sourceNodeId, backendDeviceId, deviceName)
                                    Log.i(TAG, "LINKAGE_SAVED nodeId=$sourceNodeId "
                                        + "installationId=${installationId.take(8)} "
                                        + "backendDeviceId=${backendDeviceId.take(8)}...")
                                    sendConfirmationToNode(
                                        sourceNodeId, "/pairing-confirmed",
                                        JSONObject().apply {
                                            put("success", true)
                                            put("backendDeviceId", backendDeviceId)
                                        }
                                    )
                                } else {
                                    Log.e(TAG, "LINKAGE_VERIFICATION_FAILED after pair+confirm")
                                }
                            } else {
                                val detail = runCatching {
                                    JSONObject(confirmResp.errorBody()?.string()).optString("detail")
                                }.getOrDefault("No se pudo confirmar la vinculación.")
                                reportPairingFailure(sourceNodeId, confirmResp.code(), "PAIR_CONFIRM_FAILED", detail)
                            }
                        } else if (pairResp.code() == 409) {
                            // Already registered — recover by calling GET again
                            Log.w(TAG, "PAIR_CONFLICT_409 attempting recovery via GET")
                            val retryGet = runCatching { api.getWearable() }.getOrNull()
                            if (retryGet?.isSuccessful == true && retryGet.body() != null) {
                                val backendDeviceId = retryGet.body()!!.dispositivoId
                                if (!backendDeviceId.isNullOrBlank()) {
                                    val linkage = WearableLinkageEntity(
                                        sourceNodeId, installationId, backendDeviceId,
                                        deviceName, model, manufacturer, "Vinculado",
                                        System.currentTimeMillis()
                                    )
                                    withContext(Dispatchers.IO) { linkageDao.insertLinkage(linkage) }
                                    updateWearableManagerLinked(sourceNodeId, backendDeviceId, deviceName)
                                    Log.i(TAG, "LINKAGE_SAVED_409_RECOVERY nodeId=$sourceNodeId backendDeviceId=${backendDeviceId.take(8)}...")
                                }
                            }
                        } else {
                            val errBody = pairResp.errorBody()?.string()
                            val detail = runCatching { JSONObject(errBody).optString("detail") }
                                .getOrElse { "No se pudo registrar el Galaxy Watch8." }
                            reportPairingFailure(sourceNodeId, pairResp.code(), "PAIR_FAILED", detail)
                        }
                    }
                    getResp.code() == 401 -> {
                        reportPairingFailure(
                            sourceNodeId, 401, "UNAUTHORIZED",
                            "Sesión vencida. Inicia sesión en el celular."
                        )
                    }
                    else -> {
                        val detail = runCatching {
                            JSONObject(getResp.errorBody()?.string()).optString("detail")
                        }.getOrElse { "HTTP ${getResp.code()}" }
                        reportPairingFailure(sourceNodeId, getResp.code(), "GET_WEARABLE_FAILED", detail)
                    }
                }
            } catch (e: Exception) {
                reportPairingFailure(
                    sourceNodeId, 0, "PAIRING_EXCEPTION",
                    e.message ?: "Error de red durante la vinculación."
                )
            }
        }
    }

    private suspend fun reportPairingFailure(
        sourceNodeId: String,
        httpCode: Int,
        errorCode: String,
        message: String
    ) {
        WearableManager.backendLinked = false
        WearableManager.backendDeviceId = null
        WearableManager.pairingError = message
        Log.e(TAG, "PAIRING_FAILED HTTP=$httpCode errorCode=$errorCode detail=$message")
        sendConfirmationToNode(
            sourceNodeId,
            "/pairing-failed",
            JSONObject().apply {
                put("success", false)
                put("httpCode", httpCode)
                put("errorCode", errorCode)
                put("message", message)
            }
        )
    }

    private fun updateWearableManagerLinked(sourceNodeId: String, backendDeviceId: String, deviceName: String) {
        WearableManager.backendLinked = true
        WearableManager.backendDeviceId = backendDeviceId
        WearableManager.pairingError = null
        WearableManager.lastSeenAtMs = System.currentTimeMillis()
        WearableManager.connectedDeviceName = deviceName
        WearableManager.bleState = BLEState.CONNECTED_DASHBOARD
        WearableManager.isRealConnection = true
    }

    // ─── Telemetry ───────────────────────────────────────────────────────────
    private fun handleTelemetry(rawData: String, sourceNodeId: String) {
        try {
            val json = JSONObject(rawData)
            val heartRate    = json.optInt("heartRate", 0)
            val gForce       = json.optDouble("gForce", 1.0).toFloat()
            val batteryLevel = json.optInt("batteryLevel", 100)

            WearableManager.realHeartRate    = heartRate
            WearableManager.realBatteryLevel = batteryLevel
            WearableManager.lastSeenAtMs     = System.currentTimeMillis()
            WearableManager.telemetryFresh   = true

            // Update lastSeen in Room without touching backendLinked
            scope.launch {
                try {
                    AppDatabase.getDatabase(applicationContext)
                        .wearableLinkageDao()
                        .touchLastSeen(sourceNodeId, System.currentTimeMillis())
                } catch (_: Exception) {}
            }

            // Only set CONNECTED_DASHBOARD if we have a confirmed backend link
            if (WearableManager.backendLinked) {
                WearableManager.bleState = BLEState.CONNECTED_DASHBOARD
                WearableManager.isRealConnection = true
                WearableManager.connectedDeviceName = WearableManager.connectedDeviceName ?: "Galaxy Watch8"
            }

            Log.d(TAG, "TELEMETRY HR=$heartRate G=$gForce Batt=$batteryLevel% nodeId=$sourceNodeId")
        } catch (e: Exception) {
            Log.e(TAG, "TELEMETRY_PARSE_ERROR msg=${e.message}")
        }
    }

    // ─── /device-info resolution helper for trip commands ───────────────────
    /**
     * Resolves backendDeviceId for incoming trip commands.
     * The current authenticated user's backend wearable is authoritative.
     * Room is refreshed from that response so stale linkages from a previous
     * account cannot be relayed. NEVER invents or hardcodes an ID.
     */
    private suspend fun resolveDispositivoId(
        sourceNodeId: String,
        installationId: String?,
        db: AppDatabase,
        api: com.example.impactx.data.remote.ApiService
    ): String? {
        // /device-info and /start-trip travel independently through Data Layer.
        // Retry briefly so an in-flight pair+confirm can finish before rejecting
        // the trip. Every attempt still verifies the current authenticated user.
        repeat(6) { attempt ->
            try {
                val getResp = api.getWearable()
                Log.i(TAG, "GET_WEARABLE_FOR_TRIP attempt=${attempt + 1} HTTP=${getResp.code()}")

                if (getResp.isSuccessful && getResp.body() != null) {
                    val body = getResp.body()!!
                    if (body.dispositivoId.isBlank() ||
                        !body.estado.equals("Vinculado", ignoreCase = true) ||
                        !WearableContract.isSupported(body.fabricante, body.modelo, body.plataforma)) {
                        Log.e(TAG, "RESOLVE_INVALID_BACKEND_WEARABLE " +
                            "estado=${body.estado} modelo=${body.modelo}")
                        return null
                    }

                    val linkage = WearableLinkageEntity(
                        sourceNodeId,
                        installationId ?: "",
                        body.dispositivoId,
                        body.nombre,
                        WearableContract.MODEL,
                        WearableContract.MANUFACTURER,
                        "Vinculado",
                        System.currentTimeMillis()
                    )
                    withContext(Dispatchers.IO) {
                        db.wearableLinkageDao().insertLinkage(linkage)
                    }
                    updateWearableManagerLinked(
                        sourceNodeId,
                        body.dispositivoId,
                        body.nombre.ifBlank { WearableContract.DEVICE_NAME }
                    )
                    Log.i(TAG, "TRIP_LINKAGE_READY nodeId=$sourceNodeId " +
                        "installationId=${installationId?.take(8)} backendDeviceId=${body.dispositivoId.take(8)}")
                    return body.dispositivoId
                }

                if (getResp.code() != 404) {
                    val detail = runCatching {
                        JSONObject(getResp.errorBody()?.string()).optString("detail")
                    }.getOrDefault("HTTP ${getResp.code()}")
                    Log.e(TAG, "RESOLVE_BACKEND_FAILED HTTP=${getResp.code()} detail=$detail")
                    return null
                }

                if (attempt < 5) {
                    Log.i(TAG, "TRIP_LINKAGE_WAITING_FOR_PAIR attempt=${attempt + 1}")
                    kotlinx.coroutines.delay(750)
                }
            } catch (e: Exception) {
                Log.e(TAG, "RESOLVE_BACKEND_ERROR msg=${e.message}")
                return null
            }
        }

        Log.w(TAG, "RESOLVE_NO_BACKEND_WEARABLE nodeId=$sourceNodeId")
        return null
    }

    // ─── Impact / SOS ────────────────────────────────────────────────────────
    private fun handleImpact(rawData: String, path: String, sourceNodeId: String) {
        val now = System.currentTimeMillis()
        if (now - lastImpactHandledMs < IMPACT_COOLDOWN_MS) {
            Log.w(TAG, "IMPACT_DEBOUNCED cooldown active")
            return
        }
        lastImpactHandledMs = now

        val json = runCatching { JSONObject(rawData) }.getOrNull()
        val eventId = json?.optString("eventId", "")?.takeIf { it.isNotBlank() }
            ?: java.util.UUID.randomUUID().toString()
        val action = if (path == "/sos-triggered") "SOS" else "IMPACT_DETECTED"

        triggerEmergencyAutoLaunch(applicationContext)

        scope.launch {
            val db = AppDatabase.getDatabase(applicationContext)
            val dao = db.wearSyncEventDao()

            val existing = withContext(Dispatchers.IO) { dao.findByEventId(eventId) }
            if (existing != null && existing.status == "SUCCEEDED") {
                Log.w(TAG, "[$action] eventId=${eventId.take(8)} already SUCCEEDED, ignoring")
                return@launch
            }

            withContext(Dispatchers.IO) { dao.insertEventIfAbsent(pendingEvent(eventId, action)) }

            try {
                val heartRate  = if (WearableManager.realHeartRate > 0) WearableManager.realHeartRate else 75
                val gForce     = 25.0
                val location   = LocationHelper.getLastKnownLocation(applicationContext)
                val lat        = location?.latitude ?: 0.0
                val lng        = location?.longitude ?: 0.0
                val timestamp  = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

                Log.w(TAG, "[$action] eventId=${eventId.take(8)} G=$gForce HR=$heartRate GPS=$lat,$lng")

                withContext(Dispatchers.IO) {
                    db.accidentDao().insertAccident(
                        AccidentEntity(heartRate, gForce, timestamp, lat, lng, false)
                    )
                }

                withContext(Dispatchers.IO) {
                    dao.updateStatus(eventId, "SUCCEEDED", 200, "", nowUtcString())
                }

                // A collision/alert must not silently finish the trip. The trip lifecycle
                // remains controlled by the wearable so its state, the phone and the backend
                // cannot diverge after an impact simulation.
                val prefs  = applicationContext.getSharedPreferences("impactx_prefs", Context.MODE_PRIVATE)
                val tripId = prefs.getString("active_trip_id", null) ?: WearableManager.activeWearTripId
                if (tripId != null) {
                    Log.i(TAG, "IMPACT_RECORDED_TRIP_CONTINUES tripId=${tripId.take(8)} action=$action")
                }

                WearableManager.triggerEmergencyNav = true
            } catch (e: Exception) {
                Log.e(TAG, "IMPACT_PROCESSING_ERROR eventId=${eventId.take(8)} msg=${e.message}")
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
            Log.e(TAG, "EMERGENCY_LAUNCH_ERROR msg=${e.message}")
        }
    }

    // ─── Trip Start ──────────────────────────────────────────────────────────
    private fun handleStartTrip(rawData: String, sourceNodeId: String) {
        val json = runCatching { JSONObject(rawData) }.getOrNull() ?: JSONObject()
        val eventId = json.optString("eventId", "").takeIf { it.isNotBlank() }
            ?: java.util.UUID.randomUUID().toString()
        val installationId = json.optString("installationId", "").takeIf { it.isNotBlank() }

        scope.launch {
            val db  = AppDatabase.getDatabase(applicationContext)
            val dao = db.wearSyncEventDao()

            // Idempotency check
            val existing = withContext(Dispatchers.IO) { dao.findByEventId(eventId) }
            if (existing != null && existing.status == "SUCCEEDED") {
                Log.i(TAG, "START_TRIP_CACHED eventId=${eventId.take(8)} tripId=${existing.backendTripId?.take(8)}")
                val confirm = JSONObject().apply {
                    put("eventId", eventId)
                    put("success", true)
                    put("tripId", existing.backendTripId ?: "")
                    put("status", "Activo")
                    put("message", "Viaje iniciado correctamente.")
                }
                sendConfirmationToNode(sourceNodeId, "/trip-confirmed", confirm)
                return@launch
            }
            if (existing != null && existing.status == "PENDING") {
                Log.w(TAG, "START_TRIP_PENDING eventId=${eventId.take(8)} ignoring duplicate")
                return@launch
            }

            val inserted = withContext(Dispatchers.IO) {
                dao.insertEventIfAbsent(WearSyncEventEntity().apply {
                    this.eventId = eventId
                    this.sourceNodeId = sourceNodeId
                    this.eventType = "START_TRIP"
                    this.status = "PENDING"
                    this.createdAt = nowUtcString()
                    this.updatedAt = nowUtcString()
                    this.httpCode = 0
                })
            }
            if (inserted == -1L) {
                Log.w(TAG, "START_TRIP_RACE_DEDUPED eventId=${eventId.take(8)}")
                return@launch
            }

            try {
                val api = ApiClient.getApiService(applicationContext)

                val dispositivoId = resolveDispositivoId(sourceNodeId, installationId, db, api)
                if (dispositivoId == null) {
                    val errorMsg = "El Galaxy Watch8 no está vinculado correctamente. Vuelve a vincularlo desde la aplicación móvil."
                    Log.e(TAG, "START_TRIP_FAILED eventId=${eventId.take(8)} HTTP=403 errorCode=WEARABLE_NOT_LINKED")
                    withContext(Dispatchers.IO) {
                        dao.updateFailure(eventId, "FAILED", 403, errorMsg, nowUtcString())
                    }
                    sendErrorConfirmation(sourceNodeId, eventId, 403, errorMsg, "WEARABLE_NOT_LINKED")
                    return@launch
                }

                val safeDevId = if (dispositivoId.length > 8) dispositivoId.take(8) + "..." else dispositivoId
                Log.i(TAG, "START_TRIP eventId=${eventId.take(8)} sourceNodeId=$sourceNodeId backendDeviceId=$safeDevId")

                val location  = LocationHelper.getLastKnownLocation(applicationContext)
                val rutaOrigen = if (location != null) LocationHelper.formatLocation(location.latitude, location.longitude) else null

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
                        val trip   = response.body()
                        val tripId = trip?.id?.takeIf { it.isNotBlank() }

                        if (tripId != null) {
                            WearableManager.activeWearTripId = tripId
                            val prefs = applicationContext.getSharedPreferences("impactx_prefs", Context.MODE_PRIVATE)
                            prefs.edit().putString("active_trip_id", tripId).apply()
                            withContext(Dispatchers.IO) {
                                dao.updateStatus(eventId, "SUCCEEDED", httpCode, tripId, nowUtcString())
                            }
                            Log.i(TAG, "POST_TRIP_START eventId=${eventId.take(8)} HTTP=$httpCode tripId=${tripId.take(8)}")
                            val confirm = JSONObject().apply {
                                put("eventId", eventId)
                                put("success", true)
                                put("httpCode", httpCode)
                                put("tripId", tripId)
                                put("status", trip.estado ?: "Activo")
                                put("message", "Viaje iniciado correctamente.")
                            }
                            sendConfirmationToNode(sourceNodeId, "/trip-confirmed", confirm)
                        } else {
                            // Successful response but no tripId — recover from GET /active
                            val activeTrip = runCatching { api.getActiveTrip().body() }.getOrNull()
                            val activeTripId = activeTrip?.id?.takeIf { it.isNotBlank() }
                            if (activeTripId != null) {
                                WearableManager.activeWearTripId = activeTripId
                                val prefs = applicationContext.getSharedPreferences("impactx_prefs", Context.MODE_PRIVATE)
                                prefs.edit().putString("active_trip_id", activeTripId).apply()
                                withContext(Dispatchers.IO) {
                                    dao.updateStatus(eventId, "SUCCEEDED", httpCode, activeTripId, nowUtcString())
                                }
                                Log.i(TAG, "POST_TRIP_START_RECOVERED eventId=${eventId.take(8)} HTTP=$httpCode tripId=${activeTripId.take(8)}")
                                val confirm = JSONObject().apply {
                                    put("eventId", eventId)
                                    put("success", true)
                                    put("httpCode", httpCode)
                                    put("tripId", activeTripId)
                                    put("status", activeTrip.estado ?: "Activo")
                                    put("message", "Viaje iniciado correctamente.")
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
                        // Conflict — recover the existing active trip
                        val activeTrip   = runCatching { api.getActiveTrip().body() }.getOrNull()
                        val activeTripId = activeTrip?.id?.takeIf { it.isNotBlank() }
                        if (activeTripId != null) {
                            WearableManager.activeWearTripId = activeTripId
                            val prefs = applicationContext.getSharedPreferences("impactx_prefs", Context.MODE_PRIVATE)
                            prefs.edit().putString("active_trip_id", activeTripId).apply()
                            withContext(Dispatchers.IO) {
                                dao.updateStatus(eventId, "SUCCEEDED", 409, activeTripId, nowUtcString())
                            }
                            Log.i(TAG, "POST_TRIP_START eventId=${eventId.take(8)} HTTP=409 tripId=${activeTripId.take(8)}")
                            val confirm = JSONObject().apply {
                                put("eventId", eventId)
                                put("success", true)
                                put("httpCode", 409)
                                put("tripId", activeTripId)
                                put("status", activeTrip.estado ?: "Activo")
                                put("message", "Viaje activo recuperado.")
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
                        Log.e(TAG, "START_TRIP_FAILED eventId=${eventId.take(8)} HTTP=401")
                        withContext(Dispatchers.IO) { dao.updateFailure(eventId, "FAILED", 401, detail, nowUtcString()) }
                        sendErrorConfirmation(sourceNodeId, eventId, 401, detail, "UNAUTHORIZED")
                    }
                    httpCode == 403 -> {
                        val detail = runCatching {
                            JSONObject(response.errorBody()?.string()).optString("detail")
                        }.getOrElse { "Acceso denegado." }
                        Log.e(TAG, "START_TRIP_FAILED eventId=${eventId.take(8)} HTTP=403 detail=$detail")
                        withContext(Dispatchers.IO) { dao.updateFailure(eventId, "FAILED", 403, detail, nowUtcString()) }
                        sendErrorConfirmation(sourceNodeId, eventId, 403, detail, "FORBIDDEN")
                    }
                    httpCode == 404 -> {
                        val detail = "Dispositivo o viaje no encontrado."
                        Log.e(TAG, "START_TRIP_FAILED eventId=${eventId.take(8)} HTTP=404")
                        withContext(Dispatchers.IO) { dao.updateFailure(eventId, "FAILED", 404, detail, nowUtcString()) }
                        sendErrorConfirmation(sourceNodeId, eventId, 404, detail, "NOT_FOUND")
                    }
                    else -> {
                        val detail = runCatching {
                            JSONObject(response.errorBody()?.string()).optString("detail")
                        }.getOrElse { "Error del servidor HTTP $httpCode" }
                        Log.e(TAG, "START_TRIP_FAILED eventId=${eventId.take(8)} HTTP=$httpCode")
                        withContext(Dispatchers.IO) { dao.updateFailure(eventId, "FAILED", httpCode, detail, nowUtcString()) }
                        sendErrorConfirmation(sourceNodeId, eventId, httpCode, detail, "SERVER_ERROR")
                    }
                }
            } catch (e: Exception) {
                val detail = e.message ?: "Error de red"
                Log.e(TAG, "START_TRIP_FAILED eventId=${eventId.take(8)} HTTP=0 errorCode=NETWORK_ERROR")
                withContext(Dispatchers.IO) { dao.updateFailure(eventId, "FAILED", 0, detail, nowUtcString()) }
                sendErrorConfirmation(sourceNodeId, eventId, 0, "Error de red. Reintenta desde el reloj.", "NETWORK_ERROR")
            }
        }
    }

    // ─── Trip Pause ──────────────────────────────────────────────────────────
    private fun handlePauseTrip(rawData: String, sourceNodeId: String) {
        val json    = runCatching { JSONObject(rawData) }.getOrNull() ?: JSONObject()
        val eventId = json.optString("eventId", "").takeIf { it.isNotBlank() }
            ?: java.util.UUID.randomUUID().toString()
        val tripIdFromMsg = json.optString("tripId", "").takeIf { it.isNotBlank() }

        scope.launch {
            val tripId = tripIdFromMsg
                ?: applicationContext.getSharedPreferences("impactx_prefs", Context.MODE_PRIVATE)
                    .getString("active_trip_id", null)
                ?: WearableManager.activeWearTripId

            if (tripId == null) {
                Log.w(TAG, "PAUSE_TRIP_NO_TRIP_ID eventId=${eventId.take(8)}")
                sendErrorConfirmation(sourceNodeId, eventId, 0, "No hay viaje activo para pausar.", "NO_ACTIVE_TRIP")
                return@launch
            }

            val db  = AppDatabase.getDatabase(applicationContext)
            val dao = db.wearSyncEventDao()

            val existing = withContext(Dispatchers.IO) { dao.findByEventId(eventId) }
            if (existing?.status == "SUCCEEDED") {
                Log.i(TAG, "PAUSE_TRIP_CACHED eventId=${eventId.take(8)}")
                val confirm = JSONObject().apply {
                    put("eventId", eventId); put("success", true)
                    put("tripId", tripId); put("status", "Pausado")
                    put("message", "Viaje pausado.")
                }
                sendConfirmationToNode(sourceNodeId, "/trip-confirmed", confirm)
                return@launch
            }
            if (existing?.status == "PENDING") {
                Log.w(TAG, "PAUSE_TRIP_PENDING eventId=${eventId.take(8)}")
                return@launch
            }

            val inserted = withContext(Dispatchers.IO) {
                dao.insertEventIfAbsent(pendingEvent(eventId, "PAUSE_TRIP", sourceNodeId))
            }
            if (inserted == -1L) {
                Log.w(TAG, "PAUSE_TRIP_RACE_DEDUPED eventId=${eventId.take(8)}")
                return@launch
            }

            try {
                val api      = ApiClient.getApiService(applicationContext)
                val response = api.pauseTrip(tripId)
                val httpCode = response.code()

                if (response.isSuccessful) {
                    withContext(Dispatchers.IO) {
                        dao.updateStatus(eventId, "SUCCEEDED", httpCode, tripId, nowUtcString())
                    }
                    Log.i(TAG, "PAUSE_TRIP eventId=${eventId.take(8)} HTTP=$httpCode tripId=${tripId.take(8)}")
                    val confirm = JSONObject().apply {
                        put("eventId", eventId); put("success", true)
                        put("httpCode", httpCode); put("tripId", tripId)
                        put("status", "Pausado"); put("message", "Viaje pausado.")
                    }
                    sendConfirmationToNode(sourceNodeId, "/trip-confirmed", confirm)
                } else {
                    val detail = runCatching {
                        JSONObject(response.errorBody()?.string()).optString("detail")
                    }.getOrElse { "HTTP $httpCode" }
                    Log.e(TAG, "PAUSE_TRIP_FAILED eventId=${eventId.take(8)} HTTP=$httpCode detail=$detail")
                    withContext(Dispatchers.IO) { dao.updateFailure(eventId, "FAILED", httpCode, detail, nowUtcString()) }
                    sendErrorConfirmation(sourceNodeId, eventId, httpCode, "No se pudo pausar el viaje.", "PAUSE_FAILED")
                }
            } catch (e: Exception) {
                Log.e(TAG, "PAUSE_TRIP_EXCEPTION eventId=${eventId.take(8)} msg=${e.message}")
                withContext(Dispatchers.IO) { dao.updateFailure(eventId, "FAILED", 0, e.message ?: "Error de red", nowUtcString()) }
                sendErrorConfirmation(sourceNodeId, eventId, 0, "Error de red al pausar viaje.", "NETWORK_ERROR")
            }
        }
    }

    // ─── Trip Resume ─────────────────────────────────────────────────────────
    private fun handleResumeTrip(rawData: String, sourceNodeId: String) {
        val json    = runCatching { JSONObject(rawData) }.getOrNull() ?: JSONObject()
        val eventId = json.optString("eventId", "").takeIf { it.isNotBlank() }
            ?: java.util.UUID.randomUUID().toString()
        val tripIdFromMsg = json.optString("tripId", "").takeIf { it.isNotBlank() }

        scope.launch {
            val tripId = tripIdFromMsg
                ?: applicationContext.getSharedPreferences("impactx_prefs", Context.MODE_PRIVATE)
                    .getString("active_trip_id", null)
                ?: WearableManager.activeWearTripId

            if (tripId == null) {
                Log.w(TAG, "RESUME_TRIP_NO_TRIP_ID eventId=${eventId.take(8)}")
                sendErrorConfirmation(sourceNodeId, eventId, 0, "No hay viaje para reanudar.", "NO_ACTIVE_TRIP")
                return@launch
            }

            val db  = AppDatabase.getDatabase(applicationContext)
            val dao = db.wearSyncEventDao()

            val existing = withContext(Dispatchers.IO) { dao.findByEventId(eventId) }
            if (existing?.status == "SUCCEEDED") {
                Log.i(TAG, "RESUME_TRIP_CACHED eventId=${eventId.take(8)}")
                val confirm = JSONObject().apply {
                    put("eventId", eventId); put("success", true)
                    put("tripId", tripId); put("status", "Activo")
                    put("message", "Viaje reanudado.")
                }
                sendConfirmationToNode(sourceNodeId, "/trip-confirmed", confirm)
                return@launch
            }
            if (existing?.status == "PENDING") {
                Log.w(TAG, "RESUME_TRIP_PENDING eventId=${eventId.take(8)}")
                return@launch
            }

            val inserted = withContext(Dispatchers.IO) {
                dao.insertEventIfAbsent(pendingEvent(eventId, "RESUME_TRIP", sourceNodeId))
            }
            if (inserted == -1L) {
                Log.w(TAG, "RESUME_TRIP_RACE_DEDUPED eventId=${eventId.take(8)}")
                return@launch
            }

            try {
                val api      = ApiClient.getApiService(applicationContext)
                val response = api.resumeTrip(tripId)
                val httpCode = response.code()

                if (response.isSuccessful) {
                    withContext(Dispatchers.IO) {
                        dao.updateStatus(eventId, "SUCCEEDED", httpCode, tripId, nowUtcString())
                    }
                    Log.i(TAG, "RESUME_TRIP eventId=${eventId.take(8)} HTTP=$httpCode tripId=${tripId.take(8)}")
                    val confirm = JSONObject().apply {
                        put("eventId", eventId); put("success", true)
                        put("httpCode", httpCode); put("tripId", tripId)
                        put("status", "Activo"); put("message", "Viaje reanudado.")
                    }
                    sendConfirmationToNode(sourceNodeId, "/trip-confirmed", confirm)
                } else {
                    val detail = runCatching {
                        JSONObject(response.errorBody()?.string()).optString("detail")
                    }.getOrElse { "HTTP $httpCode" }
                    Log.e(TAG, "RESUME_TRIP_FAILED eventId=${eventId.take(8)} HTTP=$httpCode detail=$detail")
                    withContext(Dispatchers.IO) { dao.updateFailure(eventId, "FAILED", httpCode, detail, nowUtcString()) }
                    sendErrorConfirmation(sourceNodeId, eventId, httpCode, "No se pudo reanudar el viaje.", "RESUME_FAILED")
                }
            } catch (e: Exception) {
                Log.e(TAG, "RESUME_TRIP_EXCEPTION eventId=${eventId.take(8)} msg=${e.message}")
                withContext(Dispatchers.IO) { dao.updateFailure(eventId, "FAILED", 0, e.message ?: "Error de red", nowUtcString()) }
                sendErrorConfirmation(sourceNodeId, eventId, 0, "Error de red al reanudar viaje.", "NETWORK_ERROR")
            }
        }
    }

    // ─── Trip Finish ─────────────────────────────────────────────────────────
    private fun handleFinishTrip(rawData: String, sourceNodeId: String) {
        val json    = runCatching { JSONObject(rawData) }.getOrNull() ?: JSONObject()
        val eventId = json.optString("eventId", "").takeIf { it.isNotBlank() }
            ?: java.util.UUID.randomUUID().toString()
        val tripIdFromMsg = json.optString("tripId", "").takeIf { it.isNotBlank() }

        scope.launch {
            val tripId = tripIdFromMsg
                ?: applicationContext.getSharedPreferences("impactx_prefs", Context.MODE_PRIVATE)
                    .getString("active_trip_id", null)
                ?: WearableManager.activeWearTripId

            if (tripId == null) {
                Log.w(TAG, "FINISH_TRIP_NO_TRIP_ID eventId=${eventId.take(8)}")
                sendErrorConfirmation(sourceNodeId, eventId, 0, "No hay viaje activo para finalizar.", "NO_ACTIVE_TRIP")
                return@launch
            }

            val db  = AppDatabase.getDatabase(applicationContext)
            val dao = db.wearSyncEventDao()

            val existing = withContext(Dispatchers.IO) { dao.findByEventId(eventId) }
            if (existing?.status == "SUCCEEDED") {
                Log.i(TAG, "FINISH_TRIP_CACHED eventId=${eventId.take(8)}")
                val confirm = JSONObject().apply {
                    put("eventId", eventId); put("success", true)
                    put("tripId", tripId); put("status", "Finalizado")
                    put("message", "Viaje finalizado.")
                }
                sendConfirmationToNode(sourceNodeId, "/trip-confirmed", confirm)
                return@launch
            }
            if (existing?.status == "PENDING") {
                Log.w(TAG, "FINISH_TRIP_PENDING eventId=${eventId.take(8)}")
                return@launch
            }

            val inserted = withContext(Dispatchers.IO) {
                dao.insertEventIfAbsent(pendingEvent(eventId, "FINISH_TRIP", sourceNodeId))
            }
            if (inserted == -1L) {
                Log.w(TAG, "FINISH_TRIP_RACE_DEDUPED eventId=${eventId.take(8)}")
                return@launch
            }

            Log.i(TAG, "FINISH_TRIP tripId=${tripId.take(8)} eventId=${eventId.take(8)}")

            try {
                val api      = ApiClient.getApiService(applicationContext)
                val response = api.finishTrip(tripId)
                val httpCode = response.code()

                if (response.isSuccessful) {
                    val prefs = applicationContext.getSharedPreferences("impactx_prefs", Context.MODE_PRIVATE)
                    prefs.edit().remove("active_trip_id").apply()
                    WearableManager.activeWearTripId = null
                    withContext(Dispatchers.IO) {
                        dao.updateStatus(eventId, "SUCCEEDED", httpCode, tripId, nowUtcString())
                    }
                    Log.i(TAG, "FINISH_TRIP_OK eventId=${eventId.take(8)} HTTP=$httpCode tripId=${tripId.take(8)}")
                    val confirm = JSONObject().apply {
                        put("eventId", eventId); put("success", true)
                        put("httpCode", httpCode); put("tripId", tripId)
                        put("status", "Finalizado"); put("message", "Viaje finalizado correctamente.")
                    }
                    sendConfirmationToNode(sourceNodeId, "/trip-confirmed", confirm)
                } else {
                    val detail = runCatching {
                        JSONObject(response.errorBody()?.string()).optString("detail")
                    }.getOrElse { "HTTP $httpCode" }

                    // Idempotent recovery for trips that were already finalized by an older
                    // mobile build during the collision flow. Treat that backend state as a
                    // successful finish so the wearable can clear its stale local trip state.
                    val alreadyFinished = httpCode == 409 &&
                        detail.contains("finalizado", ignoreCase = true)

                    if (alreadyFinished) {
                        val prefs = applicationContext.getSharedPreferences("impactx_prefs", Context.MODE_PRIVATE)
                        prefs.edit().remove("active_trip_id").apply()
                        WearableManager.activeWearTripId = null
                        withContext(Dispatchers.IO) {
                            dao.updateStatus(eventId, "SUCCEEDED", httpCode, tripId, nowUtcString())
                        }
                        Log.i(TAG, "FINISH_TRIP_ALREADY_FINISHED eventId=${eventId.take(8)} tripId=${tripId.take(8)}")
                        val confirm = JSONObject().apply {
                            put("eventId", eventId); put("success", true)
                            put("httpCode", httpCode); put("tripId", tripId)
                            put("status", "Finalizado"); put("message", "El viaje ya estaba finalizado.")
                        }
                        sendConfirmationToNode(sourceNodeId, "/trip-confirmed", confirm)
                    } else {
                        Log.e(TAG, "FINISH_TRIP_FAILED eventId=${eventId.take(8)} HTTP=$httpCode detail=$detail")
                        withContext(Dispatchers.IO) { dao.updateFailure(eventId, "FAILED", httpCode, detail, nowUtcString()) }
                        sendErrorConfirmation(sourceNodeId, eventId, httpCode, "No se pudo finalizar el viaje en el servidor.", "FINISH_FAILED")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "FINISH_TRIP_EXCEPTION eventId=${eventId.take(8)} msg=${e.message}")
                withContext(Dispatchers.IO) { dao.updateFailure(eventId, "FAILED", 0, e.message ?: "Error de red", nowUtcString()) }
                sendErrorConfirmation(sourceNodeId, eventId, 0, "Error de red al finalizar viaje.", "NETWORK_ERROR")
            }
        }
    }
}
