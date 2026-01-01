package com.app.booking.client;

import com.app.booking. client.fallback.CatalogServiceFallback;
import com.app.booking.config.FeignConfig;
import com.app.booking.dto.external.ApiResponseWrapper;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org. springframework.web.bind.annotation. PathVariable;

import java.util.Map;

@FeignClient(
        name = "catalog-service",
        url = "${services.catalog-service.url:http://localhost:8082}",
        configuration = FeignConfig.class,
        fallback = CatalogServiceFallback.class
)
public interface CatalogServiceClient {

    /**
     * Get service details by ID
     */
    @GetMapping("/api/internal/services/{serviceId}")
    Map<String, Object> getServiceById(@PathVariable("serviceId") String serviceId);

    /**
     * Validate if service exists and is active
     */
    @GetMapping("/api/internal/services/{serviceId}/validate")
    Map<String, Object> validateService(@PathVariable("serviceId") String serviceId);

    /**
     * Get service pricing details
     */
    @GetMapping("/api/internal/services/{serviceId}/pricing")
    Map<String, Object> getServicePricing(@PathVariable("serviceId") String serviceId);

    /**
     * Get full service details for booking
     */
    @GetMapping("/api/internal/services/{serviceId}/for-booking")
    Map<String, Object> getServiceForBooking(@PathVariable("serviceId") String serviceId);

    /**
     * Get category details by ID
     */
    @GetMapping("/api/internal/categories/{categoryId}")
    Map<String, Object> getCategoryById(@PathVariable("categoryId") String categoryId);
}