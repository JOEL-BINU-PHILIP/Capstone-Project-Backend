package com.app.service_catalog.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "services")
public class ServiceItem {

    @Id
    private String id;

    private String categoryId;
    private String categoryName;
    @Indexed(unique = true)
    private String name;
    private String description;

    private double basePrice;
    private String currency;
    private int estimatedDurationMinutes;

    private String imageUrl;
    private boolean active;

    private List<String> requiredSkills;

    private double taxPercentage;
    private double discountPercentage;
    private LocalDateTime discountValidUntil;

    private Instant createdAt;
    private Instant updatedAt;
}
