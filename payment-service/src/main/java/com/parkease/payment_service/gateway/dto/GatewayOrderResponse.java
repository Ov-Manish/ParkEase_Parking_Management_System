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
public class GatewayOrderResponse {
    private String orderId;            // The gateway's order tracking ID
    private BigDecimal amount;         // Amount in rupees/dollars
    private String currency;           // "INR" or "USD"
    private String status;             // "CREATED"
    private String receiptReference;   // Our internal PE-TXN reference
}