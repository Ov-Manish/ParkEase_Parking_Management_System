package com.parkease.notification_service.dto.event;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class PaymentEventPayload {

    // Event type: e.g. "SUCCESS", "FAILED", "REFUNDED", "WALLET_TOPUP"
    private String eventType;

    // Payment & User Identifiers
    private UUID paymentId;
    private UUID bookingId;
    private UUID userId;

    // Contact info
    private String recipientEmail;
    private String recipientPhone;

    // Financial details
    private BigDecimal amount;
    private String paymentMethod; // "WALLET", "UPI", "CARD", "CASH"
    private String status;        // "COMPLETED", "FAILED", "REFUNDED"
    private String transactionReference; // e.g. "PE-TXN-20261118-8A2F"

    // Error reason if payment declined
    private String failureReason;
}
