package com.example.impactx.data.sync

import android.content.Context
import android.util.Log
import com.example.impactx.data.local.AppDatabase
import com.example.impactx.data.local.PendingSosEntity
import com.example.impactx.data.local.TelemetryQueueEntity
import com.example.impactx.data.remote.ApiClient
import com.example.impactx.data.remote.SosRequest
import com.example.impactx.data.remote.TelemetryBatchRequestV2
import com.example.impactx.data.remote.TelemetryEventRequestV2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

/**
 * Drains durable synchronization queues. A run always attempts pending SOS
 * before telemetry, while independent locks keep telemetry uploads from
 * blocking a newly detected critical alert.
 */
object ImpactSyncProcessor {
    data class Outcome(
        val retryNeeded: Boolean,
        val pendingSos: Int,
        val pendingTelemetry: Int,
    )

    // Separate locks prevent a telemetry upload from delaying a newly detected
    // critical SOS. SOS calls remain serialized with each other, and telemetry
    // batches remain serialized with each other.
    private val sosMutex = Mutex()
    private val telemetryMutex = Mutex()

    suspend fun run(
        context: Context,
        forceTelemetry: Boolean,
        includeTelemetry: Boolean = true,
    ): Outcome = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val db = AppDatabase.getDatabase(appContext)
        if (db.sessionDao().session == null) {
            Log.i(TAG, "SYNC_SKIPPED_NO_SESSION")
            return@withContext Outcome(
                retryNeeded = false,
                pendingSos = db.pendingSosDao().countPending(),
                pendingTelemetry = db.telemetryQueueDao().countPending(),
            )
        }

        var retryNeeded = sosMutex.withLock {
            flushPendingSos(appContext, db)
        }
        if (includeTelemetry) {
            if (!SyncPreferences.isTelemetryHoldEnabled(appContext) || forceTelemetry) {
                retryNeeded = telemetryMutex.withLock {
                    flushTelemetry(appContext, db, forceTelemetry)
                } || retryNeeded
            } else {
                Log.i(TAG, "TELEMETRY_HELD_FOR_DEMO")
            }
        }

        Outcome(
            retryNeeded = retryNeeded,
            pendingSos = db.pendingSosDao().countPending(),
            pendingTelemetry = db.telemetryQueueDao().countPending(),
        )
    }

    private suspend fun flushPendingSos(context: Context, db: AppDatabase): Boolean {
        val dao = db.pendingSosDao()
        val api = ApiClient.getApiService(context)
        var retryNeeded = false

        for (event in dao.getPending(MAX_SOS_PER_RUN)) {
            dao.markSending(event.eventId)
            try {
                val response = api.sendSos(event.toRequest())
                if (response.isSuccessful && response.body() != null) {
                    val alert = response.body()!!
                    dao.markSent(event.eventId, alert.id, System.currentTimeMillis())
                    if (event.localAccidentId > 0) {
                        db.accidentDao().markAsSent(event.localAccidentId)
                    }
                    db.wearSyncEventDao().updateStatus(
                        event.eventId,
                        "SUCCEEDED",
                        response.code(),
                        alert.id,
                        utcNow(),
                    )
                    SyncNotificationHelper.notifySosSent(context, alert.id)
                    Log.i(
                        TAG,
                        "SOS_SYNCED eventId=${event.eventId.take(8)} alertId=${alert.id.take(8)} " +
                            "recipients=${alert.contactosNotificados.orEmpty().size}",
                    )
                } else {
                    val error = response.safeError()
                    dao.markFailed(event.eventId, error)
                    db.wearSyncEventDao().updateFailure(
                        event.eventId,
                        "FAILED",
                        response.code(),
                        error,
                        utcNow(),
                    )
                    retryNeeded = retryNeeded || response.code().isRetryableHttp()
                    Log.e(TAG, "SOS_SYNC_FAILED HTTP=${response.code()} eventId=${event.eventId.take(8)}")
                }
            } catch (exception: Exception) {
                val message = exception.message?.take(300) ?: "Error de red"
                dao.markFailed(event.eventId, message)
                db.wearSyncEventDao().updateFailure(event.eventId, "FAILED", 0, message, utcNow())
                retryNeeded = true
                Log.e(TAG, "SOS_SYNC_EXCEPTION eventId=${event.eventId.take(8)} msg=$message")
            }
        }
        return retryNeeded
    }

    private suspend fun flushTelemetry(
        context: Context,
        db: AppDatabase,
        force: Boolean,
    ): Boolean {
        val dao = db.telemetryQueueDao()
        val pendingCount = dao.countPending()
        val oldest = dao.oldestPendingCreatedAtMs()
        if (!BatchSyncPolicy.shouldFlush(pendingCount, oldest, System.currentTimeMillis(), force)) {
            return false
        }

        val api = ApiClient.getApiService(context)
        var retryNeeded = false
        var processedBatches = 0

        while (processedBatches < MAX_BATCHES_PER_RUN) {
            val tripId = dao.oldestPendingTripId() ?: break
            val events = dao.getPendingForTrip(tripId, BatchSyncPolicy.MAX_BATCH_SIZE)
            if (events.isEmpty()) break

            val retainedBatchId = events.mapNotNull { it.batchId?.takeIf(String::isNotBlank) }
                .distinct()
                .singleOrNull()
            val batchId = retainedBatchId ?: UUID.randomUUID().toString()
            val eventIds = events.map { it.eventId }
            val capturedOffline = events.any { it.capturedOffline }
            val wearableDeviceId = events.firstNotNullOfOrNull {
                it.wearableDeviceId?.takeIf(String::isNotBlank)
            }

            if (wearableDeviceId == null) {
                dao.markBatchFailed(eventIds, batchId, "No existe wearableDeviceId vinculado.")
                Log.e(TAG, "TELEMETRY_SYNC_MISSING_WEARABLE tripId=${tripId.take(8)}")
                break
            }

            val request = TelemetryBatchRequestV2(
                batchId = batchId,
                batchSequence = events.first().sequenceNumber ?: System.currentTimeMillis(),
                capturedOffline = capturedOffline,
                wearableDeviceId = wearableDeviceId,
                wearableModel = events.firstNotNullOfOrNull {
                    it.wearableModel?.takeIf(String::isNotBlank)
                } ?: "Galaxy Watch 8",
                batteryLevel = events.lastOrNull()?.batteryLevel ?: 100,
                eventos = events.map { it.toRequest() },
            )

            dao.markBatchSending(eventIds, batchId)
            try {
                val response = api.ingestTelemetry(tripId, request)
                if (response.isSuccessful && response.body() != null) {
                    val result = response.body()!!
                    dao.markBatchSent(eventIds, batchId, System.currentTimeMillis())
                    SyncPreferences.saveLastBatch(
                        context,
                        batchId,
                        result.recibidos,
                        result.insertados,
                        result.duplicados,
                        result.capturedOffline,
                        result.procesadoEnUtc,
                    )
                    Log.i(
                        TAG,
                        "TELEMETRY_BATCH_SYNCED batchId=${batchId.take(8)} received=${result.recibidos} " +
                            "inserted=${result.insertados} duplicates=${result.duplicados}",
                    )
                } else {
                    val error = response.safeError()
                    dao.markBatchFailed(eventIds, batchId, error)
                    retryNeeded = retryNeeded || response.code().isRetryableHttp()
                    Log.e(TAG, "TELEMETRY_BATCH_FAILED HTTP=${response.code()} batchId=${batchId.take(8)}")
                    break
                }
            } catch (exception: Exception) {
                val message = exception.message?.take(300) ?: "Error de red"
                dao.markBatchFailed(eventIds, batchId, message)
                retryNeeded = true
                Log.e(TAG, "TELEMETRY_BATCH_EXCEPTION batchId=${batchId.take(8)} msg=$message")
                break
            }

            processedBatches++
            if (!force && dao.countPending() < BatchSyncPolicy.MAX_BATCH_SIZE) break
        }

        return retryNeeded
    }

    private fun PendingSosEntity.toRequest() = SosRequest(
        lat = lat,
        lng = lng,
        lugar = place,
        severidad = severity ?: "severe",
        canal = channel ?: "wearable-relay-mobile",
        gForce = gForce,
        frecuenciaCardiaca = heartRate,
        modo = mode ?: "immediate",
        viajeId = tripId,
        clientEventId = eventId,
        capturedOffline = capturedOffline,
        occurredAtUtc = timestampUtc,
    )

    private fun TelemetryQueueEntity.toRequest() = TelemetryEventRequestV2(
        eventId = eventId,
        timestamp = timestampUtc?.takeIf(String::isNotBlank) ?: utcNow(),
        sequenceNumber = sequenceNumber ?: createdAtMs,
        lat = lat,
        lng = lng,
        velocidad = velocity,
        gpsAccuracyMeters = gpsAccuracyMeters ?: if (lat == 0.0 && lng == 0.0) 5_000.0 else 50.0,
        aceleracionX = accelerationX ?: 0.0,
        aceleracionY = accelerationY ?: 0.0,
        aceleracionZ = accelerationZ ?: 9.80665,
        magnitudAceleracion = accelerationMagnitude,
        giroscopioX = gyroscopeX ?: 0.0,
        giroscopioY = gyroscopeY ?: 0.0,
        giroscopioZ = gyroscopeZ ?: 0.0,
        frecuenciaCardiaca = heartRate,
        calidadSensor = if (lat == 0.0 && lng == 0.0) "low" else "high",
        sensorFlags = buildList {
            if (capturedOffline) add("captured_offline")
            if (lat == 0.0 && lng == 0.0) add("gps_degraded")
            if (heartRate == null) add("heart_rate_unavailable")
        },
    )

    private fun retrofit2.Response<*>.safeError(): String = runCatching {
        errorBody()?.string()?.take(500)
    }.getOrNull().takeUnless { it.isNullOrBlank() } ?: "HTTP ${code()}"

    private fun Int.isRetryableHttp(): Boolean = this == 408 || this == 429 || this >= 500

    private fun utcNow(): String = SimpleDateFormat(
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        Locale.US,
    ).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date())

    private const val TAG = "ImpactSync"
    private const val MAX_SOS_PER_RUN = 20
    private const val MAX_BATCHES_PER_RUN = 10
}
