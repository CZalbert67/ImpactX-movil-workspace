package com.example.impactx.data.sync

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.impactx.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class ImpactFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        FcmTokenRegistrar.registerToken(applicationContext, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title
            ?: message.data["title"]
            ?: "Alerta crítica de ImpactX"
        val body = message.notification?.body
            ?: message.data["message"]
            ?: message.data["body"]
            ?: "Una persona que monitoreas podría necesitar ayuda."
        val alertId = message.data["alertId"] ?: message.data["referenceId"]

        ImpactNotificationChannels.ensureCreated(this)
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_ALERT_ID", alertId)
            putExtra("OPEN_MONITOR_NOTIFICATION", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            alertId?.hashCode() ?: 2206,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, ImpactNotificationChannels.MONITOR_ALERTS_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(alertId?.hashCode() ?: System.currentTimeMillis().toInt(), notification)
    }


}
