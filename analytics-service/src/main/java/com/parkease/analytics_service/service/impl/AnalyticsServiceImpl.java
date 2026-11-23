package com.parkease.analytics_service.service.impl;

import com.parkease.analytics_service.dto.*;
import com.parkease.analytics_service.entity.OccupancyLog;
import com.parkease.analytics_service.repository.AnalyticsRepository;
import com.parkease.analytics_service.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsServiceImpl implements AnalyticsService {

    private final AnalyticsRepository analyticsRepository;
    private final RestTemplate restTemplate;

    @Override
    @Transactional
    public OccupancyLog logOccupancy(OccupancyLog logEntry) {
        if (logEntry.getTimestamp() == null) {
            logEntry.setTimestamp(LocalDateTime.now());
        }
        if (logEntry.getOccupancyRate() == null && logEntry.getTotalSpots() != null && logEntry.getTotalSpots() > 0) {
            int occupied = logEntry.getTotalSpots() - (logEntry.getAvailableSpots() != null ? logEntry.getAvailableSpots() : 0);
            double rate = ((double) occupied / logEntry.getTotalSpots()) * 100.0;
            logEntry.setOccupancyRate(Math.round(rate * 10.0) / 10.0);
        }
        return analyticsRepository.save(logEntry);
    }

    @Override
    public OccupancyRateResponse getOccupancyRate(UUID lotId, String period) {
        LocalDateTime since = resolveSinceDate(period);
        Double avgRate = analyticsRepository.findAvgOccupancyRateSince(lotId, since);

        List<OccupancyLog> latestLogs = analyticsRepository.findByLotIdOrderByTimestampDesc(lotId);
        OccupancyLog latest = latestLogs.isEmpty() ? null : latestLogs.get(0);

        Double currentRate = latest != null ? latest.getOccupancyRate() : (avgRate != null ? avgRate : 0.0);
        Integer total = latest != null ? latest.getTotalSpots() : 100;
        Integer avail = latest != null ? latest.getAvailableSpots() : 50;
        Integer occupied = total - avail;

        return OccupancyRateResponse.builder()
                .lotId(lotId)
                .currentOccupancyRate(currentRate)
                .averageOccupancyRate(avgRate != null ? Math.round(avgRate * 10.0) / 10.0 : currentRate)
                .totalSpots(total)
                .occupiedSpots(occupied)
                .availableSpots(avail)
                .period(period != null ? period.toUpperCase() : "TODAY")
                .build();
    }

    @Override
    public List<HourlyOccupancyResponse> getOccupancyByHour(UUID lotId, LocalDate date) {
        LocalDate queryDate = date != null ? date : LocalDate.now();
        LocalDateTime start = queryDate.atStartOfDay();
        LocalDateTime end = queryDate.atTime(LocalTime.MAX);

        List<OccupancyLog> logs = analyticsRepository.findByLotIdAndTimestampBetweenOrderByTimestampAsc(lotId, start, end);
        Map<Integer, List<OccupancyLog>> byHour = logs.stream()
                .collect(Collectors.groupingBy(l -> l.getTimestamp().getHour()));

        List<HourlyOccupancyResponse> responses = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            List<OccupancyLog> hourLogs = byHour.get(h);
            if (hourLogs != null && !hourLogs.isEmpty()) {
                double avgHourRate = hourLogs.stream()
                        .mapToDouble(OccupancyLog::getOccupancyRate)
                        .average()
                        .orElse(0.0);


                int lastOccupied = hourLogs.get(hourLogs.size() - 1).getTotalSpots() - hourLogs.get(hourLogs.size() - 1).getAvailableSpots();

                responses.add(HourlyOccupancyResponse.builder()
                        .hour(h)
                        .occupancyRate(Math.round(avgHourRate * 10.0) / 10.0)
                        .occupiedSpots(lastOccupied)
                        .timestamp(hourLogs.get(0).getTimestamp())
                        .build());
            } else {
                responses.add(HourlyOccupancyResponse.builder()
                        .hour(h)
                        .occupancyRate(0.0)
                        .occupiedSpots(0)
                        .timestamp(queryDate.atTime(h, 0))
                        .build());
            }
        }
        return responses;
    }

    @Override
    public PeakHoursResponse getPeakHours(UUID lotId, LocalDate date) {
        List<HourlyOccupancyResponse> hourly = getOccupancyByHour(lotId, date);
        HourlyOccupancyResponse peak = hourly.stream()
                .max(Comparator.comparing(HourlyOccupancyResponse::getOccupancyRate))
                .orElse(null);

        HourlyOccupancyResponse low = hourly.stream()
                .min(Comparator.comparing(HourlyOccupancyResponse::getOccupancyRate))
                .orElse(null);

        double avg = hourly.stream()
                .mapToDouble(HourlyOccupancyResponse::getOccupancyRate)
                .average()
                .orElse(0.0);

        return PeakHoursResponse.builder()
                .lotId(lotId)
                .date(date != null ? date : LocalDate.now())
                .peakHour(peak != null ? peak.getHour() : 18)
                .peakOccupancyRate(peak != null ? peak.getOccupancyRate() : 0.0)
                .lowHour(low != null ? low.getHour() : 4)
                .lowOccupancyRate(low != null ? low.getOccupancyRate() : 0.0)
                .averageOccupancyRate(Math.round(avg * 10.0) / 10.0)
                .build();
    }

    @Override
    public RevenueResponse getRevenueByLot(UUID lotId, LocalDate from, LocalDate to) {
        LocalDate start = from != null ? from : LocalDate.now().minusDays(30);
        LocalDate end = to != null ? to : LocalDate.now();

        BigDecimal revenue = BigDecimal.ZERO;
        Long bookings = 0L;
        try {
            // Attempt live sync with payment-service
            String url = "http://payment-service/api/v1/payments/lot/" + lotId + "/revenue";
            ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                    url, HttpMethod.GET, null, new ParameterizedTypeReference<Map<String, Object>>() {});
            if (resp.getBody() != null && resp.getBody().get("totalRevenue") != null) {
                revenue = new BigDecimal(resp.getBody().get("totalRevenue").toString());
                bookings = resp.getBody().get("totalBookings") != null ? Long.valueOf(resp.getBody().get("totalBookings").toString()) : 0L;
            }
        } catch (Exception e) {
            log.warn("payment-service unreachable for lot revenue, using fallback: {}", e.getMessage());
            revenue = BigDecimal.ZERO;
            bookings = 0L;
        }

        return RevenueResponse.builder()
                .lotId(lotId)
                .totalRevenue(revenue)
                .totalBookings(bookings)
                .fromDate(start)
                .toDate(end)
                .build();
    }

    @Override
    public List<DailyRevenueResponse> getRevenueByDay(UUID lotId, int days) {
        int count = days > 0 ? days : 7;
        List<DailyRevenueResponse> list = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = count - 1; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            RevenueResponse dailyRev = getRevenueByLot(lotId, d, d);
            list.add(DailyRevenueResponse.builder()
                    .date(d)
                    .revenue(dailyRev.getTotalRevenue())
                    .bookingCount(dailyRev.getTotalBookings())
                    .build());
        }
        return list;
    }

    @Override
    public List<SpotTypeUtilResponse> getMostUsedSpotTypes(UUID lotId) {
        List<SpotTypeUtilResponse> responses = new ArrayList<>();

        try {
            HttpEntity<Void> requestEntity = new HttpEntity<>(createAuthHeaders());
            String url = "http://spot-service/api/v1/spots/lot/" + lotId;

            ResponseEntity<List<Map<String, Object>>> resp = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );

            List<Map<String, Object>> spots = resp.getBody();
            if (spots != null && !spots.isEmpty()) {
                Map<String, List<Map<String, Object>>> byType = spots.stream()
                        .filter(s -> s.get("spotType") != null)
                        .collect(Collectors.groupingBy(s -> s.get("spotType").toString()));

                for (Map.Entry<String, List<Map<String, Object>>> entry : byType.entrySet()) {
                    String type = entry.getKey();
                    List<Map<String, Object>> typeSpots = entry.getValue();

                    int total = typeSpots.size();
                    int occupied = (int) typeSpots.stream()
                            .filter(s -> {
                                String status = s.get("status") != null ? s.get("status").toString() : "";
                                return "OCCUPIED".equalsIgnoreCase(status) || "RESERVED".equalsIgnoreCase(status);
                            })
                            .count();

                    double rate = total > 0 ? ((double) occupied / total) * 100.0 : 0.0;

                    responses.add(SpotTypeUtilResponse.builder()
                            .spotType(type)
                            .totalSpots(total)
                            .occupiedSpots(occupied)
                            .utilizationRate(Math.round(rate * 10.0) / 10.0)
                            .build());
                }
            }
        } catch (Exception e) {
            log.warn("spot-service unreachable for spot type utilization on lot {}: {}", lotId, e.getMessage());
        }

        return responses;
    }

    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            String token = attributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
            if (token != null) {
                headers.set(HttpHeaders.AUTHORIZATION, token);
            }
        }
        return headers;
    }

    @Override
    public AvgDurationResponse getAvgDuration(UUID lotId) {
        double avgMinutes = 0.0;
        long completedCount = 0L;

        try {
            HttpEntity<Void> requestEntity = new HttpEntity<>(createAuthHeaders());
            String url = "http://booking-service/api/v1/bookings/lot/" + lotId;

            ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (resp.getBody() != null && resp.getBody().get("data") instanceof List<?> list) {
                List<Long> durations = new ArrayList<>();

                for (Object obj : list) {
                    if (obj instanceof Map<?, ?> bookingMap) {
                        String status = Objects.toString(bookingMap.get("status"), "");
                        Object checkIn = bookingMap.get("checkInTime");
                        Object checkOut = bookingMap.get("checkOutTime");

                        if ("COMPLETED".equalsIgnoreCase(status) && checkIn != null && checkOut != null) {
                            LocalDateTime in = LocalDateTime.parse(checkIn.toString());
                            LocalDateTime out = LocalDateTime.parse(checkOut.toString());
                            long minutes = Duration.between(in, out).toMinutes();
                            if (minutes > 0) {
                                durations.add(minutes);
                            }
                        }
                    }
                }

                if (!durations.isEmpty()) {
                    completedCount = durations.size();
                    avgMinutes = durations.stream().mapToLong(Long::longValue).average().orElse(0.0);
                    avgMinutes = Math.round(avgMinutes * 10.0) / 10.0;
                }
            }
        } catch (Exception e) {
            log.warn("booking-service unreachable for avg duration on lot {}: {}", lotId, e.getMessage());
        }

        return AvgDurationResponse.builder()
                .lotId(lotId)
                .averageDurationMinutes(avgMinutes)
                .totalCompletedBookings(completedCount)
                .build();
    }

    @Override
    public PlatformSummaryResponse getPlatformSummary() {
        long totalLots = 0L;
        long totalSpots = 0L;
        long activeBookings = 0L;
        BigDecimal totalRevenue = BigDecimal.ZERO;

        // 1. Live Occupancy from database
        Double avgOccupancy = analyticsRepository.findPlatformAvgOccupancy(LocalDate.now().atStartOfDay());
        double avgPlatformOccupancy = avgOccupancy != null ? Math.round(avgOccupancy * 10.0) / 10.0 : 0.0;

        // 2. Query parkinglot-service for real lots and spot counts
        try {
            HttpEntity<Void> requestEntity = new HttpEntity<>(createAuthHeaders());
            String url = "http://parkinglot-service/api/v1/lots/admin/all";
            ResponseEntity<List<Map<String, Object>>> resp = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );

            List<Map<String, Object>> lots = resp.getBody();
            if (lots != null) {
                totalLots = lots.size();
                totalSpots = lots.stream()
                        .mapToLong(l -> l.get("totalSpots") instanceof Number n ? n.longValue() : 0L)
                        .sum();
            }
        } catch (Exception e) {
            log.warn("parkinglot-service unreachable for platform summary: {}", e.getMessage());
        }

        return PlatformSummaryResponse.builder()
                .totalLots(totalLots)
                .totalSpots(totalSpots)
                .activeBookings(activeBookings)
                .averagePlatformOccupancy(avgPlatformOccupancy)
                .totalPlatformRevenue(totalRevenue)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    @Override
    public DailyReportResponse generateDailyReport(UUID lotId, LocalDate date) {
        LocalDate queryDate = date != null ? date : LocalDate.now();
        PeakHoursResponse peak = getPeakHours(lotId, queryDate);
        RevenueResponse revenue = getRevenueByLot(lotId, queryDate, queryDate);

        return DailyReportResponse.builder()
                .lotId(lotId)
                .date(queryDate)
                .totalBookings(revenue.getTotalBookings())
                .totalRevenue(revenue.getTotalRevenue())
                .peakHour(peak.getPeakHour())
                .peakOccupancyRate(peak.getPeakOccupancyRate())
                .averageOccupancyRate(peak.getAverageOccupancyRate())
                .build();
    }

    private LocalDateTime resolveSinceDate(String period) {
        if (period == null) return LocalDate.now().atStartOfDay();
        return switch (period.toUpperCase()) {
            case "WEEK" -> LocalDateTime.now().minusWeeks(1);
            case "MONTH" -> LocalDateTime.now().minusMonths(1);
            default -> LocalDate.now().atStartOfDay();
        };
    }
}
