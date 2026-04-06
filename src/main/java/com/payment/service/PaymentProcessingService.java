package com.payment.service;

import com.payment.entity.Payment;
import com.payment.entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Orchestrates the async payment processing pipeline:
 * 1. Fraud Assessment
 * 2. Bank Processing
 */
@Service
@AllArgsConstructor
@Slf4j
public class PaymentProcessingService {

    private final PaymentService paymentService;
    private final FraudAssessmentService fraudAssessmentService;
    private final BankSimulatorService bankSimulatorService;

    /**
     * Process a payment through the full pipeline.
     * Called asynchronously via PaymentEventListener.
     */
    public void processPayment(String trackingId) {
        log.info("Starting payment processing pipeline for trackingId: {}", trackingId);

        // Phase 1: Fraud Assessment
        Payment payment = paymentService.transitionStatus(
                trackingId, PaymentStatus.FRAUD_CHECK_PENDING, "Fraud assessment started");

        FraudAssessmentService.FraudCheckResult fraudResult = fraudAssessmentService.assess(
                trackingId, payment.getAmount(), payment.getClientId());

        if (!fraudResult.isPassed()) {
            paymentService.transitionStatus(
                    trackingId, PaymentStatus.FRAUD_CHECK_FAILED, fraudResult.getReason());
            log.warn("Payment {} failed fraud check: {}", trackingId, fraudResult.getReason());
            return;
        }

        paymentService.transitionStatus(
                trackingId, PaymentStatus.FRAUD_CHECK_PASSED, fraudResult.getReason());

        // Phase 2: Bank Processing
        paymentService.transitionStatus(
                trackingId, PaymentStatus.PROCESSING, "Acquiring bank processing started");

        BankSimulatorService.BankResult bankResult = bankSimulatorService.process(
                trackingId, payment.getAmount(), payment.getCurrency());

        if (bankResult.isSuccess()) {
            paymentService.transitionStatus(
                    trackingId, PaymentStatus.COMPLETED,
                    "Bank approved. Reference: " + bankResult.getBankReference());
            log.info("Payment {} completed successfully", trackingId);
        } else {
            paymentService.transitionStatus(
                    trackingId, PaymentStatus.FAILED, bankResult.getReason());
            log.warn("Payment {} failed at bank: {}", trackingId, bankResult.getReason());
        }
    }
}
