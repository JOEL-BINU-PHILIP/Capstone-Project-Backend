package com.app.service_catalog. controller;

import com.app. service_catalog.dto.request. CreateCategoryRequest;
import com. app.service_catalog.dto. request.ReorderCategoryRequest;
import com.app.service_catalog.dto.request.UpdateCategoryRequest;
import com.app.service_catalog.dto.request.UpdateCategoryStatusRequest;
import com. app.service_catalog.model.ServiceCategory;
import com. app.service_catalog.service. CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j. Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost. PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/services/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    // =====================
    // ADMIN ONLY
    // =====================

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ServiceCategory> create(@Valid @RequestBody CreateCategoryRequest request) {
        log.info("Creating category: {}", request.getName());

        ServiceCategory category = ServiceCategory.builder()
                .name(request.getName())
                .description(request.getDescription())
                .iconUrl(request.getIconUrl())
                .displayOrder(request.getDisplayOrder())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(categoryService.createCategory(category));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ServiceCategory> update(
            @PathVariable("id") String id,
            @RequestBody UpdateCategoryRequest request) {
        log.info("Updating category: {}", id);
        return ResponseEntity.ok(categoryService.updateCategory(id, request));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ServiceCategory> updateStatus(
            @PathVariable("id") String id,
            @RequestBody UpdateCategoryStatusRequest request) {
        log.info("Updating category {} status to: {}", id, request.isActive());
        return ResponseEntity. ok(categoryService.updateCategoryStatus(id, request.isActive()));
    }

    @PutMapping("/reorder")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> reorder(@RequestBody List<ReorderCategoryRequest> requests) {
        log.info("Reordering {} categories", requests.size());
        categoryService.reorderCategories(requests);
        return ResponseEntity. ok().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable("id") String id) {
        log.info("Deleting category: {}", id);
        categoryService.deleteCategory(id);
        return ResponseEntity. noContent().build();
    }

    // =====================
    // PUBLIC - No auth required
    // =====================
    @GetMapping
    public ResponseEntity<List<ServiceCategory>> getAll(
            @RequestParam(value = "active", required = false) Boolean active
    ) {
        log.debug("Getting categories - active: {}", active);

        if (active != null) {
            return ResponseEntity.ok(categoryService. getCategories(active));
        }

        return ResponseEntity.ok(categoryService.getAllCategories());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceCategory> getById(@PathVariable String id) {
        log.debug("Getting category: {}", id);
        return ResponseEntity.ok(categoryService.getCategoryById(id));
    }
}