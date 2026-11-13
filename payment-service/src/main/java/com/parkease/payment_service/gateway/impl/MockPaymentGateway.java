package com.parkease.payment_service.gateway.impl;

import com.parkease.payment_service.gateway.PaymentGateway;
import com.parkease.payment_service.gateway.dto.GatewayOrderResponse;
import com.parkease.payment_service.gateway.dto.GatewayRefundResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;


@Slf4j
@Service
@ConditionalOnProperty(name = "app.payment.gateway", havingValue = "MOCK", matchIfMissing = true)
public class MockPaymentGateway implements PaymentGateway {

    @Override
    @Transactional
    public GatewayOrderResponse createOrder(BigDecimal amount, String currency, String receiptReference) {


//        Generating a simulating order id like razor pay has (eg, order_xxxx)
        String mockOrderId = "mock_order_" + UUID.randomUUID().toString().substring(0,12);

        log.info("[MOCK GATEWAY] Order created: {} for amount: {} {}", mockOrderId, amount, currency);

        return GatewayOrderResponse.builder()
                .orderId(mockOrderId)
                .amount(amount)
                .currency(currency != null ? currency : "INR")
                .status("CREATED")
                .receiptReference(receiptReference)
                .build();
    }

    @Override
    public boolean verifyPaymentSignature(String paymentId, String orderId, String signature) {
//        In Local Mock testing , any signature that starts with "mock_sig_" or is not blank is accepted
        log.info("[MOCK GATEWAY] Verifying signature for payment : {}, order:{}",paymentId,orderId);
        return  signature != null && !signature.isBlank();
    }

    @Override
    public GatewayRefundResponse processRefund(String gatewayPaymentId, BigDecimal amount, String reason) {
        String mockRefundId = "mock_ref_" + UUID.randomUUID().toString().substring(0,12);

        log.info("[MOCK GATEWAY] Refund processed: {} for payment: {}, amount: {}",mockRefundId,gatewayPaymentId,amount);

        return GatewayRefundResponse.builder()
                .refundId(mockRefundId)
                .paymentId(gatewayPaymentId)
                .amount(amount)
                .status("PROCESSED")
                .build();
    }
}
