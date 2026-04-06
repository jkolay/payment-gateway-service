package com.payment.repository;

import com.payment.entity.Payment;
import com.payment.entity.PaymentAuditLog;
import com.payment.entity.PaymentStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @EntityGraph(attributePaths = {"auditLogs"})
    Optional<Payment> findByTrackingIdAndClientId(String trackingId, String clientId);

    @EntityGraph(attributePaths = {"auditLogs"})
    Optional<Payment> findByTrackingId(String trackingId);


    Optional<Payment> findByIdempotencyKeyAndClientId(String idempotencyKey, String clientId);

    Page<Payment> findByClientIdAndStatus(String clientId, PaymentStatus status, Pageable pageable);

    Page<Payment> findByClientId(String clientId, Pageable pageable);

}
