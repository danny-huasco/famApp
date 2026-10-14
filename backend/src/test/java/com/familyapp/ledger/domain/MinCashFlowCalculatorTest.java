package com.familyapp.ledger.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.familyapp.ledger.api.InvalidExpenseException;

class MinCashFlowCalculatorTest {
    private final MinCashFlowCalculator calculator = new MinCashFlowCalculator();

    @Test
    void settlesFiveParticipantsAndPreservesZeroSum() {
        UUID a = id(1);
        UUID b = id(2);
        UUID c = id(3);
        UUID d = id(4);
        UUID e = id(5);
        Map<UUID, BigDecimal> paid = Map.of(
                a, money("40.00"), b, money("30.00"));
        Map<UUID, BigDecimal> owed = Map.of(
                c, money("20.00"), d, money("25.00"), e, money("25.00"));

        var outcome = calculator.calculate(paid, owed);

        assertEquals(4, outcome.transfers().size());
        assertEquals(5, outcome.netBalances().size());
        BigDecimal balanceTotal = money("0.00");
        for (BigDecimal balance : outcome.netBalances().values()) {
            balanceTotal = balanceTotal.add(balance);
        }
        assertEquals(money("0.00"), balanceTotal);
        for (var transfer : outcome.transfers()) {
            assertTrue(transfer.amount().signum() > 0);
            assertFalse(transfer.payerId().equals(transfer.payeeId()));
        }
    }

    @Test
    void zeroBalancesAndSingleParticipantNeedNoTransfers() {
        UUID participant = id(1);
        var zero = calculator.calculate(Map.of(participant, money("10.00")),
                Map.of(participant, money("10.00")));
        assertEquals(0, zero.transfers().size());
        assertEquals(money("0.00"), zero.netBalances().get(participant));

        var single = calculator.calculate(Map.of(participant, money("0.00")), Map.of());
        assertEquals(0, single.transfers().size());
    }

    @Test
    void rejectsUnbalancedInput() {
        assertThrows(InvalidExpenseException.class,
                () -> calculator.calculate(Map.of(id(1), money("5.00")), Map.of()));
    }

    @Test
    void equalDebtsSettleWithOpposingBalances() {
        UUID payer = id(1);
        UUID debtor = id(2);
        var outcome = calculator.calculate(
                Map.of(payer, money("10.00")),
                Map.of(debtor, money("10.00")));

        assertEquals(1, outcome.transfers().size());
        assertEquals(payer, outcome.transfers().getFirst().payeeId());
        assertEquals(debtor, outcome.transfers().getFirst().payerId());
        assertEquals(money("10.00"), outcome.transfers().getFirst().amount());
    }

    private static UUID id(int suffix) {
        return UUID.fromString("00000000-0000-0000-0000-%012d".formatted(suffix));
    }

    private static BigDecimal money(String amount) {
        return new BigDecimal(amount);
    }
}
