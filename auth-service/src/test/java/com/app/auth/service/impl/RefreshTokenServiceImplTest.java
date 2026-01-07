package com.app.auth.service.impl;

import com.app.auth.dto.response.AuthResponseDTO;
import com.app.auth.exception.InvalidTokenException;
import com.app.auth.exception.TokenExpiredException;
import com.app.auth.model.RefreshToken;
import com.app.auth.model.User;
import com.app.auth.model.UserRole;
import com.app.auth.repository.RefreshTokenRepository;
import com.app.auth.repository.UserRepository;
import com.app.auth.security.JwtUtils;
import com.app.auth.service.AuditLogService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {

    @InjectMocks
    private RefreshTokenServiceImpl refreshTokenService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private AuditLogService auditLogService;

    private User testUser;
    private RefreshToken testRefreshToken;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(refreshTokenService, "refreshTokenDurationMs", 604800000L);
        ReflectionTestUtils.setField(refreshTokenService, "maxRefreshTokensPerUser", 5);

        testUser = User.builder()
                .id("user123")
                .username("testuser")
                .email("test@test.com")
                .firstName("Test")
                .lastName("User")
                .roles(Set.of(UserRole.ROLE_CUSTOMER))
                .enabled(true)
                .emailVerified(true)
                .build();

        testRefreshToken = RefreshToken.builder()
                .id("token123")
                .token("valid-refresh-token")
                .userId("user123")
                .tokenFamily("family123")
                .expiryDate(Instant.now().plusSeconds(3600))
                .ipAddress("127.0.0.1")
                .userAgent("Test-Agent")
                .revoked(false)
                .build();
    }

    // ==================== CREATE REFRESH TOKEN TESTS ====================

    @Test
    void createRefreshToken_success() {
        when(refreshTokenRepository.countActiveTokensByUserId(eq("user123"), any(Instant.class)))
                .thenReturn(2L);
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        RefreshToken result = refreshTokenService.createRefreshToken("user123", "127.0.0.1", "Test-Agent");

        assertNotNull(result);
        assertEquals("user123", result.getUserId());
        assertEquals("127.0.0.1", result.getIpAddress());
        assertEquals("Test-Agent", result.getUserAgent());
        assertFalse(result.isRevoked());
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void createRefreshToken_maxTokensReached() {
        when(refreshTokenRepository.countActiveTokensByUserId(eq("user123"), any(Instant.class)))
                .thenReturn(5L);
        when(refreshTokenRepository.findByUserId("user123"))
                .thenReturn(List.of(testRefreshToken));
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(refreshTokenRepository.findByToken(anyString()))
                .thenReturn(Optional.of(testRefreshToken));

        RefreshToken result = refreshTokenService.createRefreshToken("user123", "127.0.0.1", "Test-Agent");

        assertNotNull(result);
        verify(refreshTokenRepository, atLeast(1)).save(any(RefreshToken.class));
    }

    // ==================== REFRESH ACCESS TOKEN TESTS ====================

    @Test
    void refreshAccessToken_success() {
        when(refreshTokenRepository.findByToken("valid-refresh-token"))
                .thenReturn(Optional.of(testRefreshToken));
        when(userRepository.findById("user123")).thenReturn(Optional.of(testUser));
        when(jwtUtils.generateAccessToken(any(User.class))).thenReturn("new-access-token");
        when(jwtUtils.getAccessTokenExpiration()).thenReturn(3600000L);
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        AuthResponseDTO result = refreshTokenService.refreshAccessToken(
                "valid-refresh-token", "127.0.0.1", "Test-Agent");

        assertNotNull(result);
        assertEquals("new-access-token", result.getAccessToken());
        assertNotNull(result.getRefreshToken());
        assertEquals("Bearer", result.getTokenType());
        verify(auditLogService).logTokenRefresh(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void refreshAccessToken_tokenNotFound() {
        when(refreshTokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        assertThrows(InvalidTokenException.class, () ->
                refreshTokenService.refreshAccessToken("invalid-token", "127.0.0.1", "Test-Agent")
        );
    }

    @Test
    void refreshAccessToken_tokenRevoked() {
        testRefreshToken.setRevoked(true);
        when(refreshTokenRepository.findByToken("revoked-token"))
                .thenReturn(Optional.of(testRefreshToken));
        when(refreshTokenRepository.findByTokenFamily("family123"))
                .thenReturn(List.of(testRefreshToken));

        assertThrows(InvalidTokenException.class, () ->
                refreshTokenService.refreshAccessToken("revoked-token", "127.0.0.1", "Test-Agent")
        );
    }

    @Test
    void refreshAccessToken_tokenExpired() {
        testRefreshToken.setExpiryDate(Instant.now().minusSeconds(3600));
        when(refreshTokenRepository.findByToken("expired-token"))
                .thenReturn(Optional.of(testRefreshToken));

        assertThrows(TokenExpiredException.class, () ->
                refreshTokenService.refreshAccessToken("expired-token", "127.0.0.1", "Test-Agent")
        );
    }

    @Test
    void refreshAccessToken_userNotFound() {
        when(refreshTokenRepository.findByToken("valid-refresh-token"))
                .thenReturn(Optional.of(testRefreshToken));
        when(userRepository.findById("user123")).thenReturn(Optional.empty());

        assertThrows(InvalidTokenException.class, () ->
                refreshTokenService.refreshAccessToken("valid-refresh-token", "127.0.0.1", "Test-Agent")
        );
    }

    // ==================== FIND BY TOKEN TESTS ====================

    @Test
    void findByToken_success() {
        when(refreshTokenRepository.findByToken("valid-token"))
                .thenReturn(Optional.of(testRefreshToken));

        RefreshToken result = refreshTokenService.findByToken("valid-token");

        assertNotNull(result);
        assertEquals("valid-refresh-token", result.getToken());
    }

    @Test
    void findByToken_notFound() {
        when(refreshTokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        assertThrows(InvalidTokenException.class, () ->
                refreshTokenService.findByToken("invalid-token")
        );
    }

    // ==================== REVOKE TOKEN TESTS ====================

    @Test
    void revokeToken_success() {
        when(refreshTokenRepository.findByToken("valid-token"))
                .thenReturn(Optional.of(testRefreshToken));

        refreshTokenService.revokeToken("valid-token", "User logout");

        assertTrue(testRefreshToken.isRevoked());
        assertEquals("User logout", testRefreshToken.getRevokedReason());
        verify(refreshTokenRepository).save(testRefreshToken);
    }

    @Test
    void revokeToken_notFound() {
        when(refreshTokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        assertThrows(InvalidTokenException.class, () ->
                refreshTokenService.revokeToken("invalid-token", "Reason")
        );
    }

    // ==================== REVOKE ALL USER TOKENS TESTS ====================

    @Test
    void revokeAllUserTokens_success() {
        RefreshToken token1 = RefreshToken.builder()
                .id("t1")
                .token("token1")
                .userId("user123")
                .revoked(false)
                .build();

        RefreshToken token2 = RefreshToken.builder()
                .id("t2")
                .token("token2")
                .userId("user123")
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByUserId("user123")).thenReturn(List.of(token1, token2));

        refreshTokenService.revokeAllUserTokens("user123", "Account security");

        assertTrue(token1.isRevoked());
        assertTrue(token2.isRevoked());
        assertEquals("Account security", token1.getRevokedReason());
        assertEquals("Account security", token2.getRevokedReason());
        verify(refreshTokenRepository).saveAll(anyList());
    }

    @Test
    void revokeAllUserTokens_alreadyRevoked() {
        RefreshToken revokedToken = RefreshToken.builder()
                .id("t1")
                .token("token1")
                .userId("user123")
                .revoked(true)
                .revokedReason("Previous reason")
                .build();

        when(refreshTokenRepository.findByUserId("user123")).thenReturn(List.of(revokedToken));

        refreshTokenService.revokeAllUserTokens("user123", "New reason");

        // Should not change already revoked token
        assertEquals("Previous reason", revokedToken.getRevokedReason());
        verify(refreshTokenRepository).saveAll(anyList());
    }

    // ==================== CLEANUP EXPIRED TOKENS TESTS ====================

    @Test
    void cleanupExpiredTokens_success() {
        RefreshToken expiredToken = RefreshToken.builder()
                .id("expired")
                .token("expired-token")
                .expiryDate(Instant.now().minusSeconds(3600))
                .build();

        when(refreshTokenRepository.findExpiredTokens(any(Instant.class)))
                .thenReturn(List.of(expiredToken));

        refreshTokenService.cleanupExpiredTokens();

        verify(refreshTokenRepository).deleteAll(anyList());
    }

    @Test
    void cleanupExpiredTokens_noExpiredTokens() {
        when(refreshTokenRepository.findExpiredTokens(any(Instant.class)))
                .thenReturn(List.of());

        refreshTokenService.cleanupExpiredTokens();

        verify(refreshTokenRepository, never()).deleteAll(anyList());
    }
}

