package com.gtd.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnBean(ConnectionFactory.class)
@RequiredArgsConstructor
public class RabbitMQConfig {

    private final RabbitMQProperties properties;

    @Bean
    public MessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jackson2JsonMessageConverter());
        return template;
    }

    // --- Main exchange ---

    @Bean
    public DirectExchange notificationExchange() {
        return new DirectExchange(properties.getExchange(), true, false);
    }

    // --- Dead Letter exchange ---

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(properties.getDeadLetterExchange(), true, false);
    }

    // --- Queues (all with DLX routing) ---

    @Bean
    public Queue reminderQueue() {
        return QueueBuilder.durable(properties.getReminderQueue())
                .withArgument("x-dead-letter-exchange", properties.getDeadLetterExchange())
                .withArgument("x-dead-letter-routing-key", "dlq")
                .build();
    }

    @Bean
    public Queue deadlineQueue() {
        return QueueBuilder.durable(properties.getDeadlineQueue())
                .withArgument("x-dead-letter-exchange", properties.getDeadLetterExchange())
                .withArgument("x-dead-letter-routing-key", "dlq")
                .build();
    }

    @Bean
    public Queue recurrenceQueue() {
        return QueueBuilder.durable(properties.getRecurrenceQueue())
                .withArgument("x-dead-letter-exchange", properties.getDeadLetterExchange())
                .withArgument("x-dead-letter-routing-key", "dlq")
                .build();
    }

    @Bean
    public Queue syncConflictQueue() {
        return QueueBuilder.durable(properties.getSyncConflictQueue())
                .withArgument("x-dead-letter-exchange", properties.getDeadLetterExchange())
                .withArgument("x-dead-letter-routing-key", "dlq")
                .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(properties.getDeadLetterQueue()).build();
    }

    // --- Bindings: main exchange → queues ---

    @Bean
    public Binding reminderBinding() {
        return BindingBuilder.bind(reminderQueue())
                .to(notificationExchange())
                .with(properties.getReminderRoutingKey());
    }

    @Bean
    public Binding deadlineBinding() {
        return BindingBuilder.bind(deadlineQueue())
                .to(notificationExchange())
                .with(properties.getDeadlineRoutingKey());
    }

    @Bean
    public Binding recurrenceBinding() {
        return BindingBuilder.bind(recurrenceQueue())
                .to(notificationExchange())
                .with(properties.getRecurrenceRoutingKey());
    }

    @Bean
    public Binding syncConflictBinding() {
        return BindingBuilder.bind(syncConflictQueue())
                .to(notificationExchange())
                .with(properties.getSyncConflictRoutingKey());
    }

    // --- Binding: DLX → DLQ ---

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with("dlq");
    }
}
