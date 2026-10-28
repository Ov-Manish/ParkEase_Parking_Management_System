package com.parkease.vehicle_service.service.impl;


import com.parkease.vehicle_service.dto.request.VehicleRequest;
import com.parkease.vehicle_service.dto.request.VehicleUpdateRequest;
import com.parkease.vehicle_service.dto.response.VehicleResponse;
import com.parkease.vehicle_service.entity.Vehicle;
import com.parkease.vehicle_service.enums.VehicleType;
import com.parkease.vehicle_service.exception.BadRequestException;
import com.parkease.vehicle_service.exception.ResourceNotFoundException;
import com.parkease.vehicle_service.exception.UnauthorizedException;
import com.parkease.vehicle_service.repository.VehicleRepository;
import com.parkease.vehicle_service.service.VehicleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleServiceImpl implements VehicleService {

    private final VehicleRepository vehicleRepository;

    @Override
    @Transactional
    public VehicleResponse registerVehicle(UUID currentUserId, VehicleRequest request) {
       UUID ownerId = (request.getOwnerId() != null ? request.getOwnerId() : currentUserId);
       String normalizePlate = normalize(request.getLicensePlate());

       if (vehicleRepository.existsByOwnerIdAndLicensePlateIgnoreCaseAndIsActiveTrue(ownerId , normalizePlate)){
           throw new BadRequestException("You have already registered a vehicle with plate: " + normalizePlate);
       }

       Vehicle vehicle = Vehicle.builder()
               .ownerId(ownerId)
               .licensePlate(normalizePlate)
               .make(request.getMake())
               .model(request.getModel())
               .color(request.getColor())
               .vehicleType(request.getVehicleType())
               .isEV(Boolean.TRUE.equals(request.getIsEV()))
               .isActive(true)
               .build();

       Vehicle savedVehicle = vehicleRepository.save(vehicle);
        log.info("Vehicle registered successfully: vehicleId={}, plate={}, ownerId={}",
                savedVehicle.getVehicleId(), savedVehicle.getLicensePlate(), savedVehicle.getOwnerId());


        return mapToResponse(savedVehicle);

    }

    @Override
    @Transactional(readOnly = true)
    public VehicleResponse getVehicleById(UUID vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId).orElseThrow(
                ()-> new ResourceNotFoundException("Vehicle Not Found  : "+ vehicleId)
        );

        return mapToResponse(vehicle);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleResponse> getVehiclesByOwner(UUID ownerId) {
         return vehicleRepository.findByOwnerIdAndIsActiveTrue(ownerId).stream()
                 .map(this::mapToResponse)
                 .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleResponse getByLicensePlate(String licensePlate) {
        String normalizedPlate = normalize(licensePlate);
        Vehicle vehicle = vehicleRepository.findByLicensePlateIgnoreCase(normalizedPlate).orElseThrow(
                () -> new ResourceNotFoundException("Vehicle Not Found: "+licensePlate)
        );

        return mapToResponse(vehicle);
    }


    @Override
    @Transactional
    public VehicleResponse updateVehicle(UUID vehicleId, UUID currentUserId, boolean isAdmin, VehicleUpdateRequest request) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId).orElseThrow(
                () -> new ResourceNotFoundException("Vehicle Not Found : "+vehicleId)
        );

        validateOwnership(vehicle , currentUserId , isAdmin);

        if (request.getLicensePlate() != null && !request.getLicensePlate().isBlank()){
            String newPlate = normalize(request.getLicensePlate());
            if (!newPlate.equalsIgnoreCase(vehicle.getLicensePlate()) && vehicleRepository.existsByOwnerIdAndLicensePlateIgnoreCaseAndIsActiveTrue(vehicle.getOwnerId(),newPlate)){
                throw new BadRequestException("You Already have an Active Vehicle with Plate : "+ newPlate);
            }

            vehicle.setLicensePlate(newPlate);

        }

        if (request.getMake() != null && !request.getMake().isBlank()) {
            vehicle.setMake(request.getMake().trim());
        }
        if (request.getModel() != null && !request.getModel().isBlank()) {
            vehicle.setModel(request.getModel().trim());
        }
        if (request.getColor() != null) {
            vehicle.setColor(request.getColor().trim());
        }
        if (request.getVehicleType() != null) {
            vehicle.setVehicleType(request.getVehicleType());
        }
        if (request.getIsEV() != null) {
            vehicle.setIsEV(request.getIsEV());
        }

        Vehicle updated = vehicleRepository.save(vehicle);
        log.info("Vehicle updated: vehicleId={}, plate={}", updated.getVehicleId(), updated.getLicensePlate());

        return mapToResponse(vehicle);

    }

    @Override
    @Transactional
    public void deleteVehicle(UUID vehicleId, UUID currentUserId, boolean isAdmin) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId).orElseThrow(
                () -> new ResourceNotFoundException("Vehicle Not Found : "+ vehicleId)
        );

        // Ownership Security Check: Only the owner or an ADMIN can delete
        validateOwnership(vehicle, currentUserId, isAdmin);

        vehicle.setIsActive(false);
        vehicleRepository.save(vehicle);

        log.info("Vehicle soft-deleted: vehicleId={}, ownerId={}", vehicleId, vehicle.getOwnerId());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEVVehicle(UUID vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + vehicleId));
        return Boolean.TRUE.equals(vehicle.getIsEV());
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleType getVehicleType(UUID vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + vehicleId));
        return vehicle.getVehicleType();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleResponse> getAllVehicles() {
        return vehicleRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Helper: Ownership Validation
    private void validateOwnership(Vehicle vehicle, UUID currentUserId, boolean isAdmin) {
        if (!isAdmin && !vehicle.getOwnerId().equals(currentUserId)) {
            throw new UnauthorizedException("You do not have permission to modify this vehicle");
        }
    }
    private VehicleResponse mapToResponse(Vehicle vehicle){
        return VehicleResponse.builder()
                .vehicleId(vehicle.getVehicleId())
                .ownerId(vehicle.getOwnerId())
                .licensePlate(vehicle.getLicensePlate())
                .make(vehicle.getMake())
                .model(vehicle.getModel())
                .color(vehicle.getColor())
                .vehicleType(vehicle.getVehicleType())
                .isEV(vehicle.getIsEV())
                .isActive(vehicle.getIsActive())
                .registeredAt(vehicle.getRegisteredAt())
                .build();

    }

    private String normalize(String vehiclePlate){
        if (vehiclePlate == null) return null;

        return vehiclePlate.replaceAll("[^a-zA-Z0-9]","").toUpperCase();
    }
}
