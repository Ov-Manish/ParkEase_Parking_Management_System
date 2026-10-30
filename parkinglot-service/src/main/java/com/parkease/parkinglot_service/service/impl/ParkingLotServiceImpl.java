package com.parkease.parkinglot_service.service.impl;

import com.parkease.parkinglot_service.dto.request.LotRejectionRequest;
import com.parkease.parkinglot_service.dto.request.ParkingLotRequest;
import com.parkease.parkinglot_service.dto.request.ParkingLotUpdateRequest;
import com.parkease.parkinglot_service.dto.response.ParkingLotResponse;
import com.parkease.parkinglot_service.entity.ParkingLot;
import com.parkease.parkinglot_service.enums.LotStatus;
import com.parkease.parkinglot_service.exception.BadRequestException;
import com.parkease.parkinglot_service.exception.ResourceNotFoundException;
import com.parkease.parkinglot_service.exception.UnauthorizedException;
import com.parkease.parkinglot_service.repository.ParkingLotRepository;
import com.parkease.parkinglot_service.service.ParkingLotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParkingLotServiceImpl implements ParkingLotService {

    private final ParkingLotRepository parkingLotRepository;

    // 1. Register a new parking lot (Starts in PENDING_APPROVAL)
    @Override
    @Transactional
    public ParkingLotResponse registerLot(UUID managerId, ParkingLotRequest request) {
        if (parkingLotRepository.existsByManagerIdAndNameIgnoreCaseAndIsActiveTrue(managerId, request.getName().trim())) {
            throw new BadRequestException("You already have an active parking lot registered with the name: " + request.getName());
        }

        ParkingLot lot = ParkingLot.builder()
                .managerId(managerId)
                .name(request.getName().trim())
                .address(request.getAddress().trim())
                .city(request.getCity().trim())
                .state(request.getState() != null ? request.getState().trim() : null)
                .zipCode(request.getZipCode() != null ? request.getZipCode().trim() : null)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .hourlyRate(request.getHourlyRate())
                .totalSpots(0)
                .status(LotStatus.PENDING_APPROVAL)
                .isOpen(true)
                .isActive(true) // Inactive until Admin approves
                .build();

        ParkingLot savedLot = parkingLotRepository.save(lot);
        log.info("New parking lot registered by manager {}: lotId={}", managerId, savedLot.getLotId());
        return mapToResponse(savedLot, null);
    }

    // 2. Get all active lots owned by the authenticated Lot Manager
    @Override
    @Transactional(readOnly = true)
    public List<ParkingLotResponse> getLotsByManager(UUID managerId) {
        return parkingLotRepository.findByManagerIdAndIsActiveTrue(managerId).stream()
                .map(lot -> mapToResponse(lot, null))
                .toList();
    }

    // 3. Get lot by ID
    @Override
    @Transactional(readOnly = true)
    public ParkingLotResponse getLotById(UUID lotId) {
        ParkingLot lot = findLotByIdOrThrow(lotId);
        return mapToResponse(lot, null);
    }

    // 4. Update lot details (Owner or Admin only)
    @Override
    @Transactional
    public ParkingLotResponse updateLot(UUID lotId, UUID managerId, boolean isAdmin, ParkingLotUpdateRequest request) {
        ParkingLot lot = findLotByIdOrThrow(lotId);
        checkOwnership(lot, managerId, isAdmin);

        lot.setName(request.getName().trim());
        lot.setAddress(request.getAddress().trim());
        lot.setCity(request.getCity().trim());
        lot.setState(request.getState() != null ? request.getState().trim() : null);
        lot.setZipCode(request.getZipCode() != null ? request.getZipCode().trim() : null);
        lot.setHourlyRate(request.getHourlyRate());

        ParkingLot updated = parkingLotRepository.save(lot);
        log.info("Parking lot updated: lotId={}", lotId);
        return mapToResponse(updated, null);
    }

    // 5. Toggle Open/Close status in real-time
    @Override
    @Transactional
    public ParkingLotResponse toggleLotOpenStatus(UUID lotId, UUID managerId, boolean isAdmin) {
        ParkingLot lot = findLotByIdOrThrow(lotId);
        checkOwnership(lot, managerId, isAdmin);

        if (lot.getStatus() != LotStatus.APPROVED) {
            throw new BadRequestException("Only approved parking lots can be opened or closed");
        }

        lot.setIsOpen(!lot.getIsOpen());
        ParkingLot saved = parkingLotRepository.save(lot);
        log.info("Parking lot {} open status toggled to: {}", lotId, saved.getIsOpen());
        return mapToResponse(saved, null);
    }

    // 6. Soft Delete parking lot
    @Override
    @Transactional
    public void deleteLot(UUID lotId, UUID managerId, boolean isAdmin) {
        ParkingLot lot = findLotByIdOrThrow(lotId);
        checkOwnership(lot, managerId, isAdmin);

        lot.setIsActive(false);
        parkingLotRepository.save(lot);
        log.info("Parking lot {} soft-deleted", lotId);
    }

    // 7. Public search: Search approved & open lots by city
    @Override
    @Transactional(readOnly = true)
    public List<ParkingLotResponse> searchByCity(String city) {
        if (city == null || city.isBlank()) {
            throw new BadRequestException("City parameter cannot be blank");
        }
        return parkingLotRepository.findByCityIgnoreCaseAndStatusAndIsOpenTrueAndIsActiveTrue(city.trim(), LotStatus.APPROVED)
                .stream()
                .map(lot -> mapToResponse(lot, null))
                .toList();
    }

    // 8. Public GPS nearby search (Haversine distance calculation)
    @Override
    @Transactional(readOnly = true)
    public List<ParkingLotResponse> searchNearby(double latitude, double longitude, double radiusKm) {
        if (latitude < -90.0 || latitude > 90.0) {
            throw new BadRequestException("Latitude must be between -90 and 90");
        }
        if (longitude < -180.0 || longitude > 180.0) {
            throw new BadRequestException("Longitude must be between -180 and 180");
        }
        if (radiusKm <= 0.0) {
            radiusKm = 5.0; // Default 5 km radius
        }

        List<ParkingLot> nearbyLots = parkingLotRepository.findNearbyApprovedLots(latitude, longitude, radiusKm);

        return nearbyLots.stream().map(lot -> {
            double distance = calculateHaversineKm(latitude, longitude, lot.getLatitude().doubleValue(), lot.getLongitude().doubleValue());
            double roundedDistance = Math.round(distance * 100.0) / 100.0; // Round to 2 decimal places
            return mapToResponse(lot, roundedDistance);
        }).toList();
    }

    // 9. Admin: Get all pending lots awaiting review
    @Override
    @Transactional(readOnly = true)
    public List<ParkingLotResponse> getPendingLots() {
        return parkingLotRepository.findByStatusOrderByCreatedAtDesc(LotStatus.PENDING_APPROVAL)
                .stream()
                .map(lot -> mapToResponse(lot, null))
                .toList();
    }

    // 10. Admin: Approve lot
    @Override
    @Transactional
    public ParkingLotResponse approveLot(UUID lotId) {
        ParkingLot lot = findLotByIdOrThrow(lotId);

        lot.setStatus(LotStatus.APPROVED);
        lot.setIsActive(true);
        lot.setRejectionReason(null);

        ParkingLot approved = parkingLotRepository.save(lot);
        log.info("Parking lot {} APPROVED by admin", lotId);
        return mapToResponse(approved, null);
    }

    // 11. Admin: Reject lot with feedback
    @Override
    @Transactional
    public ParkingLotResponse rejectLot(UUID lotId, LotRejectionRequest request) {
        ParkingLot lot = findLotByIdOrThrow(lotId);

        lot.setStatus(LotStatus.REJECTED);
//        lot.setIsActive(false);
        lot.setRejectionReason(request.getReason().trim());

        ParkingLot rejected = parkingLotRepository.save(lot);
        log.info("Parking lot {} REJECTED by admin. Reason: {}", lotId, request.getReason());
        return mapToResponse(rejected, null);
    }

    // 12. Admin: Get all platform lots
    @Override
    @Transactional(readOnly = true)
    public List<ParkingLotResponse> getAllLots() {
        return parkingLotRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(lot -> mapToResponse(lot, null))
                .toList();
    }

    // 13. Inter-Service: Verify lot exists, is approved and open for bookings
    @Override
    @Transactional(readOnly = true)
    public ParkingLotResponse verifyLotForBooking(UUID lotId) {
        ParkingLot lot = findLotByIdOrThrow(lotId);

        if (!Boolean.TRUE.equals(lot.getIsActive())) {
            throw new BadRequestException("Parking lot is inactive");
        }
        if (lot.getStatus() != LotStatus.APPROVED) {
            throw new BadRequestException("Parking lot is not approved for bookings");
        }
        if (!Boolean.TRUE.equals(lot.getIsOpen())) {
            throw new BadRequestException("Parking lot is currently closed");
        }

        return mapToResponse(lot, null);
    }

    // 14. Inter-Service: Spot-Service capacity synchronization
    @Override
    @Transactional
    public void updateTotalSpots(UUID lotId, int totalSpots) {
        if (totalSpots < 0) {
            throw new BadRequestException("Total spots capacity cannot be negative");
        }
        ParkingLot lot = findLotByIdOrThrow(lotId);
        lot.setTotalSpots(totalSpots);
        parkingLotRepository.save(lot);
        log.info("Parking lot {} total spots updated to: {}", lotId, totalSpots);
    }

    // --- Helper Methods ---

    private ParkingLot findLotByIdOrThrow(UUID lotId) {
        return parkingLotRepository.findById(lotId)
                .orElseThrow(() -> new ResourceNotFoundException("Parking lot not found with id: " + lotId));
    }

    private void checkOwnership(ParkingLot lot, UUID managerId, boolean isAdmin) {
        if (!isAdmin && !lot.getManagerId().equals(managerId)) {
            throw new UnauthorizedException("You do not have permission to modify this parking lot");
        }
    }

    private ParkingLotResponse mapToResponse(ParkingLot lot, Double distanceKm) {
        return ParkingLotResponse.builder()
                .lotId(lot.getLotId())
                .managerId(lot.getManagerId())
                .name(lot.getName())
                .address(lot.getAddress())
                .city(lot.getCity())
                .state(lot.getState())
                .zipCode(lot.getZipCode())
                .latitude(lot.getLatitude())
                .longitude(lot.getLongitude())
                .totalSpots(lot.getTotalSpots())
                .hourlyRate(lot.getHourlyRate())
                .status(lot.getStatus())
                .rejectionReason(lot.getRejectionReason())
                .isOpen(lot.getIsOpen())
                .isActive(lot.getIsActive())
                .distanceKm(distanceKm)
                .createdAt(lot.getCreatedAt())
                .updatedAt(lot.getUpdatedAt())
                .build();
    }

    // Calculates distance on Earth surface in kilometers between two GPS points
    private double calculateHaversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0; // Earth radius in KM
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}