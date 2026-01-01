package com.app.notification.model;

public enum NotificationType {

    // Booking related
    BOOKING_CREATED,
    BOOKING_CONFIRMED,
    BOOKING_CANCELLED,
    BOOKING_RESCHEDULED,

    // Technician related
    TECHNICIAN_ASSIGNED,
    TECHNICIAN_REJECTED,

    // Service related
    SERVICE_STARTED,
    SERVICE_COMPLETED,

    // Payment related
    INVOICE_GENERATED,
    PAYMENT_RECEIVED,

    // Account related
    ACCOUNT_CREATED,
    PASSWORD_RESET,

    // General
    REMINDER,
    SYSTEM_ALERT
}