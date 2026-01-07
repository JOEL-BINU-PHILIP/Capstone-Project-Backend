package com.app.auth.controller;

import com.app.auth.config.TestSecurityConfig;
import com.app.auth.dto.request.ChangePasswordRequest;
import com.app.auth.dto.request.UpdateProfileRequest;
import com.app.auth.dto.response.UserProfileResponseDTO;
import com.app.auth.security.JwtAuthenticationFilter;
import com.app.auth.security.JwtUtils;
import com.app.auth.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private UserProfileResponseDTO testProfile;

    @BeforeEach
    void setUp() {
        testProfile = UserProfileResponseDTO.builder()
                .id("user123")
                .username("testuser")
                .email("test@test.com")
                .firstName("Test")
                .lastName("User")
                .fullName("Test User")
                .phoneNumber("1234567890")
                .city("TestCity")
                .state("TestState")
                .zipCode("12345")
                .roles(List.of("ROLE_CUSTOMER"))
                .enabled(true)
                .emailVerified(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    // ==================== DELETE/DEACTIVATE USER ====================

    @Test
    void deleteUser_success() throws Exception {
        doNothing().when(userService).deactivateUser("user123");

        mockMvc.perform(delete("/api/users/deactivate/user123"))
                .andExpect(status().isOk());

        verify(userService).deactivateUser("user123");
    }
}
