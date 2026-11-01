package com.parkease.spot_service.service;

import com.parkease.spot_service.dto.request.BulkSpotRequest;
import com.parkease.spot_service.dto.request.SpotRequest;
import com.parkease.spot_service.dto.request.SpotUpdateRequest;
import com.parkease.spot_service.dto.response.SpotCountResponse;
import com.parkease.spot_service.dto.response.SpotResponse;
import com.parkease.spot_service.enums.SpotStatus;
import com.parkease.spot_service.enums.SpotType;
import com.parkease.spot_service.enums.VehicleType;

import java.util.List;
import java.util.UUID;

public interface ParkingSpotService {

    // --- Lot Manager Operations ---
    SpotResponse createSpot(SpotRequest request);
    List<SpotResponse> createBulkSpots(BulkSpotRequest request);
    List<SpotResponse> getSpotsByLot(UUID lotId);
    SpotResponse updateSpot(UUID spotId, SpotUpdateRequest request);
    SpotResponse updateSpotStatus(UUID spotId, SpotStatus status);
    void deleteSpot(UUID spotId);

    // --- Public / Driver Query Operations ---
    SpotResponse getSpotById(UUID spotId);
    List<SpotResponse> getAvailableSpots(UUID lotId, Integer floor, SpotType spotType, VehicleType vehicleType);
    SpotCountResponse getSpotCountsByLot(UUID lotId);

    // --- Inter-Service Lifecycle Operations (Booking-Service) ---
    SpotResponse reserveSpot(UUID spotId);
    SpotResponse occupySpot(UUID spotId);
    SpotResponse releaseSpot(UUID spotId);
}