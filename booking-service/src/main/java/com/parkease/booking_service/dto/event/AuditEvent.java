package com.parkease.booking_service.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEvent {
    private UUID actorId;
    private String action;
    private String entityName;
    private UUID entityId;
    private String beforeValue;
    private String afterValue;
    private LocalDateTime timestamp;
}