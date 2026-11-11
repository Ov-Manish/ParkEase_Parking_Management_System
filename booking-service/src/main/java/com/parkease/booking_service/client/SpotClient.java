package com.parkease.booking_service.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkease.booking_service.exception.BadRequestException;
import com.parkease.booking_service.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class SpotClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.services.spot.url:http://localhost:8083}")
    private String spotServiceUrl;

    /**
     * Verifies that the spot belongs to the lot and is active.
     */
    public void verifySpotBelongsToLot(UUID spotId, UUID lotId, String jwtToken) {
        String url = spotServiceUrl + "/api/v1/spots/" + spotId;

        HttpHeaders headers = new HttpHeaders();
        if (jwtToken != null) {
            headers.setBearerAuth(jwtToken.replace("Bearer ", ""));
        }
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode data = root.has("data") ? root.get("data") : root;

            UUID spotLotId = UUID.fromString(data.path("lotId").asText());
            if (!spotLotId.equals(lotId)) {
                throw new BadRequestException("Spot " + spotId + " does not belong to Parking Lot " + lotId);
            }

            boolean isActive = data.path("isActive").asBoolean(true);
            if (!isActive) {
                throw new BadRequestException("Spot " + spotId + " is currently inactive or under maintenance");
            }
        } catch (HttpStatusCodeException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new ResourceNotFoundException("Spot not found with ID: " + spotId);
            }
            throw new BadRequestException("Failed to verify spot: " + ex.getResponseBodyAsString());
        } catch (ResourceNotFoundException | BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Spot verification skipped: {}", ex.getMessage());
        }
    }

    /**
     * Updates spot status (e.g. OCCUPIED upon check-in, AVAILABLE upon checkout).
     */
    public void updateSpotStatus(UUID spotId, String status, String jwtToken) {
        String url = spotServiceUrl + "/api/v1/spots/" + spotId + "/status?status=" + status;

        HttpHeaders headers = new HttpHeaders();
        if (jwtToken != null) {
            headers.setBearerAuth(jwtToken.replace("Bearer ", ""));
        }
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            restTemplate.exchange(url, HttpMethod.PATCH, entity, String.class);
            log.info("Spot {} status updated to {} in spot-service", spotId, status);
        } catch (Exception ex) {
            log.warn("Could not notify spot-service of status change: {}", ex.getMessage());
        }
    }
}
