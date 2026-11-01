package com.parkease.spot_service.entity;

import com.parkease.spot_service.enums.SpotStatus;
import com.parkease.spot_service.enums.SpotType;
import com.parkease.spot_service.enums.VehicleType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "parking_spots",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_lot_spot_number", columnNames = {"lot_id", "spot_number", "is_active"})
        },
        indexes = {
                @Index(name = "idx_spot_lot_id", columnList = "lot_id"),
                @Index(name = "idx_spot_status", columnList = "status"),
                @Index(name = "idx_spot_type", columnList = "spot_type"),
                @Index(name = "idx_spot_floor", columnList = "floor")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParkingSpot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "spot_id", updatable = false, nullable = false)
    private UUID spotId;

    // The parking lot this spot physically belongs to
    @Column(name = "lot_id", nullable = false)
    private UUID lotId;

    // Spot label e.g. "A-1", "B-12", "EV-01"
    @Column(name = "spot_number", nullable = false, length = 30)
    private String spotNumber;

    // Floor number: 0 = Ground, 1 = 1st Floor, -1 = Basement
    @Builder.Default
    @Column(name = "floor", nullable = false)
    private Integer floor = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "spot_type", nullable = false, length = 30)
    private SpotType spotType;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false, length = 30)
    private VehicleType vehicleType;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private SpotStatus status = SpotStatus.AVAILABLE;

    @Builder.Default
    @Column(name = "is_ev_charging", nullable = false)
    private Boolean isEVCharging = false;

    @Builder.Default
    @Column(name = "is_handicapped", nullable = false)
    private Boolean isHandicapped = false;

    // Specific hourly rate for this spot
    @Column(name = "price_per_hour", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerHour;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    // Concurrency control: prevents race conditions during bookings!
    @Version
    @Column(name = "version")
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}