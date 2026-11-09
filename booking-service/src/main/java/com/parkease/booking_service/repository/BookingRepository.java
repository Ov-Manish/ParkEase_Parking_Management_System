package com.parkease.booking_service.repository;

import com.parkease.booking_service.entity.Booking;
import com.parkease.booking_service.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    // 1. Find booking by human-readable reference (e.g. at gate entry)
    Optional<Booking> findByBookingReferenceIgnoreCaseAndIsActiveTrue(String bookingReference);

    // 2. Fetch driver's booking history ordered by most recent
    List<Booking> findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(UUID userId);

    // 3. Fetch lot's booking history for Manager dashboard
    List<Booking> findByLotIdAndIsActiveTrueOrderByCreatedAtDesc(UUID lotId);

    // 4. Fetch all bookings with specific status in a lot (e.g. CHECKED_IN = live parked cars)
    List<Booking> findByLotIdAndStatusAndIsActiveTrueOrderByCreatedAtDesc(UUID lotId, BookingStatus status);

    // 5. Check if spot currently has an ongoing active booking (CONFIRMED or CHECKED_IN)
    boolean existsBySpotIdAndStatusInAndIsActiveTrue(UUID spotId, Collection<BookingStatus> statuses);

    // 6. Real-time count of active bookings by status in a lot
    long countByLotIdAndStatusAndIsActiveTrue(UUID lotId, BookingStatus status);

    // 7. Time-range collision check: Detects overlapping scheduled reservations for the same spot
    @Query("""
        SELECT COUNT(b) > 0 FROM Booking b
        WHERE b.spotId = :spotId
          AND b.isActive = true
          AND b.status IN :activeStatuses
          AND b.reservedFrom < :reservedTo
          AND b.reservedTo > :reservedFrom
    """)
    boolean hasOverlappingReservation(
            @Param("spotId") UUID spotId,
            @Param("reservedFrom") LocalDateTime reservedFrom,
            @Param("reservedTo") LocalDateTime reservedTo,
            @Param("activeStatuses") Collection<BookingStatus> activeStatuses
    );
}
