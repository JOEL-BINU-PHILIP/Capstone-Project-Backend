package com.app. notification.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.time. Instant;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BaseEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String eventId;
    private EventType eventType;
    private Instant timestamp;

    // User who triggered the event
    private String triggeredBy;

    // Target user (who should receive notification)
    private String userId;
    private String userEmail;
    private String userName;
    private String userRole;
}