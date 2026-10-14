package com.familyapp.event;

import java.util.Set;
import java.util.UUID;

public record OutingFinalizedEvent(
        UUID outingId,
        String outingTitle,
        UUID winnerIdeaId,
        String winnerIdeaTitle,
        Set<UUID> attendeeIds) {
    public OutingFinalizedEvent {
        attendeeIds = Set.copyOf(attendeeIds);
    }
}
