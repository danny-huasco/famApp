package com.familyapp.calendar.api;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public interface BookedEventPort {
    void recordBookedEvent(
            UUID outingId,
            String title,
            Instant startTime,
            Instant endTime,
            Set<UUID> attendeeIds);
}
