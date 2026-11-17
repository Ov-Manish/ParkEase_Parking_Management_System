package com.parkease.payment_service.controller;

import com.parkease.payment_service.dto.request.PaymentInitiateRequest;
import com.parkease.payment_service.dto.request.PaymentVerifyRequest;
import com.parkease.payment_service.dto.request.RefundRequest;
import com.parkease.payment_service.dto.response.ApiResponse;
import com.parkease.payment_service.dto.response.PaymentResponse;
import com.parkease.payment_service.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payment Management", description = "Endpoints for booking payment checkout, gateway callbacks, refunds, and receipts")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final PaymentService paymentService;

    // 1. Initiate and process payment for a parking booking
    @PostMapping("/initiate")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Initiate Booking Payment", description = "Settles parking fee via Wallet, Gateway (UPI/Card), or Cash with double-payment prevention.")
    public ResponseEntity<ApiResponse<PaymentResponse>> initiatePayment(
            @Valid @RequestBody PaymentInitiateRequest request,
            @RequestHeader("Authorization") String authHeader,
            Authentication authentication) {
        UUID userId = extractUserId(authentication);
        log.info("REST request to pay for booking: {} by user: {}", request.getBookingId(), userId);

        PaymentResponse response = paymentService.processPayment(userId, request, authHeader);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payment processed successfully", response));
    }

    // 2. Gateway signature verification (Callbacks from Razorpay/Stripe)
    @PostMapping("/verify")
    @Operation(summary = "Verify Gateway Signature", description = "Verifies cryptographic signature sent after user pays on external gateway.")
    public ResponseEntity<ApiResponse<PaymentResponse>> verifyPayment(@Valid @RequestBody PaymentVerifyRequest request) {
        log.info("REST request to verify payment for reference: {}", request.getTransactionReference());

        PaymentResponse response = paymentService.verifyPayment(request);
        return ResponseEntity.ok(ApiResponse.success("Payment verification processed", response));
    }

    // 3. Process refund on booking cancellation
    @PostMapping("/refund")
    @PreAuthorize("hasAnyRole('DRIVER', 'LOT_MANAGER', 'ADMIN')")
    @Operation(summary = "Refund Payment", description = "Processes full or partial refund back to wallet or bank source.")
    public ResponseEntity<ApiResponse<PaymentResponse>> refundPayment(
            @Valid @RequestBody RefundRequest request,
            Authentication authentication) {
        UUID userId = extractUserId(authentication);
        String role = extractRole(authentication);
        log.info("REST request to refund payment: {} by user: {} (Role: {})", request.getPaymentId(), userId, role);

        PaymentResponse response = paymentService.refundPayment(userId, role, request);
        return ResponseEntity.ok(ApiResponse.success("Refund processed successfully", response));
    }

    // 4. Get payment receipt by payment ID
    @GetMapping("/{paymentId}")
    @PreAuthorize("hasAnyRole('DRIVER', 'LOT_MANAGER', 'ADMIN')")
    @Operation(summary = "Get Payment Receipt by ID", description = "Retrieves payment details with ownership enforcement.")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentById(
            @PathVariable UUID paymentId,
            Authentication authentication) {
        UUID userId = extractUserId(authentication);
        String role = extractRole(authentication);

        PaymentResponse response = paymentService.getPaymentById(paymentId, userId, role);
        return ResponseEntity.ok(ApiResponse.success("Payment retrieved successfully", response));
    }

    // 5. Get payment receipt for a specific parking booking
    @GetMapping("/booking/{bookingId}")
    @PreAuthorize("hasAnyRole('DRIVER', 'LOT_MANAGER', 'ADMIN')")
    @Operation(summary = "Get Payment Receipt for Booking", description = "Retrieves payment receipt associated with a booking.")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentByBookingId(
            @PathVariable UUID bookingId,
            Authentication authentication) {
        UUID userId = extractUserId(authentication);
        String role = extractRole(authentication);

        PaymentResponse response = paymentService.getPaymentByBookingId(bookingId, userId, role);
        return ResponseEntity.ok(ApiResponse.success("Booking payment retrieved successfully", response));
    }

    // 6. Get all payments made by authenticated driver
    @GetMapping("/my-payments")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Get My Payments", description = "Lists all historical payment receipts for the logged-in driver.")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getMyPayments(Authentication authentication) {
        UUID userId = extractUserId(authentication);
        log.info("REST request to get all payments for driver: {}", userId);

        List<PaymentResponse> responses = paymentService.getUserPayments(userId);
        return ResponseEntity.ok(ApiResponse.success("Driver payments retrieved successfully", responses));
    }

    // --- Helper Methods ---

    private UUID extractUserId(Authentication authentication) {
        return (UUID) authentication.getPrincipal();
    }

    private String extractRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse("ROLE_DRIVER");
    }
}
