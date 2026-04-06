package com.payment.entity;


import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "payments",indexes = {@Index(name = "idx_payments_client_id", columnList = "clientId"),
        @Index(name = "idx_payments_tracking_id", columnList = "trackingId",unique = true),
        @Index(name = "idx_payments_idempotency", columnList = "clientId,idempotencyKey",unique = true)})

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false,unique = true,updatable = false)
    private String trackingId; // Unique tracking ID for the payment

    @Column(nullable = false,updatable = false)
    private String idempotencyKey; // Idempotency key to prevent duplicate processing)

    @Column(nullable = false,updatable = false)
    private String clientId; // ID of the client making the payment

    @Column(nullable = false,precision = 19, scale = 4)
    private BigDecimal amount; // Payment amount

    @Column(nullable = false,length = 3)
    private String currency; // Currency code (e.g., USD, EUR)

    @Column
    private String description; // Optional payment description

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentStatus status; // Current status of the payment

    @Column
    private String failureReason; // Reason for failure if status is FAILED

    @Column(nullable = false,updatable = false)
    private LocalDateTime createdAt; // Timestamp when the payment was created

    @Column(nullable = false)
    private LocalDateTime updatedAt; // Timestamp when the payment was last updated

    @Version
    private Long version; // For optimistic locking

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("timestamp ASC")
    @Builder.Default
    private List<PaymentAuditLog> auditLogs = new ArrayList<>(); // Audit trail of status changes

    @PrePersist
    protected  void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

}
