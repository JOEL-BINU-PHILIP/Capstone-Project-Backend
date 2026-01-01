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
@RequestMapping("/api/service-requests/manager")
@RequiredArgsConstructor
public class ServiceManagerServiceRequestController {

    private final ServiceRequestRepository repository;

    @PreAuthorize("hasRole('SERVICE_MANAGER')")
    @GetMapping("/unassigned")
    public List<ServiceRequest> unassignedRequests() {
        return repository.findByStatus(ServiceRequestStatus.REQUESTED);
    }

    @PreAuthorize("hasRole('SERVICE_MANAGER')")
    @PostMapping("/{id}/assign")
    public ServiceRequest assignTechnician(
            @PathVariable String id,
            @RequestParam String technicianId,
            Authentication authentication
    ) {
        ServiceRequest request = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        if (request.getStatus() != ServiceRequestStatus.REQUESTED) {
            throw new RuntimeException("Request already assigned");
        }

        request.setAssignedTechnicianId(technicianId);
        request.setAssignedBy(authentication.getName());
        request.setAssignedAt(Instant.now());
        request.setStatus(ServiceRequestStatus.ASSIGNED);
        request.setUpdatedAt(Instant.now());

        return repository.save(request);
    }
}
