package com.parkease.booking_service.enums;

public enum BookingStatus {
    CONFIRMED,      // Spot reserved successfully, awaiting driver arrival
    CHECKED_IN,     // Driver arrived at gate, vehicle parked, spot OCCUPIED
    COMPLETED,      // Driver departed, fare calculated and paid, spot RELEASED
    CANCELLED,      // Cancelled by driver before check-in, spot RELEASED
    EXPIRED         // Grace period elapsed without check-in, spot RELEASED
}
