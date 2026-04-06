package com.payment.service;

import com.payment.dto.PaymentInitiationRequest;
import com.payment.dto.PaymentInitiationResponse;
import com.payment.dto.PaymentStatusResponse;
import com.payment.entity.Payment;
import com.payment.entity.PaymentAuditLog;
import com.payment.entity.PaymentStatus;
import com.payment.event.PaymentEvent;
import com.payment.exception.DuplicatePaymentException;
import com.payment.exception.PaymentNotFoundException;
import com.payment.mapper.PaymentMapper;
import com.payment.repository.PaymentRepository;
import com.payment.util.MaskingUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testInitiatePayment_Success() {
        // Arrange
        PaymentInitiationRequest request = new PaymentInitiationRequest();
        request.setIdempotencyKey("key123");
        request.setAmount(new BigDecimal("100.00"));
        request.setCurrency("USD");
        request.setDescription("Test payment");

        String clientId = "client123";
        String trackingId = UUID.randomUUID().toString();

        Payment payment = Payment.builder()
                .trackingId(trackingId)
                .idempotencyKey("key123")
                .clientId(clientId)
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .status(PaymentStatus.INITIATED)
                .build();

        when(paymentRepository.findByIdempotencyKeyAndClientId("key123", clientId))
                .thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(paymentMapper.toInitiationResponse(any(Payment.class)))
                .thenReturn(new PaymentInitiationResponse(trackingId, "status-value", "message-value"));
        // Act
        PaymentInitiationResponse response = paymentService.initiatePayment(request, clientId);

        // Assert
        assertNotNull(response);
        assertEquals(trackingId, response.getTrackingId());
        verify(eventPublisher, times(1)).publishEvent(any(PaymentEvent.class));
    }

    @Test
    void testInitiatePayment_Duplicate() {
        // Arrange
        PaymentInitiationRequest request = new PaymentInitiationRequest();
        request.setIdempotencyKey("key123");
        String clientId = "client123";

        Payment existingPayment = Payment.builder()
                .trackingId("existing-tracking-id")
                .idempotencyKey("key123")
                .clientId(clientId)
                .build();

        when(paymentRepository.findByIdempotencyKeyAndClientId("key123", clientId))
                .thenReturn(Optional.of(existingPayment));
        when(paymentMapper.toInitiationResponse(existingPayment))
                .thenReturn(new PaymentInitiationResponse("existing-tracking-id", "status-value", "message-value"));

        // Act
        PaymentInitiationResponse response = paymentService.initiatePayment(request, clientId);

        // Assert
        assertNotNull(response);
        assertEquals("existing-tracking-id", response.getTrackingId());
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(eventPublisher, never()).publishEvent(any(PaymentEvent.class));
    }

    @Test
    void testGetPaymentStatus_Success() {
        // Arrange
        String trackingId = "test-tracking-id";
        String clientId = "client123";

        Payment payment = Payment.builder()
                .trackingId(trackingId)
                .clientId(clientId)
                .status(PaymentStatus.INITIATED)
                .build();

        when(paymentRepository.findByTrackingIdAndClientId(trackingId, clientId))
                .thenReturn(Optional.of(payment));
        when(paymentMapper.toStatusResponse(payment))
                .thenReturn( PaymentStatusResponse.builder().status(PaymentStatus.INITIATED.toString()).trackingId(trackingId).build());

        // Act
        PaymentStatusResponse response = paymentService.getPaymentStatus(trackingId, clientId);

        // Assert
        assertNotNull(response);
        assertEquals(trackingId, response.getTrackingId());
        assertEquals(PaymentStatus.INITIATED.toString(), response.getStatus());
    }

    @Test
    void testGetPaymentStatus_NotFound() {
        // Arrange
        String trackingId = "test-tracking-id";
        String clientId = "client123";

        when(paymentRepository.findByTrackingIdAndClientId(trackingId, clientId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(PaymentNotFoundException.class,
                () -> paymentService.getPaymentStatus(trackingId, clientId));
    }

    @Test
    void testTransitionStatus_Success() {
        // Arrange
        String trackingId = "test-tracking-id";
        Payment payment = Payment.builder()
                .trackingId(trackingId)
                .status(PaymentStatus.INITIATED)
                .build();

        when(paymentRepository.findByTrackingId(trackingId))
                .thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        // Act
        Payment updatedPayment = paymentService.transitionStatus(trackingId, PaymentStatus.COMPLETED, "Payment completed");

        // Assert
        assertNotNull(updatedPayment);
        assertEquals(PaymentStatus.COMPLETED, updatedPayment.getStatus());
        verify(paymentRepository, times(1)).save(payment);
    }

    @Test
    void testTransitionStatus_NotFound() {
        // Arrange
        String trackingId = "test-tracking-id";

        when(paymentRepository.findByTrackingId(trackingId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(PaymentNotFoundException.class,
                () -> paymentService.transitionStatus(trackingId, PaymentStatus.COMPLETED, "Payment completed"));
    }
}