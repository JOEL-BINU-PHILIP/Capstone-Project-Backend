package com.app.service_catalog.controller;

import com. app.service_catalog.model.ServiceCategory;
import com.app. service_catalog.model.ServiceItem;
import com.app.service_catalog.repository.ServiceCategoryRepository;
import com.app.service_catalog. repository.ServiceItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java. util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Internal APIs for inter-service communication.
 * These endpoints are called by other microservices (Booking, Billing).
 */
@Slf4j
@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
public class InternalApiController {

    private final ServiceItemRepository serviceItemRepository;
    private final ServiceCategoryRepository serviceCategoryRepository;

    // ==================== SERVICE APIs ====================

    /**
     * Get service details by ID
     * Called by:  Booking Service (when creating booking)
     */
    @GetMapping("/services/{serviceId}")
    public ResponseEntity<Map<String, Object>> getServiceById(
            @PathVariable String serviceId
    ) {
        log.debug("Internal API: Getting service by ID: {}", serviceId);

        Optional<ServiceItem> serviceOpt = serviceItemRepository.findById(serviceId);

        Map<String, Object> response = new HashMap<>();

        if (serviceOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "Service not found");
            response.put("data", null);
            return ResponseEntity.ok(response);
        }

        ServiceItem service = serviceOpt.get();
        Map<String, Object> serviceData = buildServiceData(service);

        response.put("success", true);
        response.put("message", "Service found");
        response.put("data", serviceData);

        return ResponseEntity.ok(response);
    }

    /**
     * Validate if service exists and is active
     * Called by: Booking Service
     */
    @GetMapping("/services/{serviceId}/validate")
    public ResponseEntity<Map<String, Object>> validateService(
            @PathVariable String serviceId
    ) {
        log.debug("Internal API: Validating service: {}", serviceId);

        Optional<ServiceItem> serviceOpt = serviceItemRepository.findById(serviceId);

        Map<String, Object> response = new HashMap<>();
        Map<String, Object> result = new HashMap<>();

        if (serviceOpt.isEmpty()) {
            result.put("exists", false);
            result.put("active", false);
            result.put("canBook", false);
            response.put("success", true);
            response.put("message", "Service not found");
            response.put("data", result);
            return ResponseEntity.ok(response);
        }

        ServiceItem service = serviceOpt. get();
        boolean isActive = service.isActive();

        result.put("exists", true);
        result.put("active", isActive);
        result.put("canBook", isActive);
        result.put("serviceId", service.getId());
        result.put("serviceName", service.getName());

        response.put("success", true);
        response.put("message", "Service validation complete");
        response.put("data", result);

        return ResponseEntity.ok(response);
    }

    /**
     * Get service pricing details
     * Called by: Booking Service, Billing Service
     */
    @GetMapping("/services/{serviceId}/pricing")
    public ResponseEntity<Map<String, Object>> getServicePricing(
            @PathVariable String serviceId
    ) {
        log.debug("Internal API: Getting pricing for service: {}", serviceId);

        Optional<ServiceItem> serviceOpt = serviceItemRepository.findById(serviceId);

        Map<String, Object> response = new HashMap<>();

        if (serviceOpt.isEmpty()) {
            response.put("success", false);
            response. put("message", "Service not found");
            response.put("data", null);
            return ResponseEntity.ok(response);
        }

        ServiceItem service = serviceOpt.get();
        Map<String, Object> pricingData = buildPricingData(service);

        response. put("success", true);
        response.put("message", "Pricing retrieved");
        response.put("data", pricingData);

        return ResponseEntity.ok(response);
    }

    /**
     * Get full service details for booking
     * Called by:  Booking Service (includes all info needed for booking)
     */
    @GetMapping("/services/{serviceId}/for-booking")
    public ResponseEntity<Map<String, Object>> getServiceForBooking(
            @PathVariable String serviceId
    ) {
        log.debug("Internal API: Getting service for booking: {}", serviceId);

        Optional<ServiceItem> serviceOpt = serviceItemRepository.findById(serviceId);

        Map<String, Object> response = new HashMap<>();

        if (serviceOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "Service not found");
            response.put("data", null);
            return ResponseEntity. ok(response);
        }

        ServiceItem service = serviceOpt.get();

        if (! service.isActive()) {
            response.put("success", false);
            response.put("message", "Service is not active");
            response.put("data", null);
            return ResponseEntity.ok(response);
        }

        Map<String, Object> bookingData = new HashMap<>();
        bookingData. put("serviceId", service.getId());
        bookingData.put("serviceName", service.getName());
        bookingData.put("description", service.getDescription());
        bookingData.put("categoryId", service.getCategoryId());
        bookingData.put("categoryName", service.getCategoryName());
        bookingData.put("basePrice", service.getBasePrice());
        bookingData.put("currency", service.getCurrency());
        bookingData.put("estimatedDurationMinutes", service.getEstimatedDurationMinutes());
        bookingData.put("taxPercentage", service.getTaxPercentage());
        bookingData.put("discountPercentage", service.getDiscountPercentage());
        bookingData.put("discountValidUntil", service.getDiscountValidUntil());
        bookingData.put("requiredSkills", service.getRequiredSkills());

        // Calculate final price
        double basePrice = service.getBasePrice();
        double taxAmount = basePrice * (service.getTaxPercentage() / 100);
        double discountAmount = 0;

        // Check if discount is still valid
        if (service.getDiscountPercentage() > 0 && service.getDiscountValidUntil() != null) {
            if (service.getDiscountValidUntil().isAfter(LocalDateTime.now())) {
                discountAmount = basePrice * (service.getDiscountPercentage() / 100);
            }
        }

        double finalPrice = basePrice + taxAmount - discountAmount;

        bookingData.put("taxAmount", taxAmount);
        bookingData.put("discountAmount", discountAmount);
        bookingData.put("finalPrice", finalPrice);

        response.put("success", true);
        response.put("message", "Service data for booking retrieved");
        response.put("data", bookingData);

        return ResponseEntity.ok(response);
    }

    // ==================== CATEGORY APIs ====================

    /**
     * Get category details by ID
     * Called by: Booking Service
     */
    @GetMapping("/categories/{categoryId}")
    public ResponseEntity<Map<String, Object>> getCategoryById(
            @PathVariable String categoryId
    ) {
        log.debug("Internal API: Getting category by ID: {}", categoryId);

        Optional<ServiceCategory> categoryOpt = serviceCategoryRepository.findById(categoryId);

        Map<String, Object> response = new HashMap<>();

        if (categoryOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "Category not found");
            response.put("data", null);
            return ResponseEntity.ok(response);
        }

        ServiceCategory category = categoryOpt.get();
        Map<String, Object> categoryData = new HashMap<>();
        categoryData.put("id", category.getId());
        categoryData.put("name", category.getName());
        categoryData.put("description", category.getDescription());
        categoryData.put("iconUrl", category.getIconUrl());
        categoryData.put("active", category.isActive());

        response.put("success", true);
        response.put("message", "Category found");
        response.put("data", categoryData);

        return ResponseEntity.ok(response);
    }

    // ==================== HELPER METHODS ====================

    private Map<String, Object> buildServiceData(ServiceItem service) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", service.getId());
        data.put("name", service.getName());
        data.put("description", service.getDescription());
        data.put("categoryId", service.getCategoryId());
        data.put("categoryName", service.getCategoryName());
        data.put("basePrice", service.getBasePrice());
        data.put("currency", service. getCurrency());
        data.put("estimatedDurationMinutes", service.getEstimatedDurationMinutes());
        data.put("imageUrl", service.getImageUrl());
        data.put("active", service.isActive());
        data.put("requiredSkills", service.getRequiredSkills());
        data.put("taxPercentage", service.getTaxPercentage());
        data.put("discountPercentage", service.getDiscountPercentage());
        data.put("discountValidUntil", service.getDiscountValidUntil());
        return data;
    }

    private Map<String, Object> buildPricingData(ServiceItem service) {
        Map<String, Object> data = new HashMap<>();
        double basePrice = service.getBasePrice();
        double taxPercentage = service.getTaxPercentage();
        double discountPercentage = service.getDiscountPercentage();

        double taxAmount = basePrice * (taxPercentage / 100);
        double discountAmount = 0;

        // Check if discount is still valid
        if (discountPercentage > 0 && service.getDiscountValidUntil() != null) {
            if (service.getDiscountValidUntil().isAfter(LocalDateTime.now())) {
                discountAmount = basePrice * (discountPercentage / 100);
            }
        }

        double finalPrice = basePrice + taxAmount - discountAmount;

        data.put("serviceId", service.getId());
        data.put("serviceName", service.getName());
        data.put("basePrice", basePrice);
        data.put("currency", service.getCurrency());
        data.put("taxPercentage", taxPercentage);
        data.put("taxAmount", taxAmount);
        data.put("discountPercentage", discountPercentage);
        data.put("discountAmount", discountAmount);
        data.put("discountValidUntil", service. getDiscountValidUntil());
        data.put("finalPrice", finalPrice);
        data.put("estimatedDurationMinutes", service. getEstimatedDurationMinutes());

        return data;
    }
}