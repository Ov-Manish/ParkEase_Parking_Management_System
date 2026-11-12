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

import java.math.BigDecimal;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ParkingLotClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.services.parkinglot.url:http://localhost:8082}")
    private String parkingLotServiceUrl;

    /**
     * Verifies that the lot exists and is active, and retrieves its hourly rate.
     */
    public BigDecimal getHourlyRate(UUID lotId, String jwtToken) {
        String url = parkingLotServiceUrl + "/api/v1/lots/" + lotId;
        HttpHeaders headers = new HttpHeaders();
        if (jwtToken != null) {
            headers.setBearerAuth(jwtToken.replace("Bearer ", ""));
        }
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode data = root.has("data") ? root.get("data") : root;

            boolean isActive = data.path("isActive").asBoolean(true);
            if (!isActive) {
                throw new BadRequestException("Parking lot is currently inactive");
            }

            // Extract hourly rate snapshot
            double rate = data.path("pricePerHour").asDouble(data.path("hourlyRate").asDouble(20.0));
            return BigDecimal.valueOf(rate);

        } catch (HttpStatusCodeException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new ResourceNotFoundException("Parking lot not found with ID: " + lotId);
            }
            throw new BadRequestException("Failed to verify parking lot: " + ex.getResponseBodyAsString());
        } catch (ResourceNotFoundException | BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Error communicating with parkinglot-service: {}", ex.getMessage());
            // Fallback default price if service is temporarily unreachable during local dev
            return BigDecimal.valueOf(20.0);
        }
    }
}
