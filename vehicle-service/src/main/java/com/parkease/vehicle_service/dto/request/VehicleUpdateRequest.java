package com.parkease.vehicle_service.dto.request;

import com.parkease.vehicle_service.enums.VehicleType;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleUpdateRequest {

    @Size(min = 4, max = 20, message = "License plate must be between 4 and 20 characters")
    private String licensePlate;

    @Size(max = 50, message = "Make cannot exceed 50 characters")
    private String make;

    @Size(max = 50, message = "Model cannot exceed 50 characters")
    private String model;

    @Size(max = 30, message = "Color cannot exceed 30 characters")
    private String color;

    private VehicleType vehicleType;

    private Boolean isEV;
}