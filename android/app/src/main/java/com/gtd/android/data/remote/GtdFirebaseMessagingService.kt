package com.gtd.android.data.remote

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.gtd.android.notification.DeviceTokenManager
import com.gtd.android.notification.NotificationHelper
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
        val type = data["type"] ?: "general"
        val taskId = data["taskId"] ?: data["task_id"] ?: ""
        val contextId = data["contextId"] ?: data["context_id"] ?: ""
        val title = data["title"] ?: message.notification?.title ?: "GTD"
        val body = data["body"] ?: message.notification?.body

        when (type) {
            "reminder" -> notificationHelper.showReminderNotification(
                taskId = taskId,
                contextId = contextId,
                title = title,
                body = body,
            )
            "deadline" -> notificationHelper.showDeadlineNotification(
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

    companion object {
        private const val TAG = "GtdFCM"
    }
}
