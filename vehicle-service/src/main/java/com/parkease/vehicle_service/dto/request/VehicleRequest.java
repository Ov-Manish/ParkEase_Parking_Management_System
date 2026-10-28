package com.parkease.vehicle_service.dto.request;

import com.parkease.vehicle_service.enums.VehicleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleRequest {

    // Optional in request body if resolved automatically from JWT token
    private UUID ownerId;

    @NotBlank(message = "License plate number is required")
    @Size(min = 4, max = 20, message = "License plate must be between 4 and 20 characters")
    private String licensePlate;

    @NotBlank(message = "Vehicle make is required (e.g. Honda, Tesla, Toyota)")
    @Size(max = 50, message = "Make cannot exceed 50 characters")
    private String make;

    @NotBlank(message = "Vehicle model is required (e.g. Civic, Model 3, Fortuner)")
    @Size(max = 50, message = "Model cannot exceed 50 characters")
    private String model;

    @Size(max = 30, message = "Color cannot exceed 30 characters")
    private String color;

    @NotNull(message = "Vehicle type is required (TWO_WHEELER, FOUR_WHEELER, HEAVY)")
    private VehicleType vehicleType;

    @Builder.Default
    private Boolean isEV = false;
}