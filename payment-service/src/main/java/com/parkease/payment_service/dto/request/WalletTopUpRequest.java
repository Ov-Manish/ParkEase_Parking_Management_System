package com.parkease.payment_service.dto.request;

import com.parkease.payment_service.enums.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletTopUpRequest {

    @NotNull(message = "Top-up amount is required")
    @DecimalMin(value = "10.00", message = "Minimum top-up amount is 10.00")
    private BigDecimal amount;

    @NotNull(message = "Payment method for top-up is required (CARD, UPI, NET_BANKING)")
    private PaymentMethod paymentMethod;
}
