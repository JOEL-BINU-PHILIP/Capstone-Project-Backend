package com.app.booking. dto.request;

import jakarta. validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignTechnicianRequest {

    @NotBlank(message = "Technician ID is required")
    private String technicianId;

    private String technicianName;
    private String technicianPhone;
}