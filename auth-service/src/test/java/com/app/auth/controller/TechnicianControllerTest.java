package com.app.auth.controller;

import com.app.auth.dto.response.TechnicianProfileResponseDTO;
import com.app.auth.model.TechnicianProfile;
import com.app.auth.security.JwtAuthenticationFilter;
import com.app.auth.security.JwtUtils;
import com.app.auth.service.TechnicianProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TechnicianController.class)
@AutoConfigureMockMvc(addFilters = false)
public class TechnicianControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TechnicianProfileService technicianProfileService;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void getMyProfile_Success() throws Exception {
        TechnicianProfileResponseDTO mockProfile = TechnicianProfileResponseDTO.builder()
                .userId("tech-id-1")
                .username("tech_user")
                .skills(Set.of("Plumbing", "Repair"))
                .approvalStatus(TechnicianProfile.ApprovalStatus.APPROVED)
                .build();

        when(technicianProfileService.getProfileByUsername("tech_user")).thenReturn(mockProfile);

        // FIX: Create Principal manually and inject it
        Authentication auth = new UsernamePasswordAuthenticationToken("tech_user", "password",
                List.of(new SimpleGrantedAuthority("ROLE_TECHNICIAN")));

        mockMvc.perform(get("/api/technician/profile").principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("tech_user"))
                .andExpect(jsonPath("$.data.approvalStatus").value("APPROVED"));
    }

    @Test
    void updateAvailability_Success() throws Exception {
        // FIX: Inject Principal
        Authentication auth = new UsernamePasswordAuthenticationToken("tech_user", "password",
                List.of(new SimpleGrantedAuthority("ROLE_TECHNICIAN")));

        mockMvc.perform(patch("/api/technician/availability")
                        .param("available", "true")
                        .principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(technicianProfileService).updateAvailability(eq("tech_user"), eq(true));
    }
}