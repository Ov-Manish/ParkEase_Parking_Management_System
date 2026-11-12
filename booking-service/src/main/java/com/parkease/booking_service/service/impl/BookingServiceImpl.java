package com.parkease.booking_service.service.impl;


import com.parkease.booking_service.client.ParkingLotClient;
import com.parkease.booking_service.client.SpotClient;
import com.parkease.booking_service.client.VehicleClient;
import com.parkease.booking_service.dto.request.BookingRequest;
import com.parkease.booking_service.dto.response.BookingResponse;
import com.parkease.booking_service.dto.response.CheckInResponse;
import com.parkease.booking_service.dto.response.CheckOutResponse;
import com.parkease.booking_service.entity.Booking;
import com.parkease.booking_service.enums.BookingStatus;
import com.parkease.booking_service.exception.BadRequestException;
import com.parkease.booking_service.exception.ConflictException;
import com.parkease.booking_service.exception.ResourceNotFoundException;
import com.parkease.booking_service.repository.BookingRepository;
import com.parkease.booking_service.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final SpotClient spotClient;
    private final ParkingLotClient parkingLotClient;
    private final VehicleClient vehicleClient;

    private static final Set<BookingStatus> ACTIVE_STATUSES = Set.of(
            BookingStatus.CONFIRMED,
            BookingStatus.CHECKED_IN
    );

    @Override
    @Transactional
    public BookingResponse createResponse(BookingRequest request, UUID userId, String jwtToken) {
        log.info("Processing reservation request for user: {}, spot: {}", userId, request.getSpotId());

//        Validate that the end time is strictly after the start time
        if (!request.getReservedTo().isAfter(request.getReservedFrom())){
            throw new BadRequestException("Reservation end time must be after the start time");
        }

//        Cross-service validation: Ensure the spot belongs to the specified lot and is active
        spotClient.verifySpotBelongsToLot(request.getSpotId(),request.getLotId(),jwtToken);

//        Collision check: Ensure no overlapping active bookings exist for this spot
        boolean hasOverlap = bookingRepository.hasOverlappingReservation(
                request.getSpotId(),
                request.getReservedFrom(),
                request.getReservedTo(),
                ACTIVE_STATUSES
        );

        if (hasOverlap) {
            throw new ConflictException("The selected spot is already reserved for the requested time window");
        }

//      Cross-service fetch: Snapshot the current hourly rate from parkinglot-service
        BigDecimal pricePerHour = parkingLotClient.getHourlyRate(request.getLotId() , jwtToken);

//      Cross-service fetch: Verify vehicle ownership and snapshot the license plate string
        String licensePlate = vehicleClient.getLicensePlateAndVerifyOwner(request.getVehicleId(),userId , jwtToken);

//      Generate a human-readable booking ticket reference
        String bookingTicketReference = bookingReferance();

//      Construct and persist the Booking entity
        Booking booking = Booking.builder()
                .bookingReference(bookingTicketReference)
                .userId(userId)
                .lotId(request.getLotId())
                .spotId(request.getSpotId())
                .vehicleId(request.getVehicleId())
                .licensePlate(licensePlate)
                .bookingType(request.getBookingType())
                .status(BookingStatus.CONFIRMED)
                .reservedFrom(request.getReservedFrom())
                .reservedTo(request.getReservedTo())
                .pricePerHour(pricePerHour)
                .penaltyAmount(BigDecimal.ZERO)
                .isActive(true)
                .build();

        Booking savedBooking = bookingRepository.save(booking);
        log.info("Booking created successfully with reference: {}", bookingTicketReference);
        return mapToResponse(savedBooking);

    }

    @Override
    @Transactional
    public CheckInResponse checkIn(UUID bookingId, UUID userId, String jwtToken) {
        log.info("Processing check-in for booking ID: {} by user: {}", bookingId, userId);

        Booking booking = findBookingByIdOrThrow(bookingId);

//     Verify that the caller is the owner of the booking
        if (!booking.getUserId().equals(userId)){
            throw new BadRequestException("You are not authorized to check in for this booking");
        }

//        Ensure the booking is in confirm state
        if (booking.getStatus() != BookingStatus.CONFIRMED){
            throw new BadRequestException("Cannot check in. Current booking status is: " + booking.getStatus());
        }

//      Set the arrival gate timestamp and update status
        booking.setCheckInTime(LocalDateTime.now());
        booking.setStatus(BookingStatus.CHECKED_IN);


        Booking updatedBooking = bookingRepository.save(booking);
        spotClient.updateSpotStatus(booking.getSpotId(),"OCCUPIED",jwtToken);

        log.info("Check-in successful for booking: {}", booking.getBookingReference());

        return CheckInResponse.builder()
                .bookingId(updatedBooking.getBookingId())
                .bookingReference(updatedBooking.getBookingReference())
                .status(updatedBooking.getStatus())
                .checkInTime(updatedBooking.getCheckInTime())
                .spotId(updatedBooking.getSpotId())
                .message("Check-in successful. Gate barrier opened.")
                .build();
    }

    @Override
    @Transactional
    public CheckOutResponse checkOut(UUID bookingId, UUID userId, String jwtToken) {
        log.info("Processing check-out for booking ID: {} by user: {}", bookingId, userId);

        Booking booking = findBookingByIdOrThrow(bookingId);

//      Verify that the caller is the owner of the booking
        if (!booking.getUserId().equals(userId)) {
            throw new BadRequestException("You are not authorized to check out for this booking");
        }

//        Ensure Fehcile is CheckedIn
        if (booking.getStatus() != BookingStatus.CHECKED_IN){
            throw new BadRequestException("Cannot check out. Booking is not in CHECKED_IN status (current: " + booking.getStatus() + ")");
        }

        LocalDateTime checkOutTime = LocalDateTime.now();
        booking.setCheckOutTime(checkOutTime);


//        Calculate the elapsed time in a minute
        long durationMinuteTime = Duration.between(booking.getCheckInTime(),checkOutTime).toMinutes();
        if (durationMinuteTime < 0) durationMinuteTime = 0;

//        Ceiling rounding for billed hours (minimum 1 hour charge)
        long billedHours  = Math.max(1,(long) Math.ceil(durationMinuteTime / 60.0));

//      Calculate base fare: billedHours * pricePerHour
        BigDecimal baseFair = booking.getPricePerHour().multiply(BigDecimal.valueOf(billedHours));

//        Overstay penalty calculation: if checkout is after reservedTo, apply 50% penalty on extra hours
        BigDecimal penaltyAmount = BigDecimal.ZERO;
        if (checkOutTime.isAfter(booking.getReservedTo())) {
            long overstayMinutes = Duration.between(booking.getReservedTo(), checkOutTime).toMinutes();

            long overStayHours = (long) Math.ceil(overstayMinutes / 60.0);

            if (overStayHours > 0) {
//                Penlaty rate = 1.5 X hourly rate
                BigDecimal penaltyRatePerHour = booking.getPricePerHour().multiply(BigDecimal.valueOf(1.5));
                penaltyAmount = penaltyRatePerHour.multiply(BigDecimal.valueOf(overStayHours));
            }
        }

            BigDecimal totalAmount = baseFair.add(penaltyAmount).setScale(2,RoundingMode.HALF_UP);
            booking.setBaseFare(baseFair.setScale(2,RoundingMode.HALF_UP));
            booking.setPenaltyAmount(penaltyAmount.setScale(2,RoundingMode.HALF_UP));
            booking.setTotalAmount(totalAmount);
            booking.setStatus(BookingStatus.COMPLETED);

            Booking completedBooking = bookingRepository.save(booking);

//            Releasing the Spot from reserved to available
            spotClient.updateSpotStatus(booking.getSpotId(),"AVAILABLE", jwtToken);

            log.info("Check-out completed for booking: {}. Total amount billed: {}", booking.getBookingReference(), totalAmount);

            return CheckOutResponse.builder()
                    .bookingId(completedBooking.getBookingId())
                    .bookingReference(completedBooking.getBookingReference())
                    .status(completedBooking.getStatus())
                    .checkInTime(completedBooking.getCheckInTime())
                    .checkOutTime(completedBooking.getCheckOutTime())
                    .durationMinutes((double) durationMinuteTime)
                    .billedHours(billedHours)
                    .pricePerHour(completedBooking.getPricePerHour())
                    .baseFare(completedBooking.getBaseFare())
                    .penaltyAmount(completedBooking.getPenaltyAmount())
                    .totalAmount(completedBooking.getTotalAmount())
                    .message("Check-out completed. Final invoice generated.")
                    .build();

    }

    @Override
    @Transactional
    public BookingResponse cancelBooking(UUID bookingId, UUID userId, String jwtToken) {
        log.info("Processing cancellation for booking ID: {} by user: {}", bookingId, userId);


        Booking booking = findBookingByIdOrThrow(bookingId);
        if (!booking.getUserId().equals(userId)){
            throw new BadRequestException("You are not authorized to cancel this booking");
        }

//        only confirmed booking cancels

        if (booking.getStatus() != BookingStatus.CONFIRMED){
            throw new BadRequestException("Cannot cancel booking. Only CONFIRMED bookings can be cancelled (current status: " + booking.getStatus() + ")");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancellationReason("Cancelled by driver");

        Booking cancelledBooking = bookingRepository.save(booking);

        spotClient.updateSpotStatus(booking.getSpotId() , "AVAILABLE", jwtToken);

        return mapToResponse(cancelledBooking);

    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingById(UUID bookingId, UUID userId, String role) {

        Booking booking = findBookingByIdOrThrow(bookingId);

//        ONLY Admins and Lot Managers can view any booking.
        if ("ROLE_DRIVER".equalsIgnoreCase(role) && !booking.getUserId().equals(userId)){
            throw new BadRequestException("Access denied. You do not own this booking");
        }
        return mapToResponse(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingByReference(String bookingReference) {
        Booking booking = bookingRepository.findByBookingReferenceIgnoreCaseAndIsActiveTrue(bookingReference)
                .orElseThrow(()-> new ResourceNotFoundException("Booking not found with reference: " + bookingReference));
        return  mapToResponse(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getUserBookings(UUID userId) {
        return bookingRepository.findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getLotBookings(UUID lotId) {
        return bookingRepository.findByLotIdAndIsActiveTrueOrderByCreatedAtDesc(lotId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<BookingResponse> getActiveBookingsForLot(UUID lotId) {
        return bookingRepository.findByLotIdAndStatusAndIsActiveTrueOrderByCreatedAtDesc(lotId,BookingStatus.CHECKED_IN)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    //    HELPER METHODS

//    Find Booking OR Throw ERROR
    private Booking findBookingByIdOrThrow(UUID bookingId){
        return bookingRepository.findById(bookingId)
                .filter(Booking::getIsActive)
                .orElseThrow(()-> new ResourceNotFoundException("Booking Not Found with ID : " +bookingId));
    }

//    Generating  unique human-readable ticket reference e.g. "PE-BK-20261111-8F2A"

    private String bookingReferance(){
        String datePrefix  = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomSuffix = UUID.randomUUID().toString().substring(0,6).toUpperCase();

        return "PE-BK-"+ datePrefix + "-" + randomSuffix;
    }

    private BookingResponse mapToResponse(Booking request){
        return BookingResponse.builder()
                .bookingId(request.getBookingId())
                .bookingReference(request.getBookingReference())
                .userId(request.getUserId())
                .lotId(request.getLotId())
                .spotId(request.getSpotId())
                .vehicleId(request.getVehicleId())
                .licensePlate(request.getLicensePlate())
                .bookingType(request.getBookingType())
                .status(request.getStatus())
                .reservedFrom(request.getReservedFrom())
                .reservedTo(request.getReservedTo())
                .checkInTime(request.getCheckInTime())
                .checkOutTime(request.getCheckOutTime())
                .pricePerHour(request.getPricePerHour())
                .baseFare(request.getBaseFare())
                .penaltyAmount(request.getPenaltyAmount())
                .totalAmount(request.getTotalAmount())
                .cancellationReason(request.getCancellationReason())
                .isActive(request.getIsActive())
                .version(request.getVersion())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .build();
    }
}
