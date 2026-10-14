package com.familyapp.consensus.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.familyapp.consensus.api.RankedChoiceResult.Round;

@Component
public class RankedChoiceTally {
    public TallyOutcome calculate(Set<UUID> ideaIds, List<List<UUID>> ballots) {
        if (ideaIds == null || ideaIds.isEmpty() || ballots == null || ballots.isEmpty()) {
            throw new IllegalArgumentException("At least one idea and one ballot are required.");
        }
        if (ideaIds.stream().anyMatch(id -> id == null)
                || ballots.stream().anyMatch(ballot -> ballot == null || ballot.isEmpty())) {
            throw new IllegalArgumentException("Ideas and ballots must not be empty or null.");
        }
        for (List<UUID> ballot : ballots) {
            if (new HashSet<>(ballot).size() != ballot.size() || !ideaIds.containsAll(ballot)) {
                throw new IllegalArgumentException("Each ballot must rank unique ideas from this outing.");
            }
        }

        Set<UUID> remaining = new HashSet<>(ideaIds);
        List<Round> rounds = new ArrayList<>();
        int roundNumber = 1;
        while (true) {
            Map<UUID, Integer> counts = new HashMap<>();
            remaining.forEach(ideaId -> counts.put(ideaId, 0));
            int activeBallotCount = 0;
            for (List<UUID> ballot : ballots) {
                UUID selected = ballot.stream()
                        .filter(remaining::contains)
                        .findFirst()
                        .orElse(null);
                if (selected != null) {
                    counts.compute(selected, (ignored, count) -> count + 1);
                    activeBallotCount++;
                }
            }

            if (remaining.size() == 1) {
                rounds.add(new Round(roundNumber, counts, List.of()));
            }

            UUID majorityWinner = null;
            if (activeBallotCount > 0) {
                for (Map.Entry<UUID, Integer> entry : counts.entrySet()) {
                    if (entry.getValue() * 2 > activeBallotCount) {
                        majorityWinner = entry.getKey();
                        break;
                    }
                }
            }
            if (majorityWinner != null) {
                rounds.add(new Round(roundNumber, counts, List.of()));
                return new TallyOutcome(majorityWinner, Set.of(), rounds);
            }

            int minimumCount = Integer.MAX_VALUE;
            int maximumCount = Integer.MIN_VALUE;
            for (int count : counts.values()) {
                minimumCount = Math.min(minimumCount, count);
                maximumCount = Math.max(maximumCount, count);
            }
            if (minimumCount == maximumCount || activeBallotCount == 0) {
                rounds.add(new Round(roundNumber, counts, List.of()));
                return new TallyOutcome(null, Set.copyOf(remaining), rounds);
            }

            Set<UUID> eliminated = new HashSet<>();
            for (Map.Entry<UUID, Integer> entry : counts.entrySet()) {
                if (entry.getValue() == minimumCount) {
                    eliminated.add(entry.getKey());
                }
            }
            rounds.add(new Round(
                    roundNumber,
                    counts,
                    eliminated.stream().sorted().toList()));
            remaining.removeAll(eliminated);
            roundNumber++;
        }

    }

    public record TallyOutcome(UUID winnerIdeaId, Set<UUID> tiedIdeaIds, List<Round> rounds) {
        public TallyOutcome {
            tiedIdeaIds = Set.copyOf(tiedIdeaIds);
            rounds = List.copyOf(rounds);
        }
    }
}
