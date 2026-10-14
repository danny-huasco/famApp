package com.familyapp.consensus.api;

import java.util.UUID;

public interface OutingMembershipPort {
    boolean isAttendee(UUID outingId, UUID familyId, UUID userId);
}
