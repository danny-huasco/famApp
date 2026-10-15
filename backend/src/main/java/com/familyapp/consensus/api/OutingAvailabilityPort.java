package com.familyapp.consensus.api;

import java.util.Optional;
import java.util.UUID;

public interface OutingAvailabilityPort {
    Optional<OutingAvailabilityContext> findAvailabilityContext(UUID outingId);
}
