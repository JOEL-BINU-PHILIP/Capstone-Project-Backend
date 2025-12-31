package com.app.service_catalog.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class CategoryResponse {

    private String id;
    private String name;
    private String description;
    private String iconUrl;
    private boolean active;
    private int displayOrder;
    private int servicesCount;
    private Instant createdAt;
}
