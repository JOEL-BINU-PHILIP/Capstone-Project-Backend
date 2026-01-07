package com.app.service_catalog.service.impl;

import com.app.service_catalog.dto.request.ReorderCategoryRequest;
import com.app.service_catalog.dto.request.UpdateCategoryRequest;
import com.app.service_catalog.exception.ResourceNotFoundException;
import com.app.service_catalog.model.ServiceCategory;
import com.app.service_catalog.repository.ServiceCategoryRepository;
import com.app.service_catalog.repository.ServiceItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private ServiceCategoryRepository categoryRepository;

    @Mock
    private ServiceItemRepository serviceItemRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private ServiceCategory testCategory;

    @BeforeEach
    void setUp() {
        testCategory = ServiceCategory.builder()
                .id("category123")
                .name("HVAC")
                .description("Heating, Ventilation, and Air Conditioning")
                .iconUrl("https://example.com/hvac-icon.png")
                .displayOrder(1)
                .active(true)
                .servicesCount(5)
                .createdAt(Instant.now())
                .build();
    }

    // ==================== CREATE TESTS ====================

    @Test
    void createCategory_ShouldReturnCreatedCategory() {
        ServiceCategory newCategory = ServiceCategory.builder()
                .name("Plumbing")
                .description("Plumbing services")
                .iconUrl("https://example.com/plumbing-icon.png")
                .displayOrder(2)
                .build();

        ServiceCategory savedCategory = ServiceCategory.builder()
                .id("category456")
                .name("Plumbing")
                .description("Plumbing services")
                .iconUrl("https://example.com/plumbing-icon.png")
                .displayOrder(2)
                .active(true)
                .servicesCount(0)
                .createdAt(Instant.now())
                .build();

        when(categoryRepository.save(any(ServiceCategory.class))).thenReturn(savedCategory);

        ServiceCategory result = categoryService.createCategory(newCategory);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("category456");
        assertThat(result.getName()).isEqualTo("Plumbing");
        assertThat(result.isActive()).isTrue();
        assertThat(result.getServicesCount()).isEqualTo(0);
        verify(categoryRepository).save(any(ServiceCategory.class));
    }

    // ==================== UPDATE TESTS ====================

    @Test
    void updateCategory_ShouldReturnUpdatedCategory_WhenAllFieldsProvided() {
        UpdateCategoryRequest request = new UpdateCategoryRequest();
        request.setName("HVAC Updated");
        request.setDescription("Updated description");
        request.setIconUrl("https://example.com/hvac-updated.png");
        request.setDisplayOrder(5);

        when(categoryRepository.findById("category123")).thenReturn(Optional.of(testCategory));
        when(categoryRepository.save(any(ServiceCategory.class))).thenReturn(testCategory);

        ServiceCategory result = categoryService.updateCategory("category123", request);

        assertThat(result).isNotNull();
        verify(categoryRepository).findById("category123");
        verify(categoryRepository).save(any(ServiceCategory.class));
    }

    @Test
    void updateCategory_ShouldUpdateOnlyProvidedFields() {
        UpdateCategoryRequest request = new UpdateCategoryRequest();
        request.setName("HVAC Updated");
        // Other fields are null

        when(categoryRepository.findById("category123")).thenReturn(Optional.of(testCategory));
        when(categoryRepository.save(any(ServiceCategory.class))).thenReturn(testCategory);

        categoryService.updateCategory("category123", request);

        verify(categoryRepository).save(argThat(category ->
            "HVAC Updated".equals(category.getName()) &&
            "Heating, Ventilation, and Air Conditioning".equals(category.getDescription())
        ));
    }

    @Test
    void updateCategory_ShouldThrowException_WhenCategoryNotFound() {
        UpdateCategoryRequest request = new UpdateCategoryRequest();
        request.setName("Updated");

        when(categoryRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.updateCategory("nonexistent", request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Category not found");
    }

    @Test
    void updateCategoryStatus_ShouldDeactivateCategory() {
        when(categoryRepository.findById("category123")).thenReturn(Optional.of(testCategory));
        when(categoryRepository.save(any(ServiceCategory.class))).thenAnswer(invocation -> {
            ServiceCategory cat = invocation.getArgument(0);
            return cat;
        });

        ServiceCategory result = categoryService.updateCategoryStatus("category123", false);

        assertThat(result.isActive()).isFalse();
        verify(categoryRepository).save(argThat(category -> !category.isActive()));
    }

    @Test
    void updateCategoryStatus_ShouldActivateCategory() {
        testCategory.setActive(false);
        when(categoryRepository.findById("category123")).thenReturn(Optional.of(testCategory));
        when(categoryRepository.save(any(ServiceCategory.class))).thenAnswer(invocation -> {
            ServiceCategory cat = invocation.getArgument(0);
            return cat;
        });

        ServiceCategory result = categoryService.updateCategoryStatus("category123", true);

        assertThat(result.isActive()).isTrue();
        verify(categoryRepository).save(argThat(ServiceCategory::isActive));
    }

    @Test
    void reorderCategories_ShouldUpdateDisplayOrder() {
        ServiceCategory cat1 = ServiceCategory.builder().id("cat1").displayOrder(1).build();
        ServiceCategory cat2 = ServiceCategory.builder().id("cat2").displayOrder(2).build();
        ServiceCategory cat3 = ServiceCategory.builder().id("cat3").displayOrder(3).build();

        List<ReorderCategoryRequest> requests = Arrays.asList(
                createReorderRequest("cat1", 3),
                createReorderRequest("cat2", 1),
                createReorderRequest("cat3", 2)
        );

        when(categoryRepository.findById("cat1")).thenReturn(Optional.of(cat1));
        when(categoryRepository.findById("cat2")).thenReturn(Optional.of(cat2));
        when(categoryRepository.findById("cat3")).thenReturn(Optional.of(cat3));
        when(categoryRepository.save(any(ServiceCategory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        categoryService.reorderCategories(requests);

        verify(categoryRepository, times(3)).save(any(ServiceCategory.class));
    }

    // ==================== DELETE TESTS ====================

    @Test
    void deleteCategory_ShouldDelete_WhenNoServicesAssociated() {
        when(categoryRepository.findById("category123")).thenReturn(Optional.of(testCategory));
        when(serviceItemRepository.findByCategoryId("category123")).thenReturn(Collections.emptyList());
        doNothing().when(categoryRepository).delete(testCategory);

        categoryService.deleteCategory("category123");

        verify(categoryRepository).delete(testCategory);
    }

    @Test
    void deleteCategory_ShouldThrowException_WhenServicesAssociated() {
        when(categoryRepository.findById("category123")).thenReturn(Optional.of(testCategory));
        when(serviceItemRepository.findByCategoryId("category123")).thenReturn(Arrays.asList(mock(com.app.service_catalog.model.ServiceItem.class)));

        assertThatThrownBy(() -> categoryService.deleteCategory("category123"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot delete category with");
    }

    @Test
    void deleteCategory_ShouldThrowException_WhenCategoryNotFound() {
        when(categoryRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.deleteCategory("nonexistent"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Category not found");
    }

    // ==================== GET TESTS ====================

    @Test
    void getAllCategories_ShouldReturnAllCategories() {
        List<ServiceCategory> categories = Arrays.asList(testCategory);
        when(categoryRepository.findAll()).thenReturn(categories);

        List<ServiceCategory> result = categoryService.getAllCategories();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("category123");
        verify(categoryRepository).findAll();
    }

    @Test
    void getActiveCategories_ShouldReturnOnlyActiveCategories() {
        List<ServiceCategory> activeCategories = Arrays.asList(testCategory);
        when(categoryRepository.findByActiveTrueOrderByDisplayOrderAsc()).thenReturn(activeCategories);

        List<ServiceCategory> result = categoryService.getActiveCategories();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isActive()).isTrue();
        verify(categoryRepository).findByActiveTrueOrderByDisplayOrderAsc();
    }

    @Test
    void getCategoryById_ShouldReturnCategory_WhenFound() {
        when(categoryRepository.findById("category123")).thenReturn(Optional.of(testCategory));

        ServiceCategory result = categoryService.getCategoryById("category123");

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("category123");
        verify(categoryRepository).findById("category123");
    }

    @Test
    void getCategoryById_ShouldThrowException_WhenNotFound() {
        when(categoryRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getCategoryById("nonexistent"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Category not found");
    }

    @Test
    void getCategories_ShouldReturnActiveCategories_WhenActiveIsTrue() {
        List<ServiceCategory> activeCategories = Arrays.asList(testCategory);
        when(categoryRepository.findByActive(true)).thenReturn(activeCategories);

        List<ServiceCategory> result = categoryService.getCategories(true);

        assertThat(result).hasSize(1);
        verify(categoryRepository).findByActive(true);
    }

    @Test
    void getCategories_ShouldReturnInactiveCategories_WhenActiveIsFalse() {
        testCategory.setActive(false);
        List<ServiceCategory> inactiveCategories = Arrays.asList(testCategory);
        when(categoryRepository.findByActive(false)).thenReturn(inactiveCategories);

        List<ServiceCategory> result = categoryService.getCategories(false);

        assertThat(result).hasSize(1);
        verify(categoryRepository).findByActive(false);
    }

    @Test
    void getCategories_ShouldReturnAllCategories_WhenActiveIsNull() {
        List<ServiceCategory> allCategories = Arrays.asList(testCategory);
        when(categoryRepository.findAll()).thenReturn(allCategories);

        List<ServiceCategory> result = categoryService.getCategories(null);

        assertThat(result).hasSize(1);
        verify(categoryRepository).findAll();
    }

    // ==================== HELPER METHODS ====================

    private ReorderCategoryRequest createReorderRequest(String id, int order) {
        ReorderCategoryRequest request = new ReorderCategoryRequest();
        request.setId(id);
        request.setDisplayOrder(order);
        return request;
    }
}

