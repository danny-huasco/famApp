package com.familyapp.ledger.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ExpenseResponse(
        UUID id,
        UUID outingId,
        String description,
        BigDecimal amount,
        UUID payerId,
        Instant createdAt,
        Map<UUID, BigDecimal> shares) {
    public ExpenseResponse {
        amount = amount.setScale(2);
        shares = Map.copyOf(shares);
    }
}
