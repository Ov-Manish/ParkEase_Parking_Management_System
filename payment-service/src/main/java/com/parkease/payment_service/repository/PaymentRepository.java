package com.parkease.payment_service.repository;

import com.parkease.payment_service.entity.Payment;
import com.parkease.payment_service.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByTransactionReference(String transactionReference);

    Optional<Payment> findByBookingId(UUID bookingId);

    Optional<Payment> findByGatewayPaymentId(String gatewayPaymentId);

    Optional<Payment> findByGatewayOrderId(String gatewayOrderId);

    List<Payment> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<Payment> findByBookingIdOrderByCreatedAtDesc(UUID bookingId);

    List<Payment> findByStatus(PaymentStatus status);
}
