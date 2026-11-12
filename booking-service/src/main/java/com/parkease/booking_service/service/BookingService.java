package com.parkease.booking_service.service;

import com.parkease.booking_service.dto.request.BookingRequest;
import com.parkease.booking_service.dto.response.BookingResponse;
import com.parkease.booking_service.dto.response.CheckInResponse;
import com.parkease.booking_service.dto.response.CheckOutResponse;

import java.util.List;
import java.util.UUID;

public interface BookingService {

    BookingResponse createResponse(BookingRequest request , UUID userId, String jwtToken);

    CheckInResponse checkIn(UUID bookingId,UUID userId,String jwtToken);

    CheckOutResponse checkOut(UUID bookingId,UUID userId, String jwtToken);

    BookingResponse cancelBooking(UUID bookingId, UUID userId , String jwtToken);

    BookingResponse getBookingById(UUID bookingId, UUID userId , String role);

//    fetching booking by ticket code
    BookingResponse getBookingByReference(String bookingReferance);

    List<BookingResponse> getUserBookings(UUID userId);

    List<BookingResponse> getLotBookings(UUID lotId);

//    Fetching Active bookings (CONFIRMED OR CHECKED_IN) for a lot

    List<BookingResponse> getActiveBookingsForLot(UUID lotId);


}
