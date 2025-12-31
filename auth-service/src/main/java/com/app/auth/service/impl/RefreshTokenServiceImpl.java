package com.app.auth.service.impl;
import com.app.auth.dto.response.AuthResponseDTO;
import com.app.auth.exception.InvalidTokenException;
import com.app.auth.exception.TokenExpiredException;
import com.app.auth.model.RefreshToken;
import com.app.auth.model.User;
import com.app.auth.repository.RefreshTokenRepository;
import com.app.auth.repository.UserRepository;
import com.app.auth.security.JwtUtils;
import com.app.auth.service.AuditLogService;
import com.app.auth.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;
    private final AuditLogService auditLogService;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenDurationMs;

    @Value("${app.security.max-refresh-tokens-per-user:5}")
    private int maxRefreshTokensPerUser;

    @Override
    @Transactional
    public RefreshToken createRefreshToken(String userId, String ipAddress, String userAgent) {

        // Cleanup old tokens if user has too many
        long activeTokenCount = refreshTokenRepository
                .countActiveTokensByUserId(userId, Instant.now());

        if (activeTokenCount >= maxRefreshTokensPerUser) {
            List<RefreshToken> userTokens = refreshTokenRepository.findByUserId(userId);
            userTokens.stream()
                    .filter(t -> !t.isRevoked() && !t.isExpired())
                    .findFirst()
                    .ifPresent(oldestToken -> {
                        revokeToken(oldestToken.getToken(), "Max tokens limit reached");
                    });
        }

        String tokenFamily = UUID.randomUUID().toString();

        RefreshToken refreshToken = RefreshToken.builder()
                .userId(userId)
                .token(UUID.randomUUID().toString())
                .tokenFamily(tokenFamily)
                .expiryDate(Instant.now().plusMillis(refreshTokenDurationMs))
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .revoked(false)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    @Override
    @Transactional
    public AuthResponseDTO refreshAccessToken(
            String refreshTokenStr,
            String ipAddress,
            String userAgent
    ) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenStr)
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        // Check if revoked
        if (refreshToken.isRevoked()) {
            // Possible token theft - revoke entire token family
            log.warn("Attempted reuse of revoked token. Revoking token family: {}",
                    refreshToken.getTokenFamily());
            revokeTokenFamily(refreshToken.getTokenFamily(), "Token reuse detected");
            throw new InvalidTokenException("Refresh token has been revoked");
        }

        // Check if expired
        if (refreshToken.isExpired()) {
            refreshTokenRepository.delete(refreshToken);
            throw new TokenExpiredException("Refresh token has expired");
        }

        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new InvalidTokenException("User not found"));

        // Token rotation: revoke current token and issue new one
        refreshToken.setRevoked(true);
        refreshToken.setRevokedReason("Token rotated");
        refreshToken.setUsedAt(Instant.now());
        refreshTokenRepository.save(refreshToken);

        // Create new refresh token in same family
        RefreshToken newRefreshToken = RefreshToken.builder()
                .userId(user.getId())
                .token(UUID.randomUUID().toString())
                .tokenFamily(refreshToken.getTokenFamily())
                .expiryDate(Instant.now().plusMillis(refreshTokenDurationMs))
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .revoked(false)
                .build();

        newRefreshToken = refreshTokenRepository.save(newRefreshToken);

        // Generate new access token
        String newAccessToken = jwtUtils.generateAccessToken(user.getUsername(), user.getRoles());

        // Audit log
        auditLogService.logTokenRefresh(
                user.getId(),
                user.getUsername(),
                ipAddress,
                userAgent
        );

        return AuthResponseDTO.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtUtils.getAccessTokenExpiration() / 1000)
                .user(AuthResponseDTO.UserInfoDTO.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .roles(user.getRoles())
                        .emailVerified(user.isEmailVerified())
                        .twoFactorEnabled(user.isTwoFactorEnabled())
                        .build())
                .build();
    }

    @Override
    public RefreshToken findByToken(String token) {
        return refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));
    }

    @Override
    @Transactional
    public void revokeToken(String token, String reason) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("Token not found"));

        refreshToken.setRevoked(true);
        refreshToken.setRevokedReason(reason);
        refreshTokenRepository.save(refreshToken);

        log.info("Refresh token revoked: {} - Reason: {}", token, reason);
    }

    @Override
    @Transactional
    public void revokeAllUserTokens(String userId, String reason) {
        List<RefreshToken> tokens = refreshTokenRepository.findByUserId(userId);
        tokens.forEach(token -> {
            if (!token.isRevoked()) {
                token.setRevoked(true);
                token.setRevokedReason(reason);
            }
        });
        refreshTokenRepository.saveAll(tokens);

        log.info("All refresh tokens revoked for user: {} - Reason: {}", userId, reason);
    }

    @Override
    @Transactional
    public void cleanupExpiredTokens() {
        List<RefreshToken> expiredTokens = refreshTokenRepository
                .findExpiredTokens(Instant.now());

        if (!expiredTokens.isEmpty()) {
            refreshTokenRepository.deleteAll(expiredTokens);
            log.info("Cleaned up {} expired refresh tokens", expiredTokens.size());
        }
    }

    private void revokeTokenFamily(String tokenFamily, String reason) {
        List<RefreshToken> familyTokens = refreshTokenRepository
                .findByTokenFamily(tokenFamily);

        familyTokens.forEach(token -> {
            if (!token.isRevoked()) {
                token.setRevoked(true);
                token.setRevokedReason(reason);
            }
        });

        refreshTokenRepository.saveAll(familyTokens);
    }
}