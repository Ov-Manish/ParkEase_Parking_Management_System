package com.parkease.analytics_service.controller;

import com.parkease.analytics_service.dto.*;
import com.parkease.analytics_service.entity.OccupancyLog;
import com.parkease.analytics_service.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics & Reports", description = "Occupancy, peak hours, revenue, and platform summary APIs")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @PostMapping("/occupancy/log")
    @Operation(summary = "Log periodic occupancy snapshot")
    public ResponseEntity<OccupancyLog> logOccupancy(@RequestBody OccupancyLog log) {
        return new ResponseEntity<>(analyticsService.logOccupancy(log), HttpStatus.CREATED);
    }

    @GetMapping("/lot/{id}/occupancy")
    @Operation(summary = "Get real-time & period occupancy rate for a lot")
    @PreAuthorize("hasAnyRole('LOT_MANAGER', 'ADMIN')")
    public ResponseEntity<OccupancyRateResponse> getOccupancyRate(
            @PathVariable("id") UUID lotId,
            @RequestParam(value = "period", required = false, defaultValue = "TODAY") String period) {
        return ResponseEntity.ok(analyticsService.getOccupancyRate(lotId, period));
    }

    @GetMapping("/lot/{id}/hourly")
    @Operation(summary = "Get 24-hour occupancy breakdown curve for a lot")
    @PreAuthorize("hasAnyRole('LOT_MANAGER', 'ADMIN')")
    public ResponseEntity<List<HourlyOccupancyResponse>> getOccupancyByHour(
            @PathVariable("id") UUID lotId,
            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(analyticsService.getOccupancyByHour(lotId, date));
    }

    @GetMapping("/lot/{id}/peak-hours")
    @Operation(summary = "Get peak rush hours vs low hours for a lot")
    @PreAuthorize("hasAnyRole('LOT_MANAGER', 'ADMIN')")
    public ResponseEntity<PeakHoursResponse> getPeakHours(
            @PathVariable("id") UUID lotId,
            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(analyticsService.getPeakHours(lotId, date));
    }

    @GetMapping("/lot/{id}/revenue")
    @Operation(summary = "Get aggregated revenue for a lot over date range")
    @PreAuthorize("hasAnyRole('LOT_MANAGER', 'ADMIN')")
    public ResponseEntity<RevenueResponse> getRevenue(
            @PathVariable("id") UUID lotId,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(analyticsService.getRevenueByLot(lotId, from, to));
    }

    @GetMapping("/lot/{id}/daily-revenue")
    @Operation(summary = "Get day-by-day revenue trend for recent days")
    @PreAuthorize("hasAnyRole('LOT_MANAGER', 'ADMIN')")
    public ResponseEntity<List<DailyRevenueResponse>> getDailyRevenue(
            @PathVariable("id") UUID lotId,
            @RequestParam(value = "days", required = false, defaultValue = "7") int days) {
        return ResponseEntity.ok(analyticsService.getRevenueByDay(lotId, days));
    }

    @GetMapping("/lot/{id}/spot-types")
    @Operation(summary = "Get spot type utilization breakdown (Standard, EV, Compact, Handicapped)")
    @PreAuthorize("hasAnyRole('LOT_MANAGER', 'ADMIN')")
    public ResponseEntity<List<SpotTypeUtilResponse>> getSpotTypeUtilization(@PathVariable("id") UUID lotId) {
        return ResponseEntity.ok(analyticsService.getMostUsedSpotTypes(lotId));
    }

    @GetMapping("/lot/{id}/avg-duration")
    @Operation(summary = "Get average parking duration for a lot")
    @PreAuthorize("hasAnyRole('LOT_MANAGER', 'ADMIN')")
    public ResponseEntity<AvgDurationResponse> getAvgDuration(@PathVariable("id") UUID lotId) {
        return ResponseEntity.ok(analyticsService.getAvgDuration(lotId));
    }

    @GetMapping("/platform/summary")
    @Operation(summary = "Platform-wide macro overview across all lots (Super Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PlatformSummaryResponse> getPlatformSummary() {
        return ResponseEntity.ok(analyticsService.getPlatformSummary());
    }

    @GetMapping("/reports/daily")
    @Operation(summary = "Generate consolidated daily occupancy and revenue report")
    @PreAuthorize("hasAnyRole('LOT_MANAGER', 'ADMIN')")
    public ResponseEntity<DailyReportResponse> getDailyReport(
            @RequestParam("lotId") UUID lotId,
            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(analyticsService.generateDailyReport(lotId, date));
    }
}
