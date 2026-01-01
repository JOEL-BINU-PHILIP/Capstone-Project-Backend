package com.app.service_request.service.impl;

import com.app.service_request.exception.InvalidStateException;
import com.app.service_request.exception.ResourceNotFoundException;
import com.app.service_request.exception.UnauthorizedActionException;
import com.app.service_request.model.ServiceRequest;
import com.app.service_request.model.ServiceRequestStatus;
import com.app.service_request.repository.ServiceRequestRepository;
import com.app.service_request.service.ServiceRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ServiceRequestServiceImpl implements ServiceRequestService {

    private final ServiceRequestRepository repository;

    @Override
    public ServiceRequest create(ServiceRequest request, String customerId) {
        request.setId(null);
        request.setRequestNumber("SR-" + UUID.randomUUID());
        request.setCustomerId(customerId);
        request.setStatus(ServiceRequestStatus.REQUESTED);
        request.setRequestedAt(Instant.now());
        request.setCreatedAt(Instant.now());
        request.setUpdatedAt(Instant.now());
        return repository.save(request);
    }

    @Override
    public List<ServiceRequest> customerRequests(String customerId) {
        return repository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    @Override
    public ServiceRequest cancel(String requestId, String customerId) {
        ServiceRequest request = get(requestId);

        if (!request.getCustomerId().equals(customerId)) {
            throw new UnauthorizedActionException("Not your request");
        }

        if (request.getStatus() == ServiceRequestStatus.IN_PROGRESS) {
            throw new InvalidStateException("Cannot cancel after job started");
        }

        request.setStatus(ServiceRequestStatus.CANCELLED);
        request.setCancelledAt(Instant.now());
        request.setCancelledBy(customerId);
        request.setUpdatedAt(Instant.now());

        return repository.save(request);
    }

    @Override
    public List<ServiceRequest> unassigned() {
        return repository.findByStatus(ServiceRequestStatus.REQUESTED);
    }

    @Override
    public ServiceRequest assign(String requestId, String technicianId, String managerId) {
        ServiceRequest request = get(requestId);

        if (request.getStatus() != ServiceRequestStatus.REQUESTED) {
            throw new InvalidStateException("Request already assigned");
        }

        request.setAssignedTechnicianId(technicianId);
        request.setAssignedBy(managerId);
        request.setAssignedAt(Instant.now());
        request.setStatus(ServiceRequestStatus.ASSIGNED);
        request.setUpdatedAt(Instant.now());

        return repository.save(request);
    }

    @Override
    public List<ServiceRequest> technicianRequests(String technicianId) {
        return repository.findByAssignedTechnicianIdOrderByAssignedAtDesc(technicianId);
    }

    @Override
    public ServiceRequest start(String requestId, String technicianId) {
        ServiceRequest request = get(requestId);

        if (!technicianId.equals(request.getAssignedTechnicianId())) {
            throw new UnauthorizedActionException("Not assigned to you");
        }

        if (request.getStatus() != ServiceRequestStatus.ASSIGNED) {
            throw new InvalidStateException("Job cannot be started");
        }

        request.setStatus(ServiceRequestStatus.IN_PROGRESS);
        request.setStartedAt(Instant.now());
        request.setUpdatedAt(Instant.now());

        return repository.save(request);
    }

    @Override
    public ServiceRequest complete(String requestId, String technicianId) {
        ServiceRequest request = get(requestId);

        if (!technicianId.equals(request.getAssignedTechnicianId())) {
            throw new UnauthorizedActionException("Not assigned to you");
        }

        if (request.getStatus() != ServiceRequestStatus.IN_PROGRESS) {
            throw new InvalidStateException("Job not in progress");
        }

        request.setStatus(ServiceRequestStatus.COMPLETED);
        request.setCompletedAt(Instant.now());
        request.setUpdatedAt(Instant.now());

        return repository.save(request);
    }

    private ServiceRequest get(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service request not found"));
    }
}
