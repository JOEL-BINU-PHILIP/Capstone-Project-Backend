package com.app.service_catalog.service;

import com.app.service_catalog.dto.request.ReorderCategoryRequest;
import com.app.service_catalog.dto.request.UpdateCategoryRequest;
import com.  app.service_catalog.model.ServiceCategory;

import java.util. List;

public interface CategoryService {

    // ==================== CREATE, UPDATE, DELETE ====================

    ServiceCategory createCategory(ServiceCategory category);

    ServiceCategory updateCategory(String id, UpdateCategoryRequest request);

    ServiceCategory updateCategoryStatus(String id, boolean active);

    void reorderCategories(List<ReorderCategoryRequest> requests);

    void deleteCategory(String id);

    // ==================== GET CATEGORIES ====================

    /**
     * Get all categories
     */
    List<ServiceCategory> getAllCategories();

    /**
     * Get active categories only
     */
    List<ServiceCategory> getActiveCategories();

    /**
     * Get single category by ID
     */
    ServiceCategory getCategoryById(String id);

    /**
     * Get categories with optional active filter
     * @param active Filter by active status (can be null for all)
     */
    List<ServiceCategory> getCategories(Boolean active);
}