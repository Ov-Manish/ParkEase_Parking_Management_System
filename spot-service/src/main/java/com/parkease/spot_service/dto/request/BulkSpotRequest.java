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
public class BulkSpotRequest {

    @NotNull(message = "Parking lot ID is required")
    private UUID lotId;

    @NotNull(message = "Floor level is required")
    private Integer floor;

    @NotBlank(message = "Prefix is required (e.g. 'A-', 'B-', 'EV-')")
    @Size(max = 10, message = "Prefix cannot exceed 10 characters")
    private String prefix;

    @Min(value = 1, message = "Start number must be at least 1")
    private int fromNumber;

    @Min(value = 1, message = "End number must be at least 1")
    private int toNumber;

    @NotNull(message = "Spot type is required")
    private SpotType spotType;

    @NotNull(message = "Vehicle type is required")
    private VehicleType vehicleType;

    @Builder.Default
    private Boolean isEVCharging = false;

    @Builder.Default
    private Boolean isHandicapped = false;

    @NotNull(message = "Price per hour is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price per hour must be greater than zero")
    private BigDecimal pricePerHour;
}