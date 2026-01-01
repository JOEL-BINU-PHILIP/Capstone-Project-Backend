package com.app.booking.dto.request;

import com.app.booking.model.Priority;
import jakarta.validation.constraints. NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util. List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBookingRequest {

    @NotBlank(message = "Service ID is required")
    private String serviceId;

    @NotBlank(message = "Problem description is required")
    private String problemDescription;

    private List<String> imageUrls;

    @NotNull(message = "Scheduled date is required")
    private LocalDateTime scheduledDate;

    private Priority priority;

    // Address
    @NotBlank(message = "Address line 1 is required")
    private String addressLine1;
    private String addressLine2;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "State is required")
    private String state;

    @NotBlank(message = "Zip code is required")
    private String zipCode;

    private Double latitude;
    private Double longitude;

    private String specialInstructions;
}