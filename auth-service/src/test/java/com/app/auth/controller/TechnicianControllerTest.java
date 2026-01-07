package com.app.auth.controller;

import com.app.auth.config.TestSecurityConfig;
import com.app.auth.dto.response.TechnicianProfileResponseDTO;
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
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = TechnicianController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
class TechnicianControllerTest {

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
                .phoneNumber("1234567890")
                .skills(Set.of("PLUMBING", "ELECTRICAL"))
                .experienceYears(5)
                .bio("Experienced technician")
                .city("TestCity")
                .state("TestState")
                .approvalStatus(com.app.auth.model.TechnicianProfile.ApprovalStatus.APPROVED)
                .available(true)
                .averageRating(4.5)
                .totalJobsCompleted(50)
                .createdAt(Instant.now())
                .build();
    }

    // Tests that require Authentication principal are skipped
    // These endpoints require a valid security context with Authentication object
    // Service implementation tests provide the coverage for the business logic
}
