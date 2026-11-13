package com.parkease.payment_service.client;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkease.payment_service.exception.BadRequestException;
import com.parkease.payment_service.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.services.booking.url:http://localhost:8084}")
    private String bookingServiceUrl;

//    Verifies that the booking exists , belongs to the authenticated user, and returns its billed total fare
    public BigDecimal verifyBookingAndGetAmount(UUID bookingId , UUID authenticatedUserId,String jwtToken){
        String url = bookingServiceUrl + "/api/v1/bookings/" + bookingId;
        HttpHeaders headers = new HttpHeaders();
        if (jwtToken != null){
            headers.setBearerAuth(jwtToken.replace("Bearer ",""));
        }

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            log.info("Verifying booking {} with booking-service at {}", bookingId, url);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode data = root.has("data") ? root.get("data") : root;

//          Verify Ownership
            String bookingUserIdStr  = data.path("userId").asText();

            if (!bookingUserIdStr.isBlank()){
                UUID bookingUserId = UUID.fromString(bookingUserIdStr);
                if (!bookingUserId.equals(authenticatedUserId)){
                    throw new BadRequestException("Access denied. You do not own this booking");
                }
            }
//          Verify Status (Cannot Pay for Cancelled Booking )
            String status = data.path("status").asText();
            if ("CANCELLED".equalsIgnoreCase(status)){
                throw new BadRequestException("Cannot process payment for a cancelled booking");
            }

//            Extracting the total amount / Base Fare
            double totalAmount = data.path("totalAmount").asDouble(data.path("baseFare").asDouble(0.0));
            return BigDecimal.valueOf(totalAmount);

        } catch (HttpStatusCodeException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND){
                throw new ResourceNotFoundException("Booking not found with ID: " + bookingId);
            }

            log.error("Failed to verify booking with booking-service: {}", ex.getResponseBodyAsString());
            throw new BadRequestException("Failed to verify booking: " + ex.getResponseBodyAsString());
        }catch (ResourceNotFoundException | BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Error communicating with booking-service: {}", ex.getMessage());
            throw new BadRequestException("Could not connect to booking-service: " + ex.getMessage());
        }
    }
}
