package com.parkease.payment_service.producer;


import com.parkease.payment_service.dto.event.PaymentSagaEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentKafkaProducer {

    private final static String TOPIC = "payment-events";
    private final KafkaTemplate<Object, PaymentSagaEvent> kafkaTemplate;


    public void sendPaymentEvent(PaymentSagaEvent event){
        log.info("[KAFKA PRODUCER] Publishing {} to [{}] for bookingId={}",
                event.getStatus(), TOPIC, event.getBookingId());

        kafkaTemplate.send(TOPIC,event.getBookingId().toString(),event);

    }

}
