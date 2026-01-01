package com.app.notification.event;

public enum EventType {
    // Booking Events (8 types)
    BOOKING_CREATED,
    TECHNICIAN_ASSIGNED,
    BOOKING_CONFIRMED,
    BOOKING_REJECTED,
    SERVICE_STARTED,
    SERVICE_COMPLETED,
    BOOKING_CANCELLED,
    BOOKING_RESCHEDULED,

    // Billing Events (1 type)
    INVOICE_GENERATED
}