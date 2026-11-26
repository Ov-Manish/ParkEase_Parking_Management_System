package com.parkease.booking_service.consumer;

import com.parkease.booking_service.dto.event.PaymentSagaEvent;
import com.parkease.booking_service.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentKafkaConsumer {

    private final BookingRepository bookingRepository;

    @Transactional
    @KafkaListener(topics = "payment-events", groupId = "booking-group")
    public void onPaymentEvent(PaymentSagaEvent event){
        log.info("[SAGA CONSUMER] Received {} for bookingId={}", event.getStatus(), event.getBookingId());

        bookingRepository.findById(event.getBookingId()).ifPresent(booking -> {
            if ("COMPLETED".equalsIgnoreCase(event.getStatus())){
                booking.setPaymentStatus("PAID");
            }else if ("REFUNDED".equalsIgnoreCase(event.getStatus())){
                booking.setPaymentStatus("REFUNDED");
            }

            bookingRepository.save(booking);
            log.info("[SAGA] Updated booking {} paymentStatus -> {}", booking.getBookingReference(), booking.getPaymentStatus());
        });
    }

}
