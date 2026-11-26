package com.parkease.payment_service.service;

import com.parkease.payment_service.dto.request.PaymentInitiateRequest;
import com.parkease.payment_service.dto.request.PaymentVerifyRequest;
import com.parkease.payment_service.dto.request.RefundRequest;
import com.parkease.payment_service.dto.response.PaymentResponse;

import java.util.List;
import java.util.UUID;

public interface PaymentService {

    /**
     * Initiates and settles a payment for a booking using Wallet, Gateway (Card/UPI), or Cash.
     */
    PaymentResponse processPayment(UUID userId, PaymentInitiateRequest request, String jwtToken);

    /**
     * Verifies cryptographic signature for gateway (Razorpay/Stripe) payments.
     */
    PaymentResponse verifyPayment(PaymentVerifyRequest request);

    /**
     * Issues a full or partial refund back to the driver's wallet or bank source.
     */
    PaymentResponse refundPayment(UUID userId, String role, RefundRequest request);

    /**
     * Retrieves payment receipt by ID with ownership enforcement.
     */
    PaymentResponse getPaymentById(UUID paymentId, UUID userId, String role);

    /**
     * Retrieves payment receipt for a specific parking booking.
     */
    PaymentResponse getPaymentByBookingId(UUID bookingId, UUID userId, String role);

    /**
     * Retrieves all payment receipts belonging to the authenticated driver.
     */
    List<PaymentResponse> getUserPayments(UUID userId);

    public void autoRefundForCancelledBooking(UUID bookingId , UUID actorId );
}