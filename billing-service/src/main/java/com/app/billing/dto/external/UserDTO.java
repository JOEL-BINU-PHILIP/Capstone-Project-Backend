package com.app.billing. dto.external;

import lombok. AllArgsConstructor;
import lombok.Builder;
import lombok. Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private String id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;
    private String phoneNumber;
    private String city;
    private String state;
    private String zipCode;
    private List<String> roles;
    private boolean enabled;
}