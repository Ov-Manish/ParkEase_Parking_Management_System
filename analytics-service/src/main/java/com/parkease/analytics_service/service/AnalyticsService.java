package com.parkease.analytics_service.service;

import com.parkease.analytics_service.dto.*;
import com.parkease.analytics_service.entity.OccupancyLog;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface AnalyticsService {

    OccupancyLog logOccupancy(OccupancyLog log);

    OccupancyRateResponse getOccupancyRate(UUID lotId, String period);

    List<HourlyOccupancyResponse> getOccupancyByHour(UUID lotId, LocalDate date);

    PeakHoursResponse getPeakHours(UUID lotId, LocalDate date);

    RevenueResponse getRevenueByLot(UUID lotId, LocalDate from, LocalDate to);

    List<DailyRevenueResponse> getRevenueByDay(UUID lotId, int days);

    List<SpotTypeUtilResponse> getMostUsedSpotTypes(UUID lotId);

    AvgDurationResponse getAvgDuration(UUID lotId);

    PlatformSummaryResponse getPlatformSummary();

    DailyReportResponse generateDailyReport(UUID lotId, LocalDate date);
}
