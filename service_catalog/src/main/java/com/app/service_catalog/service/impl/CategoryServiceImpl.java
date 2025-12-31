package com.app.service_catalog.service.impl;

import com.app.service_catalog.dto.request.ReorderCategoryRequest;
import com.app.service_catalog.dto.request.UpdateCategoryRequest;
import com.app.service_catalog.model.ServiceCategory;
import com.app.service_catalog.repository.ServiceCategoryRepository;
import com.app.service_catalog.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final ServiceCategoryRepository repository;

    @Override
    public ServiceCategory createCategory(ServiceCategory category) {

        // BLOCK DUPLICATES
        if (repository.existsByNameIgnoreCase(category.getName())) {
            throw new RuntimeException("Category with this name already exists");
        }

        category.setActive(true);
        category.setServicesCount(0);
        category.setCreatedAt(Instant.now());

        return repository.save(category);
    }

    @Override
    public List<ServiceCategory> getAllCategories() {
        return repository.findAll();
    }

    @Override
    public List<ServiceCategory> getActiveCategories() {
        return repository.findByActiveTrueOrderByDisplayOrderAsc();
    }

    @Override
    public ServiceCategory getCategoryById(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));
    }

    @Override
    public void reorderCategories(List<ReorderCategoryRequest> requests) {

        for (ReorderCategoryRequest req : requests) {
            ServiceCategory category = repository.findById(req.getId())
                    .orElseThrow(() -> new RuntimeException("Category not found"));

            category.setDisplayOrder(req.getDisplayOrder());
            repository.save(category);
        }
    }

    @Override
    public ServiceCategory updateCategory(String id, UpdateCategoryRequest request) {

        ServiceCategory category = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        // 🚫 Prevent renaming to an existing category
        if (request.getName() != null &&
                !request.getName().equalsIgnoreCase(category.getName()) &&
                repository.existsByNameIgnoreCase(request.getName())) {

            throw new RuntimeException("Category with this name already exists");
        }

        if (request.getName() != null)
            category.setName(request.getName());
        if (request.getDescription() != null)
            category.setDescription(request.getDescription());
        if (request.getIconUrl() != null)
            category.setIconUrl(request.getIconUrl());
        if (request.getDisplayOrder() != null)
            category.setDisplayOrder(request.getDisplayOrder());

        return repository.save(category);
    }

    @Override
    public ServiceCategory updateCategoryStatus(String id, boolean active) {

        ServiceCategory category = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        category.setActive(active);
        return repository.save(category);
    }

    @Override
    public void deleteCategory(String id) {

        ServiceCategory category = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        if (category.getServicesCount() > 0) {
            throw new RuntimeException("Cannot delete category with services");
        }

        repository.delete(category);
    }
}
