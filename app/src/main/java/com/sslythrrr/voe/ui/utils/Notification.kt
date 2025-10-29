package com.sslythrrr.voe.ui.utils

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.sslythrrr.voe.R

object Notification {
    private const val CHANNEL_ID = "media_processing_channel"
    const val MEDIA_SCAN_NOTIFICATION_ID = 1001
    const val LOCATION_FETCH_NOTIFICATION_ID = 1002
    const val OBJECT_DETECTOR_NOTIFICATION_ID = 1003
    const val TEXT_RECOGNIZER_NOTIFICATION_ID = 1004

    fun createNotificationChannel(context: Context) {
        val name = "Media Processing"
        val descriptionText = "Notification for media processing tasks"
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    @SuppressLint("MissingPermission")
    fun showProgressNotification(
        context: Context,
        notificationId: Int,
        title: String,
        message: String,
        progress: Int,
        max: Int
    ) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.notif_ic)
            .setContentTitle(title)
            .setContentText("$message ($progress%)")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)

        // Tampilkan progress
        if (max > 0) {
            builder.setProgress(max, progress, false)
        } else {
            builder.setProgress(0, 0, true) // Indeterminate progress
        }

        with(NotificationManagerCompat.from(context)) {
            try {
                if (hasNotificationPermission(context)) {
                    notify(notificationId, builder.build())
                }
            } catch (_: SecurityException) {
                // Handle permission denied
            }
        }
    }

    fun updateProgressNotification(
        context: Context,
        notificationId: Int,
        title: String,
        message: String,
        progress: Int,
        max: Int
    ) {
        showProgressNotification(context, notificationId, title, message, progress, max)
    }

    fun cancelNotification(context: Context, notificationId: Int) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId)
    }

    @SuppressLint("MissingPermission")
    fun finishNotification(
        context: Context,
        notificationId: Int,
        title: String,
        message: String,
    ) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.notif_ic)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(false)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            try {
                if (hasNotificationPermission(context)) {
                    notify(notificationId, builder.build())
                }
            } catch (_: SecurityException) {
            }
        }
    }
}