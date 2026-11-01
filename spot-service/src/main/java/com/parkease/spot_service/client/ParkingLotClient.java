package com.parkease.spot_service.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ParkingLotClient {

    private final RestTemplate restTemplate;

    @Value("${app.services.parkinglot:http://localhost:8082}")
    private String parkingLotServiceUrl;

    // Synchronizes the total active spots capacity in parkinglot-service
    public void syncTotalSpots(UUID lotId, long totalSpots) {
        String url = String.format("%s/api/v1/lots/%s/internal/total-spots?count=%d",
                parkingLotServiceUrl, lotId, totalSpots);
        try {
            restTemplate.exchange(url, HttpMethod.PATCH, new HttpEntity<>(new HttpHeaders()), Void.class);
            log.info("Synced capacity for lot {}: totalSpots={}", lotId, totalSpots);
        } catch (Exception ex) {
            log.warn("Could not sync total spots with parkinglot-service (is it running?): {}", ex.getMessage());
        }
    }
}
