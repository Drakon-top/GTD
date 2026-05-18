package com.gtd.backend.notification.service;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class LoggingPushNotificationServiceTest {

    private final LoggingPushNotificationService service = new LoggingPushNotificationService();

    @Test
    void shouldLogWithoutException() {
        assertDoesNotThrow(() -> service.sendPush(
                UUID.randomUUID(),
                "Test Title",
                "Test Body",
                Map.of("taskId", "123", "type", "REMINDER")
        ));
    }

    @Test
    void shouldHandleEmptyData() {
        assertDoesNotThrow(() -> service.sendPush(
                UUID.randomUUID(),
                "Title",
                "Body",
                Map.of()
        ));
    }
}
