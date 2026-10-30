package com.parkease.parkinglot_service.service;

import com.parkease.parkinglot_service.dto.request.LotRejectionRequest;
import com.parkease.parkinglot_service.dto.request.ParkingLotRequest;
import com.parkease.parkinglot_service.dto.request.ParkingLotUpdateRequest;
import com.parkease.parkinglot_service.dto.response.ParkingLotResponse;

import java.util.List;
import java.util.UUID;

public interface ParkingLotService {

    // Lot Manager Operations
    ParkingLotResponse registerLot(UUID managerId, ParkingLotRequest request);

    List<ParkingLotResponse> getLotsByManager(UUID managerId);

    ParkingLotResponse updateLot(UUID lotId, UUID managerId, boolean isAdmin, ParkingLotUpdateRequest request);

    ParkingLotResponse toggleLotOpenStatus(UUID lotId, UUID managerId, boolean isAdmin);

    void deleteLot(UUID lotId, UUID managerId, boolean isAdmin);



    // Public / Driver Search Operations
    ParkingLotResponse getLotById(UUID lotId);
    List<ParkingLotResponse> searchByCity(String city);
    List<ParkingLotResponse> searchNearby(double latitude, double longitude, double radiusKm);

    //  Platform Admin Operations
    List<ParkingLotResponse> getPendingLots();
    ParkingLotResponse approveLot(UUID lotId);
    ParkingLotResponse rejectLot(UUID lotId, LotRejectionRequest request);
    List<ParkingLotResponse> getAllLots();

    // Inter-Service Communication (Booking-Service & Spot-Service)
    ParkingLotResponse verifyLotForBooking(UUID lotId);
    void updateTotalSpots(UUID lotId, int totalSpots);
}
