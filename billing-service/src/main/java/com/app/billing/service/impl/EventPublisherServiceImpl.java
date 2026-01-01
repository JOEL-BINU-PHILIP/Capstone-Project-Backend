package com.app.billing.service.impl;

import com.app.billing.config.RabbitMQConfig;
import com.app.billing. event.BillingEvent;
import com.app.billing.service.EventPublisherService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml. jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework. amqp.rabbit.core. RabbitTemplate;
import org.springframework.scheduling.annotation. Async;
import org.springframework. stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventPublisherServiceImpl implements EventPublisherService {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Override
    @Async
    public void publishBillingEvent(BillingEvent event) {
        try {
            String routingKey = "billing." + event.getEventType().name().toLowerCase();
            String message = objectMapper.writeValueAsString(event);

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.BILLING_EXCHANGE,
                    routingKey,
                    message
            );

            log.info("Published billing event: {} with routing key: {}",
                    event.getEventType(), routingKey);

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize billing event: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Failed to publish billing event:  {}", e.getMessage());
        }
    }
}