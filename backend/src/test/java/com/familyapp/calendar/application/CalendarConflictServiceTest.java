package com.familyapp.calendar.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.familyapp.calendar.api.SlotConflictLevel;
import com.familyapp.calendar.internal.persistence.CalendarEventRepository;

class CalendarConflictServiceTest {
    private CalendarEventRepository calendarEventRepository;
    private CalendarConflictService conflictService;

    @BeforeEach
    void setUp() {
        calendarEventRepository = mock(CalendarEventRepository.class);
        conflictService = new CalendarConflictService(calendarEventRepository);
    }

    @Test
    void countsConflictedAttendeesOnceAndClassifiesExactHalfAsOrange() {
        UUID attendeeA = UUID.randomUUID();
        UUID attendeeB = UUID.randomUUID();
        UUID attendeeC = UUID.randomUUID();
        UUID attendeeD = UUID.randomUUID();
        Set<UUID> attendees = Set.of(attendeeA, attendeeB, attendeeC, attendeeD);
        when(calendarEventRepository.findConflictedAttendeeIds(
                attendees,
                Instant.parse("2026-01-01T10:00:00Z"),
                Instant.parse("2026-01-01T11:00:00Z")))
                .thenReturn(Set.of(attendeeA, attendeeB));

        var assessment = conflictService.assessConflicts(
                Instant.parse("2026-01-01T10:00:00Z"),
                Instant.parse("2026-01-01T11:00:00Z"),
                attendees);

        assertEquals(SlotConflictLevel.ORANGE, assessment.level());
        assertEquals(4, assessment.attendeeCount());
        assertEquals(2, assessment.conflictedAttendeeCount());
        assertEquals(Set.of(attendeeA, attendeeB), assessment.conflictedAttendeeIds());
    }

    @Test
    void rejectsInvalidTimeRangesAndMissingAttendees() {
        Instant start = Instant.parse("2026-01-01T10:00:00Z");
        Instant end = Instant.parse("2026-01-01T11:00:00Z");
        assertThrows(IllegalArgumentException.class,
                () -> conflictService.assessConflicts(start, start, Set.of(UUID.randomUUID())));
        assertThrows(IllegalArgumentException.class,
                () -> conflictService.assessConflicts(start, end, Set.of()));
    }
}
