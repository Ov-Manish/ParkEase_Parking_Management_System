package com.parkease.payment_service.dto.event;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditEvent {
    private UUID actorId;
    private String action;
    private String entityName;
    private UUID entityId;
    private String beforeValue;
    private String afterValue;
    private LocalDateTime timestamp;
}