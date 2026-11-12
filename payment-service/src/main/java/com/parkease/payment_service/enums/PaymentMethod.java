package com.parkease.payment_service.enums;

public enum PaymentMethod {
    WALLET,         // Deducted directly from driver's ParkEase in-app wallet balance
    CARD,           // Credit or Debit Card processed via payment gateway
    UPI,            // UPI (Google Pay, PhonePe, Paytm, QR code)
    NET_BANKING,    // Direct Internet Banking
    CASH            // Cash on arrival / exit (collected manually by parking lot attendant)
}
