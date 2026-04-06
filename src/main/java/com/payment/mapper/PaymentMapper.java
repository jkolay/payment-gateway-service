package com.payment.mapper;

import com.payment.dto.PaymentAuditLogDTO;
import com.payment.dto.PaymentInitiationResponse;
import com.payment.dto.PaymentStatusResponse;
import com.payment.entity.Payment;
import com.payment.entity.PaymentAuditLog;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mapper for converting between Payment entities and DTOs.
 */
@Component
public class PaymentMapper {

    public PaymentInitiationResponse toInitiationResponse(Payment payment) {
        if (payment == null) {
            return null;
        }
        return PaymentInitiationResponse.builder()
                .trackingId(payment.getTrackingId())
                .status(payment.getStatus().name())
                .message("Payment accepted for processing")
                .build();
    }

    public PaymentStatusResponse toStatusResponse(Payment payment) {
        if (payment == null) {
            return null;
        }

        List<PaymentAuditLogDTO> auditTrail = payment.getAuditLogs() == null
                ? List.of()
                : payment.getAuditLogs().stream()
                .map(this::toAuditLogDTO)
                .toList();

        return PaymentStatusResponse.builder()
                .trackingId(payment.getTrackingId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .description(payment.getDescription())
                .status(payment.getStatus().name())
                .failureReason(payment.getFailureReason())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .auditTrail(auditTrail)
                .build();
    }

    public PaymentAuditLogDTO toAuditLogDTO(PaymentAuditLog auditLog) {
        if (auditLog == null) {
            return null;
        }
        return PaymentAuditLogDTO.builder()
                .previousStatus(auditLog.getPreviousStatus().name())
                .newStatus(auditLog.getNewStatus().name())
                .detail(auditLog.getDetail())
                .timestamp(auditLog.getTimestamp())
                .build();
    }
}
