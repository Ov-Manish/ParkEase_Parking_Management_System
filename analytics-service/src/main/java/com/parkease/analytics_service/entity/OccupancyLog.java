package com.parkease.analytics_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "occupancy_logs",
        indexes = {
                @Index(name = "idx_occ_lot", columnList = "lot_id"),
                @Index(name = "idx_occ_timestamp", columnList = "timestamp")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OccupancyLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "log_id", updatable = false, nullable = false)
    private UUID logId;

    @Column(name = "lot_id", nullable = false)
    private UUID lotId;

    @Column(name = "spot_id")
    private UUID spotId;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "occupancy_rate", nullable = false)
    private Double occupancyRate; // e.g. 75.5%

    @Column(name = "available_spots", nullable = false)
    private Integer availableSpots;

    @Column(name = "total_spots", nullable = false)
    private Integer totalSpots;

    @Column(name = "vehicle_type")
    private String vehicleType; // e.g. "FOUR_WHEELER", "TWO_WHEELER"
}
