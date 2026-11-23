package com.parkease.analytics_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformSummaryResponse {
    private Long totalLots;
    private Long totalSpots;
    private Long activeBookings;
    private Double averagePlatformOccupancy;
    private BigDecimal totalPlatformRevenue;
    private LocalDateTime generatedAt;
}
