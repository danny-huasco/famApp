package com.familyapp.notification.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import com.familyapp.calendar.api.BookedEventPort;
import com.familyapp.calendar.internal.persistence.CalendarEventRepository;
import com.familyapp.consensus.api.CreateIdeaRequest;
import com.familyapp.consensus.api.CreateOutingRequest;
import com.familyapp.consensus.api.SubmitVoteRequest;
import com.familyapp.consensus.application.IdeaService;
import com.familyapp.consensus.application.OutingService;
import com.familyapp.consensus.application.VotingService;
import com.familyapp.ledger.api.ExpenseRequest;
import com.familyapp.ledger.application.LedgerService;
import com.familyapp.ledger.internal.persistence.OutingLedgerRepository;
import com.familyapp.notification.api.NotificationResponse;
import com.familyapp.user.api.UserAccountPort;
import com.familyapp.user.internal.persistence.FamilyRepository;
import com.familyapp.user.internal.persistence.UserEntity;
import com.familyapp.user.internal.persistence.UserRepository;

@SpringBootTest
@ActiveProfiles("test")
class OutingEventNotificationIntegrationTest {
    @Autowired
    private OutingService outingService;

    @Autowired
    private IdeaService ideaService;

    @Autowired
    private VotingService votingService;

    @Autowired
    private LedgerService ledgerService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserAccountPort userAccountPort;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FamilyRepository familyRepository;

    @Autowired
    private CalendarEventRepository calendarEventRepository;

    @Autowired
    private OutingLedgerRepository outingLedgerRepository;

    @Autowired
    private BookedEventPort bookedEventPort;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void outingLifecycleCreatesRecipientScopedNotificationsAndFinalizesCalendarAndLedger() {
        var creator = createAccount();
        UserEntity attendee = addFamilyMember(creator.familyId());
        FamilyPrincipal creatorPrincipal = new FamilyPrincipal(creator.userId(), creator.familyId());
        FamilyPrincipal attendeePrincipal = new FamilyPrincipal(attendee.getId(), creator.familyId());
        Instant start = Instant.parse("2026-01-01T10:00:00Z");
        Instant end = Instant.parse("2026-01-01T11:00:00Z");
        bookedEventPort.recordBookedEvent(
                UUID.randomUUID(), "Attendee's existing event", start, end, java.util.Set.of(attendee.getId()));

        var outing = outingService.createOuting(
                creatorPrincipal,
                new CreateOutingRequest(
                        "Family outing",
                        null,
                        start,
                        end,
                        List.of(attendee.getId())));

        List<NotificationResponse> creatorNotifications =
                notificationService.listForUser(creatorPrincipal);
        List<NotificationResponse> attendeeNotifications =
                notificationService.listForUser(attendeePrincipal);
        assertEquals(1, creatorNotifications.size());
        assertEquals(2, attendeeNotifications.size());
        assertTrue(attendeeNotifications.stream()
                .anyMatch(notification -> notification.title().equals("You have an event conflict")));
        assertTrue(attendeeNotifications.stream().allMatch(notification -> !notification.read()));

        var idea = ideaService.submitIdea(
                creatorPrincipal, outing.id(), new CreateIdeaRequest("Museum visit", null));
        votingService.submitBallot(
                creatorPrincipal, outing.id(), new SubmitVoteRequest(List.of(idea.id())));
        votingService.submitBallot(
                attendeePrincipal, outing.id(), new SubmitVoteRequest(List.of(idea.id())));
        votingService.finalizeByRankedChoice(creatorPrincipal, outing.id());

        UUID outingId = Objects.requireNonNull(outing.id());
        assertTrue(outingLedgerRepository.existsById(outingId));
        assertTrue(calendarEventRepository.findById(outingId).orElseThrow().isLocked());
        assertTrue(notificationService.listForUser(attendeePrincipal).stream()
                .anyMatch(notification -> notification.title().equals("Outing finalized")));

        ledgerService.addExpense(
                creatorPrincipal,
                outing.id(),
                new ExpenseRequest(
                        "Museum tickets",
                        new BigDecimal("20.00"),
                        creator.userId(),
                        List.of(creator.userId(), attendee.getId())));

        List<NotificationResponse> afterExpense =
                notificationService.listForUser(attendeePrincipal);
        assertEquals(4, afterExpense.size());
        assertTrue(afterExpense.stream()
                .anyMatch(notification -> notification.title().equals("New expense added")));

        var unrelatedAccount = createAccount();
        assertTrue(notificationService.listForUser(
                new FamilyPrincipal(unrelatedAccount.userId(), unrelatedAccount.familyId())).isEmpty());
        assertEquals(outing.id(), creatorNotifications.getFirst().outingId());
    }

    private UserAccountPort.UserIdentity createAccount() {
        String suffix = UUID.randomUUID().toString();
        return userAccountPort.createFamilyAccount(
                "notify-" + suffix + "@example.com",
                passwordEncoder.encode("secure-password-123"),
                "Family member",
                "Notification test family");
    }

    private UserEntity addFamilyMember(UUID familyId) {
        var family = familyRepository.findById(Objects.requireNonNull(familyId)).orElseThrow();
        String suffix = UUID.randomUUID().toString();
        return userRepository.save(new UserEntity(
                UUID.randomUUID(),
                "notify-attendee-" + suffix + "@example.com",
                passwordEncoder.encode("secure-password-123"),
                "Attendee",
                family));
    }
}
