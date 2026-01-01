package com.app.service_catalog.controller;

import com.app.service_catalog.dto.request.CreateServiceRequest;
import com.app.service_catalog.dto.request.UpdateServiceRequest;
import com.app.service_catalog.dto.response.ServiceItemResponse;
import com.app. service_catalog.service.ServiceItemService;
import jakarta.validation. Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost. PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
public class ServiceController {

    private final ServiceItemService serviceItemService;

    // =====================
    // ADMIN ONLY
    // =====================

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ServiceItemResponse> create(@Valid @RequestBody CreateServiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(serviceItemService.createService(request));
    }

    @PutMapping("/{serviceId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ServiceItemResponse> update(
            @PathVariable("serviceId") String serviceId,
            @RequestBody UpdateServiceRequest request) {
        return ResponseEntity.ok(serviceItemService.updateService(serviceId, request));
    }

    @PutMapping("/{serviceId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ServiceItemResponse> updateStatus(
            @PathVariable("serviceId") String serviceId,
            @RequestParam("active") boolean active) {
        return ResponseEntity.ok(serviceItemService.updateServiceStatus(serviceId, active));
    }

    @DeleteMapping("/{serviceId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable("serviceId") String serviceId) {
        serviceItemService.deleteService(serviceId);
        return ResponseEntity.noContent().build();
    }

    // =====================
    // ADMIN & SERVICE MANAGER
    // =====================

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SERVICE_MANAGER')")
    public ResponseEntity<List<ServiceItemResponse>> getAll() {
        return ResponseEntity.ok(serviceItemService.getAllServices());
    }

    // =====================
    // PUBLIC - No auth required
    // =====================

    @GetMapping("/active")
    public ResponseEntity<List<ServiceItemResponse>> getActiveServices() {
        return ResponseEntity.ok(serviceItemService.getActiveServices());
    }

    @GetMapping("/{serviceId}")
    public ResponseEntity<ServiceItemResponse> getById(@PathVariable("serviceId") String serviceId) {
        return ResponseEntity.ok(serviceItemService.getServiceById(serviceId));
    }

    @GetMapping("/search")
    public ResponseEntity<List<ServiceItemResponse>> search(
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "skill", required = false) String skill) {
        return ResponseEntity.ok(serviceItemService.search(query, skill));
    }
}