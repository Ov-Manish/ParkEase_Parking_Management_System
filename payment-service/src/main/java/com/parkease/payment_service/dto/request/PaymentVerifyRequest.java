package com.parkease.payment_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentVerifyRequest {

    @NotBlank(message = "Transaction reference is required")
    private String transactionReference;

    private String gatewayPaymentId;

    private String gatewayOrderId;

    private String gatewaySignature;
}
