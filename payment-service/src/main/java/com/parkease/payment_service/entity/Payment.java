package com.parkease.payment_service.entity;

import com.parkease.payment_service.enums.PaymentMethod;
import com.parkease.payment_service.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "payments",
        indexes = {
                @Index(name = "idx_payment_booking", columnList = "booking_id"),
                @Index(name = "idx_payment_user", columnList = "user_id"),
                @Index(name = "idx_payment_status", columnList = "status"),
                @Index(name = "idx_payment_txn_ref", columnList = "transaction_reference", unique = true)
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "payment_id", updatable = false, nullable = false)
    private UUID paymentId;

    // Foreign reference to booking in booking-service
    @Column(name = "booking_id", nullable = false)
    private UUID bookingId;

    // Driver / User ID who made the payment
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    // Total amount charged
    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    // Amount refunded back to the user (if cancelled or overpaid)
    @Builder.Default
    @Column(name = "refund_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal refundAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PaymentStatus status;

    // Human-readable transaction reference e.g. "PE-TXN-20261112-8A2F"
    @Column(name = "transaction_reference", nullable = false, unique = true, length = 50)
    private String transactionReference;

    // Gateway-specific identifier (e.g. Razorpay payment ID "pay_xxx", Stripe charge ID "ch_xxx")
    @Column(name = "gateway_payment_id", length = 100)
    private String gatewayPaymentId;

    // Gateway order ID (e.g. Razorpay order ID "order_xxx")
    @Column(name = "gateway_order_id", length = 100)
    private String gatewayOrderId;

    // Gateway signature for webhook / callback verification
    @Column(name = "gateway_signature", length = 255)
    private String gatewaySignature;

    // Reason if payment fails or is rejected
    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    // Optimistic locking version
    @Version
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
