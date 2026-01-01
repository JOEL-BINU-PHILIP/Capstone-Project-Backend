package com.app.billing.model;

public enum InvoiceStatus {
    DRAFT,          // Invoice created but not finalized
    PENDING,        // Invoice sent, awaiting payment
    PAID,           // Fully paid
    PARTIALLY_PAID, // Partial payment received
    OVERDUE,        // Payment past due date
    CANCELLED,      // Invoice cancelled
    REFUNDED        // Payment refunded
}