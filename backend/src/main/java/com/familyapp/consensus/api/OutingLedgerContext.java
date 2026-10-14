package com.familyapp.consensus.api;

import java.util.Set;
import java.util.UUID;

public record OutingLedgerContext(
        UUID outingId,
        UUID familyId,
        OutingStatus status,
        Set<UUID> attendeeIds) {
    public OutingLedgerContext {
        attendeeIds = Set.copyOf(attendeeIds);
    }
}
