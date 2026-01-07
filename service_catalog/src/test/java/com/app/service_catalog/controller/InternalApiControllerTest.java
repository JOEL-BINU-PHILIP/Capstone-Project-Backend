package com.app.service_catalog.controller;

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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InternalApiControllerTest {

    @Mock
    private ServiceItemRepository serviceItemRepository;

    @Mock
    private ServiceCategoryRepository serviceCategoryRepository;

    @InjectMocks
    private InternalApiController internalApiController;

    private ServiceItem testServiceItem;
    private ServiceCategory testCategory;

    @BeforeEach
    void setUp() {
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

        testCategory = ServiceCategory.builder()
                .id("category123")
                .name("HVAC")
                .description("Heating, Ventilation, and Air Conditioning")
                .iconUrl("https://example.com/hvac-icon.png")
                .active(true)
                .displayOrder(1)
                .servicesCount(5)
                .createdAt(Instant.now())
                .build();
    }

    // ==================== GET SERVICE BY ID ====================

    @Test
    void getServiceById_ShouldReturnService_WhenFound() {
        when(serviceItemRepository.findById("service123")).thenReturn(Optional.of(testServiceItem));

        ResponseEntity<Map<String, Object>> response = internalApiController.getServiceById("service123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(true);
        assertThat(response.getBody().get("data")).isNotNull();
        verify(serviceItemRepository).findById("service123");
    }

    @Test
    void getServiceById_ShouldReturnNotFound_WhenServiceNotFound() {
        when(serviceItemRepository.findById("nonexistent")).thenReturn(Optional.empty());

        ResponseEntity<Map<String, Object>> response = internalApiController.getServiceById("nonexistent");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(false);
        assertThat(response.getBody().get("data")).isNull();
        verify(serviceItemRepository).findById("nonexistent");
    }

    // ==================== VALIDATE SERVICE ====================

    @Test
    void validateService_ShouldReturnValid_WhenServiceExistsAndActive() {
        when(serviceItemRepository.findById("service123")).thenReturn(Optional.of(testServiceItem));

        ResponseEntity<Map<String, Object>> response = internalApiController.validateService("service123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(true);

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        assertThat(data.get("exists")).isEqualTo(true);
        assertThat(data.get("active")).isEqualTo(true);
        assertThat(data.get("canBook")).isEqualTo(true);
        verify(serviceItemRepository).findById("service123");
    }

    @Test
    void validateService_ShouldReturnInactive_WhenServiceExistsButInactive() {
        testServiceItem.setActive(false);
        when(serviceItemRepository.findById("service123")).thenReturn(Optional.of(testServiceItem));

        ResponseEntity<Map<String, Object>> response = internalApiController.validateService("service123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(true);

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        assertThat(data.get("exists")).isEqualTo(true);
        assertThat(data.get("active")).isEqualTo(false);
        assertThat(data.get("canBook")).isEqualTo(false);
    }

    @Test
    void validateService_ShouldReturnNotFound_WhenServiceNotFound() {
        when(serviceItemRepository.findById("nonexistent")).thenReturn(Optional.empty());

        ResponseEntity<Map<String, Object>> response = internalApiController.validateService("nonexistent");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(true);

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        assertThat(data.get("exists")).isEqualTo(false);
        assertThat(data.get("canBook")).isEqualTo(false);
    }

    // ==================== GET SERVICE PRICING ====================

    @Test
    void getServicePricing_ShouldReturnPricing_WhenServiceFound() {
        when(serviceItemRepository.findById("service123")).thenReturn(Optional.of(testServiceItem));

        ResponseEntity<Map<String, Object>> response = internalApiController.getServicePricing("service123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(true);
        assertThat(response.getBody().get("data")).isNotNull();
        verify(serviceItemRepository).findById("service123");
    }

    @Test
    void getServicePricing_ShouldReturnNotFound_WhenServiceNotFound() {
        when(serviceItemRepository.findById("nonexistent")).thenReturn(Optional.empty());

        ResponseEntity<Map<String, Object>> response = internalApiController.getServicePricing("nonexistent");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(false);
        assertThat(response.getBody().get("data")).isNull();
    }

    // ==================== GET SERVICE FOR BOOKING ====================

    @Test
    void getServiceForBooking_ShouldReturnBookingData_WhenServiceActiveAndFound() {
        when(serviceItemRepository.findById("service123")).thenReturn(Optional.of(testServiceItem));

        ResponseEntity<Map<String, Object>> response = internalApiController.getServiceForBooking("service123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(true);
        assertThat(response.getBody().get("data")).isNotNull();

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        assertThat(data.get("serviceId")).isEqualTo("service123");
        assertThat(data.get("serviceName")).isEqualTo("AC Repair");
        assertThat(data.get("basePrice")).isEqualTo(1000.0);
    }

    @Test
    void getServiceForBooking_ShouldReturnError_WhenServiceInactive() {
        testServiceItem.setActive(false);
        when(serviceItemRepository.findById("service123")).thenReturn(Optional.of(testServiceItem));

        ResponseEntity<Map<String, Object>> response = internalApiController.getServiceForBooking("service123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(false);
        assertThat(response.getBody().get("message")).isEqualTo("Service is not active");
    }

    @Test
    void getServiceForBooking_ShouldReturnNotFound_WhenServiceNotFound() {
        when(serviceItemRepository.findById("nonexistent")).thenReturn(Optional.empty());

        ResponseEntity<Map<String, Object>> response = internalApiController.getServiceForBooking("nonexistent");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(false);
        assertThat(response.getBody().get("data")).isNull();
    }

    // ==================== GET CATEGORY BY ID ====================

    @Test
    void getCategoryById_ShouldReturnCategory_WhenFound() {
        when(serviceCategoryRepository.findById("category123")).thenReturn(Optional.of(testCategory));

        ResponseEntity<Map<String, Object>> response = internalApiController.getCategoryById("category123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(true);
        assertThat(response.getBody().get("data")).isNotNull();

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        assertThat(data.get("id")).isEqualTo("category123");
        assertThat(data.get("name")).isEqualTo("HVAC");
        verify(serviceCategoryRepository).findById("category123");
    }

    @Test
    void getCategoryById_ShouldReturnNotFound_WhenCategoryNotFound() {
        when(serviceCategoryRepository.findById("nonexistent")).thenReturn(Optional.empty());

        ResponseEntity<Map<String, Object>> response = internalApiController.getCategoryById("nonexistent");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(false);
        assertThat(response.getBody().get("data")).isNull();
    }
}

