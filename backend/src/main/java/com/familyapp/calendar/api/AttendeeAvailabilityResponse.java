package com.familyapp.calendar.api;

import java.util.List;
import java.util.UUID;

public record AttendeeAvailabilityResponse(UUID userId, List<AvailabilityWindowResponse> windows) {
    public AttendeeAvailabilityResponse {
        windows = List.copyOf(windows);
    }
}
