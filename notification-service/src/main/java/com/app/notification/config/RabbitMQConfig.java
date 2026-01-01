package com.app.notification.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp. rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework. context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Queue
    public static final String NOTIFICATION_QUEUE = "notification.queue";

    // Exchanges
    public static final String BOOKING_EXCHANGE = "booking.exchange";
    public static final String BILLING_EXCHANGE = "billing.exchange";

    // Routing Keys
    public static final String BOOKING_ROUTING_KEY = "booking.#";
    public static final String BILLING_ROUTING_KEY = "billing.#";

    // Queue Definition
    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE).build();
    }

    // Exchange Definitions
    @Bean
    public TopicExchange bookingExchange() {
        return new TopicExchange(BOOKING_EXCHANGE);
    }

    @Bean
    public TopicExchange billingExchange() {
        return new TopicExchange(BILLING_EXCHANGE);
    }

    // Bindings
    @Bean
    public Binding bookingBinding(Queue notificationQueue, TopicExchange bookingExchange) {
        return BindingBuilder
                .bind(notificationQueue)
                .to(bookingExchange)
                .with(BOOKING_ROUTING_KEY);
    }

    @Bean
    public Binding billingBinding(Queue notificationQueue, TopicExchange billingExchange) {
        return BindingBuilder
                .bind(notificationQueue)
                .to(billingExchange)
                .with(BILLING_ROUTING_KEY);
    }

    // Message Converter
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