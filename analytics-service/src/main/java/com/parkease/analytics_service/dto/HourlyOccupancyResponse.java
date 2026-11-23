package com.parkease.analytics_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HourlyOccupancyResponse {
    private Integer hour;
    private Double occupancyRate;
    private Integer occupiedSpots;
    private LocalDateTime timestamp;
}
