package com.app.notification.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io. Serializable;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    // Event metadata
    private String eventId;
    private EventType eventType;
    private Instant timestamp;

    // Target user (notification recipient)
    private String userId;
    private String userEmail;
    private String userName;
    private String userRole;

    // Invoice details
    private String invoiceId;
    private String invoiceNumber;
    private Double amount;
    private String currency;

    // Booking reference
    private String bookingId;
    private String bookingNumber;
}