package com.app.auth.dto. request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok. Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String city;
    private String state;
    private String zipCode;

    // For technicians only
    private String bio;
    private Boolean available;
}