package com.gtd.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

@Configuration
@ConditionalOnBean(ConnectionFactory.class)
@RequiredArgsConstructor
public class RabbitMQConfig {

    static final int MAX_RETRY_ATTEMPTS = 3;
    static final long INITIAL_BACKOFF_MS = 1000;
    static final double BACKOFF_MULTIPLIER = 2.0;
    static final long MAX_BACKOFF_MS = 10000;

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

    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(MAX_RETRY_ATTEMPTS);

        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(INITIAL_BACKOFF_MS);
        backOffPolicy.setMultiplier(BACKOFF_MULTIPLIER);
        backOffPolicy.setMaxInterval(MAX_BACKOFF_MS);

        retryTemplate.setRetryPolicy(retryPolicy);
        retryTemplate.setBackOffPolicy(backOffPolicy);
        return retryTemplate;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jackson2JsonMessageConverter());
        factory.setDefaultRequeueRejected(false);
        factory.setAdviceChain(
                org.springframework.amqp.rabbit.config.RetryInterceptorBuilder
                        .stateless()
                        .retryOperations(retryTemplate())
                        .recoverer(new RejectAndDontRequeueRecoverer())
                        .build()
        );
        return factory;
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
