package com.app.service_catalog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateCategoryRequest {

    @NotBlank
    private String name;

    private String description;
    private String iconUrl;

    @NotNull
    private Integer displayOrder;
}
