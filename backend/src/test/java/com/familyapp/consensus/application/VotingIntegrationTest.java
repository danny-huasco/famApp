package com.familyapp.consensus.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.familyapp.auth.security.FamilyPrincipal;
import com.familyapp.consensus.api.CreateIdeaRequest;
import com.familyapp.consensus.api.CreateOutingRequest;
import com.familyapp.consensus.api.OutingStatus;
import com.familyapp.consensus.api.SubmitVoteRequest;
import com.familyapp.consensus.internal.persistence.RankedVoteRepository;
import com.familyapp.user.api.UserAccountPort;
import com.familyapp.user.internal.persistence.FamilyRepository;
import com.familyapp.user.internal.persistence.UserEntity;
import com.familyapp.user.internal.persistence.UserRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class VotingIntegrationTest {
    @Autowired
    private OutingService outingService;

    @Autowired
    private IdeaService ideaService;

    @Autowired
    private VotingService votingService;

    @Autowired
    private UserAccountPort userAccountPort;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FamilyRepository familyRepository;

    @Autowired
    private RankedVoteRepository rankedVoteRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void attendeeCanReplaceBallotAndFinalizeTheRankedChoiceWinner() {
        var creator = createAccount();
        var attendee = addFamilyMember(creator.familyId());
        FamilyPrincipal principal = new FamilyPrincipal(creator.userId(), creator.familyId());
        var outing = createOuting(creator.userId(), attendee);
        var ideaA = ideaService.submitIdea(principal, outing.id(), new CreateIdeaRequest("Idea A", null));
        var ideaB = ideaService.submitIdea(principal, outing.id(), new CreateIdeaRequest("Idea B", null));

        votingService.submitBallot(
                principal, outing.id(), new SubmitVoteRequest(List.of(ideaA.id(), ideaB.id())));
        votingService.submitBallot(
                principal, outing.id(), new SubmitVoteRequest(List.of(ideaB.id(), ideaA.id())));

        var persistedBallot = rankedVoteRepository.findAllByOuting_IdOrderByVoterIdAscRankAsc(outing.id());
        assertEquals(List.of(ideaB.id(), ideaA.id()),
                persistedBallot.stream().map(vote -> vote.getIdeaId()).toList());

        var result = votingService.finalizeByRankedChoice(principal, outing.id());

        assertEquals(ideaB.id(), result.winnerIdeaId());
        assertEquals(OutingStatus.FINALIZED, result.status());
        assertEquals("RANKED_CHOICE", result.finalizationMethod().name());
    }

    @Test
    void tiedRankingsKeepOutingOpenAndExplicitCoinFlipCanResolveOnlyThatTie() {
        var creator = createAccount();
        var attendee = addFamilyMember(creator.familyId());
        FamilyPrincipal creatorPrincipal = new FamilyPrincipal(creator.userId(), creator.familyId());
        FamilyPrincipal attendeePrincipal = new FamilyPrincipal(attendee.getId(), creator.familyId());
        var outing = createOuting(creator.userId(), attendee.getId());
        var ideaA = ideaService.submitIdea(
                creatorPrincipal, outing.id(), new CreateIdeaRequest("Idea A", null));
        var ideaB = ideaService.submitIdea(
                creatorPrincipal, outing.id(), new CreateIdeaRequest("Idea B", null));
        votingService.submitBallot(
                creatorPrincipal, outing.id(), new SubmitVoteRequest(List.of(ideaA.id(), ideaB.id())));
        votingService.submitBallot(
                attendeePrincipal, outing.id(), new SubmitVoteRequest(List.of(ideaB.id(), ideaA.id())));

        var tiedResult = votingService.finalizeByRankedChoice(creatorPrincipal, outing.id());

        assertEquals(OutingStatus.OPEN, tiedResult.status());
        assertNull(tiedResult.winnerIdeaId());
        assertEquals(
                java.util.Set.of(ideaA.id(), ideaB.id()),
                java.util.Set.copyOf(tiedResult.tiedIdeaIds()));

        var coinFlipResult = votingService.resolveTieByCoinFlip(creatorPrincipal, outing.id());

        assertEquals(OutingStatus.FINALIZED, coinFlipResult.status());
        assertTrue(java.util.Set.of(ideaA.id(), ideaB.id()).contains(coinFlipResult.winnerIdeaId()));
        assertEquals("COIN_FLIP", coinFlipResult.finalizationMethod().name());
    }

    private UserAccountPort.UserIdentity createAccount() {
        String suffix = UUID.randomUUID().toString();
        return userAccountPort.createFamilyAccount(
                "voting-" + suffix + "@example.com",
                passwordEncoder.encode("secure-password-123"),
                "Creator",
                "Voting test family");
    }

    private UserEntity addFamilyMember(UUID familyId) {
        var family = familyRepository.findById(Objects.requireNonNull(familyId)).orElseThrow();
        String suffix = UUID.randomUUID().toString();
        return userRepository.save(new UserEntity(
                UUID.randomUUID(),
                "attendee-" + suffix + "@example.com",
                passwordEncoder.encode("secure-password-123"),
                "Attendee",
                family));
    }

    private com.familyapp.consensus.api.OutingResponse createOuting(
            UUID creatorId,
            UserEntity attendee) {
        return createOuting(creatorId, attendee.getId());
    }

    private com.familyapp.consensus.api.OutingResponse createOuting(UUID creatorId, UUID attendeeId) {
        var creator = userRepository.findById(Objects.requireNonNull(creatorId)).orElseThrow();
        var attendee = userRepository.findById(Objects.requireNonNull(attendeeId)).orElseThrow();
        return outingService.createOuting(
                new FamilyPrincipal(creatorId, creator.getFamily().getId()),
                new CreateOutingRequest(
                        "Voting outing",
                        "Ideas and ranked ballots",
                        Instant.parse("2026-01-01T10:00:00Z"),
                        Instant.parse("2026-01-01T11:00:00Z"),
                        List.of(attendee.getId())));
    }
}
