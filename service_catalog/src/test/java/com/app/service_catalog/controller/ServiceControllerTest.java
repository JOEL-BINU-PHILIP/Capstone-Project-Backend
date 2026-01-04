package com.app.service_catalog. controller;

import com.app. service_catalog.dto.request. CreateServiceRequest;
import com. app.service_catalog.dto. request.UpdateServiceRequest;
import com.app.service_catalog.dto.response.PricingDetailsResponse;
import com.app. service_catalog.dto.response. ServiceItemResponse;
import com.app. service_catalog.service.ServiceItemService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit. jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security. test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java. util.Arrays;
import java. util.List;

import static org.mockito.ArgumentMatchers. any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework. test.web.servlet.request. MockMvcRequestBuilders.*;
import static org.springframework.test. web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ServiceController.class)
class ServiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ServiceItemService serviceItemService;

    private ServiceItemResponse testService;

    @BeforeEach
    void setUp() {
        PricingDetailsResponse pricingDetails = PricingDetailsResponse.builder()
                .basePrice(100.0)
                .taxPercentage(10.0)
                .taxAmount(10.0)
                .discountPercentage(0.0)
                .discountAmount(0.0)
                .finalPrice(110.0)
                .build();

        testService = ServiceItemResponse.builder()
                .id("service-1")
                .categoryId("cat-1")
                .categoryName("Plumbing")
                .name("Pipe Repair")
                .description("Fix leaking pipes")
                .basePrice(100.0)
                .currency("USD")
                .estimatedDurationMinutes(60)
                .imageUrl("image.png")
                .active(true)
                .requiredSkills(Arrays.asList("plumbing", "repair"))
                .pricingDetails(pricingDetails)
                .createdAt(Instant.now())
                .build();
    }

    // ==================== CREATE SERVICE ====================

    @Test
    @WithMockUser(roles = "ADMIN")
    void createService_ShouldReturnCreatedId_WhenValidRequest() throws Exception {
        CreateServiceRequest request = new CreateServiceRequest();
        request.setCategoryId("cat-1");
        request.setName("Pipe Repair");
        request.setDescription("Fix leaking pipes");
        request.setBasePrice(100.0);
        request.setCurrency("USD");
        request.setEstimatedDurationMinutes(60);

        when(serviceItemService.createService(any(CreateServiceRequest.class)))
                .thenReturn(testService);

        mockMvc.perform(post("/api/services")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().string("service-1"));

        verify(serviceItemService, times(1)).createService(any(CreateServiceRequest.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createService_ShouldReturnBadRequest_WhenCategoryIdIsBlank() throws Exception {
        CreateServiceRequest request = new CreateServiceRequest();
        request.setCategoryId("");
        request.setName("Pipe Repair");
        request.setBasePrice(100.0);

        mockMvc.perform(post("/api/services")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(serviceItemService, never()).createService(any());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void createService_ShouldReturnForbidden_WhenNotAdmin() throws Exception {
        CreateServiceRequest request = new CreateServiceRequest();
        request.setCategoryId("cat-1");
        request.setName("Pipe Repair");
        request.setBasePrice(100.0);

        mockMvc.perform(post("/api/services")
                        .with(csrf())
                        .contentType(MediaType. APPLICATION_JSON)
                        . content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(serviceItemService, never()).createService(any());
    }

    // ==================== UPDATE SERVICE ====================

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateService_ShouldReturnUpdatedService_WhenValidRequest() throws Exception {
        UpdateServiceRequest request = new UpdateServiceRequest();
        request.setName("Updated Pipe Repair");
        request.setBasePrice(120.0);

        ServiceItemResponse updatedService = ServiceItemResponse.builder()
                .id("service-1")
                .name("Updated Pipe Repair")
                .basePrice(120.0)
                .build();

        when(serviceItemService.updateService(eq("service-1"), any(UpdateServiceRequest. class)))
                .thenReturn(updatedService);

        mockMvc.perform(put("/api/services/service-1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("service-1"))
                .andExpect(jsonPath("$. name").value("Updated Pipe Repair"))
                .andExpect(jsonPath("$.basePrice").value(120.0));

        verify(serviceItemService, times(1)).updateService(eq("service-1"), any(UpdateServiceRequest.class));
    }

    // ==================== UPDATE STATUS ====================

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateServiceStatus_ShouldReturnUpdatedService_WhenValidRequest() throws Exception {
        ServiceItemResponse inactiveService = ServiceItemResponse. builder()
                .id("service-1")
                .name("Pipe Repair")
                .active(false)
                .build();

        when(serviceItemService.updateServiceStatus("service-1", false))
                .thenReturn(inactiveService);

        mockMvc.perform(put("/api/services/service-1/status")
                        .with(csrf())
                        .param("active", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("service-1"))
                .andExpect(jsonPath("$.active").value(false));

        verify(serviceItemService, times(1)).updateServiceStatus("service-1", false);
    }

    // ==================== DELETE SERVICE ====================

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteService_ShouldReturnNoContent_WhenSuccessful() throws Exception {
        doNothing().when(serviceItemService).deleteService("service-1");

        mockMvc.perform(delete("/api/services/service-1")
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(serviceItemService, times(1)).deleteService("service-1");
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void deleteService_ShouldReturnForbidden_WhenNotAdmin() throws Exception {
        mockMvc.perform(delete("/api/services/service-1")
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verify(serviceItemService, never()).deleteService(any());
    }

    // ==================== GET ALL SERVICES ====================

    @Test
    void getAllServices_ShouldReturnAllServices_WhenNoFilterProvided() throws Exception {
        List<ServiceItemResponse> services = Arrays.asList(testService);
        when(serviceItemService.getAllServices()).thenReturn(services);

        mockMvc.perform(get("/api/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("service-1"))
                .andExpect(jsonPath("$[0].name").value("Pipe Repair"));

        verify(serviceItemService, times(1)).getAllServices();
    }

    @Test
    void getAllServices_ShouldReturnFilteredServices_WhenFiltersProvided() throws Exception {
        List<ServiceItemResponse> services = Arrays.asList(testService);
        when(serviceItemService. getServices("cat-1", true)).thenReturn(services);

        mockMvc.perform(get("/api/services")
                        .param("categoryId", "cat-1")
                        .param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("service-1"));

        verify(serviceItemService, times(1)).getServices("cat-1", true);
    }

    // ==================== GET SERVICE BY ID ====================

    @Test
    void getServiceById_ShouldReturnService_WhenExists() throws Exception {
        when(serviceItemService.getServiceById("service-1")).thenReturn(testService);

        mockMvc.perform(get("/api/services/service-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$. id").value("service-1"))
                .andExpect(jsonPath("$.name").value("Pipe Repair"));

        verify(serviceItemService, times(1)).getServiceById("service-1");
    }

    // ==================== SEARCH SERVICES ====================

    @Test
    void searchServices_ShouldReturnMatchingServices_WhenQueryProvided() throws Exception {
        List<ServiceItemResponse> services = Arrays.asList(testService);
        when(serviceItemService.search("pipe", null)).thenReturn(services);

        mockMvc.perform(get("/api/services/search")
                        .param("query", "pipe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("service-1"));

        verify(serviceItemService, times(1)).search("pipe", null);
    }

    @Test
    void searchServices_ShouldReturnMatchingServices_WhenSkillProvided() throws Exception {
        List<ServiceItemResponse> services = Arrays.asList(testService);
        when(serviceItemService. search(null, "plumbing")).thenReturn(services);

        mockMvc.perform(get("/api/services/search")
                        . param("skill", "plumbing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("service-1"));

        verify(serviceItemService, times(1)).search(null, "plumbing");
    }

    // ==================== GET BY CATEGORY ====================

    @Test
    void getByCategory_ShouldReturnServicesInCategory() throws Exception {
        List<ServiceItemResponse> services = Arrays.asList(testService);
        when(serviceItemService.getServices("cat-1", true)).thenReturn(services);

        mockMvc.perform(get("/api/services/category/cat-1")
                        . param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("service-1"))
                .andExpect(jsonPath("$[0].categoryId").value("cat-1"));

        verify(serviceItemService, times(1)).getServices("cat-1", true);
    }
}