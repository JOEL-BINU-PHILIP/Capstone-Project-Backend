package com.app.auth.controller;

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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ServiceManagerController.class)
@AutoConfigureMockMvc(addFilters = false)
public class ServiceManagerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TechnicianProfileService technicianProfileService;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private ObjectMapper objectMapper;

    private TechnicianProfileResponseDTO techProfile;

    @BeforeEach
    void setUp() {
        techProfile = TechnicianProfileResponseDTO.builder()
                .id("tech-1")
                .username("john_tech")
                .approvalStatus(TechnicianProfile.ApprovalStatus.PENDING)
                .build();
    }

    @Test
    void getPendingTechnicians_Success() throws Exception {
        // FIX: Use PageRequest.of(...)
        Page<TechnicianProfileResponseDTO> page = new PageImpl<>(List.of(techProfile), PageRequest.of(0, 10), 1);

        when(technicianProfileService.getTechniciansByStatus(
                eq(TechnicianProfile.ApprovalStatus.PENDING),
                eq(null),
                any(Pageable.class))
        ).thenReturn(page);

        mockMvc.perform(get("/api/service-manager/technicians/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].username").value("john_tech"));
    }

    @Test
    void approveTechnician_Success() throws Exception {
        mockMvc.perform(post("/api/service-manager/technicians/{technicianId}/approve", "tech-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(technicianProfileService).approveTechnician("tech-1");
    }

    @Test
    void rejectTechnician_Success() throws Exception {
        ServiceManagerController.RejectRequest request = new ServiceManagerController.RejectRequest("Incomplete docs");

        mockMvc.perform(post("/api/service-manager/technicians/{technicianId}/reject", "tech-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(technicianProfileService).rejectTechnician("tech-1", "Incomplete docs");
    }

    @Test
    void getAllTechnicians_WithFilters_Success() throws Exception {
        // FIX: Use PageRequest.of(...)
        Page<TechnicianProfileResponseDTO> page = new PageImpl<>(List.of(techProfile), PageRequest.of(0, 10), 1);

        when(technicianProfileService.getTechniciansByStatus(
                eq(TechnicianProfile.ApprovalStatus.APPROVED),
                eq(true),
                any(Pageable.class))
        ).thenReturn(page);

        mockMvc.perform(get("/api/service-manager/technicians")
                        .param("status", "APPROVED")
                        .param("available", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}