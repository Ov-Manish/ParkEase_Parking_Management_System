package com.parkease.spot_service.service.impl;

import com.parkease.spot_service.client.ParkingLotClient;
import com.parkease.spot_service.dto.request.BulkSpotRequest;
import com.parkease.spot_service.dto.request.SpotRequest;
import com.parkease.spot_service.dto.response.SpotCountResponse;
import com.parkease.spot_service.dto.response.SpotResponse;
import com.parkease.spot_service.entity.ParkingSpot;
import com.parkease.spot_service.enums.SpotStatus;
import com.parkease.spot_service.enums.SpotType;
import com.parkease.spot_service.enums.VehicleType;
import com.parkease.spot_service.exception.BadRequestException;
import com.parkease.spot_service.exception.ResourceNotFoundException;
import com.parkease.spot_service.repository.ParkingSpotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParkingSpotServiceImplTest {

    @Mock
    private ParkingSpotRepository spotRepository;

    @Mock
    private ParkingLotClient parkingLotClient;

    @InjectMocks
    private ParkingSpotServiceImpl spotService;

    private UUID lotId;
    private UUID spotId;
    private ParkingSpot mockSpot;

    @BeforeEach
    void setUp() {
        lotId = UUID.randomUUID();
        spotId = UUID.randomUUID();

        mockSpot = ParkingSpot.builder()
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
    @DisplayName("Create spot: Success & syncs capacity")
    void testCreateSpot_Success() {
        SpotRequest request = SpotRequest.builder()
                .lotId(lotId)
                .spotNumber("A-1")
                .floor(1)
                .spotType(SpotType.COMPACT)
                .vehicleType(VehicleType.FOUR_WHEELER)
                .pricePerHour(new BigDecimal("50.00"))
                .build();

        when(spotRepository.existsByLotIdAndSpotNumberIgnoreCaseAndIsActiveTrue(lotId, "A-1")).thenReturn(false);
        when(spotRepository.save(any(ParkingSpot.class))).thenReturn(mockSpot);
        when(spotRepository.countByLotIdAndIsActiveTrue(lotId)).thenReturn(1L);

        SpotResponse response = spotService.createSpot(request);

        assertThat(response).isNotNull();
        assertThat(response.getSpotNumber()).isEqualTo("A-1");
        verify(parkingLotClient, times(1)).syncTotalSpots(lotId, 1L);
    }

    @Test
    @DisplayName("Create spot: Throws BadRequest if vehicle compatibility fails")
    void testCreateSpot_IncompatibleVehicle() {
        SpotRequest request = SpotRequest.builder()
                .lotId(lotId)
                .spotNumber("M-1")
                .floor(1)
                .spotType(SpotType.COMPACT) // Invalid! Two-wheelers require MOTORBIKE
                .vehicleType(VehicleType.TWO_WHEELER)
                .pricePerHour(new BigDecimal("20.00"))
                .build();

        assertThatThrownBy(() -> spotService.createSpot(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Two-wheelers can only be assigned to MOTORBIKE spots");
    }

    @Test
    @DisplayName("Create spot: Throws BadRequest if spot number already exists")
    void testCreateSpot_DuplicateNumber() {
        SpotRequest request = SpotRequest.builder()
                .lotId(lotId)
                .spotNumber("A-1")
                .floor(1)
                .spotType(SpotType.COMPACT)
                .vehicleType(VehicleType.FOUR_WHEELER)
                .pricePerHour(new BigDecimal("50.00"))
                .build();

        when(spotRepository.existsByLotIdAndSpotNumberIgnoreCaseAndIsActiveTrue(lotId, "A-1")).thenReturn(true);

        assertThatThrownBy(() -> spotService.createSpot(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("Bulk Create: Validates fromNumber <= toNumber")
    void testBulkCreate_InvalidRange() {
        BulkSpotRequest request = BulkSpotRequest.builder()
                .lotId(lotId)
                .fromNumber(10)
                .toNumber(5) // from > to
                .spotType(SpotType.COMPACT)
                .vehicleType(VehicleType.FOUR_WHEELER)
                .pricePerHour(new BigDecimal("50.00"))
                .build();

        assertThatThrownBy(() -> spotService.createBulkSpots(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("fromNumber cannot be greater than toNumber");
    }

    @Test
    @DisplayName("Bulk Create: Caps batch at 200")
    void testBulkCreate_ExceedLimit() {
        BulkSpotRequest request = BulkSpotRequest.builder()
                .lotId(lotId)
                .fromNumber(1)
                .toNumber(201) // 201 spots
                .spotType(SpotType.COMPACT)
                .vehicleType(VehicleType.FOUR_WHEELER)
                .pricePerHour(new BigDecimal("50.00"))
                .build();

        assertThatThrownBy(() -> spotService.createBulkSpots(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Maximum 200 spots");
    }

    @Test
    @DisplayName("Reserve Spot: Successfully transitions AVAILABLE to RESERVED")
    void testReserveSpot_Success() {
        when(spotRepository.findById(spotId)).thenReturn(Optional.of(mockSpot));
        when(spotRepository.save(any(ParkingSpot.class))).thenReturn(mockSpot);

        SpotResponse response = spotService.reserveSpot(spotId);

        assertThat(response.getStatus()).isEqualTo(SpotStatus.RESERVED);
    }

    @Test
    @DisplayName("Reserve Spot: Throws BadRequest if spot is not AVAILABLE")
    void testReserveSpot_AlreadyOccupied() {
        mockSpot.setStatus(SpotStatus.OCCUPIED);
        when(spotRepository.findById(spotId)).thenReturn(Optional.of(mockSpot));

        assertThatThrownBy(() -> spotService.reserveSpot(spotId))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("is no longer available");
    }

    @Test
    @DisplayName("Occupy Spot: Successfully transitions to OCCUPIED")
    void testOccupySpot_Success() {
        mockSpot.setStatus(SpotStatus.RESERVED);
        when(spotRepository.findById(spotId)).thenReturn(Optional.of(mockSpot));
        when(spotRepository.save(any(ParkingSpot.class))).thenReturn(mockSpot);

        SpotResponse response = spotService.occupySpot(spotId);

        assertThat(response.getStatus()).isEqualTo(SpotStatus.OCCUPIED);
    }

    @Test
    @DisplayName("Release Spot: Successfully resets to AVAILABLE")
    void testReleaseSpot_Success() {
        mockSpot.setStatus(SpotStatus.OCCUPIED);
        when(spotRepository.findById(spotId)).thenReturn(Optional.of(mockSpot));
        when(spotRepository.save(any(ParkingSpot.class))).thenReturn(mockSpot);

        SpotResponse response = spotService.releaseSpot(spotId);

        assertThat(response.getStatus()).isEqualTo(SpotStatus.AVAILABLE);
    }

    @Test
    @DisplayName("Occupancy Counts: Correctly calculates occupancy rate")
    void testGetSpotCountsByLot() {
        when(spotRepository.countByLotIdAndIsActiveTrue(lotId)).thenReturn(100L);
        when(spotRepository.countByLotIdAndStatusAndIsActiveTrue(lotId, SpotStatus.AVAILABLE)).thenReturn(60L);
        when(spotRepository.countByLotIdAndStatusAndIsActiveTrue(lotId, SpotStatus.RESERVED)).thenReturn(15L);
        when(spotRepository.countByLotIdAndStatusAndIsActiveTrue(lotId, SpotStatus.OCCUPIED)).thenReturn(25L);
        when(spotRepository.countByLotIdAndStatusAndIsActiveTrue(lotId, SpotStatus.OUT_OF_SERVICE)).thenReturn(0L);

        SpotCountResponse response = spotService.getSpotCountsByLot(lotId);

        assertThat(response.getTotalSpots()).isEqualTo(100L);
        assertThat(response.getAvailableSpots()).isEqualTo(60L);
        assertThat(response.getReservedSpots()).isEqualTo(15L);
        assertThat(response.getOccupiedSpots()).isEqualTo(25L);
        // Occupancy = (15 + 25) / 100 = 40.0%
        assertThat(response.getOccupancyRatePercentage()).isEqualTo(40.0);
    }
}
