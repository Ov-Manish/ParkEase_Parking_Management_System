package com.parkease.analytics_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OccupancyRateResponse {
    private UUID lotId;
    private Double currentOccupancyRate;
    private Double averageOccupancyRate;
    private Integer totalSpots;
    private Integer occupiedSpots;
    private Integer availableSpots;
    private String period;
}
