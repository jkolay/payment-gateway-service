package com.payment.service;

import com.payment.entity.Payment;
import com.payment.entity.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;

import static org.mockito.Mockito.*;

class PaymentProcessingServiceTest {

    @Mock
    private PaymentService paymentService;

    @Mock
    private FraudAssessmentService fraudAssessmentService;

    @Mock
    private BankSimulatorService bankSimulatorService;

    @InjectMocks
    private PaymentProcessingService paymentProcessingService;

    public PaymentProcessingServiceTest() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testProcessPayment_SuccessfulFlow() {
        // Arrange
        String trackingId = "test-tracking-id";
        Payment payment = new Payment();
        payment.setAmount(new BigDecimal("100.00"));
        payment.setClientId("test-client-id");
        payment.setCurrency("USD");

        when(paymentService.transitionStatus(anyString(), any(PaymentStatus.class), anyString()))
                .thenReturn(payment);

        FraudAssessmentService.FraudCheckResult fraudCheckResult = FraudAssessmentService.FraudCheckResult.builder()
                .passed(true)
                .reason("All checks passed")
                .build();
        when(fraudAssessmentService.assess(anyString(), any(BigDecimal.class), anyString()))
                .thenReturn(fraudCheckResult);

        BankSimulatorService.BankResult bankResult = new BankSimulatorService.BankResult(true, "BankRef123", null);
        when(bankSimulatorService.process(anyString(), any(BigDecimal.class), anyString()))
                .thenReturn(bankResult);

        // Act
        paymentProcessingService.processPayment(trackingId);

        // Assert
        verify(paymentService, times(1)).transitionStatus(trackingId, PaymentStatus.FRAUD_CHECK_PASSED, "All checks passed");
        verify(paymentService, times(1)).transitionStatus(trackingId, PaymentStatus.COMPLETED, "Bank approved. Reference: BankRef123");
    }

    @Test
    void testProcessPayment_FraudCheckFailed() {
        // Arrange
        String trackingId = "test-tracking-id";
        Payment payment = new Payment();
        payment.setAmount(new BigDecimal("100.00"));
        payment.setClientId("test-client-id");

        when(paymentService.transitionStatus(anyString(), any(PaymentStatus.class), anyString()))
                .thenReturn(payment);

        FraudAssessmentService.FraudCheckResult fraudCheckResult = FraudAssessmentService.FraudCheckResult.builder()
                .passed(false)
                .reason("Fraud detected")
                .build();
        when(fraudAssessmentService.assess(anyString(), any(BigDecimal.class), anyString()))
                .thenReturn(fraudCheckResult);

        // Act
        paymentProcessingService.processPayment(trackingId);

        // Assert
        verify(paymentService, times(1)).transitionStatus(trackingId, PaymentStatus.FRAUD_CHECK_FAILED, "Fraud detected");
        verify(bankSimulatorService, never()).process(anyString(), any(BigDecimal.class), anyString());
    }
}