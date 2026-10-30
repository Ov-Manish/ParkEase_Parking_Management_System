package com.parkease.parkinglot_service.controller;

import com.parkease.parkinglot_service.dto.request.LotRejectionRequest;
import com.parkease.parkinglot_service.dto.request.ParkingLotRequest;
import com.parkease.parkinglot_service.dto.request.ParkingLotUpdateRequest;
import com.parkease.parkinglot_service.dto.response.ApiResponse;
import com.parkease.parkinglot_service.dto.response.ParkingLotResponse;
import com.parkease.parkinglot_service.service.ParkingLotService;
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
@RequestMapping("/api/v1/lots")
@RequiredArgsConstructor
@Tag(name = "ParkingLot Management", description = "Endpoints for lot registration, approval workflow, GPS nearby search, and operational management")
public class ParkingLotController {

    private final ParkingLotService parkingLotService;

    // LOT MANAGER Endpoints

    // 1. Register a new parking lot (Starts in PENDING_APPROVAL)
    @PostMapping
    @PreAuthorize("hasAnyRole('LOT_MANAGER', 'ADMIN')")
    @Operation(summary = "Register a new parking lot (Manager / Admin)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ParkingLotResponse> registerLot(
            Authentication authentication,
            @Valid @RequestBody ParkingLotRequest request) {
        UUID currentUserId = (UUID) authentication.getPrincipal();
        ParkingLotResponse response = parkingLotService.registerLot(currentUserId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // 2. Get all lots owned by the authenticated manager
    @GetMapping("/my-lots")
    @PreAuthorize("hasAnyRole('LOT_MANAGER', 'ADMIN')")
    @Operation(summary = "Get all lots owned by the current manager", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<List<ParkingLotResponse>> getMyLots(Authentication authentication) {
        UUID currentUserId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(parkingLotService.getLotsByManager(currentUserId));
    }

    // 3. Update lot details (Owner or Admin)
    @PutMapping("/{lotId}")
    @PreAuthorize("hasAnyRole('LOT_MANAGER', 'ADMIN')")
    @Operation(summary = "Update parking lot details", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ParkingLotResponse> updateLot(
            Authentication authentication,
            @PathVariable UUID lotId,
            @Valid @RequestBody ParkingLotUpdateRequest request) {
        UUID currentUserId = (UUID) authentication.getPrincipal();
        boolean isAdmin = checkIsAdmin(authentication);
        return ResponseEntity.ok(parkingLotService.updateLot(lotId, currentUserId, isAdmin, request));
    }

    // 4. Toggle Lot Open/Closed in real-time
    @PatchMapping("/{lotId}/toggle-open")
    @PreAuthorize("hasAnyRole('LOT_MANAGER', 'ADMIN')")
    @Operation(summary = "Toggle parking lot Open/Closed state", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ParkingLotResponse> toggleLotOpenStatus(
            Authentication authentication,
            @PathVariable UUID lotId) {
        UUID currentUserId = (UUID) authentication.getPrincipal();
        boolean isAdmin = checkIsAdmin(authentication);
        return ResponseEntity.ok(parkingLotService.toggleLotOpenStatus(lotId, currentUserId, isAdmin));
    }

    // 5. Soft-Delete parking lot
    @DeleteMapping("/{lotId}")
    @PreAuthorize("hasAnyRole('LOT_MANAGER', 'ADMIN')")
    @Operation(summary = "Delete parking lot (Soft-delete)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse> deleteLot(
            Authentication authentication,
            @PathVariable UUID lotId) {
        UUID currentUserId = (UUID) authentication.getPrincipal();
        boolean isAdmin = checkIsAdmin(authentication);
        parkingLotService.deleteLot(lotId, currentUserId, isAdmin);
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("Parking lot removed successfully")
                .build());
    }

    // PUBLIC / DRIVER SEARCH ENDPOINTS

    // 6. Public Search by City
    @GetMapping("/search/city")
    @Operation(summary = "Search approved & open parking lots by city name")
    public ResponseEntity<List<ParkingLotResponse>> searchByCity(@RequestParam String city) {
        return ResponseEntity.ok(parkingLotService.searchByCity(city));
    }

    // 7. Public Geospatial Search: Find nearby lots by GPS coordinates
    @GetMapping("/search/nearby")
    @Operation(summary = "Find nearby approved parking lots using GPS coordinates (Haversine formula)")
    public ResponseEntity<List<ParkingLotResponse>> searchNearby(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "5.0") double radiusKm) {
        return ResponseEntity.ok(parkingLotService.searchNearby(lat, lng, radiusKm));
    }

    // 8. Public: Get parking lot details by ID
    @GetMapping("/{lotId}")
    @Operation(summary = "Get parking lot details by ID")
    public ResponseEntity<ParkingLotResponse> getLotById(@PathVariable UUID lotId) {
        return ResponseEntity.ok(parkingLotService.getLotById(lotId));
    }

    // PLATFORM ADMIN ENDPOINTS

    // 9. Admin: View all lots awaiting approval
    @GetMapping("/admin/pending")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: Get all parking lots awaiting approval", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<List<ParkingLotResponse>> getPendingLots() {
        return ResponseEntity.ok(parkingLotService.getPendingLots());
    }

    // 10. Admin: Approve lot
    @PutMapping("/admin/{lotId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: Approve a parking lot", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ParkingLotResponse> approveLot(@PathVariable UUID lotId) {
        return ResponseEntity.ok(parkingLotService.approveLot(lotId));
    }

    // 11. Admin: Reject lot with feedback
    @PutMapping("/admin/{lotId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: Reject a parking lot with feedback reason", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ParkingLotResponse> rejectLot(
            @PathVariable UUID lotId,
            @Valid @RequestBody LotRejectionRequest request) {
        return ResponseEntity.ok(parkingLotService.rejectLot(lotId, request));
    }

    // 12. Admin: Get all platform lots across all managers and states
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: Get all registered parking lots platform-wide", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<List<ParkingLotResponse>> getAllLots() {
        return ResponseEntity.ok(parkingLotService.getAllLots());
    }


    // INTER-SERVICE ENDPOINTS (Used by Booking-Service & Spot-Service)

    // 13. Fast verification check before creating a booking
    @GetMapping("/{lotId}/internal/verify")
    @Operation(summary = "Inter-Service: Verify parking lot is approved, active, and open for bookings")
    public ResponseEntity<ParkingLotResponse> verifyLotForBooking(@PathVariable UUID lotId) {
        return ResponseEntity.ok(parkingLotService.verifyLotForBooking(lotId));
    }

    // 14. Spot-Service capacity sync
    @PatchMapping("/{lotId}/internal/total-spots")
    @Operation(summary = "Inter-Service: Sync total spot capacity from spot-service")
    public ResponseEntity<ApiResponse> updateTotalSpots(
            @PathVariable UUID lotId,
            @RequestParam int count) {
        parkingLotService.updateTotalSpots(lotId, count);
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("Total spots updated successfully")
                .build());
    }

    // --- Helper ---
    private boolean checkIsAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}