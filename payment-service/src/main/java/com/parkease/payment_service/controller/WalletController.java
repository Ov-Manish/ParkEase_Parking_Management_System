package com.parkease.payment_service.controller;

import com.parkease.payment_service.dto.request.WalletTopUpRequest;
import com.parkease.payment_service.dto.response.ApiResponse;
import com.parkease.payment_service.dto.response.WalletResponse;
import com.parkease.payment_service.dto.response.WalletTransactionResponse;
import com.parkease.payment_service.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
@Tag(name = "Wallet Management", description = "Endpoints for driver digital wallets, top-ups, and transaction statements")
@SecurityRequirement(name = "bearerAuth")
public class WalletController {

    private final WalletService walletService;

    // 1. Get current driver's wallet (Auto-creates 0.00 wallet if first time)
    @GetMapping("/me")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Get My Wallet", description = "Retrieves current wallet balance and status. Auto-creates wallet if first-time user.")
    public ResponseEntity<ApiResponse<WalletResponse>> getMyWallet(Authentication authentication) {
        UUID userId = extractUserId(authentication);
        log.info("REST request to fetch wallet for user: {}", userId);

        WalletResponse response = walletService.getOrCreateWallet(userId);
        return ResponseEntity.ok(ApiResponse.success("Wallet retrieved successfully", response));
    }

    // 2. Top-up wallet balance
    @PostMapping("/top-up")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Top-Up Wallet", description = "Adds funds to the authenticated driver's wallet and writes a CREDIT ledger record.")
    public ResponseEntity<ApiResponse<WalletResponse>> topUpWallet(
            @Valid @RequestBody WalletTopUpRequest request,
            Authentication authentication) {
        UUID userId = extractUserId(authentication);
        log.info("REST request to top-up wallet for user: {} by amount: {}", userId, request.getAmount());

        WalletResponse response = walletService.topUpWallet(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Wallet top-up successful", response));
    }

    // 3. View statement / transaction passbook
    @GetMapping("/transactions")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Get Wallet Statement", description = "Lists complete chronological transaction history for the driver.")
    public ResponseEntity<ApiResponse<List<WalletTransactionResponse>>> getTransactionStatements(Authentication authentication) {
        UUID userId = extractUserId(authentication);
        log.info("REST request to fetch wallet statement for user: {}", userId);

        List<WalletTransactionResponse> responses = walletService.getTransactionStatements(userId);
        return ResponseEntity.ok(ApiResponse.success("Wallet statements retrieved successfully", responses));
    }

    // Helper to extract driver UUID from SecurityContext
    private UUID extractUserId(Authentication authentication) {
        return (UUID) authentication.getPrincipal();
    }
}
