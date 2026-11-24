package com.parkease.auth_service.consumer;

import com.parkease.auth_service.dto.event.AuditEvent;
import com.parkease.auth_service.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditKafkaConsumer {

    private final AuditLogService auditLogService;

    @KafkaListener(topics = "audit-events", groupId = "auth-group")
    public void consumeAuditEvent(AuditEvent event) {
        log.info("Received Kafka audit-event: action={}, entityName={}, entityId={}",
                event.getAction(), event.getEntityName(), event.getEntityId());

        auditLogService.logAction(
                event.getActorId(),
                event.getAction(),
                event.getEntityName(),
                event.getEntityId(),
                event.getBeforeValue(),
                event.getAfterValue()
        );

        log.info("Successfully persisted audit log into database!");
    }
}