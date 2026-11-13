package com.parkease.payment_service.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GatewayRefundResponse {
    private String refundId;           // Gateway refund tracking ID
    private String paymentId;          // Original payment ID
    private BigDecimal amount;         // Amount refunded
    private String status;             // "PROCESSED"
}