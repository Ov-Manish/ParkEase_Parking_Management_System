package com.parkease.vehicle_service.service;

import com.parkease.vehicle_service.dto.request.VehicleRequest;
import com.parkease.vehicle_service.dto.request.VehicleUpdateRequest;
import com.parkease.vehicle_service.dto.response.VehicleResponse;
import com.parkease.vehicle_service.enums.VehicleType;

import java.util.List;
import java.util.UUID;

public interface VehicleService {

    VehicleResponse registerVehicle(UUID currentUserId, VehicleRequest request);

    VehicleResponse getVehicleById(UUID vehicleId);

    List<VehicleResponse> getVehiclesByOwner(UUID ownerId);

    VehicleResponse getByLicensePlate(String licensePlate);

    VehicleResponse updateVehicle(UUID vehicleId, UUID currentUserId, boolean isAdmin, VehicleUpdateRequest request);

    void deleteVehicle(UUID vehicleId, UUID currentUserId, boolean isAdmin);

    // Fast checks for Booking-Service
    boolean isEVVehicle(UUID vehicleId);

    VehicleType getVehicleType(UUID vehicleId);

    // Admin
    List<VehicleResponse> getAllVehicles();
}