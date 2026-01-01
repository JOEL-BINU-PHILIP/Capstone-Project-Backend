package com.app.service_request.model;

import lombok.Data;

@Data
public class AddressSnapshot {

    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String zipCode;

    private Double latitude;
    private Double longitude;
}
