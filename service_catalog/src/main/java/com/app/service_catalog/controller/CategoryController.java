package com.app.service_catalog.controller;

import com.app.service_catalog.dto.request.CreateCategoryRequest;
import com.app.service_catalog.dto.request.ReorderCategoryRequest;
import com.app.service_catalog.dto.request.UpdateCategoryRequest;
import com.app.service_catalog.dto.request.UpdateCategoryStatusRequest;
import com.app.service_catalog.model.ServiceCategory;
import com.app.service_catalog.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/services/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    // CREATE
    @PostMapping
    public ServiceCategory create(@Valid @RequestBody CreateCategoryRequest request) {

        ServiceCategory category = ServiceCategory.builder()
                .name(request.getName())
                .description(request.getDescription())
                .iconUrl(request.getIconUrl())
                .displayOrder(request.getDisplayOrder())
                .build();

        return categoryService.createCategory(category);
    }

    // READ ALL
    @GetMapping
    public List<ServiceCategory> getAll() {
        return categoryService.getAllCategories();
    }

    // READ ACTIVE
    @GetMapping("/active")
    public List<ServiceCategory> getActive() {
        return categoryService.getActiveCategories();
    }

    // READ BY ID
    @GetMapping("/{id}")
    public ServiceCategory getById(@PathVariable String id) {
        return categoryService.getCategoryById(id);
    }

    @PutMapping("/{id}")
    public ServiceCategory update(
            @PathVariable("id") String id,
            @RequestBody UpdateCategoryRequest request) {

        return categoryService.updateCategory(id, request);
    }


    // TOGGLE STATUS
    @PutMapping("/{id}/status")
    public ServiceCategory updateStatus(
            @PathVariable("id") String id,
            @RequestBody UpdateCategoryStatusRequest request) {

        return categoryService.updateCategoryStatus(id, request.isActive());
    }


    // REORDER
    @PutMapping("/reorder")
    public void reorder(@RequestBody List<ReorderCategoryRequest> requests) {
        categoryService.reorderCategories(requests);
    }

    // DELETE
    @DeleteMapping("/{id}")
    public void delete(@PathVariable("id") String id) {
        categoryService.deleteCategory(id);
    }

}