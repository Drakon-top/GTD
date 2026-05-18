package com.gtd.backend.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RabbitMQPropertiesTest {

    @Test
    void defaults_shouldBeSetCorrectly() {
        RabbitMQProperties props = new RabbitMQProperties();

        assertEquals("notification.exchange", props.getExchange());
        assertEquals("notification.dlx", props.getDeadLetterExchange());
        assertEquals("notification.reminder", props.getReminderQueue());
        assertEquals("notification.deadline", props.getDeadlineQueue());
        assertEquals("notification.recurrence", props.getRecurrenceQueue());
        assertEquals("notification.sync_conflict", props.getSyncConflictQueue());
        assertEquals("notification.dlq", props.getDeadLetterQueue());
        assertEquals("notification.reminder", props.getReminderRoutingKey());
        assertEquals("notification.deadline", props.getDeadlineRoutingKey());
        assertEquals("notification.recurrence", props.getRecurrenceRoutingKey());
        assertEquals("notification.sync_conflict", props.getSyncConflictRoutingKey());
    }

    @Test
    void setters_shouldOverrideDefaults() {
        RabbitMQProperties props = new RabbitMQProperties();

        props.setExchange("custom.exchange");
        props.setDeadLetterExchange("custom.dlx");
        props.setReminderQueue("custom.reminder");
        props.setDeadlineQueue("custom.deadline");
        props.setRecurrenceQueue("custom.recurrence");
        props.setSyncConflictQueue("custom.sync");
        props.setDeadLetterQueue("custom.dlq");

        assertEquals("custom.exchange", props.getExchange());
        assertEquals("custom.dlx", props.getDeadLetterExchange());
        assertEquals("custom.reminder", props.getReminderQueue());
        assertEquals("custom.deadline", props.getDeadlineQueue());
        assertEquals("custom.recurrence", props.getRecurrenceQueue());
        assertEquals("custom.sync", props.getSyncConflictQueue());
        assertEquals("custom.dlq", props.getDeadLetterQueue());
    }
}
