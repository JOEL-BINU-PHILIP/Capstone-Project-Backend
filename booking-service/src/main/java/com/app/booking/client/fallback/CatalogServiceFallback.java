package com.app.booking.client. fallback;

import com.app.booking.client.CatalogServiceClient;
import lombok.extern.slf4j. Slf4j;
import org. springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Fallback class for Catalog Service when circuit breaker is open
 * or service is unavailable
 */
@Slf4j
@Component
public class CatalogServiceFallback implements CatalogServiceClient {

    @Override
    public Map<String, Object> getServiceById(String serviceId) {
        log.warn("FALLBACK: Catalog service unavailable - getServiceById({})", serviceId);
        return createFallbackResponse("Catalog service unavailable");
    }

    @Override
    public Map<String, Object> validateService(String serviceId) {
        log.warn("FALLBACK: Catalog service unavailable - validateService({})", serviceId);
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "Catalog service unavailable - fallback response");

        Map<String, Object> data = new HashMap<>();
        data.put("exists", false);
        data.put("active", false);
        data.put("canBook", false);
        data.put("fallback", true);
        response.put("data", data);

        return response;
    }

    @Override
    public Map<String, Object> getServicePricing(String serviceId) {
        log.warn("FALLBACK: Catalog service unavailable - getServicePricing({})", serviceId);
        return createFallbackResponse("Catalog service unavailable");
    }

    @Override
    public Map<String, Object> getServiceForBooking(String serviceId) {
        log.warn("FALLBACK: Catalog service unavailable - getServiceForBooking({})", serviceId);
        return createFallbackResponse("Catalog service unavailable");
    }

    @Override
    public Map<String, Object> getCategoryById(String categoryId) {
        log.warn("FALLBACK: Catalog service unavailable - getCategoryById({})", categoryId);
        return createFallbackResponse("Catalog service unavailable");
    }

    private Map<String, Object> createFallbackResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message + " - fallback response");
        response.put("data", null);
        response.put("fallback", true);
        return response;
    }
}