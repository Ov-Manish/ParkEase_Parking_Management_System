package com.parkease.spot_service.dto.response;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpotCountResponse {

    private UUID lotId;
    private long totalSpots;
    private long availableSpots;
    private long reservedSpots;
    private long occupiedSpots;
    private long outOfServiceSpots;
    private double occupancyRatePercentage;
}