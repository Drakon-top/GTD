package com.gtd.backend.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RabbitMQConfigTest {

    private RabbitMQProperties properties;
    private RabbitMQConfig config;

    @BeforeEach
    void setUp() {
        properties = new RabbitMQProperties();
        config = new RabbitMQConfig(properties);
    }

    @Test
    void notificationExchange_shouldBeDurableDirectExchange() {
        DirectExchange exchange = config.notificationExchange();

        assertEquals("notification.exchange", exchange.getName());
        assertTrue(exchange.isDurable());
        assertFalse(exchange.isAutoDelete());
        assertEquals("direct", exchange.getType());
    }

    @Test
    void deadLetterExchange_shouldBeDurableDirectExchange() {
        DirectExchange exchange = config.deadLetterExchange();

        assertEquals("notification.dlx", exchange.getName());
        assertTrue(exchange.isDurable());
        assertFalse(exchange.isAutoDelete());
    }

    @Test
    void reminderQueue_shouldBeDurableWithDlxArguments() {
        Queue queue = config.reminderQueue();

        assertEquals("notification.reminder", queue.getName());
        assertTrue(queue.isDurable());
        assertDlxArguments(queue.getArguments());
    }

    @Test
    void deadlineQueue_shouldBeDurableWithDlxArguments() {
        Queue queue = config.deadlineQueue();

        assertEquals("notification.deadline", queue.getName());
        assertTrue(queue.isDurable());
        assertDlxArguments(queue.getArguments());
    }

    @Test
    void recurrenceQueue_shouldBeDurableWithDlxArguments() {
        Queue queue = config.recurrenceQueue();

        assertEquals("notification.recurrence", queue.getName());
        assertTrue(queue.isDurable());
        assertDlxArguments(queue.getArguments());
    }

    @Test
    void syncConflictQueue_shouldBeDurableWithDlxArguments() {
        Queue queue = config.syncConflictQueue();

        assertEquals("notification.sync_conflict", queue.getName());
        assertTrue(queue.isDurable());
        assertDlxArguments(queue.getArguments());
    }

    @Test
    void deadLetterQueue_shouldBeDurableWithoutDlxArguments() {
        Queue queue = config.deadLetterQueue();

        assertEquals("notification.dlq", queue.getName());
        assertTrue(queue.isDurable());
        assertTrue(queue.getArguments() == null || queue.getArguments().isEmpty());
    }

    @Test
    void reminderBinding_shouldBindQueueToExchangeWithCorrectRoutingKey() {
        Binding binding = config.reminderBinding();

        assertEquals("notification.reminder", binding.getDestination());
        assertEquals("notification.exchange", binding.getExchange());
        assertEquals("notification.reminder", binding.getRoutingKey());
        assertEquals(Binding.DestinationType.QUEUE, binding.getDestinationType());
    }

    @Test
    void deadlineBinding_shouldBindQueueToExchangeWithCorrectRoutingKey() {
        Binding binding = config.deadlineBinding();

        assertEquals("notification.deadline", binding.getDestination());
        assertEquals("notification.exchange", binding.getExchange());
        assertEquals("notification.deadline", binding.getRoutingKey());
    }

    @Test
    void recurrenceBinding_shouldBindQueueToExchangeWithCorrectRoutingKey() {
        Binding binding = config.recurrenceBinding();

        assertEquals("notification.recurrence", binding.getDestination());
        assertEquals("notification.exchange", binding.getExchange());
        assertEquals("notification.recurrence", binding.getRoutingKey());
    }

    @Test
    void syncConflictBinding_shouldBindQueueToExchangeWithCorrectRoutingKey() {
        Binding binding = config.syncConflictBinding();

        assertEquals("notification.sync_conflict", binding.getDestination());
        assertEquals("notification.exchange", binding.getExchange());
        assertEquals("notification.sync_conflict", binding.getRoutingKey());
    }

    @Test
    void deadLetterBinding_shouldBindDlqToDlxWithDlqRoutingKey() {
        Binding binding = config.deadLetterBinding();

        assertEquals("notification.dlq", binding.getDestination());
        assertEquals("notification.dlx", binding.getExchange());
        assertEquals("dlq", binding.getRoutingKey());
    }

    @Test
    void allFourMainQueues_shouldRouteFailedMessagesToDlx() {
        Queue[] queues = {
                config.reminderQueue(),
                config.deadlineQueue(),
                config.recurrenceQueue(),
                config.syncConflictQueue()
        };

        for (Queue queue : queues) {
            assertEquals("notification.dlx",
                    queue.getArguments().get("x-dead-letter-exchange"),
                    "Queue " + queue.getName() + " should route to DLX");
            assertEquals("dlq",
                    queue.getArguments().get("x-dead-letter-routing-key"),
                    "Queue " + queue.getName() + " should use 'dlq' routing key for DLX");
        }
    }

    @Test
    void customProperties_shouldOverrideDefaults() {
        properties.setExchange("custom.exchange");
        properties.setReminderQueue("custom.reminder");
        properties.setReminderRoutingKey("custom.reminder.key");

        RabbitMQConfig customConfig = new RabbitMQConfig(properties);

        DirectExchange exchange = customConfig.notificationExchange();
        assertEquals("custom.exchange", exchange.getName());

        Queue queue = customConfig.reminderQueue();
        assertEquals("custom.reminder", queue.getName());

        Binding binding = customConfig.reminderBinding();
        assertEquals("custom.exchange", binding.getExchange());
        assertEquals("custom.reminder.key", binding.getRoutingKey());
    }

    @Test
    void jackson2JsonMessageConverter_shouldNotBeNull() {
        assertNotNull(config.jackson2JsonMessageConverter());
    }

    private void assertDlxArguments(Map<String, Object> arguments) {
        assertNotNull(arguments);
        assertEquals("notification.dlx", arguments.get("x-dead-letter-exchange"));
        assertEquals("dlq", arguments.get("x-dead-letter-routing-key"));
    }
}
