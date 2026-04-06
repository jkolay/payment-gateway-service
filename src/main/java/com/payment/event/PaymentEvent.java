package com.payment.event;

import org.springframework.context.ApplicationEvent;

/**
 * Event published when a payment is initiated and ready for async processing.
 */
public class PaymentEvent extends ApplicationEvent {

    private final String trackingId;

    public PaymentEvent(Object source, String trackingId) {
        super(source);
        this.trackingId = trackingId;
    }

    public String getTrackingId() {
        return trackingId;
    }
}
