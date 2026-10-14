package com.familyapp.ledger.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import com.familyapp.auth.security.FamilyPrincipal;
import com.familyapp.consensus.api.CreateOutingRequest;
import com.familyapp.consensus.application.OutingService;
import com.familyapp.ledger.api.ExpenseRequest;
import com.familyapp.ledger.api.InvalidExpenseException;
import com.familyapp.user.api.UserAccountPort;
import com.familyapp.user.internal.persistence.FamilyRepository;
import com.familyapp.user.internal.persistence.UserEntity;
import com.familyapp.user.internal.persistence.UserRepository;

@SpringBootTest
@ActiveProfiles("test")
class LedgerServiceIntegrationTest {
    @Autowired
    private LedgerService ledgerService;

    @Autowired
    private OutingService outingService;

    @Autowired
    private UserAccountPort userAccountPort;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FamilyRepository familyRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void recordsEqualSharesAndCalculatesFamilyScopedSettlement() {
        var creator = createAccount();
        UserEntity attendee = addFamilyMember(creator.familyId());
        FamilyPrincipal principal = new FamilyPrincipal(creator.userId(), creator.familyId());
        var outing = outingService.createOuting(
                principal,
                new CreateOutingRequest(
                        "Ledger outing",
                        null,
                        Instant.parse("2026-01-01T10:00:00Z"),
                        Instant.parse("2026-01-01T11:00:00Z"),
                        List.of(attendee.getId())));

        var expense = ledgerService.addExpense(
                principal,
                outing.id(),
                new ExpenseRequest(
                        "Lunch",
                        new BigDecimal("10.00"),
                        creator.userId(),
                        List.of(creator.userId(), attendee.getId())));
        var plan = ledgerService.calculateSettlements(principal, outing.id());

        assertEquals(new BigDecimal("5.00"), expense.shares().get(creator.userId()));
        assertEquals(new BigDecimal("5.00"), expense.shares().get(attendee.getId()));
        assertEquals(new BigDecimal("5.00"), plan.netBalances().get(creator.userId()));
        assertEquals(new BigDecimal("-5.00"), plan.netBalances().get(attendee.getId()));
        assertEquals(1, plan.transfers().size());
        assertEquals(attendee.getId(), plan.transfers().getFirst().payerId());
        assertEquals(creator.userId(), plan.transfers().getFirst().payeeId());
        assertEquals(new BigDecimal("5.00"), plan.transfers().getFirst().amount());
        assertEquals(1, ledgerService.listExpenses(principal, outing.id()).size());
    }

    @Test
    void rejectsExpenseParticipantsOutsideOuting() {
        var creator = createAccount();
        var foreignAccount = createAccount();
        FamilyPrincipal principal = new FamilyPrincipal(creator.userId(), creator.familyId());
        var outing = outingService.createOuting(
                principal,
                new CreateOutingRequest(
                        "Ledger outing",
                        null,
                        Instant.parse("2026-01-01T10:00:00Z"),
                        Instant.parse("2026-01-01T11:00:00Z"),
                        List.of(creator.userId())));

        assertThrows(
                InvalidExpenseException.class,
                () -> ledgerService.addExpense(
                        principal,
                        outing.id(),
                        new ExpenseRequest(
                                "Foreign split",
                                new BigDecimal("10.00"),
                                creator.userId(),
                                List.of(foreignAccount.userId()))));
    }

    @Test
    void concurrentExpenseSubmissionsAreAllPersistedAndIncludedInSettlement() throws Exception {
        var creator = createAccount();
        UserEntity attendee = addFamilyMember(creator.familyId());
        FamilyPrincipal principal = new FamilyPrincipal(creator.userId(), creator.familyId());
        var outing = outingService.createOuting(
                principal,
                new CreateOutingRequest(
                        "Concurrent ledger outing",
                        null,
                        Instant.parse("2026-01-01T10:00:00Z"),
                        Instant.parse("2026-01-01T11:00:00Z"),
                        List.of(attendee.getId())));

        try (var executor = Executors.newFixedThreadPool(4)) {
            List<Future<?>> submissions = new ArrayList<>();
            for (int index = 0; index < 12; index++) {
                submissions.add(executor.submit(() -> ledgerService.addExpense(
                        principal,
                        outing.id(),
                        new ExpenseRequest(
                                "Concurrent expense",
                                new BigDecimal("1.00"),
                                creator.userId(),
                                List.of(creator.userId(), attendee.getId())))));
            }
            for (Future<?> submission : submissions) {
                submission.get();
            }
        }

        assertEquals(12, ledgerService.listExpenses(principal, outing.id()).size());
        var plan = ledgerService.calculateSettlements(principal, outing.id());
        assertEquals(new BigDecimal("6.00"), plan.netBalances().get(creator.userId()));
        assertEquals(new BigDecimal("-6.00"), plan.netBalances().get(attendee.getId()));
        assertEquals(1, plan.transfers().size());
        assertEquals(new BigDecimal("6.00"), plan.transfers().getFirst().amount());
    }

    private UserAccountPort.UserIdentity createAccount() {
        String suffix = UUID.randomUUID().toString();
        return userAccountPort.createFamilyAccount(
                "ledger-" + suffix + "@example.com",
                passwordEncoder.encode("secure-password-123"),
                "Ledger member",
                "Ledger test family");
    }

    private UserEntity addFamilyMember(UUID familyId) {
        var family = familyRepository.findById(Objects.requireNonNull(familyId)).orElseThrow();
        String suffix = UUID.randomUUID().toString();
        return userRepository.save(new UserEntity(
                UUID.randomUUID(),
                "ledger-attendee-" + suffix + "@example.com",
                passwordEncoder.encode("secure-password-123"),
                "Attendee",
                family));
    }
}
