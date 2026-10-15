package com.familyapp.calendar.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import com.familyapp.auth.security.FamilyPrincipal;
import com.familyapp.calendar.api.AvailabilityController;
import com.familyapp.calendar.api.AvailabilityWindowRequest;
import com.familyapp.calendar.api.InvalidAvailabilityException;
import com.familyapp.calendar.api.ReplaceAvailabilityRequest;
import com.familyapp.consensus.api.CreateOutingRequest;
import com.familyapp.consensus.api.OutingController;
import com.familyapp.consensus.api.OutingNotFoundException;
import com.familyapp.user.api.UserAccountPort;

@SpringBootTest
@ActiveProfiles("test")
class AvailabilityControllerIntegrationTest {
    @Autowired
    private AvailabilityController availabilityController;

    @Autowired
    private OutingController outingController;

    @Autowired
    private UserAccountPort userAccountPort;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void replacesAvailabilityReturnsOverviewAndReplaysIdempotently() {
        var account = createAccount();
        FamilyPrincipal principal = new FamilyPrincipal(account.userId(), account.familyId());
        UUID outingId = createOuting(principal);
        String key = UUID.randomUUID().toString();
        ReplaceAvailabilityRequest request = availabilityRequest(
                "2026-01-01T10:00:00Z", "2026-01-01T12:00:00Z", true);

        var created = availabilityController.replaceAvailability(principal, outingId, key, request);
        var replay = availabilityController.replaceAvailability(principal, outingId, key, request);
        var overview = availabilityController.getAvailability(principal, outingId);

        assertEquals(created.myWindows(), replay.myWindows());
        assertEquals(1, overview.myWindows().size());
        assertEquals(account.userId(), overview.myWindows().getFirst().userId());
        assertEquals(1, overview.attendees().size());
        assertEquals(1, overview.commonFreeSlots().size());
        assertEquals(
                Instant.parse("2026-01-01T10:00:00Z"),
                overview.commonFreeSlots().getFirst().startTime());
        assertEquals(true, overview.commonFreeSlots().getFirst().tentative());
    }

    @Test
    void rejectsReversedWindowsAndHidesOutingsFromOtherFamilies() {
        var account = createAccount();
        var otherAccount = createAccount();
        FamilyPrincipal principal = new FamilyPrincipal(account.userId(), account.familyId());
        FamilyPrincipal outsider = new FamilyPrincipal(
                otherAccount.userId(), otherAccount.familyId());
        UUID outingId = createOuting(principal);

        assertThrows(
                InvalidAvailabilityException.class,
                () -> availabilityController.replaceAvailability(
                        principal,
                        outingId,
                        UUID.randomUUID().toString(),
                        availabilityRequest("2026-01-01T12:00:00Z", "2026-01-01T10:00:00Z", false)));
        assertThrows(
                OutingNotFoundException.class,
                () -> availabilityController.getAvailability(outsider, outingId));
    }

    @Test
    void reusingAnIdempotencyKeyWithDifferentAvailabilityConflicts() {
        var account = createAccount();
        FamilyPrincipal principal = new FamilyPrincipal(account.userId(), account.familyId());
        UUID outingId = createOuting(principal);
        String key = UUID.randomUUID().toString();

        availabilityController.replaceAvailability(
                principal,
                outingId,
                key,
                availabilityRequest("2026-01-01T10:00:00Z", "2026-01-01T12:00:00Z", false));

        assertThrows(
                com.familyapp.idempotency.api.IdempotencyConflictException.class,
                () -> availabilityController.replaceAvailability(
                        principal,
                        outingId,
                        key,
                        availabilityRequest(
                                "2026-01-01T13:00:00Z", "2026-01-01T14:00:00Z", false)));
    }

    private UUID createOuting(FamilyPrincipal principal) {
        return outingController.createOuting(
                principal,
                UUID.randomUUID().toString(),
                new CreateOutingRequest(
                        "Availability test outing",
                        "Calendar API integration test",
                        Instant.parse("2026-01-01T09:00:00Z"),
                        Instant.parse("2026-01-01T10:00:00Z"),
                        List.of(principal.userId())))
                .id();
    }

    private ReplaceAvailabilityRequest availabilityRequest(
            String startTime,
            String endTime,
            boolean tentative) {
        return new ReplaceAvailabilityRequest(List.of(new AvailabilityWindowRequest(
                Instant.parse(startTime),
                Instant.parse(endTime),
                tentative)));
    }

    private UserAccountPort.UserIdentity createAccount() {
        String suffix = UUID.randomUUID().toString();
        return userAccountPort.createFamilyAccount(
                "calendar-" + suffix + "@example.com",
                passwordEncoder.encode("secure-password-123"),
                "Calendar member",
                "Calendar test family");
    }
}
