package com.app.auth.service;

import com.app.auth.dto.request.LoginRequestDTO;
import com.app.auth.dto.response.AuthResponseDTO;
import com.app.auth.model.User;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {

    AuthResponseDTO login(LoginRequestDTO request, HttpServletRequest httpRequest);

    AuthResponseDTO registerCustomer(
            String username,
            String email,
            String password,
            String firstName,
            String lastName,
            String phoneNumber,
            String city,
            String state,
            String zipCode
    );

    AuthResponseDTO registerTechnician(
            String username,
            String email,
            String password,
            String firstName,
            String lastName,
            String phoneNumber,
            java.util.Set<String> skills,
            Integer experienceYears,
            String bio,
            String city,
            String state,
            String idProofType
    );

    void verifyEmail(String token);

    void resendVerificationEmail(String email);

    void initiatePasswordReset(String email);

    void resetPassword(String token, String newPassword);

    void changePassword(String userId, String currentPassword, String newPassword);

    void logout(String refreshToken, HttpServletRequest httpRequest);
}

