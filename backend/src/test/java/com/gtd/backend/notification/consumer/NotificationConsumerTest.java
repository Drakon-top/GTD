package com.gtd.backend.notification.consumer;

import com.gtd.backend.notification.dto.DeadlineNotification;
import com.gtd.backend.notification.dto.NotificationType;
import com.gtd.backend.notification.dto.RecurrenceNotification;
import com.gtd.backend.notification.dto.ReminderNotification;
import com.gtd.backend.notification.dto.SyncConflictNotification;
import com.gtd.backend.notification.service.PushNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationConsumerTest {

    @Mock
    private PushNotificationService pushNotificationService;

    private NotificationConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new NotificationConsumer(pushNotificationService);
    }

    @Test
    void shouldHandleReminderAndSendPush() {
        UUID userId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        ReminderNotification notification = ReminderNotification.builder()
                .reminderId(UUID.randomUUID())
                .taskId(taskId)
                .userId(userId)
                .taskTitle("Buy groceries")
                .remindAt(Instant.now().plusSeconds(3600))
                .type(NotificationType.REMINDER)
                .build();

        assertDoesNotThrow(() -> consumer.handleReminder(notification));

        ArgumentCaptor<Map<String, String>> dataCaptor = ArgumentCaptor.forClass(Map.class);
        verify(pushNotificationService).sendPush(
                eq(userId),
                eq("Reminder"),
                eq("Buy groceries"),
                dataCaptor.capture()
        );
        assertEquals("REMINDER", dataCaptor.getValue().get("type"));
        assertEquals(taskId.toString(), dataCaptor.getValue().get("taskId"));
    }

    @Test
    void shouldHandleReminderWithNullTitle() {
        UUID userId = UUID.randomUUID();
        ReminderNotification notification = ReminderNotification.builder()
                .reminderId(UUID.randomUUID())
                .taskId(UUID.randomUUID())
                .userId(userId)
                .type(NotificationType.REMINDER)
                .build();

        assertDoesNotThrow(() -> consumer.handleReminder(notification));

        verify(pushNotificationService).sendPush(
                eq(userId),
                eq("Reminder"),
                eq("You have a reminder"),
                anyMap()
        );
    }

    @Test
    void shouldHandleDeadlineAndSendPush() {
        UUID userId = UUID.randomUUID();
        DeadlineNotification notification = DeadlineNotification.builder()
                .taskId(UUID.randomUUID())
                .userId(userId)
                .taskTitle("Submit report")
                .dueDate(Instant.now().plusSeconds(86400))
                .hoursUntilDeadline(24)
                .type(NotificationType.DEADLINE)
                .build();

        assertDoesNotThrow(() -> consumer.handleDeadline(notification));

        verify(pushNotificationService).sendPush(
                eq(userId),
                eq("Submit report"),
                eq("Due in 24 hours"),
                anyMap()
        );
    }

    @Test
    void shouldHandleDeadlineWithLessThanOneHour() {
        UUID userId = UUID.randomUUID();
        DeadlineNotification notification = DeadlineNotification.builder()
                .taskId(UUID.randomUUID())
                .userId(userId)
                .taskTitle("Urgent task")
                .dueDate(Instant.now())
                .hoursUntilDeadline(0)
                .type(NotificationType.DEADLINE)
                .build();

        assertDoesNotThrow(() -> consumer.handleDeadline(notification));

        verify(pushNotificationService).sendPush(
                eq(userId),
                eq("Urgent task"),
                eq("Due in less than 1 hour!"),
                anyMap()
        );
    }

    @Test
    void shouldHandleRecurrenceAndSendPush() {
        UUID userId = UUID.randomUUID();
        UUID newTaskId = UUID.randomUUID();
        RecurrenceNotification notification = RecurrenceNotification.builder()
                .originalTaskId(UUID.randomUUID())
                .newTaskId(newTaskId)
                .userId(userId)
                .taskTitle("Daily standup")
                .recurrenceRule("{\"type\":\"daily\",\"time\":\"09:00\"}")
                .type(NotificationType.RECURRENCE)
                .build();

        assertDoesNotThrow(() -> consumer.handleRecurrence(notification));

        ArgumentCaptor<Map<String, String>> dataCaptor = ArgumentCaptor.forClass(Map.class);
        verify(pushNotificationService).sendPush(
                eq(userId),
                eq("Recurring Task"),
                eq("Daily standup — new instance created"),
                dataCaptor.capture()
        );
        assertEquals(newTaskId.toString(), dataCaptor.getValue().get("taskId"));
    }

    @Test
    void shouldHandleRecurrenceWithNullNewTaskId() {
        UUID userId = UUID.randomUUID();
        UUID originalTaskId = UUID.randomUUID();
        RecurrenceNotification notification = RecurrenceNotification.builder()
                .originalTaskId(originalTaskId)
                .userId(userId)
                .taskTitle("Weekly review")
                .recurrenceRule("{\"type\":\"weekly\",\"dayOfWeek\":\"FRIDAY\"}")
                .type(NotificationType.RECURRENCE)
                .build();

        assertDoesNotThrow(() -> consumer.handleRecurrence(notification));

        ArgumentCaptor<Map<String, String>> dataCaptor = ArgumentCaptor.forClass(Map.class);
        verify(pushNotificationService).sendPush(
                eq(userId),
                eq("Recurring Task"),
                eq("Weekly review — new instance created"),
                dataCaptor.capture()
        );
        assertEquals(originalTaskId.toString(), dataCaptor.getValue().get("taskId"));
    }

    @Test
    void shouldHandleSyncConflictAndSendPush() {
        UUID userId = UUID.randomUUID();
        SyncConflictNotification notification = SyncConflictNotification.builder()
                .syncLogId(UUID.randomUUID())
                .entityId(UUID.randomUUID())
                .userId(userId)
                .entityType("TASK")
                .fieldName("title")
                .resolvedValue("Updated title")
                .conflictStatus("RESOLVED_NOTIFY")
                .type(NotificationType.SYNC_CONFLICT)
                .build();

        assertDoesNotThrow(() -> consumer.handleSyncConflict(notification));

        verify(pushNotificationService).sendPush(
                eq(userId),
                eq("Sync Conflict Resolved"),
                eq("Field 'title' in task was auto-resolved"),
                anyMap()
        );
    }

    @Test
    void shouldHandleSyncConflictWithMinimalFields() {
        UUID userId = UUID.randomUUID();
        SyncConflictNotification notification = SyncConflictNotification.builder()
                .entityId(UUID.randomUUID())
                .userId(userId)
                .entityType("TASK")
                .conflictStatus("RESOLVED_NOTIFY")
                .type(NotificationType.SYNC_CONFLICT)
                .build();

        assertDoesNotThrow(() -> consumer.handleSyncConflict(notification));

        verify(pushNotificationService).sendPush(
                eq(userId),
                eq("Sync Conflict Resolved"),
                anyString(),
                anyMap()
        );
    }
}
