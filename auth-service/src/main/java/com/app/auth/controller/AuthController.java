package com.app.auth.controller;

import com.app.auth.dto.request.*;
import com.app.auth.dto.response.AuthResponseDTO;
import com.app.auth.payload.ApiResponse;
import com.app.auth.service.AuthService;
import com.app.auth.service.RefreshTokenService;
import com.app.auth.util.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> login(
            @Valid @RequestBody LoginRequestDTO request,
            HttpServletRequest httpRequest
    ) {
        AuthResponseDTO response = authService.login(request, httpRequest);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Login successful", response)
        );
    }

    @PostMapping("/register/customer")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> registerCustomer(
            @Valid @RequestBody RegisterCustomerDTO dto
    ) {
        AuthResponseDTO response = authService.registerCustomer(
                dto.getUsername(),
                dto.getEmail(),
                dto.getPassword(),
                dto.getFirstName(),
                dto.getLastName(),
                dto.getPhoneNumber(),
                dto.getCity(),
                dto.getState(),
                dto.getZipCode()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ApiResponse<>(
                        true,
                        "Customer registered successfully. Please verify your email.",
                        response
                ));
    }

    @PostMapping("/register/technician")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> registerTechnician(
            @Valid @RequestBody RegisterTechnicianDTO dto
    ) {
        AuthResponseDTO response = authService.registerTechnician(
                dto.getUsername(),
                dto.getEmail(),
                dto.getPassword(),
                dto.getFirstName(),
                dto.getLastName(),
                dto.getPhoneNumber(),
                dto.getSkills(),
                dto.getExperienceYears(),
                dto.getBio(),
                dto.getCity(),
                dto.getState(),
                dto.getIdProofType()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ApiResponse<>(
                        true,
                        "Technician registered successfully. Please verify your email and wait for approval.",
                        response
                ));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> refreshToken(
            @Valid @RequestBody RefreshTokenRequestDTO request,
            HttpServletRequest httpRequest
    ) {
        String ipAddress = RequestUtils.getClientIp(httpRequest);
        String userAgent = RequestUtils.getUserAgent(httpRequest);

        AuthResponseDTO response = refreshTokenService.refreshAccessToken(
                request.getRefreshToken(),
                ipAddress,
                userAgent
        );

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Token refreshed successfully", response)
        );
    }

    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(
            @RequestParam("token") String token
    ) {
        authService.verifyEmail(token);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Email verified successfully", null)
        );
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resendVerification(
            @RequestParam String email
    ) {
        authService.resendVerificationEmail(email);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Verification email sent", null)
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody RefreshTokenRequestDTO request,
            HttpServletRequest httpRequest
    ) {
        authService.logout(request.getRefreshToken(), httpRequest);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Logged out successfully", null)
        );
    }
}
