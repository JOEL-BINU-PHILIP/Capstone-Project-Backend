package com.app.auth.model;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "audit_logs")
public class AuditLog {

    @Id
    private String id;

    @Indexed
    private String userId;

    private String username;

    @Indexed
    private AuditAction action;

    private String details;

    private String ipAddress;
    private String userAgent;

    @Builder.Default
    private boolean success = true;

    private String failureReason;

    @CreatedDate
    @Indexed
    private Instant timestamp;

    public enum AuditAction {
        LOGIN_SUCCESS,
        LOGIN_FAILED,
        LOGOUT,
        REGISTER,
        EMAIL_VERIFIED,
        PASSWORD_CHANGED,
        PASSWORD_RESET_REQUESTED,
        PASSWORD_RESET_COMPLETED,
        ACCOUNT_LOCKED,
        ACCOUNT_UNLOCKED,
        TOKEN_REFRESHED,
        TOKEN_REVOKED,
        TWO_FACTOR_ENABLED,
        TWO_FACTOR_DISABLED,
        ROLE_CHANGED,
        PROFILE_UPDATED
    }
}
