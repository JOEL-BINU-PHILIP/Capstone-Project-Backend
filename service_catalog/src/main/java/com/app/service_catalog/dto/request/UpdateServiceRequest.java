package com.app.service_catalog.dto.request;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UpdateServiceRequest {

    private String name;
    private String description;
    private Double basePrice;
    private String currency;
    private Integer estimatedDurationMinutes;
    private String imageUrl;
    private List<String> requiredSkills;
    private Double taxPercentage;
    private Double discountPercentage;
    private LocalDateTime discountValidUntil;
}
