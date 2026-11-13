package com.parkease.payment_service.gateway;

import com.parkease.payment_service.gateway.dto.GatewayOrderResponse;
import com.parkease.payment_service.gateway.dto.GatewayRefundResponse;

import java.math.BigDecimal;

public interface PaymentGateway {

    // Initializes a payment order with the gateway.
    GatewayOrderResponse createOrder(BigDecimal amount, String currency, String receiptReference);


    // Verifies the cryptographic signature sent back after user completes payment.
    boolean verifyPaymentSignature(String paymentId, String orderId, String signature);

    // Processes a refund back to the customer's original payment source.
    GatewayRefundResponse processRefund(String gatewayPaymentId, BigDecimal amount, String reason);
}