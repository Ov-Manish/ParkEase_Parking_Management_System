package com.parkease.payment_service.service;

import com.parkease.payment_service.dto.request.WalletTopUpRequest;
import com.parkease.payment_service.dto.response.WalletResponse;
import com.parkease.payment_service.dto.response.WalletTransactionResponse;
import com.parkease.payment_service.entity.WalletTransaction;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface WalletService {

//    Retrieves the driver's wallet, or auto-creates an active wallet with 0.00 balance if it's their first time
    WalletResponse getOrCreateWallet(UUID userId);

//    Gets the driver's current wallet balance and status.
    WalletResponse getWallet(UUID userId);


//    Tops up the driver's wallet balance and records an immutable CREDIT ledger transaction.
    WalletResponse topUpWallet(UUID userId, WalletTopUpRequest request);

//    Debits money from the wallet to pay for parking.
//    Throws InsufficientBalanceException if balance is less than required.
    WalletTransactionResponse debitWallet(UUID userId, BigDecimal amount, String referenceId, String description);

//    Retrieves the complete transaction statement/passbook for the driver.
    List<WalletTransactionResponse> getTransactionStatements(UUID userId);

    WalletTransactionResponse creditRefund(UUID userId, BigDecimal amount, String referenceId, String description);
}
