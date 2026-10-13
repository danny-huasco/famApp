package com.familyapp.calendar.api;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record CalendarConflictAssessment(
        SlotConflictLevel level,
        int attendeeCount,
        int conflictedAttendeeCount,
        Set<UUID> conflictedAttendeeIds) {
    public CalendarConflictAssessment {
        Objects.requireNonNull(level, "Conflict level is required.");
        if (attendeeCount <= 0 || conflictedAttendeeCount < 0
                || conflictedAttendeeCount > attendeeCount) {
            throw new IllegalArgumentException("Conflict assessment attendee counts are invalid.");
        }
        conflictedAttendeeIds = Set.copyOf(conflictedAttendeeIds);
        if (conflictedAttendeeIds.size() != conflictedAttendeeCount) {
            throw new IllegalArgumentException("Conflicted attendee IDs do not match the count.");
        }
    }
}
