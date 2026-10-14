package com.familyapp.consensus.api;

import java.util.List;
import java.util.UUID;

public record VoteResponse(UUID outingId, UUID voterId, List<UUID> rankedIdeaIds) {}
