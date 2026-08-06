package com.example.impactx.data.sync

import android.content.Context
import android.os.Build
import android.util.Log
import com.example.impactx.data.local.AppDatabase
import com.example.impactx.data.remote.ApiClient
import com.example.impactx.data.remote.UpdateFcmTokenRequest
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object FcmTokenRegistrar {
    private const val TAG = "ImpactFcm"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun ensureRegistered(context: Context) {
        val appContext = context.applicationContext
        val pending = SyncPreferences.pendingFcmToken(appContext)
        if (!pending.isNullOrBlank()) {
            registerToken(appContext, pending)
            return
        }

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful || task.result.isNullOrBlank()) {
                Log.w(TAG, "FCM_TOKEN_UNAVAILABLE msg=${task.exception?.message}")
                return@addOnCompleteListener
            }
            val token = task.result
            SyncPreferences.savePendingFcmToken(appContext, token)
            registerToken(appContext, token)
        }
    }

    fun registerToken(context: Context, token: String) {
        val appContext = context.applicationContext
        SyncPreferences.savePendingFcmToken(appContext, token)
        scope.launch {
            val db = AppDatabase.getDatabase(appContext)
            if (db.sessionDao().session == null) {
                Log.i(TAG, "FCM_TOKEN_QUEUED_NO_SESSION")
                return@launch
            }

            try {
                val response = ApiClient.getApiService(appContext).updateFcmToken(
                    UpdateFcmTokenRequest(
                        deviceId = SyncPreferences.mobileInstallationId(appContext),
                        token = token,
                        name = "${Build.MANUFACTURER} ${Build.MODEL}".trim(),
                    ),
                )
                if (response.isSuccessful) {
                    SyncPreferences.clearPendingFcmToken(appContext)
                    Log.i(TAG, "FCM_TOKEN_REGISTERED")
                } else {
                    Log.e(TAG, "FCM_TOKEN_REGISTER_FAILED HTTP=${response.code()}")
                }
            } catch (exception: Exception) {
                Log.e(TAG, "FCM_TOKEN_REGISTER_EXCEPTION msg=${exception.message}")
            }
        }
    }
}
