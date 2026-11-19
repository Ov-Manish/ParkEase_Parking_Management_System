package com.parkease.payment_service.service.impl;

import com.parkease.payment_service.client.BookingClient;
import com.parkease.payment_service.dto.request.PaymentInitiateRequest;
import com.parkease.payment_service.dto.request.PaymentVerifyRequest;
import com.parkease.payment_service.dto.request.RefundRequest;
import com.parkease.payment_service.dto.response.PaymentResponse;
import com.parkease.payment_service.entity.Payment;
import com.parkease.payment_service.enums.PaymentMethod;
import com.parkease.payment_service.enums.PaymentStatus;
import com.parkease.payment_service.exception.BadRequestException;
import com.parkease.payment_service.exception.ConflictException;
import com.parkease.payment_service.exception.ResourceNotFoundException;
import com.parkease.payment_service.gateway.PaymentGateway;
import com.parkease.payment_service.gateway.dto.GatewayOrderResponse;
import com.parkease.payment_service.gateway.dto.GatewayRefundResponse;
import com.parkease.payment_service.producer.PaymentEventProducer;
import com.parkease.payment_service.repository.PaymentRepository;
import com.parkease.payment_service.service.PaymentService;
import com.parkease.payment_service.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final WalletService walletService;
    private final PaymentGateway paymentGateway;
    private final BookingClient bookingClient;
    private final PaymentEventProducer paymentEventProducer;

    @Override
    @Transactional
    public PaymentResponse processPayment(UUID userId, PaymentInitiateRequest request, String jwtToken) {
            log.info("Initiating payment for booking: {} by user: {} using method: {}",
                    request.getBookingId(), userId, request.getPaymentMethod());

            // 1. Verify booking with booking-service (checks existence, ownership, and non-cancelled status)
            BigDecimal billedAmount = bookingClient.verifyBookingAndGetAmount(request.getBookingId(),userId,jwtToken);

            // 2. Prevent Double Payment (Check if booking is already paid or refunded)
            paymentRepository.findByBookingIdOrderByCreatedAtDesc(request.getBookingId()).stream().findFirst().ifPresent(existingPayment -> {
                if (existingPayment.getStatus() == PaymentStatus.COMPLETED) {
                    throw new ConflictException("Booking " + request.getBookingId() + " has already been paid for. Ref: "
                            + existingPayment.getTransactionReference());
                }
                if (existingPayment.getStatus() == PaymentStatus.REFUNDED || existingPayment.getStatus() == PaymentStatus.PARTIALLY_REFUNDED) {
                    throw new ConflictException("Booking " + request.getBookingId() + " has already been refunded (Status: "
                            + existingPayment.getStatus() + ") and cannot be re-paid. Ref: "
                            + existingPayment.getTransactionReference());
                }
            });

            // 3. Generate unique, human-readable Transaction Reference (e.g. PE-TXN-20261117-A1B2C3D4)

            String datePart = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
            String randomPart = UUID.randomUUID().toString().substring(0,8).toUpperCase();
            String txnReference  = "PE-TXN-" + datePart + "-" + randomPart;

            Payment payment = Payment.builder()
                    .bookingId(request.getBookingId())
                    .userId(userId)
                    .amount(request.getAmount())
                    .refundAmount(BigDecimal.ZERO)
                    .paymentMethod(request.getPaymentMethod())
                    .transactionReference(txnReference)
                    .build();

            // 4. Route payment according to funding source

            if (request.getPaymentMethod() == PaymentMethod.WALLET){
                // Instant in-app wallet deduction
                walletService.debitWallet(
                        userId,
                        request.getAmount(),
                        txnReference,
                        "Parking Fee for Booking " + request.getBookingId()
                );
                payment.setStatus(PaymentStatus.COMPLETED);
                payment.setGatewayPaymentId("WALLET_" + randomPart);
                log.info("Wallet payment COMPLETED for booking: {}", request.getBookingId());
            } else if (request.getPaymentMethod() == PaymentMethod.CASH) {
    //            Cash collected at gate by attendan
                payment.setStatus(PaymentStatus.COMPLETED);
                payment.setGatewayPaymentId("CASH_"+randomPart);
                log.info("Cash payment recorded for booking: {}", request.getBookingId());
            }else {
                GatewayOrderResponse orderResponse = paymentGateway.createOrder(
                        request.getAmount(),
                        "INR",
                        txnReference
                );

                payment.setGatewayOrderId(orderResponse.getOrderId());

                // In MOCK gateway mode: Auto-complete for seamless testing
                payment.setGatewayPaymentId("mock_pay_"+UUID.randomUUID().toString().substring(0,10));

                payment.setStatus(PaymentStatus.COMPLETED);
                log.info("Gateway payment COMPLETED (Mock) for booking: {}. OrderId: {}",
                        request.getBookingId(), orderResponse.getOrderId());
            }
        Payment savedPayment = paymentRepository.save(payment);

        // Publish asynchronous payment success event to RabbitMQ
        paymentEventProducer.publishPaymentSuccess(savedPayment, null, null);

        return mapToPaymentResponse(savedPayment);

    }

    @Override
    @Transactional
    public PaymentResponse verifyPayment(PaymentVerifyRequest request) {
        log.info("Verifying gateway signature for transaction: {}", request.getTransactionReference());
        Payment payment = paymentRepository.findByTransactionReference(request.getTransactionReference())
                .orElseThrow(()-> new ResourceNotFoundException("Payment not found with reference: " + request.getTransactionReference()));

        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            return mapToPaymentResponse(payment);
        }

        boolean isValid  = paymentGateway.verifyPaymentSignature(
                request.getGatewayPaymentId(),
                request.getGatewayOrderId(),
                request.getGatewaySignature()
        );

        if (isValid){
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setGatewayPaymentId(request.getGatewayPaymentId());
            payment.setGatewaySignature(request.getGatewaySignature());

            log.info("Payment signature verified successfully for ref: {}", request.getTransactionReference());

        }else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Invalid gateway signature verification failed");
            log.warn("Payment signature verification failed for ref: {}", request.getTransactionReference());
        }
        Payment savedPayment = paymentRepository.save(payment);

        if (savedPayment.getStatus() == PaymentStatus.COMPLETED) {
            paymentEventProducer.publishPaymentSuccess(savedPayment, null, null);
        } else {
            paymentEventProducer.publishPaymentFailed(savedPayment, null, null, savedPayment.getFailureReason());
        }

        return mapToPaymentResponse(savedPayment);
    }

    @Override
    @Transactional
    public PaymentResponse refundPayment(UUID userId, String role, RefundRequest request) {
        log.info("Processing refund for paymentId: {} by user: {} (Role: {})", request.getPaymentId(), userId, role);
        Payment payment = paymentRepository.findById(request.getPaymentId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with ID: " + request.getPaymentId()));
        // Authorization: Only Admin or Lot Manager can initiate refund
        boolean isPrivileged = "ROLE_ADMIN".equalsIgnoreCase(role) || "ROLE_LOT_MANAGER".equalsIgnoreCase(role);
        if (!isPrivileged) {
            throw new BadRequestException("Access denied. Only LOT_MANAGER or ADMIN can process refunds.");
        }
        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            throw new ConflictException("Payment " + payment.getPaymentId() + " has already been fully refunded.");
        }
        if (payment.getStatus() != PaymentStatus.COMPLETED && payment.getStatus() != PaymentStatus.PARTIALLY_REFUNDED) {
            throw new BadRequestException("Cannot refund a payment with status: " + payment.getStatus());
        }
        // Validate refund amount does not exceed remaining refundable balance
        BigDecimal alreadyRefunded = payment.getRefundAmount() != null ? payment.getRefundAmount() : BigDecimal.ZERO;
        BigDecimal maxRefundable = payment.getAmount().subtract(alreadyRefunded);
        if (request.getRefundAmount().compareTo(maxRefundable) > 0) {
            throw new BadRequestException("Refund amount " + request.getRefundAmount()
                    + " exceeds remaining refundable balance: " + maxRefundable);
        }
        // Process refund according to original payment method
        String refundRef = "RFND-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        if (payment.getPaymentMethod() == PaymentMethod.WALLET) {
            walletService.creditRefund(
                    payment.getUserId(),
                    request.getRefundAmount(),
                    refundRef,
                    request.getReason() != null ? request.getReason() : "Booking cancellation refund"
            );
        } else {
            GatewayRefundResponse refundResponse = paymentGateway.processRefund(
                    payment.getGatewayPaymentId(),
                    request.getRefundAmount(),
                    request.getReason()
            );
            log.info("Gateway refund executed. Gateway Refund ID: {}", refundResponse.getRefundId());
        }
        // Update payment refund tracking
        BigDecimal newTotalRefunded = alreadyRefunded.add(request.getRefundAmount());
        payment.setRefundAmount(newTotalRefunded);
        if (newTotalRefunded.compareTo(payment.getAmount()) >= 0) {
            payment.setStatus(PaymentStatus.REFUNDED);
        } else {
            payment.setStatus(PaymentStatus.PARTIALLY_REFUNDED);
        }
        Payment savedPayment = paymentRepository.save(payment);

        // Publish asynchronous payment refund event to RabbitMQ
        paymentEventProducer.publishPaymentRefunded(savedPayment, null, null);

        return mapToPaymentResponse(savedPayment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(UUID paymentId, UUID userId, String role) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with ID: " + paymentId));
        boolean isOwner = payment.getUserId().equals(userId);
        boolean isPrivileged = "ROLE_ADMIN".equalsIgnoreCase(role) || "ROLE_LOT_MANAGER".equalsIgnoreCase(role);
        if (!isOwner && !isPrivileged) {
            throw new BadRequestException("Access denied. You do not own this payment receipt.");
        }
        return mapToPaymentResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByBookingId(UUID bookingId, UUID userId, String role) {
        Payment payment = paymentRepository.findByBookingIdOrderByCreatedAtDesc(bookingId).stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No payment found for booking: " + bookingId));
        boolean isOwner = payment.getUserId().equals(userId);
        boolean isPrivileged = "ROLE_ADMIN".equalsIgnoreCase(role) || "ROLE_LOT_MANAGER".equalsIgnoreCase(role);
        if (!isOwner && !isPrivileged) {
            throw new BadRequestException("Access denied. You do not own this payment receipt.");
        }
        return mapToPaymentResponse(payment);
    }
    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getUserPayments(UUID userId) {
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToPaymentResponse)
                .toList();
    }

    private PaymentResponse mapToPaymentResponse(Payment payment) {
        return PaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .bookingId(payment.getBookingId())
                .userId(payment.getUserId())
                .amount(payment.getAmount())
                .refundAmount(payment.getRefundAmount())
                .paymentMethod(payment.getPaymentMethod())
                .status(payment.getStatus())
                .transactionReference(payment.getTransactionReference())
                .gatewayPaymentId(payment.getGatewayPaymentId())
                .gatewayOrderId(payment.getGatewayOrderId())
                .failureReason(payment.getFailureReason())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}
