package com.parkease.booking_service.dto.event;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class BookingEventPayload {

    private String eventType; // "CONFIRMED", "CANCELLED", "OVERSTAY"

    private UUID bookingId;
    private String bookingReference;
    private UUID userId;

    private String recipientEmail;
    private String recipientPhone;

    private UUID lotId;
    private UUID spotId;
    private String spotNumber;
    private String licensePlate;

    private LocalDateTime reservedFrom;
    private LocalDateTime reservedTo;

    private BigDecimal totalAmount;
}