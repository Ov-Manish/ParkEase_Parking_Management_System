package com.parkease.spot_service.repository;

import com.parkease.spot_service.entity.ParkingSpot;
import com.parkease.spot_service.enums.SpotStatus;
import com.parkease.spot_service.enums.SpotType;
import com.parkease.spot_service.enums.VehicleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ParkingSpotRepository extends JpaRepository<ParkingSpot, UUID> {

    // 1. Fetch all active spots in a specific parking lot (ordered by floor and spot number)
    List<ParkingSpot> findByLotIdAndIsActiveTrueOrderByFloorAscSpotNumberAsc(UUID lotId);

    // 2. Fetch all spots with a specific status in a lot (e.g., all AVAILABLE spots)
    List<ParkingSpot> findByLotIdAndStatusAndIsActiveTrue(UUID lotId, SpotStatus status);

    // 3. Filter available spots by floor
    List<ParkingSpot> findByLotIdAndFloorAndStatusAndIsActiveTrue(UUID lotId, Integer floor, SpotStatus status);

    // 4. Filter available spots by Spot Type (e.g. EV, COMPACT, HANDICAPPED)
    List<ParkingSpot> findByLotIdAndSpotTypeAndStatusAndIsActiveTrue(UUID lotId, SpotType spotType, SpotStatus status);

    // 5. Filter available spots by Vehicle Compatibility (e.g. TWO_WHEELER, FOUR_WHEELER)
    List<ParkingSpot> findByLotIdAndVehicleTypeAndStatusAndIsActiveTrue(UUID lotId, VehicleType vehicleType, SpotStatus status);

    // 6. Duplicate check: Does this spotNumber already exist in this lot?
    boolean existsByLotIdAndSpotNumberIgnoreCaseAndIsActiveTrue(UUID lotId, String spotNumber);

    // 7. Find specific spot by lotId and spotNumber
    Optional<ParkingSpot> findByLotIdAndSpotNumberIgnoreCaseAndIsActiveTrue(UUID lotId, String spotNumber);

    // 8. Real-time Count: How many spots in this lot have a specific status? (e.g. AVAILABLE or OCCUPIED)
    long countByLotIdAndStatusAndIsActiveTrue(UUID lotId, SpotStatus status);

    // 9. Total active capacity count for this lot (used to sync with parkinglot-service)
    long countByLotIdAndIsActiveTrue(UUID lotId);
}