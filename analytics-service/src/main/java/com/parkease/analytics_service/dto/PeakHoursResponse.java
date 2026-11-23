package com.parkease.analytics_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeakHoursResponse {
    private UUID lotId;
    private LocalDate date;
    private Integer peakHour;
    private Double peakOccupancyRate;
    private Integer lowHour;
    private Double lowOccupancyRate;
    private Double averageOccupancyRate;
}
