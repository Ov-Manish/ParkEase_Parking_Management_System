package com.parkease.booking_service.producer;

import com.parkease.booking_service.dto.event.AuditEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditKafkaProducer {

    private static final String TOPIC = "audit-events";
    private final KafkaTemplate<Object, Object> kafkaTemplate;

    public void sendAuditEvent(AuditEvent event) {
        log.info("Publishing audit event to Kafka [{}]: action={}, entityId={}", TOPIC, event.getAction(), event.getEntityId());
        kafkaTemplate.send(TOPIC, event.getEntityId() != null ? event.getEntityId().toString() : "AUDIT", event);
    }
}