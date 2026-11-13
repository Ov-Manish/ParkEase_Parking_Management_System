package com.parkease.payment_service.repository;

import com.parkease.payment_service.entity.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, UUID> {

    List<WalletTransaction> findByWalletIdOrderByCreatedAtDesc(UUID walletId);

    List<WalletTransaction> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<WalletTransaction> findByReferenceId(String referenceId);
}
