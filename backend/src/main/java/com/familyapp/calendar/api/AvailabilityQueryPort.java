package com.familyapp.calendar.api;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface AvailabilityQueryPort {
    List<CommonFreeSlot> findCommonFreeSlots(UUID outingId, Set<UUID> attendeeIds);
}
