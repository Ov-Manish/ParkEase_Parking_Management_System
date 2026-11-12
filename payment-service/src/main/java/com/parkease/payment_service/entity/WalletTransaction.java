package com.parkease.payment_service.entity;

import com.parkease.payment_service.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "wallet_transactions",
        indexes = {
                @Index(name = "idx_wtx_wallet_id", columnList = "wallet_id"),
                @Index(name = "idx_wtx_user_id", columnList = "user_id"),
                @Index(name = "idx_wtx_created_at", columnList = "created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "transaction_id", updatable = false, nullable = false)
    private UUID transactionId;

    @Column(name = "wallet_id", nullable = false)
    private UUID walletId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private TransactionType type;

    // Transaction amount
    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    // Snapshot of balance before this transaction
    @Column(name = "balance_before", nullable = false, precision = 10, scale = 2)
    private BigDecimal balanceBefore;

    // Snapshot of balance after this transaction
    @Column(name = "balance_after", nullable = false, precision = 10, scale = 2)
    private BigDecimal balanceAfter;

    // External reference (e.g. Booking ID or Top-up Gateway Payment ID)
    @Column(name = "reference_id", length = 100)
    private String referenceId;

    // Human-readable summary (e.g. "Parking Fee for Booking PE-BK-20261112-9A3F", "Wallet Top-Up via UPI")
    @Column(name = "description", length = 255)
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
