package com.familyapp.calendar.api;

import java.util.List;
import java.util.UUID;

public record AvailabilityOverviewResponse(
        UUID outingId,
        List<AvailabilityWindowResponse> myWindows,
        List<AttendeeAvailabilityResponse> attendees,
        List<CommonFreeSlot> commonFreeSlots) {
    public AvailabilityOverviewResponse {
        myWindows = List.copyOf(myWindows);
        attendees = List.copyOf(attendees);
        commonFreeSlots = List.copyOf(commonFreeSlots);
    }
}
