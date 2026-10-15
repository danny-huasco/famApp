package com.familyapp.consensus.api;

import java.util.Set;
import java.util.UUID;

public record OutingAvailabilityContext(UUID familyId, Set<UUID> attendeeIds) {
    public OutingAvailabilityContext {
        attendeeIds = Set.copyOf(attendeeIds);
    }
}
