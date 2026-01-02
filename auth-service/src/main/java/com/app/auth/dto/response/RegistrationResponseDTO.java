package com.app.auth.dto. response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok. Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationResponseDTO {

    private String userId;
    private String username;
    private String email;
    private String message;
    private boolean emailVerificationRequired;
    private boolean approvalRequired;  // For technicians
}