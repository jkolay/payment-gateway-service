package com.payment.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Random;

/**
 * Simulates a downstream acquiring bank call with variable latency and random outcomes.
 */
@Service
@Slf4j
public class BankSimulatorService {

    private static final double SUCCESS_RATE = 0.85;
    private static final int MIN_LATENCY_MS = 100;
    private static final int MAX_LATENCY_MS = 2000;
    private final Random random = new Random();

    @Data
    @Builder
    @AllArgsConstructor
    public static class BankResult {
        private boolean success;
        private String bankReference;
        private String reason;
    }

    /**
     * Simulate a bank processing call.
     * Has variable latency (100-2000ms) and ~85% success rate.
     */
    public BankResult process(String trackingId, BigDecimal amount, String currency) {
        log.info("Initiating bank processing for trackingId: {}, amount: {} {}", trackingId, amount, currency);

        int latency = MIN_LATENCY_MS + random.nextInt(MAX_LATENCY_MS - MIN_LATENCY_MS);
        try {
            Thread.sleep(latency);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Bank processing interrupted for trackingId: {}", trackingId);
            return BankResult.builder()
                    .success(false)
                    .reason("Processing interrupted")
                    .build();
        }

        log.info("Bank processing completed in {}ms for trackingId: {}", latency, trackingId);

        if (random.nextDouble() < SUCCESS_RATE) {
            String bankRef = "BANK-" + System.currentTimeMillis() + "-" + random.nextInt(9999);
            return BankResult.builder()
                    .success(true)
                    .bankReference(bankRef)
                    .reason("Transaction approved")
                    .build();
        }

        return BankResult.builder()
                .success(false)
                .reason("Bank declined: insufficient funds or temporary issue")
                .build();
    }
}
