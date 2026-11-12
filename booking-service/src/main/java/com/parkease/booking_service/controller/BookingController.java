package com.parkease.booking_service.controller;

import com.parkease.booking_service.dto.request.BookingRequest;
import com.parkease.booking_service.dto.response.ApiResponse;
import com.parkease.booking_service.dto.response.BookingResponse;
import com.parkease.booking_service.dto.response.CheckInResponse;
import com.parkease.booking_service.dto.response.CheckOutResponse;
import com.parkease.booking_service.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Booking Controller", description = "Endpoints for spot reservation, check-in, check-out, and cancellation")
@SecurityRequirement(name = "bearerAuth")
public class BookingController {

    private final BookingService bookingService;

//     Create a new spot reservation

    @PostMapping
    @PreAuthorize("hasAnyRole('DRIVER', 'ADMIN')")
    @Operation(summary = "Reserve a parking spot", description = "Validates spot availability, prevents collision, and creates booking in CONFIRMED state")
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @Valid @RequestBody BookingRequest request,
            @RequestHeader("Authorization") String authHeader,
            Authentication authentication) {

        UUID userId = extractUserId(authentication);
        log.info("REST request to create booking by user: {}", userId);

        BookingResponse response = bookingService.createResponse(request, userId, authHeader);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Booking created successfully", response));
    }

//    Digital Check-in at entry gate

    @PostMapping("/{bookingId}/check-in")
    @PreAuthorize("hasAnyRole('DRIVER', 'ADMIN')")
    @Operation(summary = "Gate Check-In", description = "Transitions booking to CHECKED_IN, records entry time, and marks spot as OCCUPIED")
    public ResponseEntity<ApiResponse<CheckInResponse>> checkIn(
            @PathVariable UUID bookingId,
            @RequestHeader("Authorization") String authHeader,
            Authentication authentication) {

        UUID userId = extractUserId(authentication);
        log.info("REST request to check in booking: {} by user: {}", bookingId, userId);

        CheckInResponse response = bookingService.checkIn(bookingId, userId, authHeader);
        return ResponseEntity.ok(ApiResponse.success("Check-in successful. Gate opened.", response));
    }

//   Digital Check-out at exit gate

    @PostMapping("/{bookingId}/check-out")
    @PreAuthorize("hasAnyRole('DRIVER', 'ADMIN')")
    @Operation(summary = "Gate Check-Out", description = "Calculates parking duration, base fare, penalty (if late), marks COMPLETED, and releases spot")
    public ResponseEntity<ApiResponse<CheckOutResponse>> checkOut(
            @PathVariable UUID bookingId,
            @RequestHeader("Authorization") String authHeader,
            Authentication authentication) {

        UUID userId = extractUserId(authentication);
        log.info("REST request to check out booking: {} by user: {}", bookingId, userId);

        CheckOutResponse response = bookingService.checkOut(bookingId, userId, authHeader);
        return ResponseEntity.ok(ApiResponse.success("Check-out successful. Spot released.", response));
    }

//     Cancel an existing reservation before check-in

    @PatchMapping("/{bookingId}/cancel")
    @PreAuthorize("hasAnyRole('DRIVER', 'ADMIN')")
    @Operation(summary = "Cancel Reservation", description = "Cancels a CONFIRMED booking and releases spot back to AVAILABLE")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(
            @PathVariable UUID bookingId,
            @RequestHeader("Authorization") String authHeader,
            Authentication authentication) {

        UUID userId = extractUserId(authentication);
        log.info("REST request to cancel booking: {} by user: {}", bookingId, userId);

        BookingResponse response = bookingService.cancelBooking(bookingId, userId, authHeader);
        return ResponseEntity.ok(ApiResponse.success("Booking cancelled successfully", response));
    }

//     Get Booking by ID

    @GetMapping("/{bookingId}")
    @PreAuthorize("hasAnyRole('DRIVER', 'LOT_MANAGER', 'ADMIN')")
    @Operation(summary = "Get Booking by ID", description = "Retrieves reservation details with driver ownership verification")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingById(
            @PathVariable UUID bookingId,
            Authentication authentication) {

        UUID userId = extractUserId(authentication);
        String role = extractRole(authentication);

        BookingResponse response = bookingService.getBookingById(bookingId, userId, role);
        return ResponseEntity.ok(ApiResponse.success("Booking retrieved successfully", response));
    }

//    Get Booking by Ticket Reference Code (e.g. for QR scanners)

    @GetMapping("/reference/{reference}")
    @PreAuthorize("hasAnyRole('DRIVER', 'LOT_MANAGER', 'ADMIN')")
    @Operation(summary = "Get Booking by Ticket Reference", description = "Retrieves booking using human-readable reference code like PE-BK-20261111-8F2A")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingByReference(
            @PathVariable String reference) {

        BookingResponse response = bookingService.getBookingByReference(reference);
        return ResponseEntity.ok(ApiResponse.success("Booking retrieved successfully", response));
    }

//     Get all bookings for the logged-in Driver

    @GetMapping("/my-bookings")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Get My Bookings", description = "Lists all historical and upcoming bookings belonging to the authenticated driver")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getMyBookings(
            Authentication authentication) {

        UUID userId = extractUserId(authentication);
        List<BookingResponse> responses = bookingService.getUserBookings(userId);
        return ResponseEntity.ok(ApiResponse.success("User bookings retrieved successfully", responses));
    }


//     Get all bookings for a Parking Lot (Manager / Admin)

    @GetMapping("/lot/{lotId}")
    @PreAuthorize("hasAnyRole('LOT_MANAGER', 'ADMIN')")
    @Operation(summary = "Get Lot Bookings", description = "Lists all bookings for a parking lot")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getLotBookings(
            @PathVariable UUID lotId) {

        List<BookingResponse> responses = bookingService.getLotBookings(lotId);
        return ResponseEntity.ok(ApiResponse.success("Lot bookings retrieved successfully", responses));
    }

//     Get live active parked bookings for a Lot (Currently Checked-In)

    @GetMapping("/lot/{lotId}/active")
    @PreAuthorize("hasAnyRole('LOT_MANAGER', 'ADMIN')")
    @Operation(summary = "Get Active Lot Bookings", description = "Lists currently checked-in vehicles in the parking lot")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getActiveLotBookings(
            @PathVariable UUID lotId) {

        List<BookingResponse> responses = bookingService.getActiveBookingsForLot(lotId);
        return ResponseEntity.ok(ApiResponse.success("Active lot bookings retrieved successfully", responses));
    }

//    Helper Methods
    private UUID extractUserId(Authentication authentication) {
        // Our JwtAuthenticationFilter sets the principal as the user's UUID
        return (UUID) authentication.getPrincipal();
    }

    private String extractRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse("ROLE_DRIVER");
    }
}