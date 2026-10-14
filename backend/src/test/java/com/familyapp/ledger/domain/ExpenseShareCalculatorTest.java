package com.familyapp.ledger.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.familyapp.ledger.api.InvalidExpenseException;

class ExpenseShareCalculatorTest {
    private final ExpenseShareCalculator calculator = new ExpenseShareCalculator();

    @Test
    void dividesOddCentsDeterministicallyByAscendingParticipantId() {
        UUID participantA = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID participantB = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID participantC = UUID.fromString("00000000-0000-0000-0000-000000000003");

        var shares = calculator.splitEqually(
                new BigDecimal("100.00"), List.of(participantC, participantA, participantB));

        assertEquals(new BigDecimal("33.34"), shares.get(participantA));
        assertEquals(new BigDecimal("33.33"), shares.get(participantB));
        assertEquals(new BigDecimal("33.33"), shares.get(participantC));
    }

    @Test
    void roundsCurrencyUsingHalfEvenBeforeDividing() {
        UUID participant = UUID.randomUUID();

        var shares = calculator.splitEqually(new BigDecimal("1.015"), List.of(participant));

        assertEquals(new BigDecimal("1.02"), shares.get(participant));
    }

    @Test
    void handlesOneParticipantAndRejectsInvalidAmountOrDuplicateParticipants() {
        UUID participant = UUID.randomUUID();
        assertEquals(
                new BigDecimal("12.34"),
                calculator.splitEqually(new BigDecimal("12.34"), List.of(participant)).get(participant));
        assertThrows(InvalidExpenseException.class,
                () -> calculator.splitEqually(new BigDecimal("0.005"), List.of(participant)));
        assertThrows(InvalidExpenseException.class,
                () -> calculator.splitEqually(
                        new BigDecimal("10.00"), List.of(participant, participant)));
    }
}
