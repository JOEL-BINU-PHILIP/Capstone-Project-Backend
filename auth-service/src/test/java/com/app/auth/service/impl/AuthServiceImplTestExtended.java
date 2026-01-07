package com.app.auth.service.impl;

import com.app.auth.dto.request.LoginRequestDTO;
import com.app.auth.dto.response.AuthResponseDTO;
import com.app.auth.dto.response.RegistrationResponseDTO;
import com.app.auth.exception.*;
import com.app.auth.model.*;
import com.app.auth.repository.TechnicianProfileRepository;
import com.app.auth.repository.UserRepository;
import com.app.auth.security.JwtUtils;
import com.app.auth.service.*;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceImplTestExtended {

    @InjectMocks
    private AuthServiceImpl authService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TechnicianProfileRepository technicianProfileRepository;

    @Mock
    private TechnicianProfileService technicianProfileService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private RateLimitService rateLimitService;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private HttpServletRequest httpRequest;

    private User testUser;
    private RefreshToken testRefreshToken;

    @BeforeEach
    void setUp() {
        // Set up @Value fields
        ReflectionTestUtils.setField(authService, "maxLoginAttempts", 5);
        ReflectionTestUtils.setField(authService, "accountLockDurationMinutes", 30L);
        ReflectionTestUtils.setField(authService, "emailVerificationTokenExpiryHours", 24L);

        testUser = User.builder()
                .id("user123")
                .username("testuser")
                .email("test@test.com")
                .password("hashedPassword")
                .firstName("Test")
                .lastName("User")
                .roles(Set.of(UserRole.ROLE_CUSTOMER))
                .enabled(true)
                .emailVerified(true)
                .accountNonLocked(true)
                .failedLoginAttempts(0)
                .build();

        testRefreshToken = RefreshToken.builder()
                .id("token123")
                .token("refresh-token-value")
                .userId("user123")
                .expiryDate(Instant.now().plusSeconds(3600))
                .build();

        // Mock all potential header calls
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(httpRequest.getHeader(anyString())).thenReturn(null);
        when(httpRequest.getHeader("User-Agent")).thenReturn("Test-Agent");
    }

    // ==================== LOGIN TESTS ====================

    @Test
    void login_success() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtUtils.generateAccessToken(any(User.class))).thenReturn("access-token");
        when(jwtUtils.getAccessTokenExpiration()).thenReturn(3600000L);
        when(refreshTokenService.createRefreshToken(anyString(), anyString(), anyString()))
                .thenReturn(testRefreshToken);

        LoginRequestDTO request = new LoginRequestDTO("testuser", "password", null);
        AuthResponseDTO response = authService.login(request, httpRequest);

        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token-value", response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        verify(auditLogService).logSuccessfulLogin(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void login_userNotFound() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        LoginRequestDTO request = new LoginRequestDTO("unknown", "password", null);

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request, httpRequest));
        verify(auditLogService).logFailedLogin(eq("unknown"), eq("User not found"), anyString(), anyString());
    }

    @Test
    void login_accountLocked() {
        testUser.setAccountNonLocked(false);
        testUser.setLockedUntil(Instant.now().plusSeconds(3600));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        LoginRequestDTO request = new LoginRequestDTO("testuser", "password", null);

        assertThrows(AccountLockedException.class, () -> authService.login(request, httpRequest));
    }

    @Test
    void login_invalidPassword() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        LoginRequestDTO request = new LoginRequestDTO("testuser", "wrongpassword", null);

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request, httpRequest));
    }

    @Test
    void login_emailNotVerified() {
        testUser.setEmailVerified(false);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        LoginRequestDTO request = new LoginRequestDTO("testuser", "password", null);

        assertThrows(EmailNotVerifiedException.class, () -> authService.login(request, httpRequest));
    }

    @Test
    void login_technicianPending() {
        User techUser = User.builder()
                .id("tech123")
                .username("techuser")
                .password("hashedPassword")
                .roles(Set.of(UserRole.ROLE_TECHNICIAN))
                .enabled(true)
                .emailVerified(true)
                .accountNonLocked(true)
                .build();

        TechnicianProfile profile = TechnicianProfile.builder()
                .id("profile123")
                .userId("tech123")
                .approvalStatus(TechnicianProfile.ApprovalStatus.PENDING)
                .build();

        when(userRepository.findByUsername("techuser")).thenReturn(Optional.of(techUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(technicianProfileRepository.findByUserId("tech123")).thenReturn(Optional.of(profile));

        LoginRequestDTO request = new LoginRequestDTO("techuser", "password", null);

        assertThrows(TechnicianNotApprovedException.class, () -> authService.login(request, httpRequest));
    }

    @Test
    void login_technicianRejected() {
        User techUser = User.builder()
                .id("tech123")
                .username("techuser")
                .password("hashedPassword")
                .roles(Set.of(UserRole.ROLE_TECHNICIAN))
                .enabled(true)
                .emailVerified(true)
                .accountNonLocked(true)
                .build();

        TechnicianProfile profile = TechnicianProfile.builder()
                .id("profile123")
                .userId("tech123")
                .approvalStatus(TechnicianProfile.ApprovalStatus.REJECTED)
                .rejectionReason("Invalid documents")
                .build();

        when(userRepository.findByUsername("techuser")).thenReturn(Optional.of(techUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(technicianProfileRepository.findByUserId("tech123")).thenReturn(Optional.of(profile));

        LoginRequestDTO request = new LoginRequestDTO("techuser", "password", null);

        assertThrows(TechnicianNotApprovedException.class, () -> authService.login(request, httpRequest));
    }

    @Test
    void login_technicianSuspended() {
        User techUser = User.builder()
                .id("tech123")
                .username("techuser")
                .password("hashedPassword")
                .roles(Set.of(UserRole.ROLE_TECHNICIAN))
                .enabled(true)
                .emailVerified(true)
                .accountNonLocked(true)
                .build();

        TechnicianProfile profile = TechnicianProfile.builder()
                .id("profile123")
                .userId("tech123")
                .approvalStatus(TechnicianProfile.ApprovalStatus.SUSPENDED)
                .build();

        when(userRepository.findByUsername("techuser")).thenReturn(Optional.of(techUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(technicianProfileRepository.findByUserId("tech123")).thenReturn(Optional.of(profile));

        LoginRequestDTO request = new LoginRequestDTO("techuser", "password", null);

        assertThrows(TechnicianNotApprovedException.class, () -> authService.login(request, httpRequest));
    }

    @Test
    void login_technicianProfileNotFound() {
        User techUser = User.builder()
                .id("tech123")
                .username("techuser")
                .password("hashedPassword")
                .roles(Set.of(UserRole.ROLE_TECHNICIAN))
                .enabled(true)
                .emailVerified(true)
                .accountNonLocked(true)
                .build();

        when(userRepository.findByUsername("techuser")).thenReturn(Optional.of(techUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(technicianProfileRepository.findByUserId("tech123")).thenReturn(Optional.empty());

        LoginRequestDTO request = new LoginRequestDTO("techuser", "password", null);

        assertThrows(AuthException.class, () -> authService.login(request, httpRequest));
    }

    @Test
    void login_locksAccountAfterMaxAttempts() {
        testUser.setFailedLoginAttempts(4);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        LoginRequestDTO request = new LoginRequestDTO("testuser", "wrongpassword", null);

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request, httpRequest));
        verify(auditLogService).log(anyString(), anyString(), eq(AuditLog.AuditAction.ACCOUNT_LOCKED),
                anyString(), anyString(), anyString(), eq(false), anyString());
    }

    // ==================== REGISTER CUSTOMER TESTS ====================

    @Test
    void registerCustomer_success() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId("newUser123");
            return user;
        });

        RegistrationResponseDTO response = authService.registerCustomer(
                "newuser", "new@test.com", "Password@123",
                "New", "User", "1234567890",
                "City", "State", "12345"
        );

        assertNotNull(response);
        assertEquals("newUser123", response.getUserId());
        assertEquals("newuser", response.getUsername());
        assertTrue(response.isEmailVerificationRequired());
        assertFalse(response.isApprovalRequired());
        verify(emailService).sendVerificationEmail(eq("new@test.com"), eq("newuser"), anyString());
    }

    @Test
    void registerCustomer_usernameExists() {
        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () ->
                authService.registerCustomer("existinguser", "new@test.com", "Password@123",
                        "New", "User", "1234567890", "City", "State", "12345")
        );
    }

    @Test
    void registerCustomer_emailExists() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("existing@test.com")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () ->
                authService.registerCustomer("newuser", "existing@test.com", "Password@123",
                        "New", "User", "1234567890", "City", "State", "12345")
        );
    }

    // ==================== REGISTER TECHNICIAN TESTS ====================

    @Test
    void registerTechnician_success() {
        when(userRepository.existsByUsername("newtech")).thenReturn(false);
        when(userRepository.existsByEmail("tech@test.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId("newTech123");
            return user;
        });

        RegistrationResponseDTO response = authService.registerTechnician(
                "newtech", "tech@test.com", "Password@123",
                "New", "Tech", "1234567890",
                Set.of("PLUMBING"), 5, "Experienced plumber",
                "City", "State", "AADHAAR"
        );

        assertNotNull(response);
        assertEquals("newTech123", response.getUserId());
        assertTrue(response.isEmailVerificationRequired());
        assertTrue(response.isApprovalRequired());
        verify(technicianProfileService).createProfile(eq("newTech123"), anySet(), anyInt(),
                anyString(), anyString(), anyString(), anyString());
        verify(emailService).sendVerificationEmail(eq("tech@test.com"), eq("newtech"), anyString());
    }

    // ==================== VERIFY EMAIL TESTS ====================

    @Test
    void verifyEmail_success() {
        testUser.setEmailVerificationToken("valid-token");
        testUser.setEmailVerificationTokenExpiry(Instant.now().plusSeconds(3600));
        testUser.setEmailVerified(false);

        when(userRepository.findByEmailVerificationToken("valid-token")).thenReturn(Optional.of(testUser));

        authService.verifyEmail("valid-token");

        assertTrue(testUser.isEmailVerified());
        assertNull(testUser.getEmailVerificationToken());
        verify(userRepository).save(testUser);
    }

    @Test
    void verifyEmail_invalidToken() {
        when(userRepository.findByEmailVerificationToken("invalid-token")).thenReturn(Optional.empty());

        assertThrows(InvalidTokenException.class, () -> authService.verifyEmail("invalid-token"));
    }

    @Test
    void verifyEmail_expiredToken() {
        testUser.setEmailVerificationToken("expired-token");
        testUser.setEmailVerificationTokenExpiry(Instant.now().minusSeconds(3600));

        when(userRepository.findByEmailVerificationToken("expired-token")).thenReturn(Optional.of(testUser));

        assertThrows(TokenExpiredException.class, () -> authService.verifyEmail("expired-token"));
    }

    // ==================== RESEND VERIFICATION EMAIL TESTS ====================

    @Test
    void resendVerificationEmail_success() {
        testUser.setEmailVerified(false);
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(testUser));

        authService.resendVerificationEmail("test@test.com");

        verify(userRepository).save(testUser);
        verify(emailService).sendVerificationEmail(eq("test@test.com"), eq("testuser"), anyString());
    }

    @Test
    void resendVerificationEmail_userNotFound() {
        when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                authService.resendVerificationEmail("unknown@test.com")
        );
    }

    @Test
    void resendVerificationEmail_alreadyVerified() {
        testUser.setEmailVerified(true);
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(testUser));

        assertThrows(IllegalStateException.class, () ->
                authService.resendVerificationEmail("test@test.com")
        );
    }

    // ==================== LOGOUT TESTS ====================

    @Test
    void logout_success() {
        when(refreshTokenService.findByToken("refresh-token")).thenReturn(testRefreshToken);
        when(userRepository.findById("user123")).thenReturn(Optional.of(testUser));

        authService.logout("refresh-token", httpRequest);

        verify(refreshTokenService).revokeToken("refresh-token", "User logout");
        verify(auditLogService).log(eq("user123"), eq("testuser"), eq(AuditLog.AuditAction.LOGOUT),
                anyString(), anyString(), anyString(), eq(true), isNull());
    }

    @Test
    void logout_userNotFound() {
        when(refreshTokenService.findByToken("refresh-token")).thenReturn(testRefreshToken);
        when(userRepository.findById("user123")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                authService.logout("refresh-token", httpRequest)
        );
    }

    // ==================== TECHNICIAN APPROVED LOGIN ====================

    @Test
    void login_technicianApproved_success() {
        User techUser = User.builder()
                .id("tech123")
                .username("techuser")
                .email("tech@test.com")
                .password("hashedPassword")
                .roles(Set.of(UserRole.ROLE_TECHNICIAN))
                .enabled(true)
                .emailVerified(true)
                .accountNonLocked(true)
                .failedLoginAttempts(0)
                .build();

        TechnicianProfile profile = TechnicianProfile.builder()
                .id("profile123")
                .userId("tech123")
                .approvalStatus(TechnicianProfile.ApprovalStatus.APPROVED)
                .build();

        when(userRepository.findByUsername("techuser")).thenReturn(Optional.of(techUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(technicianProfileRepository.findByUserId("tech123")).thenReturn(Optional.of(profile));
        when(jwtUtils.generateAccessToken(any(User.class))).thenReturn("access-token");
        when(jwtUtils.getAccessTokenExpiration()).thenReturn(3600000L);
        when(refreshTokenService.createRefreshToken(anyString(), anyString(), anyString()))
                .thenReturn(testRefreshToken);

        LoginRequestDTO request = new LoginRequestDTO("techuser", "password", null);
        AuthResponseDTO response = authService.login(request, httpRequest);

        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
    }
}

