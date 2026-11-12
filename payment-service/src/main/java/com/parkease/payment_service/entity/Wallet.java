package com.parkease.payment_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "wallets",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_wallet_user_id", columnNames = {"user_id"})
        },
        indexes = {
                @Index(name = "idx_wallet_user", columnList = "user_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "wallet_id", updatable = false, nullable = false)
    private UUID walletId;

    // Driver's user UUID from auth-service (1-to-1 relationship with user)
    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    // Current available wallet balance
    @Builder.Default
    @Column(name = "balance", nullable = false, precision = 10, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    // Whether wallet is active or frozen/suspended
    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    // Optimistic locking version: Prevents race conditions and double-spending
    @Version
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
