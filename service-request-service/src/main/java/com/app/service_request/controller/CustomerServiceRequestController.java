package com.app.service_request.controller;

import com.app.service_request.model.ServiceRequest;
import com.app.service_request.service.ServiceRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/service-requests/customer")
@RequiredArgsConstructor
public class CustomerServiceRequestController {

    private final ServiceRequestService service;

    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping
    public ServiceRequest createRequest(
            @RequestBody ServiceRequest request,
            Authentication authentication
    ) {
        return service.create(request, authentication.getName());
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping
    public List<ServiceRequest> myRequests(Authentication authentication) {
        return service.customerRequests(authentication.getName());
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping("/{id}/cancel")
    public ServiceRequest cancelRequest(
            @PathVariable String id,
            Authentication authentication
    ) {
        return service.cancel(id, authentication.getName());
    }
}
