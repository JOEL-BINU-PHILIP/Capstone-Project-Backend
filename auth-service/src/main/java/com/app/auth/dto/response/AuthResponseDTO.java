package com.app.auth.dto.response;

import com.app.auth.model.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDTO {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;

    private UserInfoDTO user;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfoDTO {
        private String id;
        private String username;
        private String email;
        private String firstName;
        private String lastName;
        private Set<UserRole> roles;
        private boolean emailVerified;
        private boolean twoFactorEnabled;
    }
}
