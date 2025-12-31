package com.app.service_catalog.controller;

import com.app.service_catalog.dto.request.CreateServiceRequest;
import com.app.service_catalog.dto.request.UpdateServiceRequest;
import com.app.service_catalog.dto.response.ServiceItemResponse;
import com.app.service_catalog.service.ServiceItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
public class ServiceController {

    private final ServiceItemService serviceItemService;

    @PostMapping
    public ServiceItemResponse create(@RequestBody CreateServiceRequest request) {
        return serviceItemService.createService(request);
    }

    @GetMapping
    public List<ServiceItemResponse> getAll() {
        return serviceItemService.getAllServices();
    }

    // ✅ ADD THIS
    @GetMapping("/active")
    public List<ServiceItemResponse> getActiveServices() {
        return serviceItemService.getActiveServices();
    }

    @GetMapping("/{serviceId}")
    public ServiceItemResponse getById(
            @PathVariable("serviceId") String serviceId
    ) {
        return serviceItemService.getServiceById(serviceId);
    }

    @PutMapping("/{serviceId}")
    public ServiceItemResponse update(
            @PathVariable("serviceId") String serviceId,
            @RequestBody UpdateServiceRequest request
    ) {
        return serviceItemService.updateService(serviceId, request);
    }

    @PutMapping("/{serviceId}/status")
    public ServiceItemResponse updateStatus(
            @PathVariable("serviceId") String serviceId,
            @RequestParam("active") boolean active
    ) {
        return serviceItemService.updateServiceStatus(serviceId, active);
    }

    @DeleteMapping("/{serviceId}")
    public void delete(@PathVariable("serviceId") String serviceId) {
        serviceItemService.deleteService(serviceId);
    }

    @GetMapping("/search")
    public List<ServiceItemResponse> search(
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "skill", required = false) String skill
    ) {
        return serviceItemService.search(query, skill);
    }
}
