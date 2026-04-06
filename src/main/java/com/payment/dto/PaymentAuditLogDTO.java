package com.payment.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentAuditLogDTO {

    private String previousStatus;
    private String newStatus;
    private String detail;
    private LocalDateTime timestamp;
}
