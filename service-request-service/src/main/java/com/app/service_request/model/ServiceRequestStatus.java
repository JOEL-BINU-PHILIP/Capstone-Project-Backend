package com.app.service_request.model;

public enum ServiceRequestStatus {

    REQUESTED,     // Created by customer
    ASSIGNED,      // Technician assigned by service manager
    IN_PROGRESS,   // Technician started the job
    COMPLETED,     // Job completed successfully
    CANCELLED      // Cancelled by customer or manager
}
