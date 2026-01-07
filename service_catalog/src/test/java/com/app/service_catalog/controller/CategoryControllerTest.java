package com.app.service_catalog.controller;

import com.app.service_catalog.dto.request.CreateCategoryRequest;
import com.app.service_catalog.dto.request.ReorderCategoryRequest;
import com.app.service_catalog.dto.request.UpdateCategoryRequest;
import com.app.service_catalog.dto.request.UpdateCategoryStatusRequest;
import com.app.service_catalog.model.ServiceCategory;
import com.app.service_catalog.service.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryControllerTest {

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private CategoryController categoryController;

    private ServiceCategory testCategory;
    private CreateCategoryRequest createRequest;
    private UpdateCategoryRequest updateRequest;

    @BeforeEach
    void setUp() {
        testCategory = ServiceCategory.builder()
                .id("category123")
                .name("HVAC")
                .description("Heating, Ventilation, and Air Conditioning services")
                .iconUrl("https://example.com/hvac-icon.png")
                .displayOrder(1)
                .active(true)
                .servicesCount(5)
                .createdAt(Instant.now())
                .build();

        createRequest = new CreateCategoryRequest();
        createRequest.setName("HVAC");
        createRequest.setDescription("Heating, Ventilation, and Air Conditioning services");
        createRequest.setIconUrl("https://example.com/hvac-icon.png");
        createRequest.setDisplayOrder(1);

        updateRequest = new UpdateCategoryRequest();
        updateRequest.setName("HVAC Updated");
        updateRequest.setDescription("Updated description");
    }

    // ==================== CREATE TESTS ====================

    @Test
    void create_ShouldReturnCategoryId_WhenValidRequest() {
        when(categoryService.createCategory(any(ServiceCategory.class))).thenReturn(testCategory);

        ResponseEntity<String> response = categoryController.create(createRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo("category123");
        verify(categoryService).createCategory(any(ServiceCategory.class));
    }

    // ==================== UPDATE TESTS ====================

    @Test
    void update_ShouldReturnUpdatedCategory() {
        when(categoryService.updateCategory(eq("category123"), any(UpdateCategoryRequest.class)))
                .thenReturn(testCategory);

        ResponseEntity<ServiceCategory> response = categoryController.update("category123", updateRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo("category123");
        verify(categoryService).updateCategory(eq("category123"), any(UpdateCategoryRequest.class));
    }

    @Test
    void updateStatus_ShouldReturnUpdatedCategory() {
        UpdateCategoryStatusRequest statusRequest = new UpdateCategoryStatusRequest();
        statusRequest.setActive(false);
        testCategory.setActive(false);

        when(categoryService.updateCategoryStatus("category123", false)).thenReturn(testCategory);

        ResponseEntity<ServiceCategory> response = categoryController.updateStatus("category123", statusRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isActive()).isFalse();
        verify(categoryService).updateCategoryStatus("category123", false);
    }

    @Test
    void reorder_ShouldReturnOk() {
        List<ReorderCategoryRequest> requests = Arrays.asList(
                createReorderRequest("cat1", 1),
                createReorderRequest("cat2", 2),
                createReorderRequest("cat3", 3)
        );

        doNothing().when(categoryService).reorderCategories(anyList());

        ResponseEntity<Void> response = categoryController.reorder(requests);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(categoryService).reorderCategories(requests);
    }

    // ==================== DELETE TESTS ====================

    @Test
    void delete_ShouldReturnNoContent() {
        doNothing().when(categoryService).deleteCategory("category123");

        ResponseEntity<Void> response = categoryController.delete("category123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(categoryService).deleteCategory("category123");
    }

    // ==================== GET TESTS ====================

    @Test
    void getAll_ShouldReturnAllCategories_WhenNoFilter() {
        List<ServiceCategory> categories = Arrays.asList(testCategory);
        when(categoryService.getAllCategories()).thenReturn(categories);

        ResponseEntity<List<ServiceCategory>> response = categoryController.getAll(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        verify(categoryService).getAllCategories();
    }

    @Test
    void getAll_ShouldReturnActiveCategories_WhenActiveFilterTrue() {
        List<ServiceCategory> categories = Arrays.asList(testCategory);
        when(categoryService.getCategories(true)).thenReturn(categories);

        ResponseEntity<List<ServiceCategory>> response = categoryController.getAll(true);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        verify(categoryService).getCategories(true);
    }

    @Test
    void getAll_ShouldReturnInactiveCategories_WhenActiveFilterFalse() {
        testCategory.setActive(false);
        List<ServiceCategory> categories = Arrays.asList(testCategory);
        when(categoryService.getCategories(false)).thenReturn(categories);

        ResponseEntity<List<ServiceCategory>> response = categoryController.getAll(false);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        verify(categoryService).getCategories(false);
    }

    @Test
    void getById_ShouldReturnCategory() {
        when(categoryService.getCategoryById("category123")).thenReturn(testCategory);

        ResponseEntity<ServiceCategory> response = categoryController.getById("category123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo("category123");
        assertThat(response.getBody().getName()).isEqualTo("HVAC");
        verify(categoryService).getCategoryById("category123");
    }

    // ==================== HELPER METHODS ====================

    private ReorderCategoryRequest createReorderRequest(String id, int order) {
        ReorderCategoryRequest request = new ReorderCategoryRequest();
        request.setId(id);
        request.setDisplayOrder(order);
        return request;
    }
}

