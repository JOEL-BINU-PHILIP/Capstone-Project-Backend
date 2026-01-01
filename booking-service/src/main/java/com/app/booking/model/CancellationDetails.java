package com.app.booking.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time. Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancellationDetails {
    private String cancelledBy;         // userId who cancelled
    private String cancelledByRole;     // CUSTOMER, SERVICE_MANAGER
    private String cancellationReason;
    private Instant cancelledAt;
}
