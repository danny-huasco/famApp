package com.familyapp.calendar.api;

import java.time.Instant;

import jakarta.validation.constraints.NotNull;

public record AvailabilityWindowRequest(
        @NotNull Instant startTime,
        @NotNull Instant endTime,
        boolean tentative) {}
