package com.example.impactxwearable.data

import android.content.Context
import android.os.Build
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

/**
 * Provides a stable installation identity for this wearable device.
 * The installationId is generated once and persisted in SharedPreferences.
 * It NEVER changes across reboots, app updates, or reconnections.
 * This is the authoritative identity used to associate this device with a
 * backendDeviceId in the mobile app and backend.
 */
object WearableIdentity {

    private const val PREFS_NAME = "impactx_wear_identity"
    private const val KEY_INSTALLATION_ID = "installation_id"
    private const val TAG = "WearSync"

    /**
     * Returns the stable installationId for this wearable installation.
     * Creates and persists a new UUID on first call.
     */
    fun getOrCreateInstallationId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY_INSTALLATION_ID, null)
        if (!existing.isNullOrBlank()) {
            return existing
        }
        val newId = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_INSTALLATION_ID, newId).apply()
        Log.i(TAG, "INSTALLATION_ID_CREATED id=${newId.take(8)}...")
        return newId
    }

    /**
     * Builds the /device-info payload to send to the mobile app.
     * The mobile app uses this to resolve: sourceNodeId → installationId → backendDeviceId.
     */
    fun buildDeviceInfoPayload(context: Context, appVersion: String = "1.0"): String {
        val installationId = getOrCreateInstallationId(context)
        val timestampUtc = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())
        val osVersion = Build.VERSION.RELEASE
        val hardwareModel = Build.MODEL
        val contractModel = WearableContract.canonicalModel(hardwareModel)

        return org.json.JSONObject().apply {
            put("installationId", installationId)
            put("model", contractModel)
            put("hardwareModel", hardwareModel)
            put("deviceName", WearableContract.DEVICE_NAME)
            put("manufacturer", WearableContract.MANUFACTURER)
            put("platform", WearableContract.PLATFORM)
            put("appVersion", appVersion)
            put("versionSistemaOperativo", osVersion)
            put("timestampUtc", timestampUtc)
        }.toString()
    }
}
