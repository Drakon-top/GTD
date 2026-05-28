package com.gtd.backend.notification.consumer;

import com.gtd.backend.notification.dto.DeadlineNotification;
import com.gtd.backend.notification.dto.RecurrenceNotification;
import com.gtd.backend.notification.dto.ReminderNotification;
import com.gtd.backend.notification.dto.SyncConflictNotification;
import com.gtd.backend.notification.service.PushNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBean(ConnectionFactory.class)
public class NotificationConsumer {

    private final PushNotificationService pushNotificationService;

    @RabbitListener(queues = "${rabbitmq.notification.reminder-queue:notification.reminder}")
    public void handleReminder(ReminderNotification notification) {
        log.info("Processing reminder notification: reminderId={}, taskId={}, userId={}, taskTitle='{}', remindAt={}",
                notification.getReminderId(),
                notification.getTaskId(),
                notification.getUserId(),
                notification.getTaskTitle(),
                notification.getRemindAt());

        Map<String, String> data = new HashMap<>();
        data.put("type", "REMINDER");
        data.put("taskId", notification.getTaskId().toString());
        if (notification.getContextId() != null) {
            data.put("contextId", notification.getContextId().toString());
        }

        pushNotificationService.sendPush(
                notification.getUserId(),
                "Reminder",
                notification.getTaskTitle() != null ? notification.getTaskTitle() : "You have a reminder",
                data
        );
    }

    @RabbitListener(queues = "${rabbitmq.notification.deadline-queue:notification.deadline}")
    public void handleDeadline(DeadlineNotification notification) {
        log.info("Processing deadline notification: taskId={}, userId={}, taskTitle='{}', dueDate={}, hoursUntilDeadline={}",
                notification.getTaskId(),
                notification.getUserId(),
                notification.getTaskTitle(),
                notification.getDueDate(),
                notification.getHoursUntilDeadline());

        Map<String, String> data = new HashMap<>();
        data.put("type", "DEADLINE");
        data.put("taskId", notification.getTaskId().toString());
        if (notification.getContextId() != null) {
            data.put("contextId", notification.getContextId().toString());
        }

        String body = notification.getHoursUntilDeadline() <= 1
                ? "Due in less than 1 hour!"
                : String.format("Due in %d hours", notification.getHoursUntilDeadline());

        pushNotificationService.sendPush(
                notification.getUserId(),
                notification.getTaskTitle() != null ? notification.getTaskTitle() : "Deadline approaching",
                body,
                data
        );
    }

    @RabbitListener(queues = "${rabbitmq.notification.recurrence-queue:notification.recurrence}")
    public void handleRecurrence(RecurrenceNotification notification) {
        log.info("Processing recurrence notification: originalTaskId={}, newTaskId={}, userId={}, taskTitle='{}', recurrenceRule='{}'",
                notification.getOriginalTaskId(),
                notification.getNewTaskId(),
                notification.getUserId(),
                notification.getTaskTitle(),
                notification.getRecurrenceRule());

        Map<String, String> data = new HashMap<>();
        data.put("type", "RECURRENCE");
        data.put("taskId", notification.getNewTaskId() != null
                ? notification.getNewTaskId().toString()
                : notification.getOriginalTaskId().toString());
        if (notification.getContextId() != null) {
            data.put("contextId", notification.getContextId().toString());
        }

        pushNotificationService.sendPush(
                notification.getUserId(),
                "Recurring Task",
                notification.getTaskTitle() != null
                        ? notification.getTaskTitle() + " — new instance created"
                        : "A recurring task instance was created",
                data
        );
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

        Map<String, String> data = new HashMap<>();
        data.put("type", "SYNC_CONFLICT");
        data.put("entityId", notification.getEntityId().toString());
        data.put("entityType", notification.getEntityType());

        pushNotificationService.sendPush(
                notification.getUserId(),
                "Sync Conflict Resolved",
                String.format("Field '%s' in %s was auto-resolved",
                        notification.getFieldName() != null ? notification.getFieldName() : "unknown",
                        notification.getEntityType() != null ? notification.getEntityType().toLowerCase() : "entity"),
                data
        );
    }
}
