package com.payment.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentStatusResponse {
    private String trackingId;
    private BigDecimal amount;
    private String currency;
    private String description;
    private String status;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<PaymentAuditLogDTO> auditTrail;
}
