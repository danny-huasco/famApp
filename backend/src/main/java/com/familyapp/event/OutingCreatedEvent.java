package com.familyapp.event;

import java.util.Set;
import java.util.UUID;

public record OutingCreatedEvent(
        UUID outingId,
        String title,
        Set<UUID> attendeeIds,
        Set<UUID> conflictedAttendeeIds) {
    public OutingCreatedEvent {
        attendeeIds = Set.copyOf(attendeeIds);
        conflictedAttendeeIds = Set.copyOf(conflictedAttendeeIds);
    }
}
