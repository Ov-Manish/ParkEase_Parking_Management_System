package com.parkease.booking_service.dto.response;

import com.parkease.booking_service.enums.BookingStatus;
import com.parkease.booking_service.enums.BookingType;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponse {
    private UUID bookingId;
    private String bookingReference;
    private UUID userId;
    private UUID lotId;
    private UUID spotId;
    private UUID vehicleId;
    private String licensePlate;
    private BookingType bookingType;
    private BookingStatus status;
    private LocalDateTime reservedFrom;
    private LocalDateTime reservedTo;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private BigDecimal pricePerHour;
    private BigDecimal baseFare;
    private BigDecimal penaltyAmount;
    private BigDecimal totalAmount;
    private String cancellationReason;
    private Boolean isActive;
    private Long version;
    private Instant createdAt;
    private Instant updatedAt;
}
