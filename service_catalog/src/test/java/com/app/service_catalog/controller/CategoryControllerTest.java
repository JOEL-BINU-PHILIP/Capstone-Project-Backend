package com.app.service_catalog. controller;

import com.app. service_catalog.dto.request.CreateCategoryRequest;
import com. app.service_catalog.dto. request.ReorderCategoryRequest;
import com. app.service_catalog.dto. request.UpdateCategoryRequest;
import com.app.service_catalog. dto.request.UpdateCategoryStatusRequest;
import com.app.service_catalog.model.ServiceCategory;
import com.app.service_catalog.service.CategoryService;
import com. fasterxml.jackson.databind.ObjectMapper;
import org. junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet. WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework. security.test.context.support.WithMockUser;
import org. springframework.test.web.servlet. MockMvc;

import java.time.Instant;
import java. util.Arrays;
import java. util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework. test.web.servlet.request. MockMvcRequestBuilders.*;
import static org.springframework.test. web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategoryController.class)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CategoryService categoryService;

    private ServiceCategory testCategory;

    @BeforeEach
    void setUp() {
        testCategory = ServiceCategory.builder()
                .id("cat-1")
                .name("Plumbing")
                .description("All plumbing services")
                .iconUrl("icon.png")
                .active(true)
                .displayOrder(1)
                .createdAt(Instant.now())
                .build();
    }

    // ==================== CREATE CATEGORY ====================

    @Test
    @WithMockUser(roles = "ADMIN")
    void createCategory_ShouldReturnCreatedId_WhenValidRequest() throws Exception {
        CreateCategoryRequest request = new CreateCategoryRequest();
        request.setName("Plumbing");
        request.setDescription("All plumbing services");
        request.setIconUrl("icon.png");
        request.setDisplayOrder(1);

        when(categoryService.createCategory(any(ServiceCategory.class)))
                .thenReturn(testCategory);

        mockMvc.perform(post("/api/services/categories")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().string("cat-1"));

        verify(categoryService, times(1)).createCategory(any(ServiceCategory.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createCategory_ShouldReturnBadRequest_WhenNameIsBlank() throws Exception {
        CreateCategoryRequest request = new CreateCategoryRequest();
        request.setName("");
        request.setDescription("Description");
        request.setDisplayOrder(1);

        mockMvc.perform(post("/api/services/categories")
                        . with(csrf())
                        . contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(categoryService, never()).createCategory(any());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void createCategory_ShouldReturnForbidden_WhenNotAdmin() throws Exception {
        CreateCategoryRequest request = new CreateCategoryRequest();
        request.setName("Plumbing");
        request.setDisplayOrder(1);

        mockMvc.perform(post("/api/services/categories")
                        . with(csrf())
                        . contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(categoryService, never()).createCategory(any());
    }

    // ==================== UPDATE CATEGORY ====================

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateCategory_ShouldReturnUpdatedCategory_WhenValidRequest() throws Exception {
        UpdateCategoryRequest request = new UpdateCategoryRequest();
        request.setName("Updated Plumbing");
        request.setDescription("Updated description");

        ServiceCategory updatedCategory = ServiceCategory. builder()
                .id("cat-1")
                .name("Updated Plumbing")
                .description("Updated description")
                .build();

        when(categoryService. updateCategory(eq("cat-1"), any(UpdateCategoryRequest.class)))
                .thenReturn(updatedCategory);

        mockMvc.perform(put("/api/services/categories/cat-1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("cat-1"))
                .andExpect(jsonPath("$.name").value("Updated Plumbing"));

        verify(categoryService, times(1)).updateCategory(eq("cat-1"), any(UpdateCategoryRequest.class));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void updateCategory_ShouldReturnForbidden_WhenNotAdmin() throws Exception {
        UpdateCategoryRequest request = new UpdateCategoryRequest();
        request.setName("Updated Plumbing");

        mockMvc.perform(put("/api/services/categories/cat-1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(categoryService, never()).updateCategory(any(), any());
    }

    // ==================== UPDATE STATUS ====================

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateStatus_ShouldReturnUpdatedCategory_WhenValidRequest() throws Exception {
        UpdateCategoryStatusRequest request = new UpdateCategoryStatusRequest();
        request.setActive(false);

        ServiceCategory inactiveCategory = ServiceCategory.builder()
                .id("cat-1")
                .name("Plumbing")
                .active(false)
                .build();

        when(categoryService.updateCategoryStatus("cat-1", false))
                .thenReturn(inactiveCategory);

        mockMvc.perform(put("/api/services/categories/cat-1/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("cat-1"))
                .andExpect(jsonPath("$.active").value(false));

        verify(categoryService, times(1)).updateCategoryStatus("cat-1", false);
    }

    // ==================== REORDER CATEGORIES ====================

    @Test
    @WithMockUser(roles = "ADMIN")
    void reorderCategories_ShouldReturnOk_WhenValidRequest() throws Exception {
        ReorderCategoryRequest req1 = new ReorderCategoryRequest();
        req1.setId("cat-1");
        req1.setDisplayOrder(2);

        ReorderCategoryRequest req2 = new ReorderCategoryRequest();
        req2.setId("cat-2");
        req2.setDisplayOrder(1);

        List<ReorderCategoryRequest> requests = Arrays.asList(req1, req2);

        doNothing().when(categoryService).reorderCategories(any());

        mockMvc.perform(put("/api/services/categories/reorder")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isOk());

        verify(categoryService, times(1)).reorderCategories(any());
    }

    // ==================== DELETE CATEGORY ====================

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteCategory_ShouldReturnNoContent_WhenSuccessful() throws Exception {
        doNothing().when(categoryService).deleteCategory("cat-1");

        mockMvc.perform(delete("/api/services/categories/cat-1")
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(categoryService, times(1)).deleteCategory("cat-1");
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void deleteCategory_ShouldReturnForbidden_WhenNotAdmin() throws Exception {
        mockMvc.perform(delete("/api/services/categories/cat-1")
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verify(categoryService, never()).deleteCategory(any());
    }

    // ==================== GET ALL CATEGORIES ====================

    @Test
    void getAllCategories_ShouldReturnAllCategories_WhenNoFilterProvided() throws Exception {
        List<ServiceCategory> categories = Arrays. asList(testCategory);
        when(categoryService.getAllCategories()).thenReturn(categories);

        mockMvc.perform(get("/api/services/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("cat-1"))
                .andExpect(jsonPath("$[0]. name").value("Plumbing"));

        verify(categoryService, times(1)).getAllCategories();
    }

    @Test
    void getAllCategories_ShouldReturnActiveCategories_WhenActiveFilterIsTrue() throws Exception {
        List<ServiceCategory> categories = Arrays.asList(testCategory);
        when(categoryService.getCategories(true)).thenReturn(categories);

        mockMvc.perform(get("/api/services/categories")
                        .param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("cat-1"));

        verify(categoryService, times(1)).getCategories(true);
    }

    // ==================== GET CATEGORY BY ID ====================

    @Test
    void getCategoryById_ShouldReturnCategory_WhenExists() throws Exception {
        when(categoryService.getCategoryById("cat-1")).thenReturn(testCategory);

        mockMvc.perform(get("/api/services/categories/cat-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("cat-1"))
                .andExpect(jsonPath("$.name").value("Plumbing"));

        verify(categoryService, times(1)).getCategoryById("cat-1");
    }
}