package com.familyapp.calendar.api;

import java.time.Instant;

public record CommonFreeSlot(Instant startTime, Instant endTime, boolean tentative) {
    public CommonFreeSlot {
        if (startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("Common free slot end must be after its start.");
        }
    }
}
