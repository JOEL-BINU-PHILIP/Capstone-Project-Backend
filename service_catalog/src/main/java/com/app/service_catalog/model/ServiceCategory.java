package com.app.service_catalog.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "service_categories")
public class ServiceCategory {

    @Id
    private String id;

    // 🔒 Enforced by MongoDB unique index
    @Indexed(unique = true)
    private String name;

    private String description;
    private String iconUrl;

    private boolean active = true;

    private int displayOrder;

    private int servicesCount = 0;

    private Instant createdAt;
}
