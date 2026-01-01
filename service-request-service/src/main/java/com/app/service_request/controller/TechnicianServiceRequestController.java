package com.app.service_request.controller;

import com.app.service_request.dto.response.ServiceRequestResponseDTO;
import com.app.service_request.service.ServiceRequestService;
import com.app.service_request.util.ServiceRequestMapper;
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
    public List<ServiceRequestResponseDTO> myAssignments(Authentication authentication) {
        return service.technicianRequests(authentication.getName())
                .stream()
                .map(ServiceRequestMapper::toResponse)
                .toList();
    }

    @PreAuthorize("hasRole('TECHNICIAN')")
    @PostMapping("/{id}/start")
    public ServiceRequestResponseDTO startJob(
            @PathVariable String id,
            Authentication authentication
    ) {
        return ServiceRequestMapper.toResponse(
                service.start(id, authentication.getName())
        );
    }

    @PreAuthorize("hasRole('TECHNICIAN')")
    @PostMapping("/{id}/complete")
    public ServiceRequestResponseDTO completeJob(
            @PathVariable String id,
            Authentication authentication
    ) {
        return ServiceRequestMapper.toResponse(
                service.complete(id, authentication.getName())
        );
    }
}
