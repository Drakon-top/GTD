package com.gtd.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "rabbitmq.notification")
public class RabbitMQProperties {

    private String exchange = "notification.exchange";
    private String deadLetterExchange = "notification.dlx";

    private String reminderQueue = "notification.reminder";
    private String deadlineQueue = "notification.deadline";
    private String recurrenceQueue = "notification.recurrence";
    private String syncConflictQueue = "notification.sync_conflict";

    private String deadLetterQueue = "notification.dlq";

    private String reminderRoutingKey = "notification.reminder";
    private String deadlineRoutingKey = "notification.deadline";
    private String recurrenceRoutingKey = "notification.recurrence";
    private String syncConflictRoutingKey = "notification.sync_conflict";
}
