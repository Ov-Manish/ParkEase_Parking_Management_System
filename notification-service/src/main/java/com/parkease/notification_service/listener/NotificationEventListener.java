package com.parkease.notification_service.listener;


import com.parkease.notification_service.dto.event.BookingEventPayload;
import com.parkease.notification_service.dto.event.PaymentEventPayload;
import com.parkease.notification_service.dto.request.NotificationSendRequest;
import com.parkease.notification_service.enums.NotificationChannel;
import com.parkease.notification_service.enums.NotificationType;
import com.parkease.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {
    private final NotificationService notificationService;

//    Consume Booking Events from "notification.booking.queue"
    @RabbitListener(queues = "${app.rabbitmq.queues.booking:notification.booking.queue}")
    public void handleBookingEvent(BookingEventPayload event){
        log.info("[RABBITMQ CONSUMER] Received Booking Event: {} for bookingId: {}",
                event.getEventType(), event.getBookingId());

        try {
            NotificationType type = switch (event.getEventType().toUpperCase()){
                case "CONFIRMED","CREATED" -> NotificationType.BOOKING_CONFIRMED;
                case "CANCELLED" -> NotificationType.BOOKING_CANCELLED;
                case "OVERSTAY" -> NotificationType.OVERSTAY_ALERT;
                default -> NotificationType.BOOKING_CONFIRMED;
            };

            String title = switch (type) {
                case BOOKING_CONFIRMED -> "Booking Confirmed - Spot " + event.getSpotNumber();
                case BOOKING_CANCELLED -> "⚠️ Spot Released: Booking Expired - " + event.getBookingReference();
                case OVERSTAY_ALERT -> "⚠️ Overstay Alert - Vehicle " + event.getLicensePlate();
                default -> "Booking Update";
            };

            String message = switch (type) {
                case BOOKING_CONFIRMED -> String.format(
                        "Your parking reservation at spot %s is confirmed from %s to %s. Total: ₹%s. Ref: %s",
                        event.getSpotNumber(), event.getReservedFrom(), event.getReservedTo(),
                        event.getTotalAmount(), event.getBookingReference());
                case BOOKING_CANCELLED -> String.format(
                        "⚠️ Your reservation %s for Spot %s has expired and been cancelled because check-in was not completed within the 15-minute grace period. The spot has been released back to public availability.",
                        event.getBookingReference(), event.getSpotNumber());
                case OVERSTAY_ALERT -> String.format(
                        "Your vehicle %s has exceeded the reserved parking time at spot %s. Please exit or extend your booking to avoid penalties.",
                        event.getLicensePlate(), event.getSpotNumber());
                default -> "Booking details updated for reference: " + event.getBookingReference();
            };

//            Building metadata JSON snippet for client app navigation

            String metadata = String.format("{\"bookingId\":\"%s\",\"bookingReference\":\"%s\"}",event.getBookingId(), event.getBookingReference());

//            Dispatch IN_APP notification
            notificationService.sendNotification(NotificationSendRequest.builder()
                    .userId(event.getUserId())
                    .recipientEmail(event.getRecipientEmail())
                    .recipientPhone(event.getRecipientPhone())
                    .type(type)
                    .channel(NotificationChannel.IN_APP)
                    .title(title)
                    .message(message)
                    .metadataJson(metadata)
                    .build());

//            If recipient has an email, dispatch EMAIL confirmation as well

            if (event.getRecipientEmail() != null && !event.getRecipientEmail().isBlank()) {
                notificationService.sendNotification(NotificationSendRequest.builder()
                        .userId(event.getUserId())
                        .recipientEmail(event.getRecipientEmail())
                        .type(type)
                        .channel(NotificationChannel.EMAIL)
                        .title(title)
                        .message(message)
                        .metadataJson(metadata)
                        .build());
            }
        } catch (Exception ex) {
            log.error("[RABBITMQ CONSUMER ERROR] Failed to process booking event: {}", ex.getMessage(), ex);
        }
    }


//     Consume Payment Events from "notification.payment.queue"

    @RabbitListener(queues = "${app.rabbitmq.queues.payment:notification.payment.queue}")
    public void handlePaymentEvent(PaymentEventPayload event){
        log.info("[RABBITMQ CONSUMER] Received Payment Event: {} for paymentId: {}",
                event.getEventType(), event.getPaymentId());

        try {
            NotificationType type = switch (event.getEventType().toUpperCase()){
                case "SUCCESS","COMPLETED" -> NotificationType.PAYMENT_SUCCESS;
                case "FAILED" -> NotificationType.PAYMENT_FAILED;
                case "REFUNDED" -> NotificationType.REFUND_ISSUED;
                case "WALLET_TOPUP" -> NotificationType.WALLET_TOPUP;

                default -> NotificationType.PAYMENT_SUCCESS;
            };

            String title = switch (type) {
                case PAYMENT_SUCCESS -> "Payment Successful - ₹" + event.getAmount();
                case PAYMENT_FAILED -> "Payment Failed - ₹" + event.getAmount();
                case REFUND_ISSUED -> "Refund Processed - ₹" + event.getAmount();
                case WALLET_TOPUP -> "Wallet Top-Up - ₹" + event.getAmount();
                default -> "Payment Notification";
            };


            String message = switch (type) {
                case PAYMENT_SUCCESS -> String.format(
                        "Payment of ₹%s via %s completed successfully. Transaction Ref: %s",
                        event.getAmount(), event.getPaymentMethod(), event.getTransactionReference());
                case PAYMENT_FAILED -> String.format(
                        "Payment of ₹%s failed. Reason: %s. Ref: %s",
                        event.getAmount(),
                        event.getFailureReason() != null ? event.getFailureReason() : "Declined by gateway",
                        event.getTransactionReference());
                case REFUND_ISSUED -> String.format(                               // ✅ Corrected to REFUND_ISSUED
                        "Refund of ₹%s for reference %s has been credited back to your account.",
                        event.getAmount(), event.getTransactionReference());
                case WALLET_TOPUP -> String.format(
                        "Your prepaid wallet has been credited with ₹%s. Transaction Ref: %s",
                        event.getAmount(), event.getTransactionReference());
                default -> "Transaction update for reference: " + event.getTransactionReference();
            };

            String metadata = String.format("{\"paymentId\":\"%s\",\"transactionReference\":\"%s\"}",
                    event.getPaymentId(), event.getTransactionReference());

//             Dispatch IN_APP notification
            notificationService.sendNotification(NotificationSendRequest.builder()
                    .userId(event.getUserId())
                    .recipientEmail(event.getRecipientEmail())
                    .recipientPhone(event.getRecipientPhone())
                    .type(type)
                    .channel(NotificationChannel.IN_APP)
                    .title(title)
                    .message(message)
                    .metadataJson(metadata)
                    .build());

//            If email is available, dispatch receipt email
            if (event.getRecipientEmail() != null && !event.getRecipientEmail().isBlank()) {
                notificationService.sendNotification(NotificationSendRequest.builder()
                        .userId(event.getUserId())
                        .recipientEmail(event.getRecipientEmail())
                        .type(type)
                        .channel(NotificationChannel.EMAIL)
                        .title(title)
                        .message(message)
                        .metadataJson(metadata)
                        .build());
            }

        } catch (Exception ex) {
            log.error("[RABBITMQ CONSUMER ERROR] Failed to process payment event: {}", ex.getMessage(), ex);
        }
    }
}
