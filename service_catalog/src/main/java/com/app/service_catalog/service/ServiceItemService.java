package com.app.service_catalog.service;

import com.app.service_catalog.dto.request.CreateServiceRequest;
import com.app.service_catalog.dto.request.UpdateServiceRequest;
import com.app.service_catalog.dto.response.ServiceItemResponse;

import java.util.List;

public interface ServiceItemService {

    // ==================== CREATE, UPDATE, DELETE ====================

    ServiceItemResponse createService(CreateServiceRequest request);

    ServiceItemResponse updateService(String serviceId, UpdateServiceRequest request);

    ServiceItemResponse updateServiceStatus(String serviceId, boolean active);

    void deleteService(String serviceId);

    // ==================== GET SERVICES ====================

    /**
     * Get all services (no filter)
     */
    List<ServiceItemResponse> getAllServices();

    /**
     * Get active services only
     */
    List<ServiceItemResponse> getActiveServices();

    /**
     * Get single service by ID
     */
    ServiceItemResponse getServiceById(String serviceId);

    /**
     * Get services by category
     */
    List<ServiceItemResponse> getServicesByCategory(String categoryId);

    /**
     * Get services with filters
     * @param categoryId Filter by category (can be null)
     * @param active Filter by active status (can be null)
     */
    List<ServiceItemResponse> getServices(String categoryId, Boolean active);

    // ==================== SEARCH ====================

    /**
     * Search services
     */
    List<ServiceItemResponse> search(String query, String skill);
}