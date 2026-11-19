package com.parkease.payment_service.producer;

import com.parkease.payment_service.dto.event.PaymentEventPayload;
import com.parkease.payment_service.entity.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventProducer {

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchange:parkease.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.routing-keys.payment-success:payment.success}")
    private String paymentSuccessRoutingKey;

    @Value("${app.rabbitmq.routing-keys.payment-failed:payment.failed}")
    private String paymentFailedRoutingKey;

    @Value("${app.rabbitmq.routing-keys.payment-refunded:payment.refunded}")
    private String paymentRefundedRoutingKey;

    public void publishPaymentSuccess(Payment payment, String userEmail, String userPhone) {
        PaymentEventPayload payload = PaymentEventPayload.builder()
                .eventType("SUCCESS")
                .paymentId(payment.getPaymentId())
                .bookingId(payment.getBookingId())
                .userId(payment.getUserId())
                .recipientEmail(userEmail)
                .recipientPhone(userPhone)
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod() != null ? payment.getPaymentMethod().name() : "WALLET")
                .status(payment.getStatus() != null ? payment.getStatus().name() : "COMPLETED")
                .transactionReference(payment.getTransactionReference())
                .build();

        sendEvent(paymentSuccessRoutingKey, payload);
    }

    public void publishPaymentFailed(Payment payment, String userEmail, String userPhone, String failureReason) {
        PaymentEventPayload payload = PaymentEventPayload.builder()
                .eventType("FAILED")
                .paymentId(payment.getPaymentId())
                .bookingId(payment.getBookingId())
                .userId(payment.getUserId())
                .recipientEmail(userEmail)
                .recipientPhone(userPhone)
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod() != null ? payment.getPaymentMethod().name() : "UNKNOWN")
                .status("FAILED")
                .transactionReference(payment.getTransactionReference())
                .failureReason(failureReason)
                .build();

        sendEvent(paymentFailedRoutingKey, payload);
    }

    public void publishPaymentRefunded(Payment payment, String userEmail, String userPhone) {
        PaymentEventPayload payload = PaymentEventPayload.builder()
                .eventType("REFUNDED")
                .paymentId(payment.getPaymentId())
                .bookingId(payment.getBookingId())
                .userId(payment.getUserId())
                .recipientEmail(userEmail)
                .recipientPhone(userPhone)
                .amount(payment.getRefundAmount() != null ? payment.getRefundAmount() : payment.getAmount())
                .paymentMethod(payment.getPaymentMethod() != null ? payment.getPaymentMethod().name() : "WALLET")
                .status(payment.getStatus() != null ? payment.getStatus().name() : "REFUNDED")
                .transactionReference(payment.getTransactionReference())
                .build();

        sendEvent(paymentRefundedRoutingKey, payload);
    }

    private void sendEvent(String routingKey, PaymentEventPayload payload) {
        try {
            log.info("[RABBITMQ PRODUCER] Publishing {} event for payment: {} to exchange: {} with key: {}",
                    payload.getEventType(), payload.getTransactionReference(), exchange, routingKey);

            rabbitTemplate.convertAndSend(exchange, routingKey, payload);

            log.info("[RABBITMQ PRODUCER] Successfully published payment event for ref: {}", payload.getTransactionReference());
        } catch (Exception ex) {
            // Fail-safe: A message broker issue must NEVER fail a captured payment or refund transaction!
            log.warn("[RABBITMQ PRODUCER ERROR] Failed to publish payment event (RabbitMQ may be offline): {}", ex.getMessage());
        }
    }
}
