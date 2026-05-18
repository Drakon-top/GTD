package com.gtd.backend.notification.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class NotificationDtoTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void reminderNotification_shouldSerializeAndDeserialize() throws Exception {
        ReminderNotification original = ReminderNotification.builder()
                .reminderId(UUID.randomUUID())
                .taskId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .taskTitle("Test task")
                .remindAt(Instant.now())
                .type(NotificationType.REMINDER)
                .build();

        String json = objectMapper.writeValueAsString(original);
        ReminderNotification deserialized = objectMapper.readValue(json, ReminderNotification.class);

        assertEquals(original.getReminderId(), deserialized.getReminderId());
        assertEquals(original.getTaskId(), deserialized.getTaskId());
        assertEquals(original.getUserId(), deserialized.getUserId());
        assertEquals(original.getTaskTitle(), deserialized.getTaskTitle());
        assertEquals(original.getType(), deserialized.getType());
    }

    @Test
    void deadlineNotification_shouldSerializeAndDeserialize() throws Exception {
        DeadlineNotification original = DeadlineNotification.builder()
                .taskId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .taskTitle("Deadline task")
                .dueDate(Instant.now())
                .hoursUntilDeadline(12)
                .type(NotificationType.DEADLINE)
                .build();

        String json = objectMapper.writeValueAsString(original);
        DeadlineNotification deserialized = objectMapper.readValue(json, DeadlineNotification.class);

        assertEquals(original.getTaskId(), deserialized.getTaskId());
        assertEquals(original.getHoursUntilDeadline(), deserialized.getHoursUntilDeadline());
        assertEquals(original.getType(), deserialized.getType());
    }

    @Test
    void recurrenceNotification_shouldSerializeAndDeserialize() throws Exception {
        RecurrenceNotification original = RecurrenceNotification.builder()
                .originalTaskId(UUID.randomUUID())
                .newTaskId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .taskTitle("Recurring task")
                .recurrenceRule("{\"type\":\"daily\"}")
                .type(NotificationType.RECURRENCE)
                .build();

        String json = objectMapper.writeValueAsString(original);
        RecurrenceNotification deserialized = objectMapper.readValue(json, RecurrenceNotification.class);

        assertEquals(original.getOriginalTaskId(), deserialized.getOriginalTaskId());
        assertEquals(original.getNewTaskId(), deserialized.getNewTaskId());
        assertEquals(original.getRecurrenceRule(), deserialized.getRecurrenceRule());
        assertEquals(original.getType(), deserialized.getType());
    }

    @Test
    void syncConflictNotification_shouldSerializeAndDeserialize() throws Exception {
        SyncConflictNotification original = SyncConflictNotification.builder()
                .syncLogId(UUID.randomUUID())
                .entityId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .entityType("TASK")
                .fieldName("title")
                .resolvedValue("Winner value")
                .conflictStatus("RESOLVED_NOTIFY")
                .type(NotificationType.SYNC_CONFLICT)
                .build();

        String json = objectMapper.writeValueAsString(original);
        SyncConflictNotification deserialized = objectMapper.readValue(json, SyncConflictNotification.class);

        assertEquals(original.getSyncLogId(), deserialized.getSyncLogId());
        assertEquals(original.getEntityType(), deserialized.getEntityType());
        assertEquals(original.getFieldName(), deserialized.getFieldName());
        assertEquals(original.getConflictStatus(), deserialized.getConflictStatus());
        assertEquals(original.getType(), deserialized.getType());
    }

    @Test
    void notificationType_shouldHaveFourValues() {
        assertEquals(4, NotificationType.values().length);
        assertNotNull(NotificationType.REMINDER);
        assertNotNull(NotificationType.DEADLINE);
        assertNotNull(NotificationType.RECURRENCE);
        assertNotNull(NotificationType.SYNC_CONFLICT);
    }

    @Test
    void reminderNotification_shouldHaveDefaultCreatedAt() {
        ReminderNotification notification = ReminderNotification.builder()
                .reminderId(UUID.randomUUID())
                .build();

        assertNotNull(notification.getCreatedAt());
    }

    @Test
    void deadlineNotification_shouldHaveDefaultCreatedAt() {
        DeadlineNotification notification = DeadlineNotification.builder()
                .taskId(UUID.randomUUID())
                .build();

        assertNotNull(notification.getCreatedAt());
    }

    @Test
    void recurrenceNotification_shouldHaveDefaultCreatedAt() {
        RecurrenceNotification notification = RecurrenceNotification.builder()
                .originalTaskId(UUID.randomUUID())
                .build();

        assertNotNull(notification.getCreatedAt());
    }

    @Test
    void syncConflictNotification_shouldHaveDefaultCreatedAt() {
        SyncConflictNotification notification = SyncConflictNotification.builder()
                .entityId(UUID.randomUUID())
                .build();

        assertNotNull(notification.getCreatedAt());
    }
}
