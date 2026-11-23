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
public class AvgDurationResponse {
    private UUID lotId;
    private Double averageDurationMinutes;
    private Long totalCompletedBookings;
}
