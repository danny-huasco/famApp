package com.familyapp.consensus.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.familyapp.auth.security.FamilyPrincipal;
import com.familyapp.calendar.api.BookedEventPort;
import com.familyapp.calendar.internal.persistence.CalendarEventRepository;
import com.familyapp.consensus.api.CreateOutingRequest;
import com.familyapp.consensus.api.OutingConflictException;
import com.familyapp.consensus.internal.persistence.OutingRepository;
import com.familyapp.user.api.UserAccountPort;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OutingCreationIntegrationTest {
    @Autowired
    private OutingService outingService;

    @Autowired
    private UserAccountPort userAccountPort;

    @Autowired
    private BookedEventPort bookedEventPort;

    @Autowired
    private OutingRepository outingRepository;

    @Autowired
    private CalendarEventRepository calendarEventRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void allowedOutingAndCalendarBookingPersistTogether() {
        var user = createAccount();
        CreateOutingRequest request = request(user.userId());

        var response = outingService.createOuting(
                new FamilyPrincipal(user.userId(), user.familyId()), request);

        assertEquals(Set.of(user.userId()), response.attendeeIds());
        UUID outingId = Objects.requireNonNull(response.id());
        assertTrue(outingRepository.existsById(outingId));
        assertTrue(calendarEventRepository.existsById(outingId));
    }

    @Test
    void redConflictDoesNotCreateOutingOrAnotherCalendarBooking() {
        var user = createAccount();
        Instant start = Instant.parse("2026-01-01T10:00:00Z");
        Instant end = Instant.parse("2026-01-01T11:00:00Z");
        bookedEventPort.recordBookedEvent(
                UUID.randomUUID(), "Existing event", start, end, Set.of(user.userId()));
        long outingCountBefore = outingRepository.count();
        long eventCountBefore = calendarEventRepository.count();

        assertThrows(
                OutingConflictException.class,
                () -> outingService.createOuting(
                        new FamilyPrincipal(user.userId(), user.familyId()),
                        new CreateOutingRequest(
                                "Conflicting outing",
                                null,
                                start,
                                end,
                                List.of(user.userId()))));

        assertEquals(outingCountBefore, outingRepository.count());
        assertEquals(eventCountBefore, calendarEventRepository.count());
    }

    private UserAccountPort.UserIdentity createAccount() {
        String suffix = UUID.randomUUID().toString();
        return userAccountPort.createFamilyAccount(
                "outing-" + suffix + "@example.com",
                passwordEncoder.encode("secure-password-123"),
                "Family member",
                "Outing test family");
    }

    private CreateOutingRequest request(UUID attendeeId) {
        return new CreateOutingRequest(
                "Family outing",
                "Created in an integration test",
                Instant.parse("2026-01-01T10:00:00Z"),
                Instant.parse("2026-01-01T11:00:00Z"),
                List.of(attendeeId));
    }
}
