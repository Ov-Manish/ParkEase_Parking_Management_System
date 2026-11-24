package com.parkease.booking_service.dto.event;


import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OccupancyEvent {
    private UUID lotId;
    private UUID spotId;
    private String vehicleType;
    private String eventType; // "CHECK_IN" or "CHECK_OUT"
    private LocalDateTime timestamp;
}
