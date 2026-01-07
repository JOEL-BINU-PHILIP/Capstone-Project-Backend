package com.app.service_catalog.service.impl;

import com.app.service_catalog.dto.request.CreateServiceRequest;
import com.app.service_catalog.dto.request.UpdateServiceRequest;
import com.app.service_catalog.dto.response.ServiceItemResponse;
import com.app.service_catalog.exception.ResourceNotFoundException;
import com.app.service_catalog.model.ServiceCategory;
import com.app.service_catalog.model.ServiceItem;
import com.app.service_catalog.repository.ServiceCategoryRepository;
import com.app.service_catalog.repository.ServiceItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceItemServiceImplTest {

    @Mock
    private ServiceItemRepository serviceItemRepository;

    @Mock
    private ServiceCategoryRepository categoryRepository;

    @InjectMocks
    private ServiceItemServiceImpl serviceItemService;

    private ServiceItem testServiceItem;
    private ServiceCategory testCategory;
    private CreateServiceRequest createRequest;
    private UpdateServiceRequest updateRequest;

    @BeforeEach
    void setUp() {
        testCategory = ServiceCategory.builder()
                .id("category123")
                .name("HVAC")
                .description("Heating, Ventilation, and Air Conditioning")
                .active(true)
                .servicesCount(5)
                .build();

        testServiceItem = ServiceItem.builder()
                .id("service123")
                .name("AC Repair")
                .description("Air conditioner repair and maintenance")
                .categoryId("category123")
                .categoryName("HVAC")
                .basePrice(1000.0)
                .currency("INR")
                .estimatedDurationMinutes(60)
                .requiredSkills(Arrays.asList("HVAC", "Electrical"))
                .imageUrl("https://example.com/ac-repair.png")
                .active(true)
                .taxPercentage(18.0)
                .discountPercentage(10.0)
                .discountValidUntil(LocalDateTime.now().plusDays(30))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        createRequest = new CreateServiceRequest();
        createRequest.setCategoryId("category123");
        createRequest.setName("AC Repair");
        createRequest.setDescription("Air conditioner repair and maintenance");
        createRequest.setBasePrice(1000.0);
        createRequest.setCurrency("INR");
        createRequest.setEstimatedDurationMinutes(60);
        createRequest.setRequiredSkills(Arrays.asList("HVAC", "Electrical"));
        createRequest.setTaxPercentage(18.0);
        createRequest.setDiscountPercentage(10.0);

        updateRequest = new UpdateServiceRequest();
        updateRequest.setName("AC Repair Updated");
        updateRequest.setDescription("Updated description");
        updateRequest.setBasePrice(1200.0);
    }

    // ==================== CREATE TESTS ====================

    @Test
    void createService_ShouldReturnCreatedService() {
        when(categoryRepository.findById("category123")).thenReturn(Optional.of(testCategory));
        when(serviceItemRepository.save(any(ServiceItem.class))).thenReturn(testServiceItem);
        when(categoryRepository.save(any(ServiceCategory.class))).thenReturn(testCategory);

        ServiceItemResponse result = serviceItemService.createService(createRequest);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("service123");
        assertThat(result.getName()).isEqualTo("AC Repair");
        assertThat(result.getCategoryId()).isEqualTo("category123");
        assertThat(result.getCategoryName()).isEqualTo("HVAC");
        verify(categoryRepository).findById("category123");
        verify(serviceItemRepository).save(any(ServiceItem.class));
        verify(categoryRepository).save(any(ServiceCategory.class)); // Category count updated
    }

    @Test
    void createService_ShouldThrowException_WhenCategoryNotFound() {
        when(categoryRepository.findById("nonexistent")).thenReturn(Optional.empty());
        createRequest.setCategoryId("nonexistent");

        assertThatThrownBy(() -> serviceItemService.createService(createRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Category not found");
    }

    @Test
    void createService_ShouldUseDefaultValues_WhenOptionalFieldsNull() {
        CreateServiceRequest requestWithNulls = new CreateServiceRequest();
        requestWithNulls.setCategoryId("category123");
        requestWithNulls.setName("Basic Service");
        requestWithNulls.setBasePrice(500.0);
        // currency, estimatedDurationMinutes, taxPercentage, discountPercentage are null

        when(categoryRepository.findById("category123")).thenReturn(Optional.of(testCategory));
        when(serviceItemRepository.save(any(ServiceItem.class))).thenAnswer(invocation -> {
            ServiceItem saved = invocation.getArgument(0);
            saved.setId("newService123");
            return saved;
        });
        when(categoryRepository.save(any(ServiceCategory.class))).thenReturn(testCategory);

        ServiceItemResponse result = serviceItemService.createService(requestWithNulls);

        assertThat(result).isNotNull();
        verify(serviceItemRepository).save(argThat(service ->
                "INR".equals(service.getCurrency()) &&
                service.getEstimatedDurationMinutes() == 60 &&
                service.getTaxPercentage() == 18.0 &&
                service.getDiscountPercentage() == 0.0
        ));
    }

    // ==================== UPDATE TESTS ====================

    @Test
    void updateService_ShouldReturnUpdatedService_WhenAllFieldsProvided() {
        UpdateServiceRequest fullUpdateRequest = new UpdateServiceRequest();
        fullUpdateRequest.setName("AC Repair Updated");
        fullUpdateRequest.setDescription("Updated description");
        fullUpdateRequest.setBasePrice(1200.0);
        fullUpdateRequest.setCurrency("USD");
        fullUpdateRequest.setEstimatedDurationMinutes(90);
        fullUpdateRequest.setRequiredSkills(Arrays.asList("HVAC", "Electrical", "Plumbing"));
        fullUpdateRequest.setImageUrl("https://example.com/updated.png");
        fullUpdateRequest.setTaxPercentage(20.0);
        fullUpdateRequest.setDiscountPercentage(15.0);
        fullUpdateRequest.setDiscountValidUntil(LocalDateTime.now().plusDays(60));

        when(serviceItemRepository.findById("service123")).thenReturn(Optional.of(testServiceItem));
        when(serviceItemRepository.save(any(ServiceItem.class))).thenReturn(testServiceItem);

        ServiceItemResponse result = serviceItemService.updateService("service123", fullUpdateRequest);

        assertThat(result).isNotNull();
        verify(serviceItemRepository).findById("service123");
        verify(serviceItemRepository).save(any(ServiceItem.class));
    }

    @Test
    void updateService_ShouldUpdateOnlyProvidedFields() {
        UpdateServiceRequest partialUpdate = new UpdateServiceRequest();
        partialUpdate.setName("Only Name Updated");
        // Other fields are null

        when(serviceItemRepository.findById("service123")).thenReturn(Optional.of(testServiceItem));
        when(serviceItemRepository.save(any(ServiceItem.class))).thenReturn(testServiceItem);

        serviceItemService.updateService("service123", partialUpdate);

        verify(serviceItemRepository).save(argThat(service ->
                "Only Name Updated".equals(service.getName()) &&
                "Air conditioner repair and maintenance".equals(service.getDescription()) &&
                service.getBasePrice() == 1000.0
        ));
    }

    @Test
    void updateService_ShouldThrowException_WhenServiceNotFound() {
        when(serviceItemRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> serviceItemService.updateService("nonexistent", updateRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Service not found");
    }

    @Test
    void updateServiceStatus_ShouldDeactivateService() {
        when(serviceItemRepository.findById("service123")).thenReturn(Optional.of(testServiceItem));
        when(serviceItemRepository.save(any(ServiceItem.class))).thenAnswer(invocation -> {
            ServiceItem saved = invocation.getArgument(0);
            return saved;
        });

        ServiceItemResponse result = serviceItemService.updateServiceStatus("service123", false);

        assertThat(result.isActive()).isFalse();
        verify(serviceItemRepository).save(argThat(service -> !service.isActive()));
    }

    @Test
    void updateServiceStatus_ShouldActivateService() {
        testServiceItem.setActive(false);
        when(serviceItemRepository.findById("service123")).thenReturn(Optional.of(testServiceItem));
        when(serviceItemRepository.save(any(ServiceItem.class))).thenAnswer(invocation -> {
            ServiceItem saved = invocation.getArgument(0);
            return saved;
        });

        ServiceItemResponse result = serviceItemService.updateServiceStatus("service123", true);

        assertThat(result.isActive()).isTrue();
        verify(serviceItemRepository).save(argThat(ServiceItem::isActive));
    }

    // ==================== DELETE TESTS ====================

    @Test
    void deleteService_ShouldDeleteAndUpdateCategoryCount() {
        when(serviceItemRepository.findById("service123")).thenReturn(Optional.of(testServiceItem));
        when(categoryRepository.findById("category123")).thenReturn(Optional.of(testCategory));
        doNothing().when(serviceItemRepository).delete(testServiceItem);
        when(categoryRepository.save(any(ServiceCategory.class))).thenReturn(testCategory);

        serviceItemService.deleteService("service123");

        verify(serviceItemRepository).delete(testServiceItem);
        verify(categoryRepository).save(argThat(category -> category.getServicesCount() == 4));
    }

    @Test
    void deleteService_ShouldThrowException_WhenServiceNotFound() {
        when(serviceItemRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> serviceItemService.deleteService("nonexistent"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Service not found");
    }

    @Test
    void deleteService_ShouldHandleNullCategoryId() {
        testServiceItem.setCategoryId(null);
        when(serviceItemRepository.findById("service123")).thenReturn(Optional.of(testServiceItem));
        doNothing().when(serviceItemRepository).delete(testServiceItem);

        serviceItemService.deleteService("service123");

        verify(serviceItemRepository).delete(testServiceItem);
        verify(categoryRepository, never()).findById(anyString());
    }

    // ==================== GET TESTS ====================

    @Test
    void getAllServices_ShouldReturnAllServices() {
        List<ServiceItem> services = Arrays.asList(testServiceItem);
        when(serviceItemRepository.findAll()).thenReturn(services);

        List<ServiceItemResponse> result = serviceItemService.getAllServices();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("service123");
        verify(serviceItemRepository).findAll();
    }

    @Test
    void getActiveServices_ShouldReturnOnlyActiveServices() {
        List<ServiceItem> activeServices = Arrays.asList(testServiceItem);
        when(serviceItemRepository.findByActive(true)).thenReturn(activeServices);

        List<ServiceItemResponse> result = serviceItemService.getActiveServices();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isActive()).isTrue();
        verify(serviceItemRepository).findByActive(true);
    }

    @Test
    void getServiceById_ShouldReturnService_WhenFound() {
        when(serviceItemRepository.findById("service123")).thenReturn(Optional.of(testServiceItem));

        ServiceItemResponse result = serviceItemService.getServiceById("service123");

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("service123");
        assertThat(result.getName()).isEqualTo("AC Repair");
        verify(serviceItemRepository).findById("service123");
    }

    @Test
    void getServiceById_ShouldThrowException_WhenNotFound() {
        when(serviceItemRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> serviceItemService.getServiceById("nonexistent"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Service not found");
    }

    @Test
    void getServicesByCategory_ShouldReturnServicesForCategory() {
        List<ServiceItem> categoryServices = Arrays.asList(testServiceItem);
        when(serviceItemRepository.findByCategoryId("category123")).thenReturn(categoryServices);

        List<ServiceItemResponse> result = serviceItemService.getServicesByCategory("category123");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCategoryId()).isEqualTo("category123");
        verify(serviceItemRepository).findByCategoryId("category123");
    }

    @Test
    void getServices_ShouldReturnFilteredServices_WhenBothFiltersProvided() {
        List<ServiceItem> filteredServices = Arrays.asList(testServiceItem);
        when(serviceItemRepository.findByCategoryIdAndActive("category123", true)).thenReturn(filteredServices);

        List<ServiceItemResponse> result = serviceItemService.getServices("category123", true);

        assertThat(result).hasSize(1);
        verify(serviceItemRepository).findByCategoryIdAndActive("category123", true);
    }

    @Test
    void getServices_ShouldReturnServicesByCategory_WhenOnlyCategoryProvided() {
        List<ServiceItem> categoryServices = Arrays.asList(testServiceItem);
        when(serviceItemRepository.findByCategoryId("category123")).thenReturn(categoryServices);

        List<ServiceItemResponse> result = serviceItemService.getServices("category123", null);

        assertThat(result).hasSize(1);
        verify(serviceItemRepository).findByCategoryId("category123");
    }

    @Test
    void getServices_ShouldReturnActiveServices_WhenOnlyActiveProvided() {
        List<ServiceItem> activeServices = Arrays.asList(testServiceItem);
        when(serviceItemRepository.findByActive(true)).thenReturn(activeServices);

        List<ServiceItemResponse> result = serviceItemService.getServices(null, true);

        assertThat(result).hasSize(1);
        verify(serviceItemRepository).findByActive(true);
    }

    @Test
    void getServices_ShouldReturnAllServices_WhenNoFiltersProvided() {
        List<ServiceItem> allServices = Arrays.asList(testServiceItem);
        when(serviceItemRepository.findAll()).thenReturn(allServices);

        List<ServiceItemResponse> result = serviceItemService.getServices(null, null);

        assertThat(result).hasSize(1);
        verify(serviceItemRepository).findAll();
    }

    // ==================== SEARCH TESTS ====================

    @Test
    void search_ShouldReturnServices_WhenQueryProvided() {
        List<ServiceItem> searchResults = Arrays.asList(testServiceItem);
        when(serviceItemRepository.findByNameContainingIgnoreCase("AC")).thenReturn(searchResults);

        List<ServiceItemResponse> result = serviceItemService.search("AC", null);

        assertThat(result).hasSize(1);
        verify(serviceItemRepository).findByNameContainingIgnoreCase("AC");
    }

    @Test
    void search_ShouldReturnServices_WhenSkillProvided() {
        List<ServiceItem> searchResults = Arrays.asList(testServiceItem);
        when(serviceItemRepository.findByRequiredSkillsContainingIgnoreCase("HVAC")).thenReturn(searchResults);

        List<ServiceItemResponse> result = serviceItemService.search(null, "HVAC");

        assertThat(result).hasSize(1);
        verify(serviceItemRepository).findByRequiredSkillsContainingIgnoreCase("HVAC");
    }

    @Test
    void search_ShouldReturnIntersection_WhenBothQueryAndSkillProvided() {
        List<ServiceItem> byName = Arrays.asList(testServiceItem);
        List<ServiceItem> bySkill = Arrays.asList(testServiceItem);

        when(serviceItemRepository.findByNameContainingIgnoreCase("AC")).thenReturn(byName);
        when(serviceItemRepository.findByRequiredSkillsContainingIgnoreCase("HVAC")).thenReturn(bySkill);

        List<ServiceItemResponse> result = serviceItemService.search("AC", "HVAC");

        assertThat(result).hasSize(1);
        verify(serviceItemRepository).findByNameContainingIgnoreCase("AC");
        verify(serviceItemRepository).findByRequiredSkillsContainingIgnoreCase("HVAC");
    }

    @Test
    void search_ShouldReturnEmptyList_WhenNoIntersection() {
        ServiceItem anotherService = ServiceItem.builder()
                .id("service456")
                .name("Plumbing Service")
                .requiredSkills(Arrays.asList("Plumbing"))
                .active(true)
                .build();

        List<ServiceItem> byName = Arrays.asList(testServiceItem); // AC Repair
        List<ServiceItem> bySkill = Arrays.asList(anotherService); // Plumbing Service

        when(serviceItemRepository.findByNameContainingIgnoreCase("AC")).thenReturn(byName);
        when(serviceItemRepository.findByRequiredSkillsContainingIgnoreCase("Plumbing")).thenReturn(bySkill);

        List<ServiceItemResponse> result = serviceItemService.search("AC", "Plumbing");

        assertThat(result).isEmpty();
    }

    @Test
    void search_ShouldReturnActiveServices_WhenNoSearchCriteria() {
        List<ServiceItem> activeServices = Arrays.asList(testServiceItem);
        when(serviceItemRepository.findByActive(true)).thenReturn(activeServices);

        List<ServiceItemResponse> result = serviceItemService.search(null, null);

        assertThat(result).hasSize(1);
        verify(serviceItemRepository).findByActive(true);
    }

    @Test
    void search_ShouldReturnActiveServices_WhenEmptyStringsProvided() {
        List<ServiceItem> activeServices = Arrays.asList(testServiceItem);
        when(serviceItemRepository.findByActive(true)).thenReturn(activeServices);

        List<ServiceItemResponse> result = serviceItemService.search("", "");

        assertThat(result).hasSize(1);
        verify(serviceItemRepository).findByActive(true);
    }

    // ==================== PRICING CALCULATION TESTS ====================

    @Test
    void getServiceById_ShouldIncludePricingDetails() {
        when(serviceItemRepository.findById("service123")).thenReturn(Optional.of(testServiceItem));

        ServiceItemResponse result = serviceItemService.getServiceById("service123");

        assertThat(result.getPricingDetails()).isNotNull();
        assertThat(result.getPricingDetails().getBasePrice()).isEqualTo(1000.0);
        assertThat(result.getPricingDetails().getTaxPercentage()).isEqualTo(18.0);
        assertThat(result.getPricingDetails().getDiscountPercentage()).isEqualTo(10.0);
    }

    @Test
    void createService_ShouldCalculatePricingDetails() {
        when(categoryRepository.findById("category123")).thenReturn(Optional.of(testCategory));
        when(serviceItemRepository.save(any(ServiceItem.class))).thenReturn(testServiceItem);
        when(categoryRepository.save(any(ServiceCategory.class))).thenReturn(testCategory);

        ServiceItemResponse result = serviceItemService.createService(createRequest);

        assertThat(result.getPricingDetails()).isNotNull();
        // Tax: 1000 * 18% = 180
        assertThat(result.getPricingDetails().getTaxAmount()).isEqualTo(180.0);
        // Discount: 1000 * 10% = 100
        assertThat(result.getPricingDetails().getDiscountAmount()).isEqualTo(100.0);
        // Final: 1000 + 180 - 100 = 1080
        assertThat(result.getPricingDetails().getFinalPrice()).isEqualTo(1080.0);
    }
}

