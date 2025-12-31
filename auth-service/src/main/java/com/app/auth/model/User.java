package com.app.auth.model;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
@CompoundIndex(name = "username_email_idx", def = "{'username': 1, 'email': 1}", unique = true)
public class User {

    @Id
    private String id;

    @Indexed(unique = true)
    private String username;

    @Indexed(unique = true)
    private String email;

    private String password;

    @Builder.Default
    private Set<UserRole> roles = new HashSet<>();

    // Account status
    @Builder.Default
    private boolean enabled = false; // Start disabled until email verified

    @Builder.Default
    private boolean accountNonLocked = true;

    @Builder.Default
    private boolean accountNonExpired = true;

    @Builder.Default
    private boolean credentialsNonExpired = true;

    // Email verification
    @Builder.Default
    private boolean emailVerified = false;

    private String emailVerificationToken;
    private Instant emailVerificationTokenExpiry;

    // Password reset
    private String passwordResetToken;
    private Instant passwordResetTokenExpiry;

    // Security
    @Builder.Default
    private int failedLoginAttempts = 0;

    private Instant lockedUntil;
    private String lastLoginIp;
    private Instant lastLoginAt;

    // 2FA
    @Builder.Default
    private boolean twoFactorEnabled = false;

    private String twoFactorSecret;

    // Profile fields
    private String firstName;
    private String lastName;
    private String phoneNumber;

    // Location (for technicians/customers)
    private String city;
    private String state;
    private String country;
    private String zipCode;

    // For Service Manager - assigned service
    private String assignedServiceId;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    // Helper methods
    public boolean isLocked() {
        return !accountNonLocked ||
                (lockedUntil != null && lockedUntil.isAfter(Instant.now()));
    }

    public void incrementFailedAttempts() {
        this.failedLoginAttempts++;
    }

    public void resetFailedAttempts() {
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
    }

    public void lockAccount(long durationMinutes) {
        this.accountNonLocked = false;
        this.lockedUntil = Instant.now().plusSeconds(durationMinutes * 60);
    }
}