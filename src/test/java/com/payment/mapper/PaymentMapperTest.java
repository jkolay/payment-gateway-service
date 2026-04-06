package com.payment.mapper;

import com.payment.dto.PaymentAuditLogDTO;
import com.payment.dto.PaymentInitiationResponse;
import com.payment.dto.PaymentStatusResponse;
import com.payment.entity.Payment;
import com.payment.entity.PaymentAuditLog;
import com.payment.entity.PaymentStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PaymentMapperTest {

    private final PaymentMapper paymentMapper = new PaymentMapper();

    @Test
    void testToInitiationResponse() {
        Payment payment = Payment.builder()
                .trackingId("tracking123")
                .status(PaymentStatus.INITIATED)
                .build();

        PaymentInitiationResponse response = paymentMapper.toInitiationResponse(payment);

        assertNotNull(response);
        assertEquals("tracking123", response.getTrackingId());
        assertEquals("INITIATED", response.getStatus());
        assertEquals("Payment accepted for processing", response.getMessage());
    }

    @Test
    void testToInitiationResponse_NullPayment() {
        PaymentInitiationResponse response = paymentMapper.toInitiationResponse(null);
        assertNull(response);
    }

    @Test
    void testToStatusResponse() {
        PaymentAuditLog auditLog = PaymentAuditLog.builder()
                .previousStatus(PaymentStatus.INITIATED)
                .newStatus(PaymentStatus.COMPLETED)
                .detail("Payment completed")
                .timestamp(LocalDateTime.now())
                .build();

        Payment payment = Payment.builder()
                .trackingId("tracking123")
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .description("Test payment")
                .status(PaymentStatus.COMPLETED)
                .failureReason(null)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now())
                .auditLogs(List.of(auditLog))
                .build();

        PaymentStatusResponse response = paymentMapper.toStatusResponse(payment);

        assertNotNull(response);
        assertEquals("tracking123", response.getTrackingId());
        assertEquals(new BigDecimal("100.00"), response.getAmount());
        assertEquals("USD", response.getCurrency());
        assertEquals("Test payment", response.getDescription());
        assertEquals("COMPLETED", response.getStatus());
        assertNull(response.getFailureReason());
        assertNotNull(response.getCreatedAt());
        assertNotNull(response.getUpdatedAt());
        assertEquals(1, response.getAuditTrail().size());
    }

    @Test
    void testToStatusResponse_NullPayment() {
        PaymentStatusResponse response = paymentMapper.toStatusResponse(null);
        assertNull(response);
    }

    @Test
    void testToAuditLogDTO() {
        PaymentAuditLog auditLog = PaymentAuditLog.builder()
                .previousStatus(PaymentStatus.INITIATED)
                .newStatus(PaymentStatus.COMPLETED)
                .detail("Payment completed")
                .timestamp(LocalDateTime.now())
                .build();

        PaymentAuditLogDTO auditLogDTO = paymentMapper.toAuditLogDTO(auditLog);

        assertNotNull(auditLogDTO);
        assertEquals("INITIATED", auditLogDTO.getPreviousStatus());
        assertEquals("COMPLETED", auditLogDTO.getNewStatus());
        assertEquals("Payment completed", auditLogDTO.getDetail());
        assertNotNull(auditLogDTO.getTimestamp());
    }

    @Test
    void testToAuditLogDTO_NullAuditLog() {
        PaymentAuditLogDTO auditLogDTO = paymentMapper.toAuditLogDTO(null);
        assertNull(auditLogDTO);
    }
}