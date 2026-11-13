package com.parkease.payment_service.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletResponse {

    private UUID walletId;
    private UUID userId;
    private BigDecimal balance;
    private Boolean isActive;
    private Instant updatedAt;
}
