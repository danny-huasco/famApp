package com.familyapp.ledger.api;

import java.math.BigDecimal;
import java.util.UUID;

public record SettlementTransfer(UUID payerId, UUID payeeId, BigDecimal amount) {
    public SettlementTransfer {
        amount = amount.setScale(2);
    }
}
