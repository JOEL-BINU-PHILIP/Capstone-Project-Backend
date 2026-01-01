package com.app.service_request.controller;

import com.app.service_request.model.ServiceRequest;
import com.app.service_request.model.ServiceRequestStatus;
import com.app.service_request.repository.ServiceRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/service-requests/technician")
@RequiredArgsConstructor
public class TechnicianServiceRequestController {

    private final ServiceRequestRepository repository;

    @PreAuthorize("hasRole('TECHNICIAN')")
    @GetMapping
    public List<ServiceRequest> myAssignments(Authentication authentication) {
        return repository.findByAssignedTechnicianIdOrderByAssignedAtDesc(
                authentication.getName()
        );
    }

    @PreAuthorize("hasRole('TECHNICIAN')")
    @PostMapping("/{id}/start")
    public ServiceRequest startJob(
            @PathVariable String id,
            Authentication authentication
    ) {
        ServiceRequest request = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        if (!authentication.getName().equals(request.getAssignedTechnicianId())) {
            throw new RuntimeException("Not assigned to you");
        }

        request.setStatus(ServiceRequestStatus.IN_PROGRESS);
        request.setStartedAt(Instant.now());
        request.setUpdatedAt(Instant.now());

        return repository.save(request);
    }

    @PreAuthorize("hasRole('TECHNICIAN')")
    @PostMapping("/{id}/complete")
    public ServiceRequest completeJob(
            @PathVariable String id,
            Authentication authentication
    ) {
        ServiceRequest request = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        if (!authentication.getName().equals(request.getAssignedTechnicianId())) {
            throw new RuntimeException("Not assigned to you");
        }

        request.setStatus(ServiceRequestStatus.COMPLETED);
        request.setCompletedAt(Instant.now());
        request.setUpdatedAt(Instant.now());

        return repository.save(request);
    }
}
