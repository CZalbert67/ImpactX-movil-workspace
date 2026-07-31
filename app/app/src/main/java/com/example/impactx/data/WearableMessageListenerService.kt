package com.example.impactx.data

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.example.impactx.ui.screens.WearableManager
import com.example.impactx.ui.screens.BLEState
import org.json.JSONObject
import android.util.Log

class WearableMessageListenerService : WearableListenerService() {
    override fun onMessageReceived(messageEvent: MessageEvent) {
        when (messageEvent.path) {
            "/telemetry" -> {
                try {
                    val json = JSONObject(String(messageEvent.data))
                    val heartRate = json.getInt("heartRate")
                    val gForce = json.getDouble("gForce").toFloat()
                    val isImpact = json.getBoolean("isImpact")
                    
                    // Sync WearableManager singleton states for Home and Sync Screens
                    WearableManager.realHeartRate = heartRate
                    WearableManager.bleState = BLEState.CONNECTED_DASHBOARD
                    WearableManager.isRealConnection = true
                    WearableManager.connectedDeviceName = "Galaxy Watch 8 (Sincronizado)"
                    
                    Log.d("WearSync", "PPG: $heartRate, G-Force: $gForce G, Impact: $isImpact")
                } catch (e: Exception) {
                    Log.e("WearSync", "Error parsing wearable telemetry: ${e.message}")
                }
            }
            "/impact-detected" -> {
                Log.w("WearSync", "CRITICAL: Impact detected on Wear OS Watch!")
            }
        }
    }
}
