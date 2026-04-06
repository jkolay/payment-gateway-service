package com.payment.event;

import com.payment.service.PaymentProcessingService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listens for payment events and delegates to the async processing pipeline.
 */
@Component
@AllArgsConstructor
@Slf4j
public class PaymentEventListener {

    private final PaymentProcessingService paymentProcessingService;

    @Async("paymentProcessingExecutor")
    @EventListener
    public void handlePaymentEvent(PaymentEvent event) {
        log.info("Received payment event for trackingId: {}", event.getTrackingId());
        paymentProcessingService.processPayment(event.getTrackingId());
    }
}
 