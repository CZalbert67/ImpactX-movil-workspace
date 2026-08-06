package com.example.impactx.data.sync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

object SyncNotificationHelper {
    private const val CHANNEL_ID = "impactx_sync_status"

    fun notifySosSent(context: Context, alertId: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Estado de alertas ImpactX",
                    NotificationManager.IMPORTANCE_HIGH,
                ),
            )
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload_done)
            .setContentTitle("SOS confirmado por ImpactX")
            .setContentText(
                "El backend confirmó la alerta ${alertId.take(8)} y procesó a los destinatarios autorizados.",
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        manager.notify(alertId.hashCode(), notification)
    }
}
