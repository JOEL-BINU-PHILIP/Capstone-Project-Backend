package com. app.service_catalog.service. impl;

import com.app. service_catalog.dto.request. ReorderCategoryRequest;
import com.app.service_catalog.dto.request.UpdateCategoryRequest;
import com.app.service_catalog.exception.ResourceNotFoundException;
import com. app.service_catalog.model. ServiceCategory;
import com.app.service_catalog.repository.ServiceCategoryRepository;
import com. app.service_catalog.repository.ServiceItemRepository;
import com.app.service_catalog.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j. Slf4j;
import org. springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final ServiceCategoryRepository categoryRepository;
    private final ServiceItemRepository serviceItemRepository;

    // ==================== CREATE ====================

    @Override
    public ServiceCategory createCategory(ServiceCategory category) {
        category.setActive(true);
        category.setServicesCount(0);
        category.setCreatedAt(Instant.now());

        ServiceCategory saved = categoryRepository. save(category);
        log.info("Category created: {}", saved.getId());

        return saved;
    }

    // ==================== UPDATE ====================

    @Override
    public ServiceCategory updateCategory(String id, UpdateCategoryRequest request) {
        ServiceCategory category = getCategoryById(id);

        if (request.getName() != null) {
            category.setName(request. getName());
        }
        if (request.getDescription() != null) {
            category.setDescription(request.getDescription());
        }
        if (request.getIconUrl() != null) {
            category.setIconUrl(request. getIconUrl());
        }
        if (request.getDisplayOrder() != null) {
            category.setDisplayOrder(request. getDisplayOrder());
        }

        ServiceCategory saved = categoryRepository. save(category);
        log.info("Category updated: {}", id);

        return saved;
    }

    @Override
    public ServiceCategory updateCategoryStatus(String id, boolean active) {
        ServiceCategory category = getCategoryById(id);
        category.setActive(active);

        ServiceCategory saved = categoryRepository. save(category);
        log.info("Category {} status updated to:  {}", id, active);

        return saved;
    }

    @Override
    public void reorderCategories(List<ReorderCategoryRequest> requests) {
        for (ReorderCategoryRequest request :  requests) {
            ServiceCategory category = getCategoryById(request.getId());
            category.setDisplayOrder(request.getDisplayOrder());
            categoryRepository.save(category);
        }
        log.info("Reordered {} categories", requests.size());
    }

    // ==================== DELETE ====================

    @Override
    public void deleteCategory(String id) {
        ServiceCategory category = getCategoryById(id);

        // Check if category has services
        long serviceCount = serviceItemRepository.findByCategoryId(id).size();
        if (serviceCount > 0) {
            throw new IllegalStateException("Cannot delete category with " + serviceCount + " services.  Remove or reassign services first.");
        }

        categoryRepository.delete(category);
        log.info("Category deleted: {}", id);
    }

    // ==================== GET CATEGORIES ====================

    @Override
    public List<ServiceCategory> getAllCategories() {
        return categoryRepository. findAll();
    }

    @Override
    public List<ServiceCategory> getActiveCategories() {
        return categoryRepository. findByActiveTrueOrderByDisplayOrderAsc();
    }

    @Override
    public ServiceCategory getCategoryById(String id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
    }

    @Override
    public List<ServiceCategory> getCategories(Boolean active) {
        if (active != null) {
            return categoryRepository.findByActive(active);
        }
        return categoryRepository.findAll();
    }
}