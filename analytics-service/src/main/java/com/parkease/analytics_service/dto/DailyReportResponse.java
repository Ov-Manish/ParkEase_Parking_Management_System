package com.parkease.analytics_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyReportResponse {
    private UUID lotId;
    private LocalDate date;
    private Long totalBookings;
    private BigDecimal totalRevenue;
    private Double peakOccupancyRate;
    private Integer peakHour;
    private Double averageOccupancyRate;
}
