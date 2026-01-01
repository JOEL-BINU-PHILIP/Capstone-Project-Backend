package com.app.booking.service.impl;

import com.app.booking.config.RabbitMQConfig;
import com.app.booking. event.BookingEvent;
import com.app.booking.service. EventPublisherService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com. fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j. Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventPublisherServiceImpl implements EventPublisherService {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Override
    @Async
    public void publishBookingEvent(BookingEvent event) {
        try {
            String routingKey = "booking." + event.getEventType().name().toLowerCase();
            String message = objectMapper.writeValueAsString(event);

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.BOOKING_EXCHANGE,
                    routingKey,
                    message
            );

            log.info("Published booking event: {} with routing key: {}",
                    event.getEventType(), routingKey);

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize booking event: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Failed to publish booking event: {}", e.getMessage());
        }
    }
}