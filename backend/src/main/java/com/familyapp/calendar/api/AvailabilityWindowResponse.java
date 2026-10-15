package com.familyapp.calendar.api;

import java.time.Instant;
import java.util.UUID;

public record AvailabilityWindowResponse(
        UUID id,
        UUID userId,
        Instant startTime,
        Instant endTime,
        boolean tentative) {}
