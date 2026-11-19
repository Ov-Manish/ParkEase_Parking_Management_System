package com.parkease.booking_service.dto.event;

import com.parkease.booking_service.dto.event.BookingEventPayload;
import com.parkease.booking_service.entity.Booking;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingEventProducer {

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchange:parkease.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.routing-keys.booking-confirmed:booking.confirmed}")
    private String bookingConfirmedRoutingKey;

    @Value("${app.rabbitmq.routing-keys.booking-cancelled:booking.cancelled}")
    private String bookingCancelledRoutingKey;

    public void publishBookingConfirmed(Booking booking, String spotNumber, String userEmail) {
        BookingEventPayload payload = BookingEventPayload.builder()
                .eventType("CONFIRMED")
                .bookingId(booking.getBookingId())
                .bookingReference(booking.getBookingReference())
                .userId(booking.getUserId())
                .recipientEmail(userEmail)
                .lotId(booking.getLotId())
                .spotId(booking.getSpotId())
                .spotNumber(spotNumber)
                .licensePlate(booking.getLicensePlate())
                .reservedFrom(booking.getReservedFrom())
                .reservedTo(booking.getReservedTo())
                .totalAmount(booking.getTotalAmount() != null ? booking.getTotalAmount() : booking.getPricePerHour())
                .build();

        sendEvent(bookingConfirmedRoutingKey, payload);
    }

    public void publishBookingCancelled(Booking booking, String spotNumber, String userEmail) {
        BookingEventPayload payload = BookingEventPayload.builder()
                .eventType("CANCELLED")
                .bookingId(booking.getBookingId())
                .bookingReference(booking.getBookingReference())
                .userId(booking.getUserId())
                .recipientEmail(userEmail)
                .lotId(booking.getLotId())
                .spotId(booking.getSpotId())
                .spotNumber(spotNumber)
                .licensePlate(booking.getLicensePlate())
                .reservedFrom(booking.getReservedFrom())
                .reservedTo(booking.getReservedTo())
                .build();

        sendEvent(bookingCancelledRoutingKey, payload);
    }

    private void sendEvent(String routingKey, BookingEventPayload payload) {
        try {
            log.info("[RABBITMQ PRODUCER] Publishing {} event for booking: {} to exchange: {} with key: {}",
                    payload.getEventType(), payload.getBookingReference(), exchange, routingKey);

            rabbitTemplate.convertAndSend(exchange, routingKey, payload);

            log.info("[RABBITMQ PRODUCER] Successfully published event for booking: {}", payload.getBookingReference());
        } catch (Exception ex) {
            // Fail-safe: A message queue hiccup must NEVER rollback a valid parking reservation!
            log.warn("[RABBITMQ PRODUCER ERROR] Failed to publish booking event (RabbitMQ may be offline): {}", ex.getMessage());
        }
    }
}