package com.parkease.spot_service.controller;
import com.parkease.spot_service.dto.request.BulkSpotRequest;
import com.parkease.spot_service.dto.request.SpotRequest;
import com.parkease.spot_service.dto.request.SpotUpdateRequest;
import com.parkease.spot_service.dto.response.ApiResponse;
import com.parkease.spot_service.dto.response.SpotCountResponse;
import com.parkease.spot_service.dto.response.SpotResponse;
import com.parkease.spot_service.enums.SpotStatus;
import com.parkease.spot_service.enums.SpotType;
import com.parkease.spot_service.enums.VehicleType;
import com.parkease.spot_service.service.ParkingSpotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;
@RestController
@RequestMapping("/api/v1/spots")
@RequiredArgsConstructor
@Tag(name = "Parking Spot Management", description = "APIs for spot provisioning, driver availability queries, and booking lifecycle")
public class ParkingSpotController {

    private final ParkingSpotService parkingSpotService;

//    Lot Manager Api's

//    Create a single Customised spot
    @PostMapping
    @PreAuthorize("hasAnyRole('LOT_MANAGER','ADMIN')")
    @Operation(summary = "Manager: Create a single parking spot", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<SpotResponse> createSpot(@Valid @RequestBody SpotRequest request){
        return ResponseEntity.status(HttpStatus.CREATED).body(parkingSpotService.createSpot(request));
    }

//    High-Performance Bulk Spot Generator
    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('LOT_MANAGER', 'ADMIN')")
    @Operation(summary = "Manager: Bulk-create spots with range (up to 200 per batch)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<List<SpotResponse>> createBulkSpots(@Valid @RequestBody BulkSpotRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(parkingSpotService.createBulkSpots(request));
    }


//    Get all spots in a lot (Manager dashboard view)

    @GetMapping("/lot/{lotId}")
    @PreAuthorize("hasAnyRole('LOT_MANAGER', 'ADMIN')")
    @Operation(summary = "Manager: Get all spots in a parking lot", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<List<SpotResponse>> getSpotsByLot(@PathVariable UUID lotId){
        return ResponseEntity.ok(parkingSpotService.getSpotsByLot(lotId));
    }

//     Update spot details

    @PutMapping("/{spotId}")
    @PreAuthorize("hasAnyRole('LOT_MANAGER', 'ADMIN')")
    @Operation(summary = "Manager: Update parking spot configuration", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<SpotResponse> updateSpot(@PathVariable UUID spotId,
                                                   @Valid @RequestBody SpotUpdateRequest request){
        return ResponseEntity.ok(parkingSpotService.updateSpot(spotId,request));
    }


//    Update Spot Status
    @PatchMapping("/{spotId}/status")
    @PreAuthorize("hasAnyRole('LOT_MANAGER', 'ADMIN')")
    @Operation(summary = "Manager: Toggle spot status (e.g. OUT_OF_SERVICE)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<SpotResponse> updateSpotStatus(
            @PathVariable UUID spotId,
            @RequestParam SpotStatus status
    ){
        return ResponseEntity.ok(parkingSpotService.updateSpotStatus(spotId,status));
    }


//    Soft Delete a Spot
    @DeleteMapping("/{spotId}")
    @PreAuthorize("hasAnyRole('LOT_MANAGER', 'ADMIN')")
    @Operation(summary = "Manager: Soft-delete a parking spot", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse> deleteSpot(@PathVariable UUID spotId){
        parkingSpotService.deleteSpot(spotId);

        return ResponseEntity.ok(
                ApiResponse.builder()
                        .success(true)
                        .message("Parking Spot Deleted Successfully")
                        .build()
        );
    }


//    Public Api's (Exmaple for Drivers)


//    Get a Single Spot Details
    @GetMapping("/{spotId}")
    @Operation(summary = "Public: Get spot details by ID")
    public ResponseEntity<SpotResponse> getSpotById(
            @PathVariable UUID spotId
    ){
        return ResponseEntity.ok(parkingSpotService.getSpotById(spotId));
    }

//    Find Available spots with optional Filter
    @GetMapping("/lot/{lotId}/available")
    @Operation(summary = "Public: Find available spots in a lot (filter by floor, spotType, vehicleType)")
    public ResponseEntity<List<SpotResponse>> getAvailableSpots(
            @PathVariable UUID lotId,
            @RequestParam(required = false) Integer floor,
            @RequestParam(required = false) SpotType spotType,
            @RequestParam(required = false) VehicleType vehicleType){

            return ResponseEntity.ok(parkingSpotService.getAvailableSpots(lotId, floor, spotType, vehicleType));
    }

//    Real-time count metrics and occupancy rate
    @GetMapping("/lot/{lotId}/count")
    @Operation(summary = "Public: Get real-time spot occupancy metrics and rates")
    public ResponseEntity<SpotCountResponse> getSpotCountsbyLot(@PathVariable UUID lotId){
        return ResponseEntity.ok(parkingSpotService.getSpotCountsByLot(lotId));
    }


//    INTER-SERVICE LIFECYCLE (Called by Booking-Service)


//    Reserve spot on booking creation
    @PostMapping("/{spotId}/reserve")
    @Operation(summary = "Inter-Service: Reserve spot on booking creation")
    public ResponseEntity<SpotResponse> reserveSpot(
            @PathVariable UUID spotId
    ){
        return ResponseEntity.ok(parkingSpotService.reserveSpot(spotId));
    }

//     Occupy spot on driver check-in
    @PostMapping("/{spotId}/occupy")
    @Operation(summary = "Inter-Service: Mark spot occupied on check-in")
    public ResponseEntity<SpotResponse> occupySpot(@PathVariable UUID spotId){
        return  ResponseEntity.ok(parkingSpotService.occupySpot(spotId));
    }

//    Release spot on checkout or cancellation
    @PostMapping("/{spotId}/release")
    @Operation(summary = "Inter-Service: Release spot back to AVAILABLE")
    public ResponseEntity<SpotResponse> releaseSpot(@PathVariable UUID spotId){
        return ResponseEntity.ok(parkingSpotService.releaseSpot(spotId));
    }

}


