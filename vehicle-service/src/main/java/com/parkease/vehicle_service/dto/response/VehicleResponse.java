package com.parkease.vehicle_service.dto.response;

import com.parkease.vehicle_service.enums.VehicleType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleResponse {
    private UUID vehicleId;
    private UUID ownerId;
    private String licensePlate;
    private String make;
    private String model;
    private String color;
    private VehicleType vehicleType;
    private Boolean isEV;
    private Boolean isActive;
    private LocalDateTime registeredAt;
}