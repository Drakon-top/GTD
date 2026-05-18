package com.gtd.backend.notification.consumer;

import com.gtd.backend.notification.dto.DeadlineNotification;
import com.gtd.backend.notification.dto.RecurrenceNotification;
import com.gtd.backend.notification.dto.ReminderNotification;
import com.gtd.backend.notification.dto.SyncConflictNotification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnBean(ConnectionFactory.class)
public class NotificationConsumer {

    @RabbitListener(queues = "${rabbitmq.notification.reminder-queue:notification.reminder}")
    public void handleReminder(ReminderNotification notification) {
        log.info("Processing reminder notification: reminderId={}, taskId={}, userId={}, taskTitle='{}', remindAt={}",
                notification.getReminderId(),
                notification.getTaskId(),
                notification.getUserId(),
                notification.getTaskTitle(),
                notification.getRemindAt());
    }

    @RabbitListener(queues = "${rabbitmq.notification.deadline-queue:notification.deadline}")
    public void handleDeadline(DeadlineNotification notification) {
        log.info("Processing deadline notification: taskId={}, userId={}, taskTitle='{}', dueDate={}, hoursUntilDeadline={}",
                notification.getTaskId(),
                notification.getUserId(),
                notification.getTaskTitle(),
                notification.getDueDate(),
                notification.getHoursUntilDeadline());
    }

    @RabbitListener(queues = "${rabbitmq.notification.recurrence-queue:notification.recurrence}")
    public void handleRecurrence(RecurrenceNotification notification) {
        log.info("Processing recurrence notification: originalTaskId={}, newTaskId={}, userId={}, taskTitle='{}', recurrenceRule='{}'",
                notification.getOriginalTaskId(),
                notification.getNewTaskId(),
                notification.getUserId(),
                notification.getTaskTitle(),
                notification.getRecurrenceRule());
    }

    @RabbitListener(queues = "${rabbitmq.notification.sync-conflict-queue:notification.sync_conflict}")
    public void handleSyncConflict(SyncConflictNotification notification) {
        log.info("Processing sync conflict notification: syncLogId={}, entityId={}, userId={}, entityType='{}', fieldName='{}', conflictStatus='{}'",
                notification.getSyncLogId(),
                notification.getEntityId(),
                notification.getUserId(),
                notification.getEntityType(),
                notification.getFieldName(),
                notification.getConflictStatus());
    }
}
