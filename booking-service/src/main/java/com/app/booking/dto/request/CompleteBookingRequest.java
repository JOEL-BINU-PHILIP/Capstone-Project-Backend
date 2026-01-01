package com.app.booking.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompleteBookingRequest {

    @NotBlank(message = "Completion OTP is required")
    private String otp;

    private String technicianNotes;
    private List<String> completionImageUrls;
}