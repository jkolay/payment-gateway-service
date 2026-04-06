package com.payment.exception;

import lombok.Getter;

@Getter
public class DuplicatePaymentException extends RuntimeException {
    private final String trackingId;
    public DuplicatePaymentException(String message, String trackingId) {
        super(message);
        this.trackingId = trackingId;
    }
}
