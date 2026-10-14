package com.familyapp.consensus.api;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import com.familyapp.calendar.api.SlotConflictLevel;

public record OutingResponse(
        UUID id,
        String title,
        String description,
        OutingStatus status,
        Instant startTime,
        Instant endTime,
        Set<UUID> attendeeIds,
        SlotConflictLevel conflictLevel,
        int conflictedAttendeeCount,
        Set<UUID> conflictedAttendeeIds) {}
