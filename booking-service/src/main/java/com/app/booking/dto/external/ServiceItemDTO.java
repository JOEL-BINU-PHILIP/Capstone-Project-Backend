package com.app.booking.dto.external;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceItemDTO {
    private String id;
    private String serviceId;
    private String name;
    private String serviceName;
    private String description;
    private String categoryId;
    private String categoryName;
    private Double basePrice;
    private String currency;
    private Integer estimatedDurationMinutes;
    private String imageUrl;
    private boolean active;
    private Set<String> requiredSkills;
    private Double taxPercentage;
    private Double taxAmount;
    private Double discountPercentage;
    private Double discountAmount;
    private Double finalPrice;
    private String discountValidUntil;
}