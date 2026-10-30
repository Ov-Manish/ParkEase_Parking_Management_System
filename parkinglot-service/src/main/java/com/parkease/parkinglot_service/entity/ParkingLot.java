package com.parkease.parkinglot_service.entity;

import com.parkease.parkinglot_service.enums.LotStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "parking_lots",
    indexes = {
        @Index(name = "idx_lot_city", columnList = "city"),
        @Index(name = "idx_lot_manager", columnList = "manager_id"),
        @Index(name = "idx_lot_lat_lng", columnList = "latitude, longitude")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParkingLot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "lot_id", updatable = false, nullable = false)
    private UUID lotId;

    // The Lot Manager who owns/manages this facility (from auth-service)
    @Column(name = "manager_id", nullable = false)
    private UUID managerId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "address", nullable = false, length = 255)
    private String address;

    @Column(name = "city", nullable = false, length = 50)
    private String city;

    @Column(name = "state", length = 50)
    private String state;

    @Column(name = "zip_code", length = 20)
    private String zipCode;

    // GPS Latitude: -90.00000000 to +90.00000000
    @Column(name = "latitude", nullable = false, precision = 10, scale = 8)
    private BigDecimal latitude;

    // GPS Longitude: -180.00000000 to +180.00000000
    @Column(name = "longitude", nullable = false, precision = 11, scale = 8)
    private BigDecimal longitude;

    // Total spots in the facility (synced with spot-service)
    @Builder.Default
    @Column(name = "total_spots", nullable = false)
    private Integer totalSpots = 0;

    // Base parking fee per hour
    @Column(name = "hourly_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal hourlyRate;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private LotStatus status = LotStatus.PENDING_APPROVAL;

    // Feedback given by Admin if rejected
    @Column(name = "rejection_reason", length = 255)
    private String rejectionReason;

    // Quick toggle: Manager can open/close the lot in real-time
    @Builder.Default
    @Column(name = "is_open", nullable = false)
    private Boolean isOpen = true;

    // Platform visibility: true only when approved and active
    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = false;

    // Concurrency control: prevents race conditions during updates
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
