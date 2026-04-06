package com.payment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_audit_logs",indexes = {@Index(name = "idx_audit_payment_id", columnList = "payment_id")})
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentAuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false,updatable = false)
    private Payment payment; // Reference to the associated payment

    @Column(nullable = false,updatable = false)
    @Enumerated(EnumType.STRING)
    private PaymentStatus previousStatus; // Previous status of the payment


    @Column(nullable = false,updatable = false)
    @Enumerated(EnumType.STRING)
    private PaymentStatus newStatus; // New status of the payment

    @Column(updatable = false)
    private String detail;

    @Column(nullable = false,updatable = false)
    private LocalDateTime timestamp; // Timestamp of the status change

    @PrePersist
    public void onCreate() {
        if(timestamp == null) {
            timestamp = LocalDateTime.now();
        }
        this.timestamp = LocalDateTime.now();
    }
}
