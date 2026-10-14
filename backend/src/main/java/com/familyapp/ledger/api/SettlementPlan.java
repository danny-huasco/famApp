package com.familyapp.ledger.api;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record SettlementPlan(
        UUID outingId,
        Map<UUID, BigDecimal> netBalances,
        List<SettlementTransfer> transfers) {
    public SettlementPlan {
        netBalances = Map.copyOf(netBalances);
        transfers = List.copyOf(transfers);
    }
}
