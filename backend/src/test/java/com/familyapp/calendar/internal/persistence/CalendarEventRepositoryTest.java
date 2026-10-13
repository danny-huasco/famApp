package com.familyapp.calendar.internal.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class CalendarEventRepositoryTest {
    @Autowired
    private CalendarEventRepository calendarEventRepository;

    @Test
    void findsDistinctConflictedAttendeesUsingHalfOpenIntervalOverlap() {
        UUID attendeeWithTwoConflicts = UUID.randomUUID();
        UUID attendeeEndingAtStart = UUID.randomUUID();
        UUID attendeeStartingAtEnd = UUID.randomUUID();
        UUID attendeeWithOneConflict = UUID.randomUUID();

        calendarEventRepository.save(Objects.requireNonNull(
                event(attendeeWithTwoConflicts, "2026-01-01T09:30:00Z", "2026-01-01T10:15:00Z")));
        calendarEventRepository.save(Objects.requireNonNull(
                event(attendeeWithTwoConflicts, "2026-01-01T10:30:00Z", "2026-01-01T11:30:00Z")));
        calendarEventRepository.save(Objects.requireNonNull(
                event(attendeeEndingAtStart, "2026-01-01T09:00:00Z", "2026-01-01T10:00:00Z")));
        calendarEventRepository.save(Objects.requireNonNull(
                event(attendeeStartingAtEnd, "2026-01-01T11:00:00Z", "2026-01-01T12:00:00Z")));
        calendarEventRepository.save(Objects.requireNonNull(
                event(attendeeWithOneConflict, "2026-01-01T10:10:00Z", "2026-01-01T10:20:00Z")));

        Set<UUID> conflicts = calendarEventRepository.findConflictedAttendeeIds(
                Set.of(
                        attendeeWithTwoConflicts,
                        attendeeEndingAtStart,
                        attendeeStartingAtEnd,
                        attendeeWithOneConflict),
                Instant.parse("2026-01-01T10:00:00Z"),
                Instant.parse("2026-01-01T11:00:00Z"));

        assertEquals(Set.of(attendeeWithTwoConflicts, attendeeWithOneConflict), conflicts);
    }

    private CalendarEventEntity event(UUID attendeeId, String start, String end) {
        CalendarEventEntity event = new CalendarEventEntity(
                UUID.randomUUID(),
                "Existing event",
                Instant.parse(start),
                Instant.parse(end));
        event.addAttendee(attendeeId);
        return event;
    }
}
