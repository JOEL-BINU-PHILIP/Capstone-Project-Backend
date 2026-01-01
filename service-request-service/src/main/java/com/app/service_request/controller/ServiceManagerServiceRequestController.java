package com.app.service_request.controller;

import com.app.service_request.dto.request.AssignTechnicianDTO;
import com.app.service_request.dto.response.ServiceRequestResponseDTO;
import com.app.service_request.service.ServiceRequestService;
import com.app.service_request.util.ServiceRequestMapper;
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
    public List<ServiceRequestResponseDTO> unassignedRequests() {
        return service.unassigned()
                .stream()
                .map(ServiceRequestMapper::toResponse)
                .toList();
    }

    @PreAuthorize("hasRole('SERVICE_MANAGER')")
    @PostMapping("/{id}/assign")
    public ServiceRequestResponseDTO assignTechnician(
            @PathVariable String id,
            @RequestBody AssignTechnicianDTO dto,
            Authentication authentication
    ) {
        return ServiceRequestMapper.toResponse(
                service.assign(
                        id,
                        dto.getTechnicianId(),
                        authentication.getName()
                )
        );
    }
}
