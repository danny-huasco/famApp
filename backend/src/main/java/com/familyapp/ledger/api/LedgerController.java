package com.familyapp.ledger.api;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.familyapp.auth.security.FamilyPrincipal;
import com.familyapp.ledger.application.LedgerService;
import com.familyapp.idempotency.api.Idempotent;

@RestController
@RequestMapping("/api/outings/{outingId}")
public class LedgerController {
    private final LedgerService ledgerService;

    public LedgerController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @PostMapping("/expenses")
    @ResponseStatus(HttpStatus.CREATED)
    @Idempotent(operation = "expense.create")
    public ExpenseResponse addExpense(
            @AuthenticationPrincipal FamilyPrincipal principal,
            @PathVariable UUID outingId,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody ExpenseRequest request) {
        return ledgerService.addExpense(principal, outingId, request);
    }

    @GetMapping("/expenses")
    public List<ExpenseResponse> listExpenses(
            @AuthenticationPrincipal FamilyPrincipal principal,
            @PathVariable UUID outingId) {
        return ledgerService.listExpenses(principal, outingId);
    }

    @GetMapping("/settlements")
    public SettlementPlan settlements(
            @AuthenticationPrincipal FamilyPrincipal principal,
            @PathVariable UUID outingId) {
        return ledgerService.calculateSettlements(principal, outingId);
    }
}
