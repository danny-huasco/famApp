package com.familyapp.ledger.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.familyapp.ledger.api.InvalidExpenseException;
import com.familyapp.ledger.api.SettlementTransfer;

@Component
public class MinCashFlowCalculator {
    private static final BigDecimal ZERO = new BigDecimal("0.00");

    public SettlementOutcome calculate(
            Map<UUID, BigDecimal> amountsPaid,
            Map<UUID, BigDecimal> amountsOwed) {
        Map<UUID, BigDecimal> balances = new HashMap<>();
        amountsPaid.forEach((participantId, amount) -> balances.merge(
                participantId,
                amount.setScale(2),
                (existing, added) -> existing.add(added)));
        amountsOwed.forEach((participantId, amount) -> balances.merge(
                participantId,
                amount.setScale(2).negate(),
                (existing, added) -> existing.add(added)));
        balances.replaceAll((ignored, balance) -> balance.setScale(2));

        BigDecimal total = ZERO;
        for (BigDecimal balance : balances.values()) {
            total = total.add(balance);
        }
        if (total.compareTo(ZERO) != 0) {
            throw new InvalidExpenseException("Expense ledger balances do not sum to zero.");
        }

        List<Balance> creditors = new ArrayList<>();
        List<Balance> debtors = new ArrayList<>();
        balances.forEach((participantId, balance) -> {
            if (balance.signum() > 0) {
                creditors.add(new Balance(participantId, balance));
            } else if (balance.signum() < 0) {
                debtors.add(new Balance(participantId, balance.negate()));
            }
        });
        Comparator<Balance> largestFirst = Comparator
                .comparing((Balance balance) -> balance.amount())
                .reversed()
                .thenComparing(balance -> balance.participantId());
        creditors.sort(largestFirst);
        debtors.sort(largestFirst);

        List<SettlementTransfer> transfers = new ArrayList<>();
        int creditorIndex = 0;
        int debtorIndex = 0;
        while (creditorIndex < creditors.size() && debtorIndex < debtors.size()) {
            Balance creditor = creditors.get(creditorIndex);
            Balance debtor = debtors.get(debtorIndex);
            BigDecimal transferAmount = creditor.amount().min(debtor.amount()).setScale(2);
            if (transferAmount.signum() <= 0 || creditor.participantId().equals(debtor.participantId())) {
                throw new IllegalStateException("Settlement calculation produced an invalid transfer.");
            }
            transfers.add(new SettlementTransfer(
                    debtor.participantId(), creditor.participantId(), transferAmount));

            BigDecimal creditorRemaining = creditor.amount().subtract(transferAmount).setScale(2);
            BigDecimal debtorRemaining = debtor.amount().subtract(transferAmount).setScale(2);
            if (creditorRemaining.signum() == 0) {
                creditorIndex++;
            } else {
                creditors.set(creditorIndex, new Balance(creditor.participantId(), creditorRemaining));
            }
            if (debtorRemaining.signum() == 0) {
                debtorIndex++;
            } else {
                debtors.set(debtorIndex, new Balance(debtor.participantId(), debtorRemaining));
            }
        }
        if (creditorIndex != creditors.size() || debtorIndex != debtors.size()) {
            throw new IllegalStateException("Settlement calculation did not settle every balance.");
        }
        return new SettlementOutcome(Map.copyOf(balances), List.copyOf(transfers));
    }

    public record SettlementOutcome(
            Map<UUID, BigDecimal> netBalances,
            List<SettlementTransfer> transfers) {}

    private record Balance(UUID participantId, BigDecimal amount) {}
}
