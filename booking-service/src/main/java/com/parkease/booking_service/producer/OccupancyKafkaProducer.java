package com.parkease.booking_service.producer;


import com.parkease.booking_service.dto.event.OccupancyEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OccupancyKafkaProducer {

    private static final String TOPIC = "occupancy-events";
    private final KafkaTemplate<Object,Object> kafkaTemplate;

    public void sendOcuupancyEvent(OccupancyEvent event){
        log.info("Publishing occupancy event to Kafka [{}]: lotId={}, type={}", TOPIC, event.getLotId(), event.getEventType());

        kafkaTemplate.send(TOPIC,event.getLotId().toString(),event);
    }


}
