package com.app.booking.service.impl;

import com.app.booking.config.RabbitMQConfig;
import com.app.booking.event.BookingEvent;
import com.app.booking.event.EventType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventPublisherServiceImplTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private EventPublisherServiceImpl eventPublisherService;

    private BookingEvent testEvent;

    @BeforeEach
    void setUp() {
        testEvent = BookingEvent.builder()
                .eventId("event123")
                .eventType(EventType.BOOKING_CREATED)
                .bookingId("booking123")
                .bookingNumber("BK-2026-00001")
                .userId("customer123")
                .userName("John Doe")
                .userEmail("john@test.com")
                .serviceName("AC Repair")
                .timestamp(Instant.now())
                .build();
    }

    @Test
    void publishBookingEvent_ShouldPublishSuccessfully() throws JsonProcessingException {
        when(objectMapper.writeValueAsString(any(BookingEvent.class))).thenReturn("{}");
        doNothing().when(rabbitTemplate).convertAndSend(anyString(), anyString(), anyString());

        eventPublisherService.publishBookingEvent(testEvent);

        verify(objectMapper).writeValueAsString(testEvent);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.BOOKING_EXCHANGE),
                eq("booking.booking_created"),
                anyString()
        );
    }

    @Test
    void publishBookingEvent_ShouldHandleJsonProcessingException() throws JsonProcessingException {
        when(objectMapper.writeValueAsString(any(BookingEvent.class)))
                .thenThrow(new JsonProcessingException("Test error") {});

        eventPublisherService.publishBookingEvent(testEvent);

        verify(objectMapper).writeValueAsString(testEvent);
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void publishBookingEvent_ShouldHandleRabbitMQException() throws JsonProcessingException {
        when(objectMapper.writeValueAsString(any(BookingEvent.class))).thenReturn("{}");
        doThrow(new RuntimeException("Connection failed"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), anyString());

        eventPublisherService.publishBookingEvent(testEvent);

        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), anyString());
    }

    @Test
    void publishBookingEvent_ShouldUseCorrectRoutingKey_ForBookingCreated() throws JsonProcessingException {
        testEvent.setEventType(EventType.BOOKING_CREATED);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        eventPublisherService.publishBookingEvent(testEvent);

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.BOOKING_EXCHANGE),
                eq("booking.booking_created"),
                anyString()
        );
    }

    @Test
    void publishBookingEvent_ShouldUseCorrectRoutingKey_ForServiceCompleted() throws JsonProcessingException {
        testEvent.setEventType(EventType.SERVICE_COMPLETED);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        eventPublisherService.publishBookingEvent(testEvent);

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.BOOKING_EXCHANGE),
                eq("booking.service_completed"),
                anyString()
        );
    }

    @Test
    void publishBookingEvent_ShouldUseCorrectRoutingKey_ForTechnicianAssigned() throws JsonProcessingException {
        testEvent.setEventType(EventType.TECHNICIAN_ASSIGNED);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        eventPublisherService.publishBookingEvent(testEvent);

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.BOOKING_EXCHANGE),
                eq("booking.technician_assigned"),
                anyString()
        );
    }

    @Test
    void publishBookingEvent_ShouldUseCorrectRoutingKey_ForBookingCancelled() throws JsonProcessingException {
        testEvent.setEventType(EventType.BOOKING_CANCELLED);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        eventPublisherService.publishBookingEvent(testEvent);

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.BOOKING_EXCHANGE),
                eq("booking.booking_cancelled"),
                anyString()
        );
    }
}

