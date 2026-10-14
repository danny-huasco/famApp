package com.familyapp.consensus.api;

import java.time.Instant;
import java.util.UUID;

public record IdeaResponse(
        UUID id,
        UUID outingId,
        UUID proposerId,
        String title,
        String description,
        Instant createdAt) {}
