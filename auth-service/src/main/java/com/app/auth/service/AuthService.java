package com.app. auth.service;

import com.app. auth.dto.request.LoginRequestDTO;
import com.app.auth. dto.response.AuthResponseDTO;
import com.app.auth.dto.response. RegistrationResponseDTO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Set;

public interface AuthService {

    AuthResponseDTO login(LoginRequestDTO request, HttpServletRequest httpRequest);

    // CHANGED: Return RegistrationResponseDTO instead of AuthResponseDTO
    RegistrationResponseDTO registerCustomer(
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

    // CHANGED: Return RegistrationResponseDTO instead of AuthResponseDTO
    RegistrationResponseDTO registerTechnician(
            String username,
            String email,
            String password,
            String firstName,
            String lastName,
            String phoneNumber,
            Set<String> skills,
            Integer experienceYears,
            String bio,
            String city,
            String state,
            String idProofType
    );

    void verifyEmail(String token);

    void resendVerificationEmail(String email);

    void logout(String refreshToken, HttpServletRequest httpRequest);
}