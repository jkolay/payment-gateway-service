package com.payment.service;

import com.payment.dto.PaymentInitiationRequest;
import com.payment.dto.PaymentInitiationResponse;
import com.payment.dto.PaymentStatusResponse;
import com.payment.dto.PagedResponse;

import com.payment.entity.Payment;
import com.payment.entity.PaymentAuditLog;
import com.payment.entity.PaymentStatus;
import com.payment.event.PaymentEvent;
import com.payment.exception.DuplicatePaymentException;
import com.payment.exception.PaymentNotFoundException;
import com.payment.mapper.PaymentMapper;
import com.payment.repository.PaymentRepository;
import com.payment.util.MaskingUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Core service for payment operations: ingestion, status inquiry, and state management.
 */
@Service
@AllArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Ingest a new payment request. Returns immediately with a tracking ID.
     * Publishes a PaymentEvent for async processing.
     *
     * @throws DuplicatePaymentException if idempotency key already used by this client
     */
    @Transactional
    public PaymentInitiationResponse initiatePayment(PaymentInitiationRequest request, String clientId) {
        String idempotencyKey = MaskingUtil.trimToNullSafe(request.getIdempotencyKey());
        log.info("Payment initiation request from clientId: {}, idempotencyKey: {}", clientId, idempotencyKey);

        Optional<Payment> existing = paymentRepository.findByIdempotencyKeyAndClientId(idempotencyKey, clientId);
        if (existing.isPresent()) {
            Payment existingPayment = existing.get();
            log.info("Duplicate payment detected for idempotencyKey: {}, returning existing trackingId: {}",
                    idempotencyKey, MaskingUtil.maskTrackingId(existingPayment.getTrackingId()));
            return paymentMapper.toInitiationResponse(existingPayment);
        }

        String trackingId = UUID.randomUUID().toString();

        Payment payment = Payment.builder()
                .trackingId(trackingId)
                .idempotencyKey(idempotencyKey)
                .clientId(clientId)
                .amount(request.getAmount())
                .currency(request.getCurrency().toUpperCase())
                .description(request.getDescription())
                .status(PaymentStatus.INITIATED)
                .build();

        PaymentAuditLog auditLog = PaymentAuditLog.builder()
                .payment(payment)
                .previousStatus(PaymentStatus.INITIATED)
                .newStatus(PaymentStatus.INITIATED)
                .detail("Payment initiated")
                .build();
        payment.getAuditLogs().add(auditLog);

        paymentRepository.save(payment);
        log.info("Payment saved with trackingId: {}", MaskingUtil.maskTrackingId(trackingId));

        eventPublisher.publishEvent(new PaymentEvent(this, trackingId));
        log.info("Payment event published for trackingId: {}", MaskingUtil.maskTrackingId(trackingId));

        return paymentMapper.toInitiationResponse(payment);
    }

    /**
     * Query the current state of a payment. Enforces tenant isolation.
     */
    @Transactional(readOnly = true)
    public PaymentStatusResponse getPaymentStatus(String trackingId, String clientId) {
        log.info("Status inquiry for trackingId: {}, clientId: {}",
                MaskingUtil.maskTrackingId(trackingId), clientId);

        Payment payment = paymentRepository.findByTrackingIdAndClientId(trackingId, clientId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found with trackingId: " + trackingId));

        return paymentMapper.toStatusResponse(payment);
    }

    /**
     * List payments for a client with pagination.
     */
    @Transactional(readOnly = true)
    public PagedResponse<PaymentStatusResponse> listPayments(String clientId, PaymentStatus status, Pageable pageable) {
        log.info("Listing payments for clientId: {}, status: {}", clientId, status);

        Page<Payment> page = (status != null)
                ? paymentRepository.findByClientIdAndStatus(clientId, status, pageable)
                : paymentRepository.findByClientId(clientId, pageable);

        var content = page.getContent().stream()
                .map(paymentMapper::toStatusResponse)
                .toList();

        return PagedResponse.<PaymentStatusResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .sort(pageable.getSort().toString())
                .build();
    }

    /**
     * Transition a payment to a new status and record an audit log.
     * Used internally by PaymentProcessingService.
     */
    @Transactional
    public Payment transitionStatus(String trackingId, PaymentStatus newStatus, String detail) {
        Payment payment = paymentRepository.findByTrackingId(trackingId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + trackingId));

        PaymentStatus previousStatus = payment.getStatus();
        payment.setStatus(newStatus);

        if (newStatus == PaymentStatus.FAILED || newStatus == PaymentStatus.FRAUD_CHECK_FAILED) {
            payment.setFailureReason(detail);
        }

        PaymentAuditLog auditLog = PaymentAuditLog.builder()
                .payment(payment)
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .detail(detail)
                .build();
        payment.getAuditLogs().add(auditLog);

        Payment saved = paymentRepository.save(payment);
        log.info("Payment {} transitioned: {} → {} ({})",
                MaskingUtil.maskTrackingId(trackingId), previousStatus, newStatus, detail);
        return saved;
    }
}
