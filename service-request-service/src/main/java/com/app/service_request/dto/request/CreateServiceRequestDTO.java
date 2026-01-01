package com.app.service_request.dto.request;

import lombok.Data;

import java.time.Instant;

@Data
public class CreateServiceRequestDTO {

    private String serviceId;
    private String problemDescription;
    private Instant scheduledDate;

    // Address snapshot
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String zipCode;
}
