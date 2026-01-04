package com.app.auth.service.impl;

import com.app.auth.dto.request.LoginRequestDTO;
import com.app.auth.dto.response.AuthResponseDTO;
import com.app.auth.dto.response.RegistrationResponseDTO;
import com.app.auth.exception.InvalidCredentialsException;
import com.app.auth.exception.UserAlreadyExistsException;
import com.app.auth.model.RefreshToken;
import com.app.auth.model.User;
import com.app.auth.model.UserRole;
import com.app.auth.repository.TechnicianProfileRepository;
import com.app.auth.repository.UserRepository;
import com.app.auth.security.JwtUtils;
import com.app.auth.service.AuditLogService;
import com.app.auth.service.EmailService;
import com.app.auth.service.RateLimitService;
import com.app.auth.service.RefreshTokenService;
import com.app.auth.service.TechnicianProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private TechnicianProfileRepository technicianProfileRepository;
    @Mock private TechnicianProfileService technicianProfileService;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private AuditLogService auditLogService;
    @Mock private RateLimitService rateLimitService;
    @Mock private EmailService emailService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtils jwtUtils;
    @Mock private HttpServletRequest httpRequest;

    @InjectMocks
    private AuthServiceImpl authService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id("1")
                .username("testuser")
                .email("test@example.com")
                .password("encodedPassword")
                .roles(Set.of(UserRole.ROLE_CUSTOMER))
                .enabled(true)
                .emailVerified(true)
                .build();
    }

    @Test
    void login_Success() {
        LoginRequestDTO request = new LoginRequestDTO("testuser", "password", null);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "encodedPassword")).thenReturn(true);
        when(jwtUtils.generateAccessToken(any(User.class))).thenReturn("access-token");

        RefreshToken refreshToken = RefreshToken.builder().token("refresh-token").build();
        when(refreshTokenService.createRefreshToken(anyString(), any(), any())).thenReturn(refreshToken);

        AuthResponseDTO response = authService.login(request, httpRequest);

        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        verify(auditLogService).logSuccessfulLogin(any(), any(), any(), any());
    }

    @Test
    void login_InvalidPassword_ThrowsException() {
        LoginRequestDTO request = new LoginRequestDTO("testuser", "wrongpassword", null);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpassword", "encodedPassword")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request, httpRequest));
        verify(auditLogService).logFailedLogin(anyString(), anyString(), any(), any());
    }

    @Test
    void registerCustomer_Success() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password@123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            savedUser.setId("new-id");
            return savedUser;
        });

        RegistrationResponseDTO response = authService.registerCustomer(
                "newuser", "new@example.com", "Password@123",
                "John", "Doe", "1234567890", "City", "State", "12345"
        );

        assertNotNull(response);
        assertEquals("new-id", response.getUserId());
        verify(emailService).sendVerificationEmail(anyString(), anyString(), anyString());
    }

    @Test
    void registerCustomer_UserExists_ThrowsException() {
        when(userRepository.existsByUsername("existing")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () ->
                authService.registerCustomer(
                        "existing", "email@example.com", "pass",
                        "First", "Last", "1234567890", "City", "State", "12345"
                )
        );
    }
}