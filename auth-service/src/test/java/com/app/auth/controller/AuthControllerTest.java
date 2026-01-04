package com.app.auth.controller;

import com.app.auth.dto.request.LoginRequestDTO;
import com.app.auth.dto.request.RefreshTokenRequestDTO;
import com.app.auth.dto.request.RegisterCustomerDTO;
import com.app.auth.dto.response.AuthResponseDTO;
import com.app.auth.dto.response.RegistrationResponseDTO;
import com.app.auth.model.UserRole;
import com.app.auth.security.JwtAuthenticationFilter;
import com.app.auth.security.JwtUtils;
import com.app.auth.service.AuthService;
import com.app.auth.service.RefreshTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for unit testing
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private RefreshTokenService refreshTokenService;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private ObjectMapper objectMapper;

    private LoginRequestDTO loginRequest;
    private AuthResponseDTO authResponse;

    @BeforeEach
    void setUp() {
        loginRequest = new LoginRequestDTO("testuser", "password123", null);

        AuthResponseDTO.UserInfoDTO userInfo = AuthResponseDTO.UserInfoDTO.builder()
                .id("1")
                .username("testuser")
                .email("test@example.com")
                .roles(Set.of(UserRole.ROLE_CUSTOMER))
                .build();

        authResponse = AuthResponseDTO.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .user(userInfo)
                .build();
    }

    @Test
    void login_Success() throws Exception {
        when(authService.login(any(LoginRequestDTO.class), any())).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"));
    }

    @Test
    void registerCustomer_Success() throws Exception {
        RegisterCustomerDTO registerRequest = new RegisterCustomerDTO(
                "newuser", "new@example.com", "Password@123", "John", "Doe", "9876543210", "City", "State", "123456"
        );

        RegistrationResponseDTO regResponse = RegistrationResponseDTO.builder()
                .userId("user-id-123")
                .username("newuser")
                .build();

        when(authService.registerCustomer(
                eq(registerRequest.getUsername()),
                eq(registerRequest.getEmail()),
                eq(registerRequest.getPassword()),
                eq(registerRequest.getFirstName()),
                eq(registerRequest.getLastName()),
                eq(registerRequest.getPhoneNumber()),
                eq(registerRequest.getCity()),
                eq(registerRequest.getState()),
                eq(registerRequest.getZipCode())
        )).thenReturn(regResponse);

        mockMvc.perform(post("/api/auth/register/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$").value("user-id-123"));
    }

    @Test
    void refreshToken_Success() throws Exception {
        RefreshTokenRequestDTO refreshRequest = new RefreshTokenRequestDTO("valid-refresh-token");

        when(refreshTokenService.refreshAccessToken(any(), any(), any())).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"));
    }
}