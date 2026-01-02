package com.app.notification.model;

public enum NotificationType {
    // Booking related (8 types)
    BOOKING_CREATED,
    TECHNICIAN_ASSIGNED,
    BOOKING_CONFIRMED,
    BOOKING_REJECTED,
    SERVICE_STARTED,
    SERVICE_COMPLETED,
    BOOKING_CANCELLED,
    BOOKING_RESCHEDULED,
    PAYMENT_RECEIVED, // Billing related (1 type)
    INVOICE_GENERATED
}