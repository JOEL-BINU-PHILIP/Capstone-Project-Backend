package com.app.auth.service;

import com.app.auth.model.RefreshToken;

public interface RefreshTokenService {

    RefreshToken createRefreshToken(String userId);

    RefreshToken verifyExpiration(String token);

    void deleteByUserId(String userId);
}
