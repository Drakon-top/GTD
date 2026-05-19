package com.gtd.android.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.gtd.android.MainActivity
import com.gtd.android.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        const val CHANNEL_REMINDERS = "gtd_reminders"
        const val CHANNEL_DEADLINES = "gtd_deadlines"
        const val CHANNEL_SYNC = "gtd_sync"
        const val CHANNEL_GENERAL = "gtd_general"

        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_CONTEXT_ID = "context_id"
        const val EXTRA_NOTIFICATION_TYPE = "notification_type"
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun createNotificationChannels() {
        val channels = listOf(
            NotificationChannel(
                CHANNEL_REMINDERS,
                "Reminders",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Task reminder notifications"
                enableVibration(true)
            },
            NotificationChannel(
                CHANNEL_DEADLINES,
                "Deadlines",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Task deadline notifications"
                enableVibration(true)
            },
            NotificationChannel(
                CHANNEL_SYNC,
                "Sync Conflicts",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Sync conflict notifications"
            },
            NotificationChannel(
                CHANNEL_GENERAL,
                "General",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "General notifications"
            },
        )
        notificationManager.createNotificationChannels(channels)
    }

    fun showReminderNotification(
        taskId: String,
        contextId: String,
        title: String,
        body: String?,
        notificationId: Int = taskId.hashCode(),
    ) {
        val intent = createDeepLinkIntent(taskId, contextId, "reminder")
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body ?: "Reminder")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    fun showDeadlineNotification(
        taskId: String,
        contextId: String,
        title: String,
        body: String?,
        notificationId: Int = (taskId + "deadline").hashCode(),
    ) {
        val intent = createDeepLinkIntent(taskId, contextId, "deadline")
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_DEADLINES)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body ?: "Deadline approaching")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    fun showSyncConflictNotification(
        taskId: String,
        contextId: String,
        title: String,
    ) {
        val notificationId = (taskId + "sync").hashCode()
        val intent = createDeepLinkIntent(taskId, contextId, "sync_conflict")
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_SYNC)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Sync Conflict")
            .setContentText("Task \"$title\" was modified on another device")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    private fun createDeepLinkIntent(taskId: String, contextId: String, type: String): Intent {
        return Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_CONTEXT_ID, contextId)
            putExtra(EXTRA_NOTIFICATION_TYPE, type)
        }
    }
}
