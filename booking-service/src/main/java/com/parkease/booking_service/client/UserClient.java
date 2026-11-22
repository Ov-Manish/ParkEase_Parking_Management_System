package com.parkease.booking_service.client;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.util.Json;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class UserClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.services.auth.url:http://localhost:8081}")
    private String authServiceUrl;

    public String getUserEmail(UUID userId){
        String url = authServiceUrl + "/api/v1/auth/users/" + userId;

        try {
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode data = root.has("data") ? root.get("data") : root;
            return data.hasNonNull("email") ? data.path("email").asText() : null;

        } catch (Exception ex) {
            log.warn("Could not retrieve email for userId {}: {}", userId, ex.getMessage());
            return null;
        }
    }
}
