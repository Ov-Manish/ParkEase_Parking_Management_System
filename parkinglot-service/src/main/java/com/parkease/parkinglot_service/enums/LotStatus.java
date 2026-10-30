package com.parkease.parkinglot_service.enums;

public enum LotStatus {
    PENDING_APPROVAL,  // Newly registered by Lot Manager, awaiting admin review
    APPROVED,          // Approved by Admin and operational
    REJECTED,          // Rejected by Admin with feedback
    SUSPENDED          // Temporarily taken down by Admin
}
