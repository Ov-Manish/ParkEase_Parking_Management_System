package com.parkease.vehicle_service.testing;


import com.parkease.vehicle_service.dto.request.VehicleRequest;
import com.parkease.vehicle_service.dto.response.VehicleResponse;
import com.parkease.vehicle_service.entity.Vehicle;
import com.parkease.vehicle_service.enums.VehicleType;
import com.parkease.vehicle_service.repository.VehicleRepository;
import com.parkease.vehicle_service.service.impl.VehicleServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)  // it activated mockito
public class VehicleServiceTest {

    @Mock
    private VehicleRepository vehicleRepository; // fake databse repository

    @InjectMocks
    private VehicleServiceImpl vehicleService;  // Real service being tested (Injects the Fake Repo)


    @Test
    @DisplayName("Should register vehicle and normalize license plate to uppercase")
    void registerVehicle_Sucess(){

        // --- ARRANGE (Given) ---
        UUID ownerId = UUID.randomUUID();
        VehicleRequest request = VehicleRequest.builder()
                .licensePlate("mh 12 ab 1234") // lowercase with spaces
                .make("Honda")
                .model("City")
                .color("White")
                .vehicleType(VehicleType.FOUR_WHEELER)
                .isEV(false)
                .build();


        // Tell Mockito: Yes, this plate already exists!
        when(vehicleRepository.existsByOwnerIdAndLicensePlateIgnoreCaseAndIsActiveTrue(ownerId, "MH12AB1234"))
                .thenReturn(false);

        // --- ACT & ASSERT ---

        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> {
            Vehicle v= invocation.getArgument(0);
            v.setVehicleId(UUID.randomUUID());
            return v;
        });


        VehicleResponse response = vehicleService.registerVehicle(ownerId, request);

        // --- ASSERT (Then) ---
        assertThat(response).isNotNull();
        assertThat(response.getVehicleId()).isNotNull();
        assertThat(response.getLicensePlate()).isEqualTo("MH12AB1234"); // Plate must be cleaned!
        assertThat(response.getMake()).isEqualTo("Honda");
        assertThat(response.getVehicleType()).isEqualTo(VehicleType.FOUR_WHEELER);
        assertThat(response.getIsEV()).isFalse();
        // Verify repository save was called exactly 1 time
        verify(vehicleRepository, times(1)).save(any(Vehicle.class));

    }


}
