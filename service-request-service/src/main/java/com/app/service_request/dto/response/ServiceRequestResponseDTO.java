package com.app.service_request.dto.response;

import com.app.service_request.model.ServiceRequestStatus;
import lombok.Data;

import java.time.Instant;

@Data
public class ServiceRequestResponseDTO {

    private String id;
    private String requestNumber;

    private String serviceId;
    private String customerId;
    private String assignedTechnicianId;

    private ServiceRequestStatus status;

    private Instant requestedAt;
    private Instant assignedAt;
    private Instant startedAt;
    private Instant completedAt;
}
