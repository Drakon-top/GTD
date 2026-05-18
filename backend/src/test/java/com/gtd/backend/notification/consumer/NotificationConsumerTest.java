package com.gtd.backend.notification.consumer;

import com.gtd.backend.notification.dto.DeadlineNotification;
import com.gtd.backend.notification.dto.NotificationType;
import com.gtd.backend.notification.dto.RecurrenceNotification;
import com.gtd.backend.notification.dto.ReminderNotification;
import com.gtd.backend.notification.dto.SyncConflictNotification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class NotificationConsumerTest {

    private NotificationConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new NotificationConsumer();
    }

    @Test
    void handleReminder_shouldProcessSuccessfully() {
        ReminderNotification notification = ReminderNotification.builder()
                .reminderId(UUID.randomUUID())
                .taskId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .taskTitle("Buy groceries")
                .remindAt(Instant.now().plusSeconds(3600))
                .type(NotificationType.REMINDER)
                .build();

        assertDoesNotThrow(() -> consumer.handleReminder(notification));
    }

    @Test
    void handleReminder_withNullOptionalFields_shouldProcessSuccessfully() {
        ReminderNotification notification = ReminderNotification.builder()
                .reminderId(UUID.randomUUID())
                .taskId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .type(NotificationType.REMINDER)
                .build();

        assertDoesNotThrow(() -> consumer.handleReminder(notification));
    }

    @Test
    void handleDeadline_shouldProcessSuccessfully() {
        DeadlineNotification notification = DeadlineNotification.builder()
                .taskId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .taskTitle("Submit report")
                .dueDate(Instant.now().plusSeconds(86400))
                .hoursUntilDeadline(24)
                .type(NotificationType.DEADLINE)
                .build();

        assertDoesNotThrow(() -> consumer.handleDeadline(notification));
    }

    @Test
    void handleDeadline_withZeroHoursUntilDeadline_shouldProcessSuccessfully() {
        DeadlineNotification notification = DeadlineNotification.builder()
                .taskId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .taskTitle("Urgent task")
                .dueDate(Instant.now())
                .hoursUntilDeadline(0)
                .type(NotificationType.DEADLINE)
                .build();

        assertDoesNotThrow(() -> consumer.handleDeadline(notification));
    }

    @Test
    void handleRecurrence_shouldProcessSuccessfully() {
        RecurrenceNotification notification = RecurrenceNotification.builder()
                .originalTaskId(UUID.randomUUID())
                .newTaskId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .taskTitle("Daily standup")
                .recurrenceRule("{\"type\":\"daily\",\"time\":\"09:00\"}")
                .type(NotificationType.RECURRENCE)
                .build();

        assertDoesNotThrow(() -> consumer.handleRecurrence(notification));
    }

    @Test
    void handleRecurrence_withNullNewTaskId_shouldProcessSuccessfully() {
        RecurrenceNotification notification = RecurrenceNotification.builder()
                .originalTaskId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .taskTitle("Weekly review")
                .recurrenceRule("{\"type\":\"weekly\",\"dayOfWeek\":\"FRIDAY\"}")
                .type(NotificationType.RECURRENCE)
                .build();

        assertDoesNotThrow(() -> consumer.handleRecurrence(notification));
    }

    @Test
    void handleSyncConflict_shouldProcessSuccessfully() {
        SyncConflictNotification notification = SyncConflictNotification.builder()
                .syncLogId(UUID.randomUUID())
                .entityId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .entityType("TASK")
                .fieldName("title")
                .resolvedValue("Updated title")
                .conflictStatus("RESOLVED_NOTIFY")
                .type(NotificationType.SYNC_CONFLICT)
                .build();

        assertDoesNotThrow(() -> consumer.handleSyncConflict(notification));
    }

    @Test
    void handleSyncConflict_withMinimalFields_shouldProcessSuccessfully() {
        SyncConflictNotification notification = SyncConflictNotification.builder()
                .entityId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .entityType("TASK")
                .conflictStatus("RESOLVED_NOTIFY")
                .type(NotificationType.SYNC_CONFLICT)
                .build();

        assertDoesNotThrow(() -> consumer.handleSyncConflict(notification));
    }
}
