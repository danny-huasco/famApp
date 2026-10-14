package com.familyapp.idempotency.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import com.familyapp.auth.security.FamilyPrincipal;
import com.familyapp.consensus.api.CreateOutingRequest;
import com.familyapp.consensus.api.OutingController;
import com.familyapp.consensus.internal.persistence.OutingRepository;
import com.familyapp.idempotency.api.IdempotencyConflictException;
import com.familyapp.ledger.api.ExpenseRequest;
import com.familyapp.ledger.api.LedgerController;
import com.familyapp.ledger.internal.persistence.ExpenseRepository;
import com.familyapp.user.api.UserAccountPort;

@SpringBootTest
@ActiveProfiles("test")
class IdempotencyIntegrationTest {
    @Autowired
    private OutingController outingController;

    @Autowired
    private LedgerController ledgerController;

    @Autowired
    private OutingRepository outingRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private UserAccountPort userAccountPort;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void repeatedOutingRequestReturnsStoredResponseAndRejectsKeyReuseWithDifferentInput() {
        var account = createAccount();
        FamilyPrincipal principal = new FamilyPrincipal(account.userId(), account.familyId());
        String idempotencyKey = UUID.randomUUID().toString();
        CreateOutingRequest request = outingRequest(account.userId(), "Museum visit");
        long outingCountBefore = outingRepository.count();

        var created = outingController.createOuting(principal, idempotencyKey, request);
        var replay = outingController.createOuting(principal, idempotencyKey, request);

        assertEquals(created.id(), replay.id());
        assertEquals(outingCountBefore + 1, outingRepository.count());
        assertThrows(
                IdempotencyConflictException.class,
                () -> outingController.createOuting(
                        principal,
                        idempotencyKey,
                        outingRequest(account.userId(), "Different outing")));
        assertEquals(outingCountBefore + 1, outingRepository.count());
    }

    @Test
    void repeatedExpenseRequestDoesNotCreateAnotherExpense() {
        var account = createAccount();
        FamilyPrincipal principal = new FamilyPrincipal(account.userId(), account.familyId());
        var outing = outingController.createOuting(
                principal,
                UUID.randomUUID().toString(),
                outingRequest(account.userId(), "Picnic"));
        UUID outingId = Objects.requireNonNull(outing.id());
        String idempotencyKey = UUID.randomUUID().toString();
        ExpenseRequest request = new ExpenseRequest(
                "Picnic supplies",
                new BigDecimal("18.50"),
                account.userId(),
                List.of(account.userId()));

        var created = ledgerController.addExpense(principal, outingId, idempotencyKey, request);
        var replay = ledgerController.addExpense(principal, outingId, idempotencyKey, request);

        assertEquals(created.id(), replay.id());
        assertEquals(1, expenseRepository.findAllByOutingIdOrderByCreatedAtAscIdAsc(outingId).size());
    }

    private UserAccountPort.UserIdentity createAccount() {
        String suffix = UUID.randomUUID().toString();
        return userAccountPort.createFamilyAccount(
                "idempotency-" + suffix + "@example.com",
                passwordEncoder.encode("secure-password-123"),
                "Family member",
                "Idempotency test family");
    }

    private CreateOutingRequest outingRequest(UUID attendeeId, String title) {
        return new CreateOutingRequest(
                title,
                "Idempotency integration test",
                Instant.parse("2026-01-01T10:00:00Z"),
                Instant.parse("2026-01-01T11:00:00Z"),
                List.of(attendeeId));
    }
}
