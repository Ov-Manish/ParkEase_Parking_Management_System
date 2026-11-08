package com.parkease.spot_service.service.impl;


import com.parkease.spot_service.client.ParkingLotClient;
import com.parkease.spot_service.dto.request.BulkSpotRequest;
import com.parkease.spot_service.dto.request.SpotRequest;
import com.parkease.spot_service.dto.request.SpotUpdateRequest;
import com.parkease.spot_service.dto.response.SpotCountResponse;
import com.parkease.spot_service.dto.response.SpotResponse;
import com.parkease.spot_service.entity.ParkingSpot;
import com.parkease.spot_service.enums.SpotStatus;
import com.parkease.spot_service.enums.SpotType;
import com.parkease.spot_service.enums.VehicleType;
import com.parkease.spot_service.exception.BadRequestException;
import com.parkease.spot_service.exception.ResourceNotFoundException;
import com.parkease.spot_service.repository.ParkingSpotRepository;
import com.parkease.spot_service.service.ParkingSpotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParkingSpotServiceImpl implements ParkingSpotService {
    private final ParkingSpotRepository parkingSpotRepository;
    private final ParkingLotClient parkingLotClient;

//     1. Create a single customized spot


    @Override
    @Transactional
    public SpotResponse createSpot(SpotRequest request) {
        validateVehicleCompatibility(request.getVehicleType() , request.getSpotType());

        if (parkingSpotRepository.existsByLotIdAndSpotNumberIgnoreCaseAndIsActiveTrue(request.getLotId(),request.getSpotNumber().trim())){
            throw new BadRequestException("Spot Number " + request.getSpotNumber() + "Already exists in this Parking Lot ");
        }

        ParkingSpot spot = ParkingSpot.builder()
                .lotId(request.getLotId())
                .spotNumber(request.getSpotNumber().trim().toUpperCase())
                .floor(request.getFloor())
                .spotType(request.getSpotType())
                .vehicleType(request.getVehicleType())
                .status(SpotStatus.AVAILABLE)
                .isEVCharging(Boolean.TRUE.equals(request.getIsEVCharging()))
                .isHandicapped(Boolean.TRUE.equals(request.getIsHandicapped()))
                .pricePerHour(request.getPricePerHour())
                .isActive(true)
                .build();

        ParkingSpot savedSpot = parkingSpotRepository.save(spot);

        log.info("Created spot {} for lot {}", savedSpot.getSpotNumber(), savedSpot.getLotId());

//        Sync Total capacity with ParkingLot - service
        syncCapacityWithParkingLot(request.getLotId());
        return mapToResponse(savedSpot);
    }


//    High-Performance Bulk Spot Creation Engine

    @Override
    @Transactional
    public List<SpotResponse> createBulkSpots(BulkSpotRequest request) {
        if (request.getFromNumber() > request.getToNumber()){
            throw new BadRequestException("fromNumber cannot be greater than toNumber");
        }

        int count = request.getToNumber() - request.getFromNumber() + 1;

        if (count > 200){
            throw new BadRequestException("Bulk creation limit exceeded. Maximum 200 spots can be created per batch.");
        }

        validateVehicleCompatibility(request.getVehicleType(),request.getSpotType());
        List<ParkingSpot> spotToSave = new ArrayList<>();
        String prefix  = request.getPrefix() != null ? request.getPrefix().trim().toUpperCase() : "";

        for (int i = request.getFromNumber() ; i <= request.getToNumber() ; i++){
            String spotNumber = prefix + i;

            if (parkingSpotRepository.existsByLotIdAndSpotNumberIgnoreCaseAndIsActiveTrue(request.getLotId() , spotNumber)){
                throw new BadRequestException("Cannot bulk-create spots: Spot number " + spotNumber + " already exists in this lot");
            }

            spotToSave.add(ParkingSpot.builder()
                    .lotId(request.getLotId())
                            .spotNumber(spotNumber)
                            .floor(request.getFloor())
                            .spotType(request.getSpotType())
                            .vehicleType(request.getVehicleType())
                            .status(SpotStatus.AVAILABLE)
                            .isEVCharging(Boolean.TRUE.equals(request.getIsEVCharging()))
                            .isHandicapped(Boolean.TRUE.equals(request.getIsHandicapped()))
                            .pricePerHour(request.getPricePerHour())
                            .isActive(true)
                            .build());
        }

        List<ParkingSpot> savedSpots= parkingSpotRepository.saveAll(spotToSave);
        log.info("Bulk-created {} spots for lot {}", savedSpots.size(), request.getLotId());

        // Sync total capacity with parkinglot-service
        syncCapacityWithParkingLot(request.getLotId());

        return savedSpots.stream().map(this::mapToResponse).toList();
    }

    //    Get all active spots in a lot (for Manager dashboard)

    @Override
    @Transactional(readOnly = true)
    public List<SpotResponse> getSpotsByLot(UUID lotId) {
        return parkingSpotRepository.findByLotIdAndIsActiveTrueOrderByFloorAscSpotNumberAsc(lotId)
                .stream().map(this::mapToResponse).toList();
    }



//    Update spot details

    @Override
    @Transactional
    public SpotResponse updateSpot(UUID spotId, SpotUpdateRequest request) {
        ParkingSpot spot = findByIdOrThrow(spotId);

        validateVehicleCompatibility(request.getVehicleType() , request.getSpotType());


        String newNumber = request.getSpotNumber().trim().toUpperCase();
        if (!spot.getSpotNumber().equalsIgnoreCase(newNumber) && parkingSpotRepository.existsByLotIdAndSpotNumberIgnoreCaseAndIsActiveTrue(spot.getLotId(),newNumber)){
            throw new BadRequestException("Spot number " + newNumber + " already exists in this lot");
        }


        spot.setSpotNumber(newNumber);
        spot.setFloor(request.getFloor());
        spot.setSpotType(request.getSpotType());
        spot.setVehicleType(request.getVehicleType());
        spot.setIsEVCharging(request.getIsEVCharging());
        spot.setIsHandicapped(request.getIsHandicapped());
        spot.setPricePerHour(request.getPricePerHour());


        ParkingSpot updated = parkingSpotRepository.save(spot);

        log.info("Updated spot {}: id={}", updated.getSpotNumber(), spotId);

        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public SpotResponse updateSpotStatus(UUID spotId, SpotStatus status) {
        ParkingSpot spot = findByIdOrThrow(spotId);

        if (status == SpotStatus.OUT_OF_SERVICE && (spot.getStatus() == SpotStatus.OCCUPIED || spot.getStatus() == SpotStatus.RESERVED) ){
            throw new BadRequestException("Cannot set spot to OUT_OF_SERVICE while it is currently " + spot.getStatus());
        }

        spot.setStatus(status);
        ParkingSpot updatedStatus  = parkingSpotRepository.save(spot);

        log.info("Spot {} status updated to : {}",spot.getSpotNumber(), status);

        return mapToResponse(updatedStatus);
    }


//    Soft Delete a Spot

    @Override
    @Transactional
    public void deleteSpot(UUID spotId) {
        ParkingSpot spot = findByIdOrThrow(spotId);
        if (spot.getStatus() == SpotStatus.OCCUPIED || spot.getStatus() == SpotStatus.RESERVED){
            throw new BadRequestException("Cannot delete spot while it is currently "+spot.getStatus());
        }

        spot.setIsActive(false);

        parkingSpotRepository.save(spot);

        log.info("Spot {} Soft - Deleted , "+ spot.getSpotNumber());

//        Sync decreased capacity with parkinglot-service
        syncCapacityWithParkingLot(spot.getLotId());

    }

//     Get spot details by ID

    @Override
    @Transactional
    public SpotResponse getSpotById(UUID spotId) {
        return mapToResponse(findByIdOrThrow(spotId));
    }

//    Public driver query: Filter available spots in a lot

    @Override
    @Transactional(readOnly = true)
    public List<SpotResponse> getAvailableSpots(UUID lotId, Integer floor, SpotType spotType, VehicleType vehicleType) {
        List<ParkingSpot> spots;

        if (floor != null){
            spots= parkingSpotRepository.findByLotIdAndFloorAndStatusAndIsActiveTrue(lotId,floor,SpotStatus.AVAILABLE);
        } else if (spotType != null) {
            spots = parkingSpotRepository.findByLotIdAndSpotTypeAndStatusAndIsActiveTrue(lotId,spotType,SpotStatus.AVAILABLE);
        } else if (vehicleType != null) {
            spots = parkingSpotRepository.findByLotIdAndVehicleTypeAndStatusAndIsActiveTrue(lotId, vehicleType,SpotStatus.AVAILABLE);
        }else {
            spots = parkingSpotRepository.findByLotIdAndStatusAndIsActiveTrue(lotId,SpotStatus.AVAILABLE);
        }

        return spots.stream().map(this::mapToResponse).toList();
    }

//     Real-time count metrics and occupancy rate

    @Override
    @Transactional(readOnly = true)
    public SpotCountResponse getSpotCountsByLot(UUID lotId) {
        long totalCount = parkingSpotRepository.countByLotIdAndIsActiveTrue(lotId);

        long available = parkingSpotRepository.countByLotIdAndStatusAndIsActiveTrue(lotId,SpotStatus.AVAILABLE);
        long reserved = parkingSpotRepository.countByLotIdAndStatusAndIsActiveTrue(lotId,SpotStatus.RESERVED);
        long occupied = parkingSpotRepository.countByLotIdAndStatusAndIsActiveTrue(lotId,SpotStatus.OCCUPIED);
        long outOfService = parkingSpotRepository.countByLotIdAndStatusAndIsActiveTrue(lotId,SpotStatus.OUT_OF_SERVICE);

        double occupancyRate = totalCount > 0 ? ((double) (occupied + reserved) / totalCount) * 100.0 : 0.0;
        double roundedRate = Math.round(occupancyRate * 100.0) / 100.0;

        return SpotCountResponse.builder()
                .lotId(lotId)
                .totalSpots(totalCount)
                .availableSpots(available)
                .reservedSpots(reserved)
                .occupiedSpots(occupied)
                .outOfServiceSpots(outOfService)
                .occupancyRatePercentage(roundedRate)
                .build();
    }


//    INTER-SERVICE LIFECYCLE (Called by Booking-Service with @Version lock)

    @Override
    @Transactional
    public SpotResponse reserveSpot(UUID spotId) {
        ParkingSpot spot = findByIdOrThrow(spotId);

        if (spot.getStatus() != SpotStatus.AVAILABLE){
            throw new BadRequestException("Spot " + spot.getSpotNumber() + " is no longer available (Current status: " + spot.getStatus() + ")");
        }

        spot.setStatus(SpotStatus.RESERVED);
        ParkingSpot updatedSpotStatus = parkingSpotRepository.save(spot);
        log.info("Spot {} RESERVED", spot.getSpotNumber());
        return mapToResponse(updatedSpotStatus);
    }

//    Occupy spot on driver check-in

    @Override
    @Transactional
    public SpotResponse occupySpot(UUID spotId) {
        ParkingSpot spots = findByIdOrThrow(spotId);
        if (spots.getStatus() != SpotStatus.RESERVED && spots.getStatus() != SpotStatus.AVAILABLE){
            throw new BadRequestException("Cannot check in: Spot " + spots.getSpotNumber() + " is currently " + spots.getStatus());
        }
        spots.setStatus(SpotStatus.OCCUPIED);
        ParkingSpot updatedSpot = parkingSpotRepository.save(spots);
        log.info("Spot {} OCCUPIED", spots.getSpotNumber());
        return mapToResponse(updatedSpot);

    }

//    Release spot on checkout or cancellation

    @Override
    @Transactional
    public SpotResponse releaseSpot(UUID spotId) {
        ParkingSpot spot = findByIdOrThrow(spotId);
        spot.setStatus(SpotStatus.AVAILABLE);
        ParkingSpot updatedSpot = parkingSpotRepository.save(spot);

        log.info("Spot {} RELEASED to AVAILABLE", spot.getSpotNumber());

        return  mapToResponse(updatedSpot);
    }


//     Helper Methods

    private ParkingSpot findByIdOrThrow(UUID spotId){
        return parkingSpotRepository.findById(spotId)
                .filter(ParkingSpot::getIsActive)
                .orElseThrow(()-> new ResourceNotFoundException("Parking Spot Not Found With Id : "+ spotId));
    }

    private void validateVehicleCompatibility(VehicleType vehicleType, SpotType spotType){
        if (vehicleType == VehicleType.TWO_WHEELER && spotType != SpotType.MOTORBIKE ){
            throw new BadRequestException("Two-wheelers can only be assigned to MOTORBIKE spots");
        }
        if (vehicleType == VehicleType.HEAVY && spotType != SpotType.LARGE){
            throw new BadRequestException("Heavy vehicles can only be assigned to LARGE spots");
        }

    }

    private void syncCapacityWithParkingLot(UUID lotId){
        long currentTotal = parkingSpotRepository.countByLotIdAndIsActiveTrue(lotId);
        parkingLotClient.syncTotalSpots(lotId,currentTotal);
    }


//  Map To Response
    private SpotResponse mapToResponse(ParkingSpot spot){
        return SpotResponse.builder()
                .spotId(spot.getSpotId())
                .lotId(spot.getLotId())
                .spotNumber(spot.getSpotNumber())
                .floor(spot.getFloor())
                .spotType(spot.getSpotType())
                .vehicleType(spot.getVehicleType())
                .status(spot.getStatus())
                .isEVCharging(spot.getIsEVCharging())
                .isHandicapped(spot.getIsHandicapped())
                .pricePerHour(spot.getPricePerHour())
                .isActive(spot.getIsActive())
                .version(spot.getVersion())
                .createdAt(spot.getCreatedAt())
                .updatedAt(spot.getUpdatedAt())
                .build();
    }
}
