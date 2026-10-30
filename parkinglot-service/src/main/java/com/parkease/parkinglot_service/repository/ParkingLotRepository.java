package com.parkease.parkinglot_service.repository;

import com.parkease.parkinglot_service.entity.ParkingLot;
import com.parkease.parkinglot_service.enums.LotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ParkingLotRepository extends JpaRepository<ParkingLot, UUID> {

    // 1. Get all active lots owned by a specific Lot Manager
    List<ParkingLot> findByManagerIdAndIsActiveTrue(UUID managerId);

    // 2. Prevent a manager from creating two lots with the exact same name
    boolean existsByManagerIdAndNameIgnoreCaseAndIsActiveTrue(UUID managerId, String name);

    // 3. Search lots by city (Drivers search for approved, operational lots)
    List<ParkingLot> findByCityIgnoreCaseAndStatusAndIsOpenTrueAndIsActiveTrue(String city, LotStatus status);

    // 4. Admin queue: Fetch all lots awaiting approval (or rejected/suspended)
    List<ParkingLot> findByStatusOrderByCreatedAtDesc(LotStatus status);

    // 5. Admin: Fetch all platform lots ordered by newest
    List<ParkingLot> findAllByOrderByCreatedAtDesc();

    // 6. Geospatial Search: Find nearby approved lots within a specific radius (in Kilometers)
    // Uses the spherical Haversine formula (Earth radius = 6371 km)
    @Query(value = """
            SELECT p.* FROM parking_lots p
            WHERE p.status = 'APPROVED'
              AND p.is_open = true
              AND p.is_active = true
              AND (6371 * acos(
                    LEAST(1.0, GREATEST(-1.0,
                        cos(radians(:latitude)) * cos(radians(p.latitude)) *
                        cos(radians(p.longitude) - radians(:longitude)) +
                        sin(radians(:latitude)) * sin(radians(p.latitude))
                    ))
              )) <= :radiusKm
            ORDER BY (6371 * acos(
                    LEAST(1.0, GREATEST(-1.0,
                        cos(radians(:latitude)) * cos(radians(p.latitude)) *
                        cos(radians(p.longitude) - radians(:longitude)) +
                        sin(radians(:latitude)) * sin(radians(p.latitude))
                    ))
            )) ASC
            """, nativeQuery = true)
    List<ParkingLot> findNearbyApprovedLots(
            @Param("latitude") double latitude,
            @Param("longitude") double longitude,
            @Param("radiusKm") double radiusKm
    );
}