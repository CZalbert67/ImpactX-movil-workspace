package com.example.impactx.data.sync

import android.content.Context

object SyncPreferences {
    private const val FILE = "impactx_sync"
    private const val KEY_TELEMETRY_HOLD = "telemetry_hold_demo"
    private const val KEY_SEQUENCE = "telemetry_sequence"
    private const val KEY_LAST_BATCH_ID = "last_batch_id"
    private const val KEY_LAST_BATCH_COUNT = "last_batch_count"
    private const val KEY_LAST_BATCH_INSERTED = "last_batch_inserted"
    private const val KEY_LAST_BATCH_DUPLICATES = "last_batch_duplicates"
    private const val KEY_LAST_BATCH_OFFLINE = "last_batch_offline"
    private const val KEY_LAST_BATCH_AT = "last_batch_at"
    private const val KEY_LAST_SYNC_AT = "last_sync_at"
    private const val KEY_PENDING_FCM_TOKEN = "pending_fcm_token"
    private const val KEY_MOBILE_INSTALLATION_ID = "mobile_installation_id"

    data class LastBatch(
        val batchId: String?,
        val count: Int,
        val inserted: Int,
        val duplicates: Int,
        val capturedOffline: Boolean,
        val processedAt: String?,
    )

    private fun prefs(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun isTelemetryHoldEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_TELEMETRY_HOLD, false)

    fun setTelemetryHoldEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_TELEMETRY_HOLD, enabled).apply()
    }

    @Synchronized
    fun nextSequence(context: Context): Long {
        val value = prefs(context).getLong(KEY_SEQUENCE, 0L) + 1L
        prefs(context).edit().putLong(KEY_SEQUENCE, value).commit()
        return value
    }

    fun saveLastBatch(
        context: Context,
        batchId: String,
        count: Int,
        inserted: Int,
        duplicates: Int,
        capturedOffline: Boolean,
        processedAt: String,
    ) {
        prefs(context).edit()
            .putString(KEY_LAST_BATCH_ID, batchId)
            .putInt(KEY_LAST_BATCH_COUNT, count)
            .putInt(KEY_LAST_BATCH_INSERTED, inserted)
            .putInt(KEY_LAST_BATCH_DUPLICATES, duplicates)
            .putBoolean(KEY_LAST_BATCH_OFFLINE, capturedOffline)
            .putString(KEY_LAST_BATCH_AT, processedAt)
            .putLong(KEY_LAST_SYNC_AT, System.currentTimeMillis())
            .apply()
    }

    fun lastBatch(context: Context): LastBatch {
        val p = prefs(context)
        return LastBatch(
            batchId = p.getString(KEY_LAST_BATCH_ID, null),
            count = p.getInt(KEY_LAST_BATCH_COUNT, 0),
            inserted = p.getInt(KEY_LAST_BATCH_INSERTED, 0),
            duplicates = p.getInt(KEY_LAST_BATCH_DUPLICATES, 0),
            capturedOffline = p.getBoolean(KEY_LAST_BATCH_OFFLINE, false),
            processedAt = p.getString(KEY_LAST_BATCH_AT, null),
        )
    }

    fun lastSyncAt(context: Context): Long = prefs(context).getLong(KEY_LAST_SYNC_AT, 0L)

    fun savePendingFcmToken(context: Context, token: String) {
        prefs(context).edit().putString(KEY_PENDING_FCM_TOKEN, token).apply()
    }

    fun pendingFcmToken(context: Context): String? =
        prefs(context).getString(KEY_PENDING_FCM_TOKEN, null)

    fun clearPendingFcmToken(context: Context) {
        prefs(context).edit().remove(KEY_PENDING_FCM_TOKEN).apply()
    }

    fun mobileInstallationId(context: Context): String {
        val current = prefs(context).getString(KEY_MOBILE_INSTALLATION_ID, null)
        if (!current.isNullOrBlank()) return current
        val generated = java.util.UUID.randomUUID().toString()
        prefs(context).edit().putString(KEY_MOBILE_INSTALLATION_ID, generated).commit()
        return generated
    }
}
