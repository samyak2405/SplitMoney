package com.splitmoney.application.domain.payment;

public enum PaymentStatus {
    INITIATED,
    VALIDATING,
    PROCESSING,
    PENDING,
    COMPLETED,
    FAILED,
    CANCELLED,
    REFUND_INITIATED,
    REFUNDED
}
