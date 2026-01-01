package com.app.service_request.model;

import lombok.Data;

@Data
public class PricingSnapshot {

    private Double basePrice;
    private Double discount;
    private Double finalPrice;
    private String currency;
}
