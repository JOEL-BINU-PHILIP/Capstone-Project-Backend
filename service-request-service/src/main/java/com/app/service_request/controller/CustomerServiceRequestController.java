package com.app.service_request.controller;

import com.app.service_request.model.ServiceRequest;
import com.app.service_request.model.ServiceRequestStatus;
import com.app.service_request.repository.ServiceRequestRepository;
import com.app.service_request.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/service-requests/customer")
@RequiredArgsConstructor
public class CustomerServiceRequestController {

    private final ServiceRequestRepository repository;
    private final JwtUtil jwtUtil;

    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping
    public ServiceRequest createRequest(
            @RequestBody ServiceRequest request,
            Authentication authentication
    ) {
        request.setId(null);
        request.setRequestNumber("SR-" + UUID.randomUUID());
        request.setCustomerId(authentication.getName());
        request.setStatus(ServiceRequestStatus.REQUESTED);
        request.setRequestedAt(Instant.now());
        request.setCreatedAt(Instant.now());
        request.setUpdatedAt(Instant.now());

        return repository.save(request);
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping
    public List<ServiceRequest> myRequests(Authentication authentication) {
        return repository.findByCustomerIdOrderByCreatedAtDesc(
                authentication.getName()
        );
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping("/{id}/cancel")
    public ServiceRequest cancelRequest(
            @PathVariable String id,
            Authentication authentication
    ) {
        ServiceRequest request = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        if (!request.getCustomerId().equals(authentication.getName())) {
            throw new RuntimeException("Not your request");
        }

        if (request.getStatus() == ServiceRequestStatus.IN_PROGRESS) {
            throw new RuntimeException("Cannot cancel after work has started");
        }

        request.setStatus(ServiceRequestStatus.CANCELLED);
        request.setCancelledAt(Instant.now());
        request.setCancelledBy(authentication.getName());
        request.setUpdatedAt(Instant.now());

        return repository.save(request);
    }
}
