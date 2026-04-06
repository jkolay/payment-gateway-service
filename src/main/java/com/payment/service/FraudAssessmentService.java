package com.payment.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Random;

/**
 * Pluggable fraud assessment service.
 * Simulates an external fraud-check microservice with configurable rules.
 */
@Service
@Slf4j
public class FraudAssessmentService {

    private static final BigDecimal HIGH_AMOUNT_THRESHOLD = new BigDecimal("10000.00");
    private static final double RANDOM_FRAUD_PROBABILITY = 0.05;
    private final Random random = new Random();

    @Data
    @Builder
    @AllArgsConstructor
    public static class FraudCheckResult {
        private boolean passed;
        private String reason;
    }

    /**
     * Assess a payment for fraud indicators.
     * Rules:
     * 1. Amount above threshold → flagged
     * 2. Random 5% chance of flagging (simulates external ML model)
     */
    public FraudCheckResult assess(String trackingId, BigDecimal amount, String clientId) {
        log.info("Running fraud assessment for trackingId: {}, amount: {}, clientId: {}", trackingId, amount, clientId);

        if (amount.compareTo(HIGH_AMOUNT_THRESHOLD) > 0) {
            log.warn("Fraud flagged: amount {} exceeds threshold for trackingId: {}", amount, trackingId);
            return FraudCheckResult.builder()
                    .passed(false)
                    .reason("Amount exceeds high-value threshold of " + HIGH_AMOUNT_THRESHOLD)
                    .build();
        }

        if (random.nextDouble() < RANDOM_FRAUD_PROBABILITY) {
            log.warn("Fraud flagged: random ML model flag for trackingId: {}", trackingId);
            return FraudCheckResult.builder()
                    .passed(false)
                    .reason("Flagged by fraud detection model")
                    .build();
        }

        log.info("Fraud assessment passed for trackingId: {}", trackingId);
        return FraudCheckResult.builder()
                .passed(true)
                .reason("All checks passed")
                .build();
    }
}
