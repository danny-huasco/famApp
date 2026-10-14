package com.familyapp.event;

import java.util.Map;
import java.util.UUID;

public record VoteTallyUpdatedEvent(
        UUID outingId,
        int ballotCount,
        Map<UUID, Integer> firstPreferenceCounts) {
    public VoteTallyUpdatedEvent {
        firstPreferenceCounts = Map.copyOf(firstPreferenceCounts);
    }
}
