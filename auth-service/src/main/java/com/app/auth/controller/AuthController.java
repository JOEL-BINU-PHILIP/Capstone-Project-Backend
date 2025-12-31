package com.app.auth.controller;

import com.app.auth.dto.RegisterRequestDTO;
import com.app.auth.model.RefreshToken;
import com.app.auth.model.User;
import com.app.auth.security.JwtUtils;
import com.app.auth.service.AuthService;
import com.app.auth.service.RefreshTokenService;
import com.app.auth.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtUtils jwtUtils;
    private final RefreshTokenService refreshTokenService;
    private final UserService userService;

    // ===============================
    // LOGIN
    // ===============================
    @PostMapping("/login")
    public Map<String, String> login(
            @RequestParam String username,
            @RequestParam String password
    ) {

        User user = authService.authenticate(username, password);

        String accessToken = jwtUtils.generateAccessToken(user.getUsername());
        RefreshToken refreshToken =
                refreshTokenService.createRefreshToken(user.getId());

        return Map.of(
                "accessToken", accessToken,
                "tokenType", "Bearer",
                "refreshToken", refreshToken.getToken()
        );
    }

    // ===============================
    // REFRESH TOKEN
    // ===============================
    @PostMapping("/refresh")
    public Map<String, String> refresh(@RequestParam String refreshToken) {

        RefreshToken token =
                refreshTokenService.verifyExpiration(refreshToken);

        User user = userService
                .findById(token.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String newAccessToken =
                jwtUtils.generateAccessToken(user.getUsername());

        return Map.of(
                "accessToken", newAccessToken,
                "tokenType", "Bearer"
        );
    }

    // ===============================
    // LOGOUT
    // ===============================
    @PostMapping("/logout")
    public void logout(@RequestParam String refreshToken) {

        RefreshToken token =
                refreshTokenService.verifyExpiration(refreshToken);

        refreshTokenService.deleteByUserId(token.getUserId());
    }

    @PostMapping("/register")
    public Map<String, String> register(
            @Valid @RequestBody RegisterRequestDTO dto
    ) {
        User user = authService.register(
                dto.getUsername(),
                dto.getEmail(),
                dto.getPassword()
        );

        String accessToken = jwtUtils.generateAccessToken(user.getUsername());
        RefreshToken refreshToken =
                refreshTokenService.createRefreshToken(user.getId());

        return Map.of(
                "accessToken", accessToken,
                "tokenType", "Bearer",
                "refreshToken", refreshToken.getToken()
        );
    }

}
