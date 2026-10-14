package com.familyapp.consensus.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class RankedChoiceTallyTest {
    private final RankedChoiceTally tally = new RankedChoiceTally();

    @Test
    void redistributesVotesAfterEliminatingLowestIdea() {
        UUID ideaA = UUID.randomUUID();
        UUID ideaB = UUID.randomUUID();
        UUID ideaC = UUID.randomUUID();

        var outcome = tally.calculate(
                Set.of(ideaA, ideaB, ideaC),
                List.of(
                        List.of(ideaA, ideaB, ideaC),
                        List.of(ideaA, ideaB, ideaC),
                        List.of(ideaB, ideaC, ideaA),
                        List.of(ideaC, ideaB, ideaA),
                        List.of(ideaC, ideaB, ideaA)));

        assertEquals(ideaC, outcome.winnerIdeaId());
        assertEquals(2, outcome.rounds().size());
        assertEquals(Set.of(ideaB), Set.copyOf(outcome.rounds().getFirst().eliminatedIdeaIds()));
    }

    @Test
    void reportsAnUnresolvedTieWithoutPickingAnIdea() {
        UUID ideaA = UUID.randomUUID();
        UUID ideaB = UUID.randomUUID();
        UUID ideaC = UUID.randomUUID();

        var outcome = tally.calculate(
                Set.of(ideaA, ideaB, ideaC),
                List.of(List.of(ideaA), List.of(ideaB), List.of(ideaC)));

        assertNull(outcome.winnerIdeaId());
        assertEquals(Set.of(ideaA, ideaB, ideaC), outcome.tiedIdeaIds());
    }

    @Test
    void rejectsDuplicateOrForeignIdeasInBallots() {
        UUID ideaA = UUID.randomUUID();
        UUID ideaB = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class,
                () -> tally.calculate(
                        Set.of(ideaA, ideaB),
                        List.of(List.of(ideaA, ideaA))));
        assertThrows(IllegalArgumentException.class,
                () -> tally.calculate(Set.of(ideaA), List.of(List.of(ideaB))));
    }
}
