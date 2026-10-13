package com.familyapp.calendar.api;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public interface ScheduleConflictPort {
    CalendarConflictAssessment assessConflicts(
            Instant proposedStart,
            Instant proposedEnd,
            Set<UUID> attendeeIds);
}
