package com.app.service_catalog. controller;

import com.app. service_catalog.dto.request. CreateServiceRequest;
import com.app.service_catalog.dto.request.UpdateServiceRequest;
import com.app.service_catalog.dto.response.ServiceItemResponse;
import com.app. service_catalog.service.ServiceItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j. Slf4j;
import org. springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework. security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
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
        log.info("Creating service: {}", request.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(serviceItemService.createService(request));
    }

    @PutMapping("/{serviceId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ServiceItemResponse> update(
            @PathVariable("serviceId") String serviceId,
            @RequestBody UpdateServiceRequest request) {
        log.info("Updating service: {}", serviceId);
        return ResponseEntity.ok(serviceItemService.updateService(serviceId, request));
    }

    @PutMapping("/{serviceId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ServiceItemResponse> updateStatus(
            @PathVariable("serviceId") String serviceId,
            @RequestParam("active") boolean active) {
        log.info("Updating service {} status to: {}", serviceId, active);
        return ResponseEntity. ok(serviceItemService.updateServiceStatus(serviceId, active));
    }

    @DeleteMapping("/{serviceId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable("serviceId") String serviceId) {
        log.info("Deleting service: {}", serviceId);
        serviceItemService.deleteService(serviceId);
        return ResponseEntity.noContent().build();
    }

    // =====================
    // PUBLIC - No auth required
    // =====================


    @GetMapping
    public ResponseEntity<List<ServiceItemResponse>> getAll(
            @RequestParam(value = "categoryId", required = false) String categoryId,
            @RequestParam(value = "active", required = false) Boolean active
    ) {
        log.debug("Getting services - categoryId: {}, active: {}", categoryId, active);

        // If filters provided, use filtered method
        if (categoryId != null || active != null) {
            return ResponseEntity.ok(serviceItemService.getServices(categoryId, active));
        }

        return ResponseEntity.ok(serviceItemService.getAllServices());
    }

    @GetMapping("/{serviceId}")
    public ResponseEntity<ServiceItemResponse> getById(@PathVariable("serviceId") String serviceId) {
        log.debug("Getting service: {}", serviceId);
        return ResponseEntity. ok(serviceItemService.getServiceById(serviceId));
    }

    @GetMapping("/search")
    public ResponseEntity<List<ServiceItemResponse>> search(
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "skill", required = false) String skill) {
        log.debug("Searching services - query: {}, skill: {}", query, skill);
        return ResponseEntity.ok(serviceItemService. search(query, skill));
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<ServiceItemResponse>> getByCategory(
            @PathVariable("categoryId") String categoryId,
            @RequestParam(value = "active", required = false) Boolean active) {
        log.debug("Getting services for category: {}, active: {}", categoryId, active);
        return ResponseEntity.ok(serviceItemService.getServices(categoryId, active));
    }
}