package com.example.receiver

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity

object NotificationHelper {
    private const val CHANNEL_CALLER_ID = "channel_caller_id"
    private const val CHANNEL_SPAM_BLOCKS = "channel_spam_blocks"
    private const val CHANNEL_SERVICE = "channel_call_monitoring"
    
    private const val NOTIFICATION_ID_CALL = 1001
    private const val NOTIFICATION_ID_SMS = 1002
    const val NOTIFICATION_ID_SERVICE = 1003

    fun initNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Channel 1: Caller ID (High importance for call notifications)
            val callChannel = NotificationChannel(
                CHANNEL_CALLER_ID,
                "Caller ID Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows real-time Caller ID details for incoming calls"
                enableVibration(true)
            }
            
            // Channel 2: Spam SMS Blocked (Low/Default importance to avoid noisy spam alerts)
            val spamChannel = NotificationChannel(
                CHANNEL_SPAM_BLOCKS,
                "Spam Filter Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifies when a spam text message is intercepted or blocked"
            }

            // Channel 3: Service Monitoring
            val serviceChannel = NotificationChannel(
                CHANNEL_SERVICE,
                "Call Monitoring Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the local caller ID and screening service active"
            }
            
            manager.createNotificationChannel(callChannel)
            manager.createNotificationChannel(spamChannel)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    fun buildServiceNotification(context: Context): Notification {
        initNotificationChannels(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_SERVICE)
            .setSmallIcon(android.R.drawable.stat_sys_phone_call)
            .setContentTitle("🛡️ Elyzareth Call Protection Active")
            .setContentText("Local Caller ID and Call Screening is actively running.")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    fun showCallNotification(context: Context, number: String, title: String, isSpam: Boolean, reason: String?) {
        initNotificationChannels(context)
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val text = if (isSpam) {
            "🚨 SPAM WARNING: $reason"
        } else {
            "Identified: $title"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_CALLER_ID)
            .setSmallIcon(android.R.drawable.stat_sys_phone_call)
            .setContentTitle("📞 Elyzareth Caller ID")
            .setContentText(text)
            .setSubText(number)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID_CALL, notification)
    }

    fun showSmsNotification(context: Context, sender: String, body: String, isSpam: Boolean, reason: String?) {
        initNotificationChannels(context)
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isSpam) {
            "🛡️ Spam Message Blocked"
        } else {
            "✉️ New Message Intercepted"
        }

        val text = if (isSpam) {
            "Blocked from $sender ($reason): \"$body\""
        } else {
            "From $sender: \"$body\""
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_SPAM_BLOCKS)
            .setSmallIcon(android.R.drawable.sym_action_chat)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID_SMS, notification)
    }
}
