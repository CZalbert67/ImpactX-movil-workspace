package com.example.impactx.data.sync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.impactx.R

/** Shared notification channels used by foreground and background FCM delivery. */
object ImpactNotificationChannels {
    const val MONITOR_ALERTS_CHANNEL_ID = "impactx_monitor_alerts"

    fun ensureCreated(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(
                MONITOR_ALERTS_CHANNEL_ID,
                context.getString(R.string.monitor_notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.monitor_notification_channel_description)
                enableVibration(true)
                enableLights(true)
                setBypassDnd(false)
            },
        )
    }
}
