package com.app.service_catalog.dto.request;

import lombok.Data;

@Data
public class UpdateCategoryRequest {

    private String name;
    private String description;
    private String iconUrl;
    private Integer displayOrder;
}
