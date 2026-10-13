package com.familyapp.calendar.api;

import java.time.Instant;

public record AvailabilityWindow(Instant startTime, Instant endTime, boolean tentative) {
    public AvailabilityWindow {
        if (startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("Availability window end must be after its start.");
        }
    }
}
