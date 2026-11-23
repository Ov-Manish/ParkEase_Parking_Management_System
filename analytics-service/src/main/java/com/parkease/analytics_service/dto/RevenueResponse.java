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
public class RevenueResponse {
    private UUID lotId;
    private BigDecimal totalRevenue;
    private Long totalBookings;
    private LocalDate fromDate;
    private LocalDate toDate;
}
