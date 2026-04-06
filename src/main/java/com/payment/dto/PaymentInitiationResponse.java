package com.payment.dto;

import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentInitiationResponse {
    private String trackingId;
    private String status;
    private String message;
}
