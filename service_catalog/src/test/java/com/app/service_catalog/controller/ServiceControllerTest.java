package com.app.service_catalog.controller;

import com.app.service_catalog.dto.request.CreateServiceRequest;
import com.app.service_catalog.dto.request.UpdateServiceRequest;
import com.app.service_catalog.dto.response.PricingDetailsResponse;
import com.app.service_catalog.dto.response.ServiceItemResponse;
import com.app.service_catalog.service.ServiceItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceControllerTest {

    @Mock
    private ServiceItemService serviceItemService;

    @InjectMocks
    private ServiceController serviceController;

    private ServiceItemResponse serviceResponse;
    private CreateServiceRequest createRequest;
    private UpdateServiceRequest updateRequest;

    @BeforeEach
    void setUp() {
        PricingDetailsResponse pricingDetails = PricingDetailsResponse.builder()
                .basePrice(1000.0)
                .taxPercentage(18.0)
                .taxAmount(180.0)
                .discountPercentage(10.0)
                .discountAmount(100.0)
                .finalPrice(1080.0)
                .discountValidUntil(LocalDateTime.now().plusDays(30))
                .build();

        serviceResponse = ServiceItemResponse.builder()
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
                .pricingDetails(pricingDetails)
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
    void create_ShouldReturnServiceId_WhenValidRequest() {
        when(serviceItemService.createService(any(CreateServiceRequest.class))).thenReturn(serviceResponse);

        ResponseEntity<String> response = serviceController.create(createRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo("service123");
        verify(serviceItemService).createService(any(CreateServiceRequest.class));
    }

    // ==================== UPDATE TESTS ====================

    @Test
    void update_ShouldReturnUpdatedService() {
        when(serviceItemService.updateService(eq("service123"), any(UpdateServiceRequest.class)))
                .thenReturn(serviceResponse);

        ResponseEntity<ServiceItemResponse> response = serviceController.update("service123", updateRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo("service123");
        verify(serviceItemService).updateService(eq("service123"), any(UpdateServiceRequest.class));
    }

    @Test
    void updateStatus_ShouldReturnUpdatedService_WhenActivatingService() {
        serviceResponse.setActive(true);
        when(serviceItemService.updateServiceStatus("service123", true)).thenReturn(serviceResponse);

        ResponseEntity<ServiceItemResponse> response = serviceController.updateStatus("service123", true);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isActive()).isTrue();
        verify(serviceItemService).updateServiceStatus("service123", true);
    }

    @Test
    void updateStatus_ShouldReturnUpdatedService_WhenDeactivatingService() {
        serviceResponse.setActive(false);
        when(serviceItemService.updateServiceStatus("service123", false)).thenReturn(serviceResponse);

        ResponseEntity<ServiceItemResponse> response = serviceController.updateStatus("service123", false);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isActive()).isFalse();
        verify(serviceItemService).updateServiceStatus("service123", false);
    }

    // ==================== DELETE TESTS ====================

    @Test
    void delete_ShouldReturnNoContent() {
        doNothing().when(serviceItemService).deleteService("service123");

        ResponseEntity<Void> response = serviceController.delete("service123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(serviceItemService).deleteService("service123");
    }

    // ==================== GET TESTS ====================

    @Test
    void getAll_ShouldReturnAllServices_WhenNoFilter() {
        List<ServiceItemResponse> services = Arrays.asList(serviceResponse);
        when(serviceItemService.getAllServices()).thenReturn(services);

        ResponseEntity<List<ServiceItemResponse>> response = serviceController.getAll(null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        verify(serviceItemService).getAllServices();
    }

    @Test
    void getAll_ShouldReturnFilteredServices_WhenCategoryIdProvided() {
        List<ServiceItemResponse> services = Arrays.asList(serviceResponse);
        when(serviceItemService.getServices("category123", null)).thenReturn(services);

        ResponseEntity<List<ServiceItemResponse>> response = serviceController.getAll("category123", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        verify(serviceItemService).getServices("category123", null);
    }

    @Test
    void getAll_ShouldReturnFilteredServices_WhenActiveProvided() {
        List<ServiceItemResponse> services = Arrays.asList(serviceResponse);
        when(serviceItemService.getServices(null, true)).thenReturn(services);

        ResponseEntity<List<ServiceItemResponse>> response = serviceController.getAll(null, true);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        verify(serviceItemService).getServices(null, true);
    }

    @Test
    void getAll_ShouldReturnFilteredServices_WhenBothFiltersProvided() {
        List<ServiceItemResponse> services = Arrays.asList(serviceResponse);
        when(serviceItemService.getServices("category123", true)).thenReturn(services);

        ResponseEntity<List<ServiceItemResponse>> response = serviceController.getAll("category123", true);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        verify(serviceItemService).getServices("category123", true);
    }

    @Test
    void getById_ShouldReturnService() {
        when(serviceItemService.getServiceById("service123")).thenReturn(serviceResponse);

        ResponseEntity<ServiceItemResponse> response = serviceController.getById("service123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo("service123");
        assertThat(response.getBody().getName()).isEqualTo("AC Repair");
        verify(serviceItemService).getServiceById("service123");
    }

    // ==================== SEARCH TESTS ====================

    @Test
    void search_ShouldReturnServices_WhenQueryProvided() {
        List<ServiceItemResponse> services = Arrays.asList(serviceResponse);
        when(serviceItemService.search("AC", null)).thenReturn(services);

        ResponseEntity<List<ServiceItemResponse>> response = serviceController.search("AC", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        verify(serviceItemService).search("AC", null);
    }

    @Test
    void search_ShouldReturnServices_WhenSkillProvided() {
        List<ServiceItemResponse> services = Arrays.asList(serviceResponse);
        when(serviceItemService.search(null, "HVAC")).thenReturn(services);

        ResponseEntity<List<ServiceItemResponse>> response = serviceController.search(null, "HVAC");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        verify(serviceItemService).search(null, "HVAC");
    }

    @Test
    void search_ShouldReturnServices_WhenBothQueryAndSkillProvided() {
        List<ServiceItemResponse> services = Arrays.asList(serviceResponse);
        when(serviceItemService.search("AC", "HVAC")).thenReturn(services);

        ResponseEntity<List<ServiceItemResponse>> response = serviceController.search("AC", "HVAC");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        verify(serviceItemService).search("AC", "HVAC");
    }

    // ==================== GET BY CATEGORY TESTS ====================

    @Test
    void getByCategory_ShouldReturnServices_WhenCategoryIdProvided() {
        List<ServiceItemResponse> services = Arrays.asList(serviceResponse);
        when(serviceItemService.getServices("category123", null)).thenReturn(services);

        ResponseEntity<List<ServiceItemResponse>> response = serviceController.getByCategory("category123", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        verify(serviceItemService).getServices("category123", null);
    }

    @Test
    void getByCategory_ShouldReturnActiveServices_WhenActiveFilterProvided() {
        List<ServiceItemResponse> services = Arrays.asList(serviceResponse);
        when(serviceItemService.getServices("category123", true)).thenReturn(services);

        ResponseEntity<List<ServiceItemResponse>> response = serviceController.getByCategory("category123", true);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        verify(serviceItemService).getServices("category123", true);
    }
}

