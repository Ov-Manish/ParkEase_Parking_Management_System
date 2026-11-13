package com.parkease.payment_service.dto.request;

import com.parkease.payment_service.enums.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentInitiateRequest {

    @NotNull(message = "Booking ID is required")
    private UUID bookingId;

    @NotNull(message = "Payment amount is required")
    @DecimalMin(value = "1.00", message = "Amount must be greater than or equal to 1.00")
    private BigDecimal amount;

    @NotNull(message = "Payment method is required (WALLET, CARD, UPI, NET_BANKING, CASH)")
    private PaymentMethod paymentMethod;

    private String description;
}
