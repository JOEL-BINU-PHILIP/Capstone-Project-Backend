package com.app.booking.model;

public enum BookingStatus {
    PENDING,        // Customer created, waiting for assignment
    ASSIGNED,       // Technician assigned by manager
    CONFIRMED,      // Technician confirmed acceptance
    IN_PROGRESS,    // Service work started
    COMPLETED,      // Service completed successfully
    CANCELLED,      // Cancelled by customer/manager
    REJECTED        // Rejected by technician
}