package com.parkease.spot_service.dto.request;

import com.parkease.spot_service.enums.SpotType;
import com.parkease.spot_service.enums.VehicleType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpotRequest {

    @NotNull(message = "Parking lot ID is required")
    private UUID lotId;

    @NotBlank(message = "Spot number is required (e.g. A-101, B-12)")
    @Size(max = 30, message = "Spot number cannot exceed 30 characters")
    private String spotNumber;

    @NotNull(message = "Floor level is required (0 for Ground, 1 for 1st Floor, etc.)")
    private Integer floor;

    @NotNull(message = "Spot type is required (COMPACT, STANDARD, LARGE, MOTORBIKE, EV, HANDICAPPED)")
    private SpotType spotType;

    @NotNull(message = "Vehicle type is required (TWO_WHEELER, FOUR_WHEELER, HEAVY)")
    private VehicleType vehicleType;

    @Builder.Default
    private Boolean isEVCharging = false;

    @Builder.Default
    private Boolean isHandicapped = false;

    @NotNull(message = "Price per hour is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price per hour must be greater than zero")
    private BigDecimal pricePerHour;
}