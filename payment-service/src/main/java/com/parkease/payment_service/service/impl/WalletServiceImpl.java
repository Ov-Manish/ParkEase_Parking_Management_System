package com.parkease.payment_service.service.impl;


import com.parkease.payment_service.dto.request.WalletTopUpRequest;
import com.parkease.payment_service.dto.response.WalletResponse;
import com.parkease.payment_service.dto.response.WalletTransactionResponse;
import com.parkease.payment_service.entity.Wallet;
import com.parkease.payment_service.entity.WalletTransaction;
import com.parkease.payment_service.enums.TransactionType;
import com.parkease.payment_service.exception.BadRequestException;
import com.parkease.payment_service.exception.InsufficientBalanceException;
import com.parkease.payment_service.exception.ResourceNotFoundException;
import com.parkease.payment_service.repository.WalletRepository;
import com.parkease.payment_service.repository.WalletTransactionRepository;
import com.parkease.payment_service.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    @Override
    @Transactional
    public WalletResponse getOrCreateWallet(UUID userId) {
        Wallet wallet  = walletRepository.findByUserId(userId).orElseGet(()->{
            log.info("Creating a new Wallet for driver : {}" + userId);
            Wallet newWallet = Wallet.builder()
                    .userId(userId)
                    .balance(BigDecimal.ZERO)
                    .isActive(true)
                    .build();
            return  walletRepository.save(newWallet);
        });

        return mapToWalletResponse(wallet);
    }


//    Get User Wallet
    @Override
    @Transactional(readOnly = true)
    public WalletResponse getWallet(UUID userId) {
        Wallet wallet = findWalletByUserIdOrThrow(userId);
        return mapToWalletResponse(wallet);
    }

    @Override
    @Transactional
    public WalletResponse topUpWallet(UUID userId, WalletTopUpRequest request) {
        log.info("Processing top-up of {} for user: {}", request.getAmount(), userId);

        // 1. Get or create active wallet

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseGet(()->
            walletRepository.save(
                    Wallet.builder()
                            .userId(userId)
                            .balance(BigDecimal.ZERO)
                            .isActive(true)
                            .build()
            ));

        validateWalletActive(wallet);

      // 2. Calculate balance snapshot

        BigDecimal balanceBefore = wallet.getBalance();
        BigDecimal balanceAfter = balanceBefore.add(request.getAmount());
        wallet.setBalance(balanceAfter);

        Wallet savedWallet = walletRepository.save(wallet);

        // 3. Record immutable CREDIT transaction in the ledger
        String topUpRef = "TOPUP-" + UUID.randomUUID().toString().substring(0,8).toUpperCase();

        recordTransaction(
                savedWallet.getWalletId(),
                userId,
                TransactionType.CREDIT,
                request.getAmount(),
                balanceBefore,
                balanceAfter,
                topUpRef,
                "Prepaid Wallet Top-up via " + request.getPaymentMethod()
        );

        log.info("Top-up successful for user: {}. New balance: {}", userId, balanceAfter);
        return mapToWalletResponse(savedWallet);

    }

    @Override
    @Transactional
    public WalletTransactionResponse debitWallet(UUID userId, BigDecimal amount, String referenceId, String description) {
        log.info("Attempting to debit {} from user: {} for reference: {}", amount, userId, referenceId);
        Wallet wallet = findWalletByUserIdOrThrow(userId);
        validateWalletActive(wallet);

        if (wallet.getBalance().compareTo(amount) < 0){
            log.warn("Debit failed for user: {}. Required: {}, Available: {}", userId, amount, wallet.getBalance());

            throw new InsufficientBalanceException(
                    "Insufficient wallet balance. Required: " + amount + ", Available: " + wallet.getBalance());
        }

        BigDecimal balanceBefore = wallet.getBalance();
        BigDecimal balanceAfter = balanceBefore.subtract(amount);
        wallet.setBalance(balanceAfter);

        Wallet savedWallet = walletRepository.save(wallet);

        WalletTransaction transaction = recordTransaction(
                savedWallet.getWalletId(),
                userId,
                TransactionType.DEBIT,
                amount,
                balanceBefore,
                balanceAfter,
                referenceId,
                description
        );
        log.info("Debit successful for user: {}. New balance: {}", userId, balanceAfter);
        return mapToTransactionResponse(transaction);

    }

    @Override
    @Transactional
    public WalletTransactionResponse creditRefund(UUID userId, BigDecimal amount, String referenceId, String description) {
        log.info("Crediting refund of {} to user: {} for reference: {}", amount, userId, referenceId);
        // Ensure wallet exists
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseGet(() -> walletRepository.save(
                        Wallet.builder()
                                .userId(userId)
                                .balance(BigDecimal.ZERO)
                                .isActive(true)
                                .build()
                ));
        validateWalletActive(wallet);

        BigDecimal balanceBefore = wallet.getBalance();
        BigDecimal balanceAfter = balanceBefore.add(amount);
        wallet.setBalance(balanceAfter);
        Wallet savedWallet = walletRepository.save(wallet);
        // Record immutable CREDIT transaction
        WalletTransaction transaction = recordTransaction(
                savedWallet.getWalletId(),
                userId,
                TransactionType.CREDIT,
                amount,
                balanceBefore,
                balanceAfter,
                referenceId,
                "Refund: " + description
        );
        log.info("Refund credit successful for user: {}. New balance: {}", userId, balanceAfter);
        return mapToTransactionResponse(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WalletTransactionResponse> getTransactionStatements(UUID userId) {
        log.info("Fetching transaction statement for user: {}", userId);
        return walletTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToTransactionResponse)
                .toList();
    }

    //    Helper Methods :

    private Wallet findWalletByUserIdOrThrow(UUID userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for user: " + userId + ". Please initialize wallet first."));
    }
    private void validateWalletActive(Wallet wallet) {
        if (!Boolean.TRUE.equals(wallet.getIsActive())) {
            throw new BadRequestException("Your wallet account is currently suspended or inactive. Please contact support.");
        }
    }
    private WalletTransaction recordTransaction(
            UUID walletId,
            UUID userId,
            TransactionType type,
            BigDecimal amount,
            BigDecimal balanceBefore,
            BigDecimal balanceAfter,
            String referenceId,
            String description) {

        WalletTransaction transaction = WalletTransaction.builder()
                .walletId(walletId)
                .userId(userId)
                .type(type)
                .amount(amount)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .referenceId(referenceId)
                .description(description)
                .build();
        return walletTransactionRepository.save(transaction);
    }
    private WalletResponse mapToWalletResponse(Wallet wallet) {
        return WalletResponse.builder()
                .walletId(wallet.getWalletId())
                .userId(wallet.getUserId())
                .balance(wallet.getBalance())
                .isActive(wallet.getIsActive())
                .updatedAt(wallet.getUpdatedAt())
                .build();
    }
    private WalletTransactionResponse mapToTransactionResponse(WalletTransaction tx) {
        return WalletTransactionResponse.builder()
                .transactionId(tx.getTransactionId())
                .walletId(tx.getWalletId())
                .userId(tx.getUserId())
                .type(tx.getType())
                .amount(tx.getAmount())
                .balanceBefore(tx.getBalanceBefore())
                .balanceAfter(tx.getBalanceAfter())
                .referenceId(tx.getReferenceId())
                .description(tx.getDescription())
                .createdAt(tx.getCreatedAt())
                .build();
    }
}
