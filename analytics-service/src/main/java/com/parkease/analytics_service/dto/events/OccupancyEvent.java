package com.parkease.analytics_service.dto.events;

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
public class OccupancyEvent {
    private UUID lotId;
    private UUID spotId;
    private String vehicleType;
    private String eventType; // "CHECK_IN" or "CHECK_OUT"
    private LocalDateTime timestamp;
}