package com.app.auth.service;

import com.app.auth.dto.response.AuthResponseDTO;
import com.app.auth.model.RefreshToken;

public interface RefreshTokenService {

    RefreshToken createRefreshToken(String userId, String ipAddress, String userAgent);

    AuthResponseDTO refreshAccessToken(String refreshToken, String ipAddress, String userAgent);

    RefreshToken findByToken(String token);

    void revokeToken(String token, String reason);

    void revokeAllUserTokens(String userId, String reason);

    void cleanupExpiredTokens();
}
