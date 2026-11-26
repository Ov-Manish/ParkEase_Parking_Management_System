package com.parkease.payment_service.consumer;


import com.parkease.payment_service.dto.event.AuditEvent;
import com.parkease.payment_service.dto.event.PaymentSagaEvent;
import com.parkease.payment_service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingCancelledKafkaConsumer {

    private final PaymentService paymentService;

    @KafkaListener(topics = "audit-events",groupId = "payment-group")
    public void onBookingCancelled(AuditEvent event){
        if ("BOOKING_CANCELLED".equalsIgnoreCase(event.getAction()) && event.getEntityId() != null){
            log.info("[SAGA CONSUMER] Received BOOKING_CANCELLED for bookingId={}. Checking for auto-refund...",
                    event.getEntityId());

            paymentService.autoRefundForCancelledBooking(event.getEntityId() , event.getActorId());
        }
    }
}
