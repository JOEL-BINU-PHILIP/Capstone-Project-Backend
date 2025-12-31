package com.app.service_catalog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CreateServiceRequest {

    @NotBlank
    private String categoryId;

    @NotBlank
    private String name;

    private String description;

    @NotNull
    private Double basePrice;

    private String currency;
    private Integer estimatedDurationMinutes;
    private String imageUrl;

    private List<String> requiredSkills;

    private Double taxPercentage;
    private Double discountPercentage;
    private LocalDateTime discountValidUntil;
}

