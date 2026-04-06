package com.payment.controller;

import com.payment.dto.PagedResponse;
import com.payment.dto.PaymentInitiationRequest;
import com.payment.dto.PaymentInitiationResponse;
import com.payment.dto.PaymentStatusResponse;
import com.payment.entity.PaymentStatus;
import com.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Payment operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@AllArgsConstructor
@Tag(name = "Payment Gateway", description = "API endpoints for payment ingestion and status tracking")
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Initiate a new payment. Acknowledges immediately with a tracking ID.
     */
    @PostMapping
    @Operation(summary = "Initiate payment", description = "Accept a payment request for async processing. Returns a tracking ID immediately.")
    public ResponseEntity<PaymentInitiationResponse> initiatePayment(
            @Valid @RequestBody PaymentInitiationRequest request,
            Authentication authentication) {
        String clientId = extractClientId(authentication);
        log.info("POST /api/v1/payments from clientId: {}", clientId);

        PaymentInitiationResponse response = paymentService.initiatePayment(request, clientId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /**
     * Query the status of a payment by tracking ID.
     */
    @GetMapping("/{trackingId}")
    @Operation(summary = "Get payment status", description = "Retrieve the current status and audit trail of a payment")
    public ResponseEntity<PaymentStatusResponse> getPaymentStatus(
            @PathVariable String trackingId,
            Authentication authentication) {
        String clientId = extractClientId(authentication);
        log.info("GET /api/v1/payments/{} from clientId: {}", trackingId, clientId);

        PaymentStatusResponse response = paymentService.getPaymentStatus(trackingId, clientId);
        return ResponseEntity.ok(response);
    }

    /**
     * List payments for the authenticated client.
     */
    @GetMapping
    @Operation(summary = "List payments", description = "Retrieve paginated list of payments for the authenticated client")
    public ResponseEntity<PagedResponse<PaymentStatusResponse>> listPayments(
            @RequestParam(value = "status", required = false)
            @Parameter(description = "Filter by payment status")
            PaymentStatus status,

            @RequestParam(value = "page", required = false, defaultValue = "0")
            @Parameter(description = "0-based page index")
            int page,

            @RequestParam(value = "size", required = false, defaultValue = "20")
            @Parameter(description = "Page size (max 100)")
            int size,

            Authentication authentication) {
        String clientId = extractClientId(authentication);
        log.info("GET /api/v1/payments from clientId: {}, status: {}, page: {}, size: {}", clientId, status, page, size);

        int clampedSize = Math.max(1, Math.min(size, 100));
        Pageable pageable = PageRequest.of(Math.max(page, 0), clampedSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        PagedResponse<PaymentStatusResponse> response = paymentService.listPayments(clientId, status, pageable);
        return ResponseEntity.ok(response);
    }

    private String extractClientId(Authentication authentication) {
        return authentication.getName();
    }
}
