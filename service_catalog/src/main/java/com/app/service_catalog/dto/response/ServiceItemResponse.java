package com.app.service_catalog.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class ServiceItemResponse {

    private String id;

    private String categoryId;
    private String categoryName;

    private String name;
    private String description;

    private double basePrice;
    private String currency;
    private int estimatedDurationMinutes;

    private String imageUrl;
    private boolean active;

    private List<String> requiredSkills;

    private PricingDetailsResponse pricingDetails;

    private Instant createdAt;
    private Instant updatedAt;
}
