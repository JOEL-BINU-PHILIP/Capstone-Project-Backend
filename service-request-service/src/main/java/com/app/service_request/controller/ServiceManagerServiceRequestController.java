package com.app.service_request.controller;

import com.app.service_request.model.ServiceRequest;
import com.app.service_request.service.ServiceRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/service-requests/manager")
@RequiredArgsConstructor
public class ServiceManagerServiceRequestController {

    private final ServiceRequestService service;

    @PreAuthorize("hasRole('SERVICE_MANAGER')")
    @GetMapping("/unassigned")
    public List<ServiceRequest> unassignedRequests() {
        return service.unassigned();
    }

    @PreAuthorize("hasRole('SERVICE_MANAGER')")
    @PostMapping("/{id}/assign")
    public ServiceRequest assignTechnician(
            @PathVariable String id,
            @RequestParam String technicianId,
            Authentication authentication
    ) {
        return service.assign(
                id,
                technicianId,
                authentication.getName()
        );
    }
}
