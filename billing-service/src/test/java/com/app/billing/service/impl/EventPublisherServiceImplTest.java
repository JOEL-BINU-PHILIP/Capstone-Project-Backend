package com.app.billing.service.impl;

import com.app.billing.config.RabbitMQConfig;
import com.app.billing.event.BillingEvent;
import com.app.billing.event.EventType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EventPublisherServiceImplTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private EventPublisherServiceImpl eventPublisherService;

    @Test
    void publishBillingEvent_Success() throws JsonProcessingException {
        BillingEvent event = BillingEvent.builder()
                .eventType(EventType.INVOICE_GENERATED)
                .invoiceId("inv-1")
                .build();

        String jsonMessage = "{\"eventType\":\"INVOICE_GENERATED\"}";

        when(objectMapper.writeValueAsString(event)).thenReturn(jsonMessage);

        eventPublisherService.publishBillingEvent(event);

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.BILLING_EXCHANGE),
                eq("billing.invoice_generated"), // Assuming lower case routing key logic
                eq(jsonMessage)
        );
    }
}