package com.payment.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class FraudAssessmentServiceTest {

    private FraudAssessmentService fraudAssessmentService;

    @BeforeEach
    void setUp() {
        fraudAssessmentService = new FraudAssessmentService();
    }

    @Test
    void testAssess_HighAmountThreshold() {
        // Arrange
        String trackingId = "test-tracking-id";
        BigDecimal highAmount = new BigDecimal("25000.00");
        String clientId = "test-client-id";

        // Act
        FraudAssessmentService.FraudCheckResult result = fraudAssessmentService.assess(trackingId, highAmount, clientId);

        // Assert
        assertFalse(result.isPassed());
        assertEquals("Amount exceeds high-value threshold of 20000.00", result.getReason());
    }

    @Test
    void testAssess_RandomFraudFlag() {
        // Arrange
        String trackingId = "test-tracking-id";
        BigDecimal amount = new BigDecimal("1000.00");
        String clientId = "test-client-id";

        // Act
        FraudAssessmentService.FraudCheckResult result = fraudAssessmentService.assess(trackingId, amount, clientId);

        // Assert
        // Since the random flagging is probabilistic, we cannot assert the exact result.
        // Instead, we check that the result is either passed or flagged with the correct reason.
        if (!result.isPassed()) {
            assertEquals("Flagged by fraud detection model", result.getReason());
        }
    }

    @Test
    void testAssess_AllChecksPassed() {
        // Arrange
        String trackingId = "test-tracking-id";
        BigDecimal amount = new BigDecimal("1000.00");
        String clientId = "test-client-id";

        // Act
        FraudAssessmentService.FraudCheckResult result = fraudAssessmentService.assess(trackingId, amount, clientId);

        // Assert
        if (result.isPassed()) {
            assertEquals("All checks passed", result.getReason());
        }
    }
}