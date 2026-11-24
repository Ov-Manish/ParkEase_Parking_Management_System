package com.parkease.analytics_service.consumer;


import com.parkease.analytics_service.dto.events.OccupancyEvent;
import com.parkease.analytics_service.entity.OccupancyLog;
import com.parkease.analytics_service.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OccupancyKafkaConsumer {

    private final AnalyticsService analyticsService;

    @KafkaListener(topics = "occupancy-events" , groupId = "analytics-group")
    public void consumerOccupancyEvent(OccupancyEvent event){
        log.info("Received Kafka occupancy-event: lotId={}, eventType={}", event.getLotId(), event.getEventType());

        // Create and save occupancy log snapshot
        OccupancyLog logEntry = OccupancyLog.builder()
                .lotId(event.getLotId())
                .spotId(event.getSpotId())
                .vehicleType(event.getVehicleType())
                .timestamp(event.getTimestamp())
                .totalSpots(100) // Or query current inventory
                .availableSpots("CHECK_IN".equalsIgnoreCase(event.getEventType()) ? 24 : 25)
                .build();

        analyticsService.logOccupancy(logEntry);
        log.info("Successfully persisted automated snapshot from Kafka event!");
    }
}
