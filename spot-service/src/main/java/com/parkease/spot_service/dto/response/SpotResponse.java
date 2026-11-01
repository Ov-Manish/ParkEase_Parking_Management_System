package com.parkease.spot_service.dto.response;

import com.parkease.spot_service.enums.SpotStatus;
import com.parkease.spot_service.enums.SpotType;
import com.parkease.spot_service.enums.VehicleType;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpotResponse {

    private UUID spotId;
    private UUID lotId;
    private String spotNumber;
    private Integer floor;
    private SpotType spotType;
    private VehicleType vehicleType;
    private SpotStatus status;
    private Boolean isEVCharging;
    private Boolean isHandicapped;
    private BigDecimal pricePerHour;
    private Boolean isActive;
    private Long version;
    private Instant createdAt;
    private Instant updatedAt;
}