package com.familyapp.consensus.api;

import java.util.Optional;
import java.util.UUID;

public interface OutingLedgerPort {
    Optional<OutingLedgerContext> findLedgerContext(UUID outingId);
}
