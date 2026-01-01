package com.app.service_request.controller;

import com.app.service_request.model.ServiceRequest;
import com.app.service_request.service.ServiceRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/service-requests/technician")
@RequiredArgsConstructor
public class TechnicianServiceRequestController {

    private final ServiceRequestService service;

    @PreAuthorize("hasRole('TECHNICIAN')")
    @GetMapping
    public List<ServiceRequest> myAssignments(Authentication authentication) {
        return service.technicianRequests(authentication.getName());
    }

    @PreAuthorize("hasRole('TECHNICIAN')")
    @PostMapping("/{id}/start")
    public ServiceRequest startJob(
            @PathVariable String id,
            Authentication authentication
    ) {
        return service.start(id, authentication.getName());
    }

    @PreAuthorize("hasRole('TECHNICIAN')")
    @PostMapping("/{id}/complete")
    public ServiceRequest completeJob(
            @PathVariable String id,
            Authentication authentication
    ) {
        return service.complete(id, authentication.getName());
    }
}
