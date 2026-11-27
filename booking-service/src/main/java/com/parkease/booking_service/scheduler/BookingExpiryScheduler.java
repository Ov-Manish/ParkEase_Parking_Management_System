package com.parkease.booking_service.scheduler;

import com.parkease.booking_service.client.SpotClient;
import com.parkease.booking_service.client.UserClient;
import com.parkease.booking_service.entity.Booking;
import com.parkease.booking_service.enums.BookingStatus;
import com.parkease.booking_service.producer.BookingEventProducer;
import com.parkease.booking_service.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingExpiryScheduler {

    private final BookingRepository bookingRepository;
    private final SpotClient spotClient;
    private final UserClient userClient;
    private final BookingEventProducer bookingEventProducer;

    @Value("${parkease.booking.grace-period-minutes:15}")
    private int gracePeriodMinutes;

    /**
     * Sweeps the database every 60 seconds for CONFIRMED reservations where
     * the driver failed to check in within the 15-minute grace period.
     */
    @Scheduled(fixedDelayString = "${parkease.booking.expiry-check-delay-ms:60000}")
    @Transactional
    public void sweepExpiredBookings() {

        Instant paymentCutOff = Instant.now().minus(10, ChronoUnit.MINUTES);

        List<Booking> unPaidBoking = bookingRepository.findByStatusAndCreatedAtBeforeAndIsActiveTrue(
                BookingStatus.PAYMENT_PENDING, paymentCutOff
        );

        for (Booking booking : unPaidBoking){
            try {
                booking.setStatus(BookingStatus.EXPIRED);
                booking.setCancellationReason("Payment timeout: pre-booking not paid within 10 minutes");
                bookingRepository.save(booking);
                spotClient.releaseSpot(booking.getSpotId());

                log.info("[PAYMENT TIMEOUT ENGINE] Expired unpaid booking: {}", booking.getBookingReference());
            }catch (Exception ex){
                log.error("[PAYMENT TIMEOUT ERROR] Failed to expire unpaid booking {}: {}",
                        booking.getBookingId(), ex.getMessage(), ex);
            }
        }


        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(gracePeriodMinutes);
        List<Booking> expiredBookings = bookingRepository.findExpiredBookings(BookingStatus.CONFIRMED, cutoffTime);

        if (expiredBookings.isEmpty()) {
            return;
        }

        log.info("[NO-SHOW EXPIRY ENGINE] Found {} expired booking(s) exceeding {}-min grace period.",
                expiredBookings.size(), gracePeriodMinutes);

        for (Booking booking : expiredBookings) {
            try {
                // 1. Mark booking as EXPIRED
                booking.setStatus(BookingStatus.EXPIRED);
                booking.setCancellationReason(String.format("No-show: check-in grace period exceeded (%d minutes)", gracePeriodMinutes));
                Booking savedBooking = bookingRepository.save(booking);

                // 2. Release spot back to AVAILABLE in spot-service
                spotClient.releaseSpot(booking.getSpotId());
                String spotNumber = spotClient.getSpotNumber(booking.getSpotId(), null);

                // 3. Lookup driver email for notification
                String userEmail = userClient.getUserEmail(booking.getUserId());

                // 4. Publish cancellation event to RabbitMQ
                bookingEventProducer.publishBookingCancelled(savedBooking, spotNumber, userEmail);

                log.info("[NO-SHOW EXPIRY ENGINE] Expired Booking: {} (Spot {} released back to AVAILABLE)",
                        booking.getBookingReference(), spotNumber);
            } catch (Exception ex) {
                log.error("[NO-SHOW EXPIRY ENGINE ERROR] Failed to expire booking {}: {}",
                        booking.getBookingId(), ex.getMessage(), ex);
            }
        }
    }
}