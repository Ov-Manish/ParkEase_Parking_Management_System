package com.parkease.booking_service.dto.response;

import com.parkease.booking_service.enums.BookingStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckInResponse {
    private UUID bookingId;
    private String bookingReference;
    private BookingStatus status;
    private LocalDateTime checkInTime;
    private UUID spotId;
    private String message;
}
