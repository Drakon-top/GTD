package com.gtd.android.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ReminderAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_CONTEXT_ID = "context_id"
        const val EXTRA_TASK_TITLE = "task_title"
    }

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var reminderDao: com.gtd.android.data.local.dao.ReminderDao

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getStringExtra(EXTRA_REMINDER_ID) ?: return
        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
        val contextId = intent.getStringExtra(EXTRA_CONTEXT_ID) ?: return
        val taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE) ?: "Task Reminder"

        notificationHelper.showReminderNotification(
            taskId = taskId,
            contextId = contextId,
            title = taskTitle,
            body = "Time to act on this task",
        )

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val reminder = reminderDao.getById(reminderId)
                if (reminder != null) {
                    reminderDao.update(reminder.copy(isSent = true))
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
