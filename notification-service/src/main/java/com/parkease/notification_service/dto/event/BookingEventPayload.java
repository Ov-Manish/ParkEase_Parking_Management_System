package com.parkease.notification_service.dto.event;

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

    // Event type: e.g. "CONFIRMED", "CANCELLED", "OVERSTAY"
    private String eventType;

    // Booking Identifiers
    private UUID bookingId;
    private String bookingReference; // e.g. "PE-BK-20261118-8F2A"
    private UUID userId;

    // Contact info for alert delivery
    private String recipientEmail;
    private String recipientPhone;

    // Parking Details for notification message
    private UUID lotId;
    private UUID spotId;
    private String spotNumber; // e.g. "A-12"
    private String licensePlate; // e.g. "MH-12-AB-1234"

    // Time schedule
    private LocalDateTime reservedFrom;
    private LocalDateTime reservedTo;

    // Financial snapshot
    private BigDecimal totalAmount;
}
