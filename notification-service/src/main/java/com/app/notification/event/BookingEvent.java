package com.app.notification.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time. Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingEvent implements Serializable {

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

    // Booking details
    private String bookingId;
    private String bookingNumber;
    private String bookingStatus;

    // Service details
    private String serviceId;
    private String serviceName;
    private String categoryName;

    // Technician details
    private String technicianId;
    private String technicianName;
    private String technicianPhone;

    // Schedule
    private String scheduledDate;

    // Additional info
    private String cancellationReason;
    private String rejectionReason;
}