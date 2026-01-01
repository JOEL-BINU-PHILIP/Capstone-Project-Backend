package com.app.service_request.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Document(collection = "service_requests")
public class ServiceRequest {

    @Id
    private String id;

    @Indexed(unique = true)
    private String requestNumber;

    @Indexed
    private String customerId;

    @Indexed
    private String serviceId;

    @Indexed
    private String assignedTechnicianId;

    @Indexed
    private ServiceRequestStatus status;

    private Priority priority;

    private String problemDescription;

    private Instant scheduledDate;

    private AddressSnapshot addressSnapshot;

    private PricingSnapshot pricingSnapshot;

    // Assignment metadata
    private String assignedBy;        // service manager userId
    private Instant assignedAt;

    // Lifecycle timestamps
    private Instant requestedAt;
    private Instant startedAt;
    private Instant completedAt;
    private Instant cancelledAt;

    // Cancellation info
    private String cancellationReason;
    private String cancelledBy;

    // Feedback
    private Integer rating;
    private String feedback;

    // Audit
    private Instant createdAt;
    private Instant updatedAt;
}
