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
@Document(collection = "login_attempts")
public class LoginAttempt {

    @Id
    private String id;

    @Indexed
    private String username;

    @Indexed
    private String ipAddress;

    @Builder.Default
    private boolean success = false;

    private String failureReason;

    @CreatedDate
    @Indexed(expireAfterSeconds = 3600) // Auto-delete after 1 hour
    private Instant timestamp;
}
