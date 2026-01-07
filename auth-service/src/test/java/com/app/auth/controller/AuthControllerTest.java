package com.app.auth.controller;

import com.app.auth.config.TestSecurityConfig;
import com.app.auth.dto.request.LoginRequestDTO;
import com.app.auth.dto.request.RefreshTokenRequestDTO;
import com.app.auth.dto.request.RegisterCustomerDTO;
import com.app.auth.dto.request.RegisterTechnicianDTO;
import com.app.auth.dto.response.AuthResponseDTO;
import com.app.auth.dto.response.RegistrationResponseDTO;
import com.app.auth.security.JwtAuthenticationFilter;
import com.app.auth.security.JwtUtils;
import com.app.auth.service.AuthService;
import com.app.auth.service.RefreshTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ===== Mocked dependencies of AuthController =====

    @MockBean
    private AuthService authService;

    @MockBean
    private RefreshTokenService refreshTokenService;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    // ===================== LOGIN =====================

    @Test
    void login_success() throws Exception {
        LoginRequestDTO request =
                new LoginRequestDTO("john", "password", null);

        AuthResponseDTO response = AuthResponseDTO.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .build();

        when(authService.login(any(LoginRequestDTO.class), any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    // ================= REGISTER CUSTOMER =================

    @Test
    void registerCustomer_success() throws Exception {
        RegisterCustomerDTO dto = new RegisterCustomerDTO(
                "john",
                "john@test.com",
                "Password@123",
                "John",
                "Doe",
                "9876543210",
                "City",
                "State",
                "123456"
        );

        RegistrationResponseDTO response =
                RegistrationResponseDTO.builder()
                        .userId("user123")
                        .build();

        when(authService.registerCustomer(
                anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString()
        )).thenReturn(response);

        mockMvc.perform(post("/api/auth/register/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());
    }

    // ================= REGISTER TECHNICIAN =================

    @Test
    void registerTechnician_success() throws Exception {
        RegisterTechnicianDTO dto = new RegisterTechnicianDTO(
                "tech",
                "tech@test.com",
                "Password@123",
                "Tech",
                "Guy",
                "9876543210",
                Set.of("AC"),
                5,
                "Experienced",
                "City",
                "State",
                "AADHAAR"
        );

        RegistrationResponseDTO response =
                RegistrationResponseDTO.builder()
                        .userId("tech123")
                        .build();

        when(authService.registerTechnician(
                anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString(),
                anySet(), anyInt(), anyString(),
                anyString(), anyString(), anyString()
        )).thenReturn(response);

        mockMvc.perform(post("/api/auth/register/technician")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());
    }

    // ================= REFRESH TOKEN =================

    @Test
    void refreshToken_success() throws Exception {
        RefreshTokenRequestDTO request =
                new RefreshTokenRequestDTO("refresh-token");

        AuthResponseDTO response = AuthResponseDTO.builder()
                .accessToken("new-access-token")
                .refreshToken("new-refresh-token")
                .build();

        when(refreshTokenService.refreshAccessToken(
                anyString(), anyString(), anyString()
        )).thenReturn(response);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    // ================= VERIFY EMAIL =================

    @Test
    void verifyEmail_success() throws Exception {
        doNothing().when(authService).verifyEmail(anyString());

        mockMvc.perform(get("/api/auth/verify-email")
                        .param("token", "dummy-token"))
                .andExpect(status().isOk());
    }

    // ================= LOGOUT =================

    @Test
    void logout_success() throws Exception {
        RefreshTokenRequestDTO request =
                new RefreshTokenRequestDTO("refresh-token");

        doNothing().when(authService).logout(anyString(), any());

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}
