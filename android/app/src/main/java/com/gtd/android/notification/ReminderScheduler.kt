package com.gtd.android.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.gtd.android.data.local.dao.ReminderDao
import com.gtd.android.data.local.dao.TaskDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val reminderDao: ReminderDao,
    private val taskDao: TaskDao,
) {
    companion object {
        private const val TAG = "ReminderScheduler"
    }

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    suspend fun scheduleReminder(
        reminderId: String,
        taskId: String,
        contextId: String,
        taskTitle: String,
        triggerAtMillis: Long,
    ) {
        if (triggerAtMillis <= System.currentTimeMillis()) {
            Log.d(TAG, "Skipping past reminder: $reminderId")
            return
        }

        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            putExtra(ReminderAlarmReceiver.EXTRA_REMINDER_ID, reminderId)
            putExtra(ReminderAlarmReceiver.EXTRA_TASK_ID, taskId)
            putExtra(ReminderAlarmReceiver.EXTRA_CONTEXT_ID, contextId)
            putExtra(ReminderAlarmReceiver.EXTRA_TASK_TITLE, taskTitle)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }

        Log.d(TAG, "Scheduled reminder $reminderId for task '$taskTitle' at $triggerAtMillis")
    }

    fun cancelReminder(reminderId: String) {
        val intent = Intent(context, ReminderAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
            Log.d(TAG, "Cancelled reminder $reminderId")
        }
    }

    suspend fun rescheduleAllPendingReminders() = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val pendingReminders = reminderDao.getUnsent(now)

        Log.d(TAG, "Rescheduling ${pendingReminders.size} pending reminders")

        for (reminder in pendingReminders) {
            val task = taskDao.getById(reminder.taskId) ?: continue
            scheduleReminder(
                reminderId = reminder.id,
                taskId = reminder.taskId,
                contextId = task.contextId,
                taskTitle = task.title,
                triggerAtMillis = reminder.remindAt,
            )
        }
    }
}
