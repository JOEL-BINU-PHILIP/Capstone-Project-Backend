package com.app.billing.model;

public enum InvoiceStatus {
    PENDING,        // Invoice generated, awaiting payment
    PAID,           // Payment completed
    CANCELLED       // Invoice cancelled
}