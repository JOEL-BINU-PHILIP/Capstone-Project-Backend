package com.app.billing.listener;

import com.app.billing.exception.DuplicateInvoiceException;
import com.app. billing.service.impl.InvoiceServiceImpl;
import com.fasterxml.jackson. databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok. extern.slf4j.Slf4j;
import org. springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Listens for booking completion events from Booking Service
 * and automatically generates invoices.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingEventListener {

    private final InvoiceServiceImpl invoiceService;
    private final ObjectMapper objectMapper;

    /**
     * Handles BOOKING_COMPLETED events from Booking Service.
     * Automatically creates an invoice when a booking is marked as completed.
     */
    @RabbitListener(queues = "${rabbitmq.queue.booking-completed: billing. booking. completed. queue}")
    public void handleBookingCompleted(String message) {
        log. info("Received booking completed event: {}", message);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> event = objectMapper.readValue(message, Map.class);

            String eventType = (String) event.get("eventType");

            // Only process BOOKING_COMPLETED events
            if (!"BOOKING_COMPLETED".equals(eventType)) {
                log.debug("Ignoring event type: {}", eventType);
                return;
            }

            String bookingId = (String) event.get("bookingId");

            if (bookingId == null || bookingId.isEmpty()) {
                log.error("Booking ID is null or empty in event");
                return;
            }

            log.info("Auto-generating invoice for completed booking: {}", bookingId);

            // Use the existing method to create invoice from booking
            invoiceService. createInvoiceFromBooking(bookingId, "SYSTEM_AUTO");

            log.info("Successfully auto-generated invoice for booking: {}", bookingId);

        } catch (DuplicateInvoiceException e) {
            // Invoice already exists - this is fine, just log it
            log.warn("Invoice already exists for booking: {}", e.getMessage());

        } catch (Exception e) {
            log.error("Failed to process booking completed event:  {}", e.getMessage(), e);
            // In production, you might want to send this to a dead letter queue
            // for manual review or retry
        }
    }
}