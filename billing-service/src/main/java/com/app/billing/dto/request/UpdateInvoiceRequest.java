package com. app.billing.dto.request;

import com.app.billing.model.InvoiceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateInvoiceRequest {

    private Double discountPercentage;
    private Double discountAmount;
    private LocalDate dueDate;
    private String notes;
    private String termsAndConditions;
    private InvoiceStatus status;
}