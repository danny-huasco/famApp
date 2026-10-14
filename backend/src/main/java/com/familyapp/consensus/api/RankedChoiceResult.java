package com.familyapp.consensus.api;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record RankedChoiceResult(
        UUID outingId,
        OutingStatus status,
        UUID winnerIdeaId,
        List<UUID> tiedIdeaIds,
        FinalizationMethod finalizationMethod,
        List<Round> rounds) {
    public RankedChoiceResult {
        tiedIdeaIds = List.copyOf(tiedIdeaIds);
        rounds = List.copyOf(rounds);
    }

    public record Round(
            int roundNumber,
            Map<UUID, Integer> voteCounts,
            List<UUID> eliminatedIdeaIds) {
        public Round {
            voteCounts = Map.copyOf(voteCounts);
            eliminatedIdeaIds = List.copyOf(eliminatedIdeaIds);
        }
    }
}
