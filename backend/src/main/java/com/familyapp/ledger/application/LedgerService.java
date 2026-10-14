package com.familyapp.ledger.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import com.familyapp.auth.security.FamilyPrincipal;
import com.familyapp.consensus.api.OutingLedgerContext;
import com.familyapp.consensus.api.OutingLedgerPort;
import com.familyapp.ledger.api.ExpenseRequest;
import com.familyapp.ledger.api.ExpenseResponse;
import com.familyapp.ledger.api.InvalidExpenseException;
import com.familyapp.ledger.api.SettlementPlan;
import com.familyapp.ledger.domain.ExpenseShareCalculator;
import com.familyapp.ledger.domain.MinCashFlowCalculator;
import com.familyapp.ledger.internal.persistence.ExpenseEntity;
import com.familyapp.ledger.internal.persistence.ExpenseRepository;
import com.familyapp.ledger.internal.persistence.ExpenseShareEntity;
import com.familyapp.consensus.api.OutingNotFoundException;
import com.familyapp.event.ExpenseAddedEvent;

@Service
public class LedgerService {
    private final OutingLedgerPort outingLedgerPort;
    private final ExpenseRepository expenseRepository;
    private final ExpenseShareCalculator shareCalculator;
    private final MinCashFlowCalculator settlementCalculator;
    private final ApplicationEventPublisher eventPublisher;

    public LedgerService(
            OutingLedgerPort outingLedgerPort,
            ExpenseRepository expenseRepository,
            ExpenseShareCalculator shareCalculator,
            MinCashFlowCalculator settlementCalculator,
            ApplicationEventPublisher eventPublisher) {
        this.outingLedgerPort = outingLedgerPort;
        this.expenseRepository = expenseRepository;
        this.shareCalculator = shareCalculator;
        this.settlementCalculator = settlementCalculator;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public ExpenseResponse addExpense(
            FamilyPrincipal principal,
            UUID outingId,
            ExpenseRequest request) {
        if (principal == null || request == null || outingId == null) {
            throw new InvalidExpenseException("Authenticated user, outing, and expense are required.");
        }
        OutingLedgerContext outing = findOuting(outingId, principal);
        if (request.payerId() == null || !outing.attendeeIds().contains(request.payerId())) {
            throw new InvalidExpenseException("Expense payer must be an attendee of this outing.");
        }
        if (request.participantIds() == null || request.participantIds().isEmpty()
                || request.participantIds().stream().anyMatch(Objects::isNull)) {
            throw new InvalidExpenseException("At least one valid expense participant is required.");
        }
        if (!outing.attendeeIds().containsAll(request.participantIds())) {
            throw new InvalidExpenseException("Expense participants must be attendees of this outing.");
        }

        Map<UUID, BigDecimal> shares = shareCalculator.splitEqually(
                request.amount(), request.participantIds());
        ExpenseEntity expense = new ExpenseEntity(
                UUID.randomUUID(),
                outingId,
                request.description(),
                request.amount().setScale(2, RoundingMode.HALF_EVEN),
                request.payerId(),
                Instant.now());
        shares.forEach(expense::addShare);
        ExpenseEntity savedExpense = expenseRepository.save(expense);
        eventPublisher.publishEvent(new ExpenseAddedEvent(
                savedExpense.getId(),
                outingId,
                savedExpense.getDescription(),
                outing.attendeeIds()));
        return toResponse(savedExpense);
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> listExpenses(FamilyPrincipal principal, UUID outingId) {
        findOuting(outingId, principal);
        return expenseRepository.findAllByOutingIdOrderByCreatedAtAscIdAsc(outingId).stream()
                .map(this::toResponse)
                .collect(Collectors.toUnmodifiableList());
    }

    @Transactional(readOnly = true)
    public SettlementPlan calculateSettlements(FamilyPrincipal principal, UUID outingId) {
        OutingLedgerContext outing = findOuting(outingId, principal);
        Map<UUID, BigDecimal> amountsPaid = new HashMap<>();
        Map<UUID, BigDecimal> amountsOwed = new HashMap<>();
        outing.attendeeIds().forEach(participantId -> {
            amountsPaid.put(participantId, new BigDecimal("0.00"));
            amountsOwed.put(participantId, new BigDecimal("0.00"));
        });
        for (ExpenseEntity expense : expenseRepository.findAllByOutingIdOrderByCreatedAtAscIdAsc(outingId)) {
            amountsPaid.merge(
                    expense.getPayerId(),
                    expense.getAmount(),
                    (existing, added) -> existing.add(added));
            for (ExpenseShareEntity share : expense.getShares()) {
                amountsOwed.merge(
                        share.getParticipantId(),
                        share.getAmountOwed(),
                        (existing, added) -> existing.add(added));
            }
        }

        MinCashFlowCalculator.SettlementOutcome outcome =
                settlementCalculator.calculate(amountsPaid, amountsOwed);
        return new SettlementPlan(outingId, outcome.netBalances(), outcome.transfers());
    }

    private OutingLedgerContext findOuting(UUID outingId, FamilyPrincipal principal) {
        if (principal == null || outingId == null) {
            throw new InvalidExpenseException("Authenticated user and outing ID are required.");
        }
        OutingLedgerContext outing = outingLedgerPort.findLedgerContext(outingId)
                .orElseThrow(OutingNotFoundException::new);
        if (!outing.familyId().equals(principal.familyId())
                || !outing.attendeeIds().contains(principal.userId())) {
            throw new OutingNotFoundException();
        }
        return outing;
    }

    private ExpenseResponse toResponse(ExpenseEntity expense) {
        Map<UUID, BigDecimal> shares = new HashMap<>();
        for (ExpenseShareEntity share : expense.getShares()) {
            shares.put(share.getParticipantId(), share.getAmountOwed());
        }
        return new ExpenseResponse(
                expense.getId(),
                expense.getOutingId(),
                expense.getDescription(),
                expense.getAmount(),
                expense.getPayerId(),
                expense.getCreatedAt(),
                shares);
    }
}
