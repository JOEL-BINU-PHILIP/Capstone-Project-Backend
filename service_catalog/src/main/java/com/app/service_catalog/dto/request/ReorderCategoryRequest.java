package com.app.service_catalog.dto.request;

import lombok.Data;

@Data
public class ReorderCategoryRequest {
    private String id;
    private int displayOrder;
}
