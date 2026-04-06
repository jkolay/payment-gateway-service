package com.payment.entity;

public enum PaymentStatus {
    INITIATED,
    FRAUD_CHECK_PENDING,
    FRAUD_CHECK_PASSED,
    FRAUD_CHECK_FAILED,
    PROCESSING,
    COMPLETED,
    FAILED,

}
