package com.gtd.android.notification

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.gtd.android.data.local.dao.ReminderDao
import com.gtd.android.data.local.dao.TaskDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class ReminderCheckWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val reminderDao: ReminderDao,
    private val taskDao: TaskDao,
    private val notificationHelper: NotificationHelper,
) : CoroutineWorker(appContext, params) {

    companion object {
        private const val TAG = "ReminderCheckWorker"
        const val WORK_NAME = "gtd_reminder_check"

        fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<ReminderCheckWorker>(
                15, TimeUnit.MINUTES,
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }

    override suspend fun doWork(): Result {
        return try {
            val now = System.currentTimeMillis()
            val dueReminders = reminderDao.getDueReminders(now)

            Log.d(TAG, "Found ${dueReminders.size} due reminders")

            for (reminder in dueReminders) {
                val task = taskDao.getById(reminder.taskId) ?: continue

                notificationHelper.showReminderNotification(
                    taskId = task.id,
                    contextId = task.contextId,
                    title = task.title,
                    body = "Time to act on this task",
                )

                reminderDao.update(reminder.copy(isSent = true))
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking reminders", e)
            Result.retry()
        }
    }
}
