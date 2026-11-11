package com.parkease.booking_service.entity;

import com.parkease.booking_service.enums.BookingStatus;
import com.parkease.booking_service.enums.BookingType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "bookings",
        indexes = {
                @Index(name = "idx_booking_ref", columnList = "booking_reference", unique = true),
                @Index(name = "idx_booking_user", columnList = "user_id"),
                @Index(name = "idx_booking_lot", columnList = "lot_id"),
                @Index(name = "idx_booking_spot", columnList = "spot_id"),
                @Index(name = "idx_booking_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "booking_id", updatable = false, nullable = false)
    private UUID bookingId;

    // Human-readable reference e.g. "PE-BK-20261109-8F2A"
    @Column(name = "booking_reference", nullable = false, unique = true, length = 32)
    private String bookingReference;

    // Driver user ID from auth-service
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    // Parking lot ID from parkinglot-service
    @Column(name = "lot_id", nullable = false)
    private UUID lotId;

    // Reserved spot ID from spot-service
    @Column(name = "spot_id", nullable = false)
    private UUID spotId;

    @Column(name = "vehicle_id", nullable = false)
    private UUID vehicleId;

    // Snapshot of license plate at time of booking
    // Registered vehicle ID from vehicle-service
    @Column(name = "license_plate", nullable = false, length = 20)
    private String licensePlate;

    @Enumerated(EnumType.STRING)
    @Column(name = "booking_type", nullable = false, length = 20)
    private BookingType bookingType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BookingStatus status;

    // Scheduled reservation start
    @Column(name = "reserved_from", nullable = false)
    private LocalDateTime reservedFrom;

    // Scheduled reservation end
    @Column(name = "reserved_to", nullable = false)
    private LocalDateTime reservedTo;

    // Actual check-in timestamp (recorded at gate entry)
    @Column(name = "check_in_time")
    private LocalDateTime checkInTime;

    // Actual check-out timestamp (recorded at gate exit)
    @Column(name = "check_out_time")
    private LocalDateTime checkOutTime;

    // Snapshot of spot's hourly rate at the time of booking
    @Column(name = "price_per_hour", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerHour;

    // Calculated base duration fare on check-out
    @Column(name = "base_fare", precision = 10, scale = 2)
    private BigDecimal baseFare;

    // Overstay / late check-out penalty amount
    @Builder.Default
    @Column(name = "penalty_amount", precision = 10, scale = 2)
    private BigDecimal penaltyAmount = BigDecimal.ZERO;

    // Final total payable amount (baseFare + penaltyAmount)
    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount;

    // Optional cancellation reason if cancelled
    @Column(name = "cancellation_reason", length = 255)
    private String cancellationReason;

    // Soft-delete flag
    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    // Optimistic locking version
    @Version
    @Column(name = "version")
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
