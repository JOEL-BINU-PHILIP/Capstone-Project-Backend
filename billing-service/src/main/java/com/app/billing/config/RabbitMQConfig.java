package com.app.billing.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier; // Import Qualifier
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // ... (Constants remain the same) ...
    public static final String BILLING_EXCHANGE = "billing.exchange";
    public static final String BOOKING_EXCHANGE = "booking.exchange";
    public static final String BOOKING_COMPLETED_QUEUE = "billing.booking.completed.queue";
    public static final String BOOKING_COMPLETED_ROUTING_KEY = "booking.service_completed";
    public static final String DLQ_EXCHANGE = "billing.dlq.exchange";
    public static final String DLQ_QUEUE = "billing.dlq.queue";

    @Bean
    public TopicExchange billingExchange() {
        return new TopicExchange(BILLING_EXCHANGE);
    }

    @Bean
    public TopicExchange bookingExchange() {
        return new TopicExchange(BOOKING_EXCHANGE);
    }

    @Bean
    public TopicExchange dlqExchange() {
        return new TopicExchange(DLQ_EXCHANGE);
    }

    // ==================== QUEUES ====================

    @Bean
    public Queue bookingCompletedQueue() {
        return QueueBuilder
                .durable(BOOKING_COMPLETED_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "billing.failed")
                .build();
    }

    @Bean
    public Queue dlqQueue() {
        return QueueBuilder.durable(DLQ_QUEUE).build();
    }

    // ==================== BINDINGS (FIX APPLIED HERE) ====================

    @Bean
    public Binding bookingCompletedBinding(
            @Qualifier("bookingCompletedQueue") Queue bookingCompletedQueue, // Add @Qualifier
            @Qualifier("bookingExchange") TopicExchange bookingExchange      // Add @Qualifier (good practice)
    ) {
        return BindingBuilder
                .bind(bookingCompletedQueue)
                .to(bookingExchange)
                .with(BOOKING_COMPLETED_ROUTING_KEY);
    }

    @Bean
    public Binding dlqBinding(
            @Qualifier("dlqQueue") Queue dlqQueue,           // Add @Qualifier
            @Qualifier("dlqExchange") TopicExchange dlqExchange // Add @Qualifier
    ) {
        return BindingBuilder
                .bind(dlqQueue)
                .to(dlqExchange)
                .with("billing.failed");
    }

    // ... (MessageConverter and RabbitTemplate remain the same) ...
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}