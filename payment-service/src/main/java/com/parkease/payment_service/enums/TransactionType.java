package com.parkease.payment_service.enums;

public enum TransactionType {
    CREDIT,     // Inflow: Wallet top-up, promotional credit, or cancellation refund
    DEBIT       // Outflow: Booking reservation fee, overstay penalty payment
}
