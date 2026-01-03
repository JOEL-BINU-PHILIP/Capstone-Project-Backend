package com.app. billing.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit. connection.ConnectionFactory;
import org.springframework. amqp.rabbit.core.RabbitTemplate;
import org. springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework. amqp.support. converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation. Configuration;

@Configuration
public class RabbitMQConfig {

    // ========== BILLING SERVICE PUBLISHING ==========
    public static final String BILLING_EXCHANGE = "billing.exchange";

    // ========== BOOKING SERVICE LISTENING ==========
    public static final String BOOKING_EXCHANGE = "booking.exchange";
    public static final String BOOKING_COMPLETED_QUEUE = "billing.booking.completed. queue";
    public static final String BOOKING_COMPLETED_ROUTING_KEY = "booking.completed";

    // ========== DEAD LETTER QUEUE (for failed messages) ==========
    public static final String DLQ_EXCHANGE = "billing.dlq.exchange";
    public static final String DLQ_QUEUE = "billing. dlq.queue";

    // ==================== EXCHANGES ====================

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

    /**
     * Queue to receive booking completed events from Booking Service.
     * This is what triggers auto-invoice generation.
     */
    @Bean
    public Queue bookingCompletedQueue() {
        return QueueBuilder
                .durable(BOOKING_COMPLETED_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "billing.failed")
                .build();
    }

    /**
     * Dead letter queue for failed invoice generation attempts
     */
    @Bean
    public Queue dlqQueue() {
        return QueueBuilder. durable(DLQ_QUEUE).build();
    }

    // ==================== BINDINGS ====================

    /**
     * Bind to booking. exchange to receive booking. completed events
     */
    @Bean
    public Binding bookingCompletedBinding(Queue bookingCompletedQueue, TopicExchange bookingExchange) {
        return BindingBuilder
                .bind(bookingCompletedQueue)
                .to(bookingExchange)
                .with(BOOKING_COMPLETED_ROUTING_KEY);
    }

    @Bean
    public Binding dlqBinding(Queue dlqQueue, TopicExchange dlqExchange) {
        return BindingBuilder
                . bind(dlqQueue)
                .to(dlqExchange)
                .with("billing.failed");
    }

    // ==================== MESSAGE CONVERTER ====================

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