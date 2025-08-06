package com.nekotts.app.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.nekotts.app.MainActivity
import com.nekotts.app.R

/**
 * Helper class for creating and managing notifications, especially for foreground services
 */
object NotificationHelper {
    
    private const val CHANNEL_ID = "neko_tts_service"
    private const val CHANNEL_NAME = "Neko TTS Service"
    private const val CHANNEL_DESCRIPTION = "Notifications for Neko TTS background service"
    
    const val NOTIFICATION_ID = 1001
    
    /**
     * Create notification channel for Android O and above
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
                setShowBadge(false)
                // Don't make sound for TTS service
                setSound(null, null)
                enableVibration(false)
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Create a foreground service notification
     */
    fun createForegroundServiceNotification(context: Context, isActive: Boolean = false): Notification {
        createNotificationChannel(context)
        
        // Create intent to open main activity when notification is tapped
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val title = context.getString(R.string.tts_notification_title)
        val content = if (isActive) {
            "Processing text-to-speech..."
        } else {
            context.getString(R.string.tts_notification_content)
        }
        
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.mipmap.ic_launcher) // Use app icon as small icon
            .setContentIntent(pendingIntent)
            .setOngoing(true) // Makes notification persistent
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setAutoCancel(false)
            .setShowWhen(false)
            .build()
    }
    
    /**
     * Update the foreground service notification with current status
     */
    fun updateForegroundServiceNotification(
        context: Context, 
        notificationManager: NotificationManager,
        isActive: Boolean,
        currentText: String? = null
    ) {
        val title = context.getString(R.string.tts_notification_title)
        val content = when {
            isActive && !currentText.isNullOrEmpty() -> {
                "Speaking: ${currentText.take(30)}${if (currentText.length > 30) "..." else ""}"
            }
            isActive -> "Processing text-to-speech..."
            else -> context.getString(R.string.tts_notification_content)
        }
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setAutoCancel(false)
            .setShowWhen(false)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    /**
     * Check if notifications are enabled
     */
    fun areNotificationsEnabled(context: Context): Boolean {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            notificationManager.areNotificationsEnabled()
        } else {
            true
        }
    }
    
    /**
     * Check if the notification channel is enabled
     */
    fun isChannelEnabled(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return true
        }
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = notificationManager.getNotificationChannel(CHANNEL_ID)
        
        return channel?.importance != NotificationManager.IMPORTANCE_NONE
    }
}