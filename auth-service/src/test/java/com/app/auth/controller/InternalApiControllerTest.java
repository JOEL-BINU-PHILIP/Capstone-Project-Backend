package com.app.auth.controller;

import com.app.auth.config.TestSecurityConfig;
import com.app.auth.model.TechnicianProfile;
import com.app.auth.model.User;
import com.app.auth.repository.TechnicianProfileRepository;
import com.app.auth.repository.UserRepository;
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
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = InternalApiController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
class InternalApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private TechnicianProfileRepository technicianProfileRepository;

    @MockBean
    private TechnicianProfileService technicianProfileService;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private User testUser;
    private TechnicianProfile testTechnicianProfile;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id("user123")
                .username("testuser")
                .email("test@test.com")
                .firstName("Test")
                .lastName("User")
                .phoneNumber("1234567890")
                .city("TestCity")
                .state("TestState")
                .zipCode("12345")
                .roles(Set.of(com.app.auth.model.UserRole.ROLE_TECHNICIAN))
                .enabled(true)
                .emailVerified(true)
                .build();

        testTechnicianProfile = TechnicianProfile.builder()
                .id("tech123")
                .userId("user123")
                .skills(Set.of("PLUMBING", "ELECTRICAL"))
                .experienceYears(5)
                .bio("Experienced technician")
                .city("TestCity")
                .state("TestState")
                .approvalStatus(TechnicianProfile.ApprovalStatus.APPROVED)
                .available(true)
                .averageRating(4.5)
                .totalJobsCompleted(50)
                .currentActiveJobs(2)
                .createdAt(Instant.now())
                .build();
    }

    // ==================== USER APIs ====================

    @Test
    void getUserById_success() throws Exception {
        when(userRepository.findById("user123")).thenReturn(Optional.of(testUser));

        mockMvc.perform(get("/api/internal/users/user123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User found"))
                .andExpect(jsonPath("$.data.id").value("user123"))
                .andExpect(jsonPath("$.data.username").value("testuser"));
    }

    @Test
    void getUserById_notFound() throws Exception {
        when(userRepository.findById("nonexistent")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/internal/users/nonexistent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    @Test
    void getUserByUsername_success() throws Exception {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        mockMvc.perform(get("/api/internal/users/username/testuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User found"));
    }

    @Test
    void getUserByUsername_notFound() throws Exception {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/internal/users/username/nonexistent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    @Test
    void validateUser_exists() throws Exception {
        when(userRepository.existsById("user123")).thenReturn(true);

        mockMvc.perform(get("/api/internal/users/user123/validate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User exists"))
                .andExpect(jsonPath("$.data.exists").value(true))
                .andExpect(jsonPath("$.data.valid").value(true));
    }

    @Test
    void validateUser_notExists() throws Exception {
        when(userRepository.existsById("nonexistent")).thenReturn(false);

        mockMvc.perform(get("/api/internal/users/nonexistent/validate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User not found"))
                .andExpect(jsonPath("$.data.exists").value(false));
    }

    // ==================== TECHNICIAN APIs ====================

    @Test
    void getAvailableTechnicians_success() throws Exception {
        when(technicianProfileRepository.findAvailableTechnicians())
                .thenReturn(List.of(testTechnicianProfile));
        when(userRepository.findById("user123")).thenReturn(Optional.of(testUser));

        mockMvc.perform(get("/api/internal/technicians/available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Available technicians retrieved"));
    }

    @Test
    void getTechnicianByUserId_success() throws Exception {
        when(technicianProfileRepository.findByUserId("user123"))
                .thenReturn(Optional.of(testTechnicianProfile));
        when(userRepository.findById("user123")).thenReturn(Optional.of(testUser));

        mockMvc.perform(get("/api/internal/technicians/user123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Technician found"));
    }

    @Test
    void getTechnicianByUserId_notFound() throws Exception {
        when(technicianProfileRepository.findByUserId("nonexistent")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/internal/technicians/nonexistent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Technician not found"));
    }

    @Test
    void validateTechnician_approved() throws Exception {
        when(technicianProfileRepository.findByUserId("user123"))
                .thenReturn(Optional.of(testTechnicianProfile));

        mockMvc.perform(get("/api/internal/technicians/user123/validate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Technician validation complete"))
                .andExpect(jsonPath("$.data.exists").value(true))
                .andExpect(jsonPath("$.data.approved").value(true))
                .andExpect(jsonPath("$.data.available").value(true))
                .andExpect(jsonPath("$.data.canAssign").value(true));
    }

    @Test
    void validateTechnician_pending() throws Exception {
        TechnicianProfile pendingProfile = TechnicianProfile.builder()
                .id("tech456")
                .userId("user456")
                .approvalStatus(TechnicianProfile.ApprovalStatus.PENDING)
                .available(false)
                .build();

        when(technicianProfileRepository.findByUserId("user456"))
                .thenReturn(Optional.of(pendingProfile));

        mockMvc.perform(get("/api/internal/technicians/user456/validate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.exists").value(true))
                .andExpect(jsonPath("$.data.approved").value(false))
                .andExpect(jsonPath("$.data.canAssign").value(false));
    }

    @Test
    void validateTechnician_notFound() throws Exception {
        when(technicianProfileRepository.findByUserId("nonexistent")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/internal/technicians/nonexistent/validate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Technician not found"))
                .andExpect(jsonPath("$.data.exists").value(false))
                .andExpect(jsonPath("$.data.canAssign").value(false));
    }

    @Test
    void incrementTechnicianJobs_success() throws Exception {
        when(technicianProfileRepository.findByUserId("user123"))
                .thenReturn(Optional.of(testTechnicianProfile));
        when(technicianProfileRepository.save(any(TechnicianProfile.class)))
                .thenReturn(testTechnicianProfile);

        mockMvc.perform(put("/api/internal/technicians/user123/increment-jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Jobs count incremented"));

        verify(technicianProfileRepository).save(any(TechnicianProfile.class));
    }

    @Test
    void incrementTechnicianJobs_notFound() throws Exception {
        when(technicianProfileRepository.findByUserId("nonexistent")).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/internal/technicians/nonexistent/increment-jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Technician not found"));
    }

    @Test
    void updateTechnicianAvailability_success() throws Exception {
        when(technicianProfileRepository.findByUserId("user123"))
                .thenReturn(Optional.of(testTechnicianProfile));
        when(technicianProfileRepository.save(any(TechnicianProfile.class)))
                .thenReturn(testTechnicianProfile);

        mockMvc.perform(put("/api/internal/technicians/user123/availability")
                        .param("available", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Availability updated"));
    }

    @Test
    void updateTechnicianAvailability_notFound() throws Exception {
        when(technicianProfileRepository.findByUserId("nonexistent")).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/internal/technicians/nonexistent/availability")
                        .param("available", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Technician not found"));
    }

    @Test
    void getTechniciansBySkill_success() throws Exception {
        when(technicianProfileRepository.findByCityAndSkills(isNull(), eq(List.of("PLUMBING"))))
                .thenReturn(List.of(testTechnicianProfile));
        when(userRepository.findById("user123")).thenReturn(Optional.of(testUser));

        mockMvc.perform(get("/api/internal/technicians/by-skill")
                        .param("skill", "PLUMBING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Technicians retrieved"));
    }

    // ==================== EDGE CASES ====================

    @Test
    void buildUserData_withNullNames() throws Exception {
        User userWithNullNames = User.builder()
                .id("user789")
                .username("nullnameuser")
                .email("null@test.com")
                .firstName(null)
                .lastName(null)
                .roles(Set.of(com.app.auth.model.UserRole.ROLE_CUSTOMER))
                .enabled(true)
                .emailVerified(true)
                .build();

        when(userRepository.findById("user789")).thenReturn(Optional.of(userWithNullNames));

        mockMvc.perform(get("/api/internal/users/user789"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fullName").value("Unknown"));
    }

    @Test
    void buildUserData_withOnlyFirstName() throws Exception {
        User userWithFirstName = User.builder()
                .id("user101")
                .username("firstnameuser")
                .email("first@test.com")
                .firstName("OnlyFirst")
                .lastName(null)
                .roles(Set.of(com.app.auth.model.UserRole.ROLE_CUSTOMER))
                .enabled(true)
                .emailVerified(true)
                .build();

        when(userRepository.findById("user101")).thenReturn(Optional.of(userWithFirstName));

        mockMvc.perform(get("/api/internal/users/user101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fullName").value("OnlyFirst"));
    }

    @Test
    void buildUserData_withOnlyLastName() throws Exception {
        User userWithLastName = User.builder()
                .id("user102")
                .username("lastnameuser")
                .email("last@test.com")
                .firstName(null)
                .lastName("OnlyLast")
                .roles(Set.of(com.app.auth.model.UserRole.ROLE_CUSTOMER))
                .enabled(true)
                .emailVerified(true)
                .build();

        when(userRepository.findById("user102")).thenReturn(Optional.of(userWithLastName));

        mockMvc.perform(get("/api/internal/users/user102"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fullName").value("OnlyLast"));
    }

    @Test
    void buildTechnicianData_userNotFound() throws Exception {
        when(technicianProfileRepository.findAvailableTechnicians())
                .thenReturn(List.of(testTechnicianProfile));
        when(userRepository.findById("user123")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/internal/technicians/available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}

