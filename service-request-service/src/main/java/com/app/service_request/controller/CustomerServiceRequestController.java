package com.app.service_request.controller;

import com.app.service_request.dto.request.CreateServiceRequestDTO;
import com.app.service_request.dto.response.ServiceRequestResponseDTO;
import com.app.service_request.model.ServiceRequest;
import com.app.service_request.service.ServiceRequestService;
import com.app.service_request.util.ServiceRequestMapper;
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
    public ServiceRequestResponseDTO createRequest(
            @RequestBody CreateServiceRequestDTO dto,
            Authentication authentication
    ) {
        ServiceRequest entity = ServiceRequestMapper.toEntity(dto);
        return ServiceRequestMapper.toResponse(
                service.create(entity, authentication.getName())
        );
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping
    public List<ServiceRequestResponseDTO> myRequests(Authentication authentication) {
        return service.customerRequests(authentication.getName())
                .stream()
                .map(ServiceRequestMapper::toResponse)
                .toList();
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping("/{id}/cancel")
    public ServiceRequestResponseDTO cancelRequest(
            @PathVariable String id,
            Authentication authentication
    ) {
        return ServiceRequestMapper.toResponse(
                service.cancel(id, authentication.getName())
        );
    }
}
