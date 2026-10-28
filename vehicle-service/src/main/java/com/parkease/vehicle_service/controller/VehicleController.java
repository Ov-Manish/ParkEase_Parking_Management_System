package com.parkease.vehicle_service.controller;

import com.parkease.vehicle_service.dto.request.VehicleRequest;
import com.parkease.vehicle_service.dto.request.VehicleUpdateRequest;
import com.parkease.vehicle_service.dto.response.ApiResponse;
import com.parkease.vehicle_service.dto.response.VehicleResponse;
import com.parkease.vehicle_service.enums.VehicleType;
import com.parkease.vehicle_service.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/vehicles")
@RequiredArgsConstructor
@Tag(name = "Vehicle Management", description = "Endpoints for driver vehicle registration, fleet lookup, and vehicle type/EV verification")
public class VehicleController {

    private final VehicleService vehicleService;

    // 1. Register a new vehicle for the authenticated driver
    @PostMapping
    @Operation(summary = "Register a new vehicle", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<VehicleResponse> registerVehicle(
            Authentication authentication,
            @Valid @RequestBody VehicleRequest request) {
        UUID currentUserId = (UUID) authentication.getPrincipal();
        VehicleResponse response = vehicleService.registerVehicle(currentUserId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // 2. Get all vehicles belonging to the currently logged-in driver
    @GetMapping("/my-vehicles")
    @Operation(summary = "Get all active vehicles of the current driver", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<List<VehicleResponse>> getMyVehicles(Authentication authentication) {
        UUID currentUserId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(vehicleService.getVehiclesByOwner(currentUserId));
    }

    // 3. Get vehicle details by Vehicle ID
    @GetMapping("/{vehicleId}")
    @Operation(summary = "Get vehicle details by ID", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<VehicleResponse> getVehicleById(@PathVariable UUID vehicleId) {
        return ResponseEntity.ok(vehicleService.getVehicleById(vehicleId));
    }

    // 4. Get all vehicles of a specific owner (Owner or Admin)
    @GetMapping("/owner/{ownerId}")
    @Operation(summary = "Get all vehicles of a specific owner", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<List<VehicleResponse>> getVehiclesByOwner(@PathVariable UUID ownerId) {
        return ResponseEntity.ok(vehicleService.getVehiclesByOwner(ownerId));
    }

    // 5. Lookup vehicle by License Plate
    @GetMapping("/plate/{licensePlate}")
    @Operation(summary = "Lookup vehicle by license plate", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<VehicleResponse> getByLicensePlate(@PathVariable String licensePlate) {
        return ResponseEntity.ok(vehicleService.getByLicensePlate(licensePlate));
    }

    // 6. Update vehicle details
    @PutMapping("/{vehicleId}")
    @Operation(summary = "Update vehicle details", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<VehicleResponse> updateVehicle(
            Authentication authentication,
            @PathVariable UUID vehicleId,
            @Valid @RequestBody VehicleUpdateRequest request) {
        UUID currentUserId = (UUID) authentication.getPrincipal();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        VehicleResponse response = vehicleService.updateVehicle(vehicleId, currentUserId, isAdmin, request);
        return ResponseEntity.ok(response);
    }

    // 7. Soft delete vehicle
    @DeleteMapping("/{vehicleId}")
    @Operation(summary = "Delete vehicle from driver account", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse> deleteVehicle(
            Authentication authentication,
            @PathVariable UUID vehicleId) {
        UUID currentUserId = (UUID) authentication.getPrincipal();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        vehicleService.deleteVehicle(vehicleId, currentUserId, isAdmin);
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("Vehicle removed successfully")
                .build());
    }

    // 8. Fast Inter-Service Check: Get Vehicle Type (Used by Booking-Service)
    @GetMapping("/{vehicleId}/type")
    @Operation(summary = "Get vehicle type (2W, 4W, HEAVY) for spot allocation")
    public ResponseEntity<VehicleType> getVehicleType(@PathVariable UUID vehicleId) {
        return ResponseEntity.ok(vehicleService.getVehicleType(vehicleId));
    }

    // 9. Fast Inter-Service Check: Check if Vehicle is EV (Used by Booking-Service)
    @GetMapping("/{vehicleId}/is-ev")
    @Operation(summary = "Check if vehicle is an Electric Vehicle (EV)")
    public ResponseEntity<Boolean> isEVVehicle(@PathVariable UUID vehicleId) {
        return ResponseEntity.ok(vehicleService.isEVVehicle(vehicleId));
    }

    // 10. Admin: Get all registered vehicles platform-wide
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: Get all platform vehicles", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<List<VehicleResponse>> getAllVehicles() {
        return ResponseEntity.ok(vehicleService.getAllVehicles());
    }
}