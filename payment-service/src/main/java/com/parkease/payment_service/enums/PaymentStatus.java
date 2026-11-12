package com.parkease.payment_service.enums;

public enum PaymentStatus {
    PENDING,            // Payment initiated, awaiting processing or gateway webhook
    PROCESSING,         // Sent to payment gateway, awaiting confirmation
    COMPLETED,          // Payment successfully processed and captured
    FAILED,             // Payment declined, timed out, or gateway error
    REFUNDED,           // Fully refunded to original source / wallet
    PARTIALLY_REFUNDED  // Partial refund issued
}
