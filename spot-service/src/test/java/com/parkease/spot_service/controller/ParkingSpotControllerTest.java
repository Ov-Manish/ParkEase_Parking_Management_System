package com.parkease.spot_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkease.spot_service.dto.request.BulkSpotRequest;
import com.parkease.spot_service.dto.request.SpotRequest;
import com.parkease.spot_service.dto.request.SpotUpdateRequest;
import com.parkease.spot_service.dto.response.SpotCountResponse;
import com.parkease.spot_service.dto.response.SpotResponse;
import com.parkease.spot_service.enums.SpotStatus;
import com.parkease.spot_service.enums.SpotType;
import com.parkease.spot_service.enums.VehicleType;
import com.parkease.spot_service.exception.GlobalExceptionHandler;
import com.parkease.spot_service.service.ParkingSpotService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ParkingSpotControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private ParkingSpotService spotService;

    @InjectMocks
    private ParkingSpotController parkingSpotController;

    private UUID lotId;
    private UUID spotId;
    private SpotResponse sampleResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(parkingSpotController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();

        lotId = UUID.randomUUID();
        spotId = UUID.randomUUID();

        sampleResponse = SpotResponse.builder()
                .spotId(spotId)
                .lotId(lotId)
                .spotNumber("A-1")
                .floor(1)
                .spotType(SpotType.COMPACT)
                .vehicleType(VehicleType.FOUR_WHEELER)
                .status(SpotStatus.AVAILABLE)
                .isEVCharging(false)
                .isHandicapped(false)
                .pricePerHour(new BigDecimal("50.00"))
                .isActive(true)
                .version(0L)
                .build();
    }

    @Test
    @DisplayName("1. POST /api/v1/spots - Manager create spot")
    void testCreateSpot_Success() throws Exception {
        SpotRequest request = SpotRequest.builder()
                .lotId(lotId)
                .spotNumber("A-1")
                .floor(1)
                .spotType(SpotType.COMPACT)
                .vehicleType(VehicleType.FOUR_WHEELER)
                .pricePerHour(new BigDecimal("50.00"))
                .build();

        when(spotService.createSpot(any())).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/v1/spots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.spotNumber").value("A-1"))
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
    }

    @Test
    @DisplayName("2. POST /api/v1/spots/bulk - Bulk create spots")
    void testCreateBulkSpots_Success() throws Exception {
        BulkSpotRequest request = BulkSpotRequest.builder()
                .lotId(lotId)
                .floor(1)
                .prefix("A-")
                .fromNumber(1)
                .toNumber(5)
                .spotType(SpotType.COMPACT)
                .vehicleType(VehicleType.FOUR_WHEELER)
                .pricePerHour(new BigDecimal("50.00"))
                .build();

        when(spotService.createBulkSpots(any())).thenReturn(List.of(sampleResponse));

        mockMvc.perform(post("/api/v1/spots/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].spotNumber").value("A-1"));
    }

    @Test
    @DisplayName("3. GET /api/v1/spots/lot/{lotId} - Manager view spots")
    void testGetSpotsByLot_Success() throws Exception {
        when(spotService.getSpotsByLot(lotId)).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/v1/spots/lot/{lotId}", lotId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].spotNumber").value("A-1"));
    }

    @Test
    @DisplayName("4. PUT /api/v1/spots/{spotId} - Manager update spot")
    void testUpdateSpot_Success() throws Exception {
        SpotUpdateRequest request = SpotUpdateRequest.builder()
                .spotNumber("A-1-UPDATED")
                .floor(2)
                .spotType(SpotType.LARGE)
                .vehicleType(VehicleType.FOUR_WHEELER)
                .isEVCharging(true)
                .isHandicapped(false)
                .pricePerHour(new BigDecimal("75.00"))
                .build();

        when(spotService.updateSpot(eq(spotId), any())).thenReturn(sampleResponse);

        mockMvc.perform(put("/api/v1/spots/{spotId}", spotId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("5. PATCH /api/v1/spots/{spotId}/status - Manager update status")
    void testUpdateSpotStatus_Success() throws Exception {
        when(spotService.updateSpotStatus(eq(spotId), eq(SpotStatus.OUT_OF_SERVICE))).thenReturn(sampleResponse);

        mockMvc.perform(patch("/api/v1/spots/{spotId}/status", spotId)
                        .param("status", "OUT_OF_SERVICE"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("6. DELETE /api/v1/spots/{spotId} - Manager delete spot")
    void testDeleteSpot_Success() throws Exception {
        doNothing().when(spotService).deleteSpot(spotId);

        mockMvc.perform(delete("/api/v1/spots/{spotId}", spotId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("7. GET /api/v1/spots/{spotId} - Public get spot by ID")
    void testGetSpotById_Public_Success() throws Exception {
        when(spotService.getSpotById(spotId)).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/v1/spots/{spotId}", spotId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spotNumber").value("A-1"));
    }

    @Test
    @DisplayName("8. GET /api/v1/spots/lot/{lotId}/available - Public available spots")
    void testGetAvailableSpots_Public_Success() throws Exception {
        when(spotService.getAvailableSpots(eq(lotId), any(), any(), any())).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/v1/spots/lot/{lotId}/available", lotId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"));
    }

    @Test
    @DisplayName("9. GET /api/v1/spots/lot/{lotId}/count - Public occupancy counts")
    void testGetSpotCountsByLot_Public_Success() throws Exception {
        SpotCountResponse countResponse = SpotCountResponse.builder()
                .lotId(lotId)
                .totalSpots(50)
                .availableSpots(40)
                .reservedSpots(5)
                .occupiedSpots(5)
                .outOfServiceSpots(0)
                .occupancyRatePercentage(20.0)
                .build();

        when(spotService.getSpotCountsByLot(lotId)).thenReturn(countResponse);

        mockMvc.perform(get("/api/v1/spots/lot/{lotId}/count", lotId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSpots").value(50))
                .andExpect(jsonPath("$.occupancyRatePercentage").value(20.0));
    }

    @Test
    @DisplayName("10. POST /api/v1/spots/{spotId}/reserve - Inter-service reserve")
    void testReserveSpot_Success() throws Exception {
        sampleResponse.setStatus(SpotStatus.RESERVED);
        when(spotService.reserveSpot(spotId)).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/v1/spots/{spotId}/reserve", spotId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESERVED"));
    }

    @Test
    @DisplayName("11. POST /api/v1/spots/{spotId}/occupy - Inter-service occupy")
    void testOccupySpot_Success() throws Exception {
        sampleResponse.setStatus(SpotStatus.OCCUPIED);
        when(spotService.occupySpot(spotId)).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/v1/spots/{spotId}/occupy", spotId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OCCUPIED"));
    }

    @Test
    @DisplayName("12. POST /api/v1/spots/{spotId}/release - Inter-service release")
    void testReleaseSpot_Success() throws Exception {
        sampleResponse.setStatus(SpotStatus.AVAILABLE);
        when(spotService.releaseSpot(spotId)).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/v1/spots/{spotId}/release", spotId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
    }
}
