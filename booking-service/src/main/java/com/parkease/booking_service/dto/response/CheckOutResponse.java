package com.parkease.booking_service.dto.response;

import com.parkease.booking_service.enums.BookingStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckOutResponse {
    private UUID bookingId;
    private String bookingReference;
    private BookingStatus status;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private Double durationMinutes;
    private Long billedHours;
    private BigDecimal pricePerHour;
    private BigDecimal baseFare;
    private BigDecimal penaltyAmount;
    private BigDecimal totalAmount;
    private String message;
}
