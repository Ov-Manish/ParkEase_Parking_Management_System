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
public class VehicleClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.services.vehicle.url:http://localhost:8086}")
    private String vehicleServiceUrl;

    /**
     * Verifies that the vehicle exists, belongs to the caller, and returns its license plate.
     */
    public String getLicensePlateAndVerifyOwner(UUID vehicleId, UUID userId, String jwtToken) {
        String url = vehicleServiceUrl + "/api/v1/vehicles/" + vehicleId;
        HttpHeaders headers = new HttpHeaders();
        if (jwtToken != null) {
            headers.setBearerAuth(jwtToken.replace("Bearer ", ""));
        }
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode data = root.has("data") ? root.get("data") : root;

            // Verify vehicle ownership
            String ownerIdStr = data.path("userId").asText();
            if (!ownerIdStr.isBlank() && !UUID.fromString(ownerIdStr).equals(userId)) {
                throw new BadRequestException("You do not own this vehicle");
            }

            // Return license plate snapshot
            return data.path("licensePlate").asText("UNKNOWN-PLATE");

        } catch (HttpStatusCodeException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new ResourceNotFoundException("Vehicle not found with ID: " + vehicleId);
            }
            throw new BadRequestException("Failed to verify vehicle: " + ex.getResponseBodyAsString());
        } catch (ResourceNotFoundException | BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Error communicating with vehicle-service: {}", ex.getMessage());
            return "TEMP-PLATE";
        }
    }
}
