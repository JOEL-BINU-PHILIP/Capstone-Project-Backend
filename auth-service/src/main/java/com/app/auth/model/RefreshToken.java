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
@Document(collection = "refresh_tokens")
public class RefreshToken {

    @Id
    private String id;

    @Indexed(unique = true)
    private String token;

    @Indexed
    private String userId;

    private Instant expiryDate;

    // Token rotation - track token families
    private String tokenFamily;

    @Builder.Default
    private boolean revoked = false;

    private String revokedReason;

    // Security tracking
    private String ipAddress;
    private String userAgent;

    @CreatedDate
    private Instant createdAt;

    private Instant usedAt;

    public boolean isExpired() {
        return Instant.now().isAfter(expiryDate);
    }
}