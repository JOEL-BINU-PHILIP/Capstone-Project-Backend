package com.app.billing.service.impl;

import com.app.billing.config.RabbitMQConfig;
import com.app.billing.event.BillingEvent;
import com.app.billing.event.EventType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventPublisherServiceImplTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private EventPublisherServiceImpl eventPublisherService;

    private BillingEvent testEvent;

    @BeforeEach
    void setUp() {
        testEvent = BillingEvent.builder()
                .eventId("event123")
                .eventType(EventType.INVOICE_GENERATED)
                .userId("customer123")
                .userEmail("customer@test.com")
                .userName("John Doe")
                .userRole("CUSTOMER")
                .invoiceId("invoice123")
                .invoiceNumber("INV-2026-00001")
                .amount(1080.0)
                .currency("INR")
                .bookingId("booking123")
                .bookingNumber("BK-2026-00001")
                .timestamp(Instant.now())
                .build();
    }

    // ==================== PUBLISH TESTS ====================

    @Test
    void publishBillingEvent_ShouldPublishInvoiceGeneratedEvent() throws JsonProcessingException {
        String jsonMessage = "{\"eventType\":\"INVOICE_GENERATED\",\"invoiceId\":\"invoice123\"}";
        when(objectMapper.writeValueAsString(testEvent)).thenReturn(jsonMessage);

        eventPublisherService.publishBillingEvent(testEvent);

        verify(objectMapper).writeValueAsString(testEvent);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.BILLING_EXCHANGE),
                eq("billing.invoice_generated"),
                eq(jsonMessage)
        );
    }

    @Test
    void publishBillingEvent_ShouldPublishWithCorrectRoutingKey() throws JsonProcessingException {
        testEvent.setEventType(EventType.INVOICE_GENERATED);
        String jsonMessage = "{\"eventType\":\"INVOICE_GENERATED\",\"invoiceId\":\"invoice123\"}";
        when(objectMapper.writeValueAsString(testEvent)).thenReturn(jsonMessage);

        eventPublisherService.publishBillingEvent(testEvent);

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.BILLING_EXCHANGE),
                eq("billing.invoice_generated"),
                eq(jsonMessage)
        );
    }

    @Test
    void publishBillingEvent_ShouldIncludeAllEventFields() throws JsonProcessingException {
        String jsonMessage = "{\"eventType\":\"INVOICE_GENERATED\",\"invoiceId\":\"invoice123\",\"amount\":1080.0}";
        when(objectMapper.writeValueAsString(any(BillingEvent.class))).thenReturn(jsonMessage);

        eventPublisherService.publishBillingEvent(testEvent);

        verify(objectMapper).writeValueAsString(testEvent);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.BILLING_EXCHANGE),
                eq("billing.invoice_generated"),
                eq(jsonMessage)
        );
    }

    // ==================== ERROR HANDLING TESTS ====================

    @Test
    void publishBillingEvent_ShouldHandleJsonProcessingException() throws JsonProcessingException {
        when(objectMapper.writeValueAsString(testEvent))
                .thenThrow(new JsonProcessingException("Serialization error") {});

        // Should not throw exception, just log error
        eventPublisherService.publishBillingEvent(testEvent);

        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), anyString());
    }

    @Test
    void publishBillingEvent_ShouldHandleAmqpException() throws JsonProcessingException {
        String jsonMessage = "{\"eventType\":\"INVOICE_GENERATED\"}";
        when(objectMapper.writeValueAsString(testEvent)).thenReturn(jsonMessage);
        doThrow(new AmqpException("Connection failed")).when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), anyString());

        // Should not throw exception, just log error
        eventPublisherService.publishBillingEvent(testEvent);

        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), anyString());
    }

    @Test
    void publishBillingEvent_ShouldHandleGenericException() throws JsonProcessingException {
        String jsonMessage = "{\"eventType\":\"INVOICE_GENERATED\"}";
        when(objectMapper.writeValueAsString(testEvent)).thenReturn(jsonMessage);
        doThrow(new RuntimeException("Unexpected error")).when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), anyString());

        // Should not throw exception, just log error
        eventPublisherService.publishBillingEvent(testEvent);

        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), anyString());
    }

    // ==================== ROUTING KEY TESTS ====================

    @Test
    void publishBillingEvent_ShouldUseLowerCaseRoutingKey() throws JsonProcessingException {
        testEvent.setEventType(EventType.INVOICE_GENERATED);
        String jsonMessage = "{}";
        when(objectMapper.writeValueAsString(testEvent)).thenReturn(jsonMessage);

        eventPublisherService.publishBillingEvent(testEvent);

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.BILLING_EXCHANGE),
                eq("billing.invoice_generated"), // lowercase
                eq(jsonMessage)
        );
    }
}