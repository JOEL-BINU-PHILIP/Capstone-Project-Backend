package com.app.booking.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PricingDetails {
    private Double basePrice;
    private Double taxAmount;
    private Double discountAmount;
    private Double finalPrice;
    private String currency;
}