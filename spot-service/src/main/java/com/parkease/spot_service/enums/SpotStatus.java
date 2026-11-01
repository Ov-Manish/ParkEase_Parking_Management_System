package com.parkease.spot_service.enums;

public enum SpotStatus {
    AVAILABLE,       // Open for booking or drive-in
    RESERVED,        // Booked by a driver, waiting for them to arrive
    OCCUPIED,        // Driver has checked in and vehicle is parked
    OUT_OF_SERVICE   // Sensor broken, oil spill, or undergoing maintenance
}
