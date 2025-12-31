package com.app.service_catalog.service;

import com.app.service_catalog.dto.request.ReorderCategoryRequest;
import com.app.service_catalog.dto.request.UpdateCategoryRequest;
import com.app.service_catalog.model.ServiceCategory;

import java.util.List;

public interface CategoryService {

    ServiceCategory createCategory(ServiceCategory category);

    List<ServiceCategory> getAllCategories();

    List<ServiceCategory> getActiveCategories();

    ServiceCategory getCategoryById(String id);

    void reorderCategories(List<ReorderCategoryRequest> requests);

    ServiceCategory updateCategory(String id, UpdateCategoryRequest request);

    ServiceCategory updateCategoryStatus(String id, boolean active);

    void deleteCategory(String id);

}

