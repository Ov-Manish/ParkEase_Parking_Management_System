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
public class PaymentSagaEvent {
    private UUID paymentId;
    private UUID bookingId;
    private UUID userId;
    private BigDecimal amount;
    private String status; // COMPLETED or REFUNDED
    private LocalDateTime timestamp;
}