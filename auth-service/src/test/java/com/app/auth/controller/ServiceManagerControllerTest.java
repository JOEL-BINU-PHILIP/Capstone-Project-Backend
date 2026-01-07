package com.app.auth.controller;

import com.app.auth.config.TestSecurityConfig;
import com.app.auth.dto.response.TechnicianProfileResponseDTO;
import com.app.auth.model.TechnicianProfile;
import com.app.auth.security.JwtAuthenticationFilter;
import com.app.auth.security.JwtUtils;
import com.app.auth.service.TechnicianProfileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ServiceManagerController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
class ServiceManagerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TechnicianProfileService technicianProfileService;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private TechnicianProfileResponseDTO testProfile;

    @BeforeEach
    void setUp() {
        testProfile = TechnicianProfileResponseDTO.builder()
                .id("tech123")
                .userId("user123")
                .username("techuser")
                .email("tech@test.com")
                .firstName("Tech")
                .lastName("User")
                .skills(Set.of("PLUMBING"))
                .experienceYears(5)
                .bio("Experienced technician")
                .city("TestCity")
                .state("TestState")
                .approvalStatus(TechnicianProfile.ApprovalStatus.PENDING)
                .available(false)
                .createdAt(Instant.now())
                .build();

        // Set up service manager authentication context
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "servicemanager", null, List.of(new SimpleGrantedAuthority("ROLE_SERVICE_MANAGER")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // ==================== GET PENDING TECHNICIANS ====================

    @Test
    void getPendingTechnicians_success() throws Exception {
        Page<TechnicianProfileResponseDTO> page = new PageImpl<>(
                List.of(testProfile), PageRequest.of(0, 10), 1);

        when(technicianProfileService.getTechniciansByStatus(
                eq(TechnicianProfile.ApprovalStatus.PENDING), isNull(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/service-manager/technicians/pending")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Pending technicians retrieved"));
    }

    // ==================== APPROVE TECHNICIAN ====================

    @Test
    void approveTechnician_success() throws Exception {
        doNothing().when(technicianProfileService).approveTechnician("tech123");

        mockMvc.perform(post("/api/service-manager/technicians/tech123/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Technician approved successfully"));

        verify(technicianProfileService).approveTechnician("tech123");
    }

    // ==================== REJECT TECHNICIAN ====================

    @Test
    void rejectTechnician_success() throws Exception {
        doNothing().when(technicianProfileService).rejectTechnician(anyString(), anyString());

        ServiceManagerController.RejectRequest request =
                new ServiceManagerController.RejectRequest("Invalid documents");

        mockMvc.perform(post("/api/service-manager/technicians/tech123/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Technician rejected"));

        verify(technicianProfileService).rejectTechnician("tech123", "Invalid documents");
    }

    @Test
    void rejectTechnician_withoutReason() throws Exception {
        doNothing().when(technicianProfileService).rejectTechnician(anyString(), anyString());

        mockMvc.perform(post("/api/service-manager/technicians/tech123/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(technicianProfileService).rejectTechnician("tech123", "No reason provided");
    }

    @Test
    void rejectTechnician_nullBody() throws Exception {
        doNothing().when(technicianProfileService).rejectTechnician(anyString(), anyString());

        mockMvc.perform(post("/api/service-manager/technicians/tech123/reject"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ==================== GET ALL TECHNICIANS ====================

    @Test
    void getAllTechnicians_withStatus() throws Exception {
        Page<TechnicianProfileResponseDTO> page = new PageImpl<>(
                List.of(testProfile), PageRequest.of(0, 10), 1);

        when(technicianProfileService.getTechniciansByStatus(
                eq(TechnicianProfile.ApprovalStatus.APPROVED), isNull(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/service-manager/technicians")
                        .param("status", "APPROVED")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Technicians retrieved"));
    }

    @Test
    void getAllTechnicians_withStatusAndAvailability() throws Exception {
        Page<TechnicianProfileResponseDTO> page = new PageImpl<>(
                List.of(testProfile), PageRequest.of(0, 10), 1);

        when(technicianProfileService.getTechniciansByStatus(
                eq(TechnicianProfile.ApprovalStatus.APPROVED), eq(true), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/service-manager/technicians")
                        .param("status", "APPROVED")
                        .param("available", "true")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getAllTechnicians_withoutStatus() throws Exception {
        Page<TechnicianProfileResponseDTO> page = new PageImpl<>(
                List.of(testProfile), PageRequest.of(0, 10), 1);

        when(technicianProfileService.getAllTechnicians(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/service-manager/technicians")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ==================== GET TECHNICIAN BY ID ====================

    @Test
    void getTechnicianById_success() throws Exception {
        when(technicianProfileService.getProfileById("tech123")).thenReturn(testProfile);

        mockMvc.perform(get("/api/service-manager/technicians/tech123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Technician retrieved"));
    }
}

