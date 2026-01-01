package com.app.booking.dto. request;

import jakarta.validation. constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RescheduleBookingRequest {

    @NotNull(message = "New scheduled date is required")
    private LocalDateTime newScheduledDate;

    private String reason;
}