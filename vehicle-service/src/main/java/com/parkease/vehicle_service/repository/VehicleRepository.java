package com.parkease.vehicle_service.repository;

import com.parkease.vehicle_service.entity.Vehicle;
import com.parkease.vehicle_service.enums.VehicleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {

    // 1. Get all active vehicles belonging to a specific driver
    List<Vehicle> findByOwnerIdAndIsActiveTrue(UUID ownerId);

    // 2. Get all vehicles (including soft-deleted) for an owner
    List<Vehicle> findByOwnerId(UUID ownerId);

    // 3. Lookup vehicle by its license plate (case-insensitive)
    Optional<Vehicle> findByLicensePlateIgnoreCase(String licensePlate);

    // 4. Edge case check: Prevent a driver from adding the exact same car twice
    boolean existsByOwnerIdAndLicensePlateIgnoreCaseAndIsActiveTrue(UUID ownerId, String licensePlate);

    // 5. Filter fleet by vehicle type (e.g. all 2-Wheelers or all SUVs)
    List<Vehicle> findByVehicleTypeAndIsActiveTrue(VehicleType vehicleType);

    // 6. Filter fleet by EV status (used for EV charging analytics & checks)
    List<Vehicle> findByIsEVTrueAndIsActiveTrue();
}