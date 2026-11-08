package com.parkease.spot_service.dto.request;

import com.parkease.spot_service.enums.SpotType;
import com.parkease.spot_service.enums.VehicleType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpotUpdateRequest {

    @NotBlank(message = "Spot number is required")
    @Size(max = 30, message = "Spot number cannot exceed 30 characters")
    private String spotNumber;

    @NotNull(message = "Floor level is required")
    private Integer floor;

    @NotNull(message = "Spot type is required")
    private SpotType spotType;

    @NotNull(message = "Vehicle type is required")
    private VehicleType vehicleType;

    @NotNull(message = "isEVCharging flag is required")
    private Boolean isEVCharging;

    @NotNull(message = "isHandicapped flag is required")
    private Boolean isHandicapped;

    @NotNull(message = "Price per hour is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price per hour must be greater than zero")
    private BigDecimal pricePerHour;
}