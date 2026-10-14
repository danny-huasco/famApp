package com.familyapp.event;

import java.util.Set;
import java.util.UUID;

public record ExpenseAddedEvent(
        UUID expenseId,
        UUID outingId,
        String description,
        Set<UUID> attendeeIds) {
    public ExpenseAddedEvent {
        attendeeIds = Set.copyOf(attendeeIds);
    }
}
