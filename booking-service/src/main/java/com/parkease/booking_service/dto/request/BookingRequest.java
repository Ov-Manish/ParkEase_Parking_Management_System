package com.parkease.booking_service.dto.request;

import com.parkease.booking_service.enums.BookingType;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingRequest {

    @NotNull(message = "Parking lot ID is required")
    private UUID lotId;

    @NotNull(message = "Parking spot ID is required")
    private UUID spotId;

    @NotNull(message = "Vehicle ID is required")
    private UUID vehicleId;

    @NotNull(message = "Booking type is required (PRE_BOOKING or WALK_IN)")
    private BookingType bookingType;

    @NotNull(message = "Reservation start time is required")
    @FutureOrPresent(message = "Reservation start time must be in the present or future")
    private LocalDateTime reservedFrom;

    @NotNull(message = "Reservation end time is required")
    private LocalDateTime reservedTo;
}
