package com.app.service_catalog.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PricingDetailsResponse {

    private double basePrice;

    private double taxPercentage;
    private double taxAmount;

    private double discountPercentage;
    private double discountAmount;

    private double finalPrice;
    private LocalDateTime discountValidUntil;
}
