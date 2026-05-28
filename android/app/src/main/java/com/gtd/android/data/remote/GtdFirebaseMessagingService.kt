package com.gtd.android.data.remote

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.gtd.android.data.local.dao.ReminderDao
import com.gtd.android.notification.DeviceTokenManager
import com.gtd.android.notification.NotificationHelper
import com.gtd.android.notification.ReminderScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class GtdFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var deviceTokenManager: DeviceTokenManager

    @Inject
    lateinit var reminderDao: ReminderDao

    @Inject
    lateinit var reminderScheduler: ReminderScheduler

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM token refreshed: ${token.take(10)}...")
        CoroutineScope(Dispatchers.IO).launch {
            deviceTokenManager.onNewToken(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "FCM message received: ${message.data}")

        val data = message.data
        val type = (data["type"] ?: "general").lowercase()
        val taskId = data["taskId"] ?: data["task_id"] ?: ""
        val contextId = data["contextId"] ?: data["context_id"] ?: ""
        val title = data["title"] ?: message.notification?.title ?: "GTD"
        val body = data["body"] ?: message.notification?.body

        when (type) {
            "reminder" -> {
                notificationHelper.showReminderNotification(
                    taskId = taskId,
                    contextId = contextId,
                    title = title,
                    body = body,
                )
                CoroutineScope(Dispatchers.IO).launch {
                    markRemindersAsSent(taskId)
                }
            }
            "deadline" -> notificationHelper.showDeadlineNotification(
                taskId = taskId,
                contextId = contextId,
                title = title,
                body = body,
            )
            "recurrence" -> notificationHelper.showReminderNotification(
                taskId = taskId,
                contextId = contextId,
                title = title,
                body = body,
            )
            "sync_conflict" -> notificationHelper.showSyncConflictNotification(
                taskId = taskId,
                contextId = contextId,
                title = title,
            )
            else -> notificationHelper.showReminderNotification(
                taskId = taskId,
                contextId = contextId,
                title = title,
                body = body,
            )
        }
    }

    private suspend fun markRemindersAsSent(taskId: String) {
        try {
            val reminders = reminderDao.getByTaskId(taskId)
            for (reminder in reminders) {
                if (!reminder.isSent) {
                    reminderDao.update(reminder.copy(isSent = true))
                    reminderScheduler.cancelReminder(reminder.id)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to mark reminders as sent for taskId=$taskId", e)
        }
    }

    companion object {
        private const val TAG = "GtdFCM"
    }
}
