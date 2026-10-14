package com.familyapp.ledger.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.familyapp.ledger.api.InvalidExpenseException;

@Component
public class ExpenseShareCalculator {
    private static final int CURRENCY_SCALE = 2;
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("9999999999.99");

    public Map<UUID, BigDecimal> splitEqually(BigDecimal rawAmount, List<UUID> rawParticipants) {
        if (rawAmount == null || rawParticipants == null || rawParticipants.isEmpty()
                || rawParticipants.stream().anyMatch(id -> id == null)) {
            throw new InvalidExpenseException("Amount and at least one valid participant are required.");
        }
        BigDecimal amount = rawAmount.setScale(CURRENCY_SCALE, RoundingMode.HALF_EVEN);
        if (amount.signum() <= 0 || amount.compareTo(MAX_AMOUNT) > 0) {
            throw new InvalidExpenseException("Expense amount must be between 0.01 and 9999999999.99.");
        }

        List<UUID> participants = new ArrayList<>(rawParticipants);
        if (participants.stream().distinct().count() != participants.size()) {
            throw new InvalidExpenseException("Expense participants must be unique.");
        }
        participants.sort(Comparator.naturalOrder());

        long totalCents = amount.movePointRight(CURRENCY_SCALE).longValueExact();
        long baseCents = totalCents / participants.size();
        long remainingCents = totalCents % participants.size();
        Map<UUID, BigDecimal> shares = participants.stream()
                .collect(Collectors.toMap(
                        participantId -> participantId,
                        ignored -> BigDecimal.valueOf(baseCents, CURRENCY_SCALE)));
        for (int index = 0; index < remainingCents; index++) {
            UUID participantId = participants.get(index);
            shares.compute(
                    participantId,
                    (ignored, share) -> share.add(BigDecimal.valueOf(1, CURRENCY_SCALE)));
        }
        return Map.copyOf(shares);
    }
}
