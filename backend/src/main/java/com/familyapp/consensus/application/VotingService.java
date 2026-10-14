package com.familyapp.consensus.application;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.familyapp.auth.security.FamilyPrincipal;
import com.familyapp.consensus.api.FinalizationMethod;
import com.familyapp.consensus.api.InvalidOutingRequestException;
import com.familyapp.consensus.api.OutingNotFoundException;
import com.familyapp.consensus.api.OutingStateException;
import com.familyapp.consensus.api.OutingStatus;
import com.familyapp.consensus.api.RankedChoiceResult;
import com.familyapp.consensus.api.SubmitVoteRequest;
import com.familyapp.consensus.api.VoteResponse;
import com.familyapp.consensus.domain.RankedChoiceTally;
import com.familyapp.consensus.internal.persistence.IdeaEntity;
import com.familyapp.consensus.internal.persistence.IdeaRepository;
import com.familyapp.consensus.internal.persistence.OutingEntity;
import com.familyapp.consensus.internal.persistence.OutingRepository;
import com.familyapp.consensus.internal.persistence.RankedVoteEntity;
import com.familyapp.consensus.internal.persistence.RankedVoteRepository;
import com.familyapp.event.OutingFinalizedEvent;
import com.familyapp.event.VoteTallyUpdatedEvent;

@Service
public class VotingService {
    private final OutingRepository outingRepository;
    private final IdeaRepository ideaRepository;
    private final RankedVoteRepository rankedVoteRepository;
    private final RankedChoiceTally tally;
    private final SecureRandom secureRandom;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public VotingService(
            OutingRepository outingRepository,
            IdeaRepository ideaRepository,
            RankedVoteRepository rankedVoteRepository,
            RankedChoiceTally tally,
            ApplicationEventPublisher eventPublisher) {
        this(
                outingRepository,
                ideaRepository,
                rankedVoteRepository,
                tally,
                new SecureRandom(),
                eventPublisher);
    }

    VotingService(
            OutingRepository outingRepository,
            IdeaRepository ideaRepository,
            RankedVoteRepository rankedVoteRepository,
            RankedChoiceTally tally,
            SecureRandom secureRandom,
            ApplicationEventPublisher eventPublisher) {
        this.outingRepository = outingRepository;
        this.ideaRepository = ideaRepository;
        this.rankedVoteRepository = rankedVoteRepository;
        this.tally = tally;
        this.secureRandom = secureRandom;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public VoteResponse submitBallot(
            FamilyPrincipal principal,
            UUID outingId,
            SubmitVoteRequest request) {
        OutingEntity outing = findOuting(principal, outingId);
        requireOpenAttendee(outing, principal);
        if (request == null || request.rankedIdeaIds() == null || request.rankedIdeaIds().isEmpty()) {
            throw new InvalidOutingRequestException("At least one ranked idea is required.");
        }

        List<UUID> rankedIdeaIds = request.rankedIdeaIds();
        if (rankedIdeaIds.stream().anyMatch(id -> id == null)
                || new HashSet<>(rankedIdeaIds).size() != rankedIdeaIds.size()) {
            throw new InvalidOutingRequestException("A ballot must rank unique, valid ideas.");
        }
        List<IdeaEntity> ideas = ideaRepository.findAllByOuting_IdOrderByCreatedAtAscIdAsc(outingId);
        Map<UUID, IdeaEntity> ideasById = new HashMap<>();
        for (IdeaEntity idea : ideas) {
            ideasById.put(idea.getId(), idea);
        }
        if (!ideasById.keySet().containsAll(rankedIdeaIds)) {
            throw new InvalidOutingRequestException("Every ranked idea must belong to this outing.");
        }

        rankedVoteRepository.deleteBallot(outingId, principal.userId());
        List<RankedVoteEntity> ballot = new ArrayList<>();
        for (int index = 0; index < rankedIdeaIds.size(); index++) {
            ballot.add(new RankedVoteEntity(
                    UUID.randomUUID(),
                    outing,
                    ideasById.get(rankedIdeaIds.get(index)),
                    principal.userId(),
                    index + 1));
        }
        rankedVoteRepository.saveAll(ballot);
        outing.recordChange();
        publishTallyUpdate(outingId);
        return new VoteResponse(outingId, principal.userId(), List.copyOf(rankedIdeaIds));
    }

    @Transactional
    public RankedChoiceResult finalizeByRankedChoice(FamilyPrincipal principal, UUID outingId) {
        OutingEntity outing = findOuting(principal, outingId);
        requireOpenAttendee(outing, principal);
        RankedChoiceTally.TallyOutcome outcome = calculate(outingId);
        if (outcome.winnerIdeaId() != null) {
            outing.finalizeWith(outcome.winnerIdeaId(), FinalizationMethod.RANKED_CHOICE);
            publishFinalizedEvent(outing, outcome.winnerIdeaId());
        }
        return result(outing, outcome.winnerIdeaId(), outcome.tiedIdeaIds(), outcome.rounds());
    }

    @Transactional
    public RankedChoiceResult resolveTieByCoinFlip(FamilyPrincipal principal, UUID outingId) {
        OutingEntity outing = findOuting(principal, outingId);
        requireOpenAttendee(outing, principal);
        RankedChoiceTally.TallyOutcome outcome = calculate(outingId);
        if (outcome.tiedIdeaIds().size() < 2) {
            throw new OutingStateException("A coin flip is only available for a current ranked-choice tie.");
        }
        List<UUID> tiedIds = outcome.tiedIdeaIds().stream().sorted(Comparator.naturalOrder()).toList();
        UUID winnerId = tiedIds.get(secureRandom.nextInt(tiedIds.size()));
        outing.finalizeWith(winnerId, FinalizationMethod.COIN_FLIP);
        publishFinalizedEvent(outing, winnerId);
        return result(outing, winnerId, Set.of(), outcome.rounds());
    }

    private RankedChoiceTally.TallyOutcome calculate(UUID outingId) {
        List<IdeaEntity> ideas = ideaRepository.findAllByOuting_IdOrderByCreatedAtAscIdAsc(outingId);
        List<RankedVoteEntity> votes = rankedVoteRepository.findAllByOuting_IdOrderByVoterIdAscRankAsc(outingId);
        if (ideas.isEmpty() || votes.isEmpty()) {
            throw new OutingStateException("At least one idea and one ballot are required to finalize.");
        }
        Set<UUID> ideaIds = new HashSet<>();
        for (IdeaEntity idea : ideas) {
            ideaIds.add(idea.getId());
        }
        Map<UUID, List<UUID>> ballotsByVoter = new TreeMap<>();
        for (RankedVoteEntity vote : votes) {
            ballotsByVoter.computeIfAbsent(vote.getVoterId(), ignored -> new ArrayList<>())
                    .add(vote.getIdeaId());
        }
        List<List<UUID>> ballots = new ArrayList<>(ballotsByVoter.values());
        return tally.calculate(ideaIds, ballots);
    }

    private OutingEntity findOuting(FamilyPrincipal principal, UUID outingId) {
        if (principal == null || outingId == null) {
            throw new InvalidOutingRequestException("Authenticated user and outing ID are required.");
        }
        return outingRepository.findByIdAndFamilyId(outingId, principal.familyId())
                .orElseThrow(OutingNotFoundException::new);
    }

    private void requireOpenAttendee(OutingEntity outing, FamilyPrincipal principal) {
        if (outing.getStatus() != OutingStatus.OPEN) {
            throw new OutingStateException("Voting is only available while the outing is open.");
        }
        if (!outing.getAttendeeIds().contains(principal.userId())) {
            throw new InvalidOutingRequestException("Only outing attendees can vote.");
        }
    }

    private void publishFinalizedEvent(
            OutingEntity outing,
            UUID winnerIdeaId) {
        IdeaEntity winningIdea = ideaRepository.findById(Objects.requireNonNull(winnerIdeaId))
                .orElseThrow(() -> new OutingStateException("The selected winning idea no longer exists."));
        outing.recordChange();
        eventPublisher.publishEvent(new OutingFinalizedEvent(
                outing.getId(),
                outing.getTitle(),
                winnerIdeaId,
                winningIdea.getTitle(),
                outing.getAttendeeIds()));
    }

    private void publishTallyUpdate(UUID outingId) {
        Map<UUID, Integer> firstPreferenceCounts = new HashMap<>();
        Set<UUID> voterIds = new HashSet<>();
        for (RankedVoteEntity vote : rankedVoteRepository.findAllByOuting_IdOrderByVoterIdAscRankAsc(outingId)) {
            if (vote.getRank() == 1) {
                UUID ideaId = vote.getIdeaId();
                Integer count = firstPreferenceCounts.get(ideaId);
                firstPreferenceCounts.put(ideaId, count == null ? 1 : count + 1);
                voterIds.add(vote.getVoterId());
            }
        }
        eventPublisher.publishEvent(new VoteTallyUpdatedEvent(
                outingId,
                voterIds.size(),
                firstPreferenceCounts));
    }

    private RankedChoiceResult result(
            OutingEntity outing,
            UUID winnerIdeaId,
            Set<UUID> tiedIdeaIds,
            List<RankedChoiceResult.Round> rounds) {
        return new RankedChoiceResult(
                outing.getId(),
                outing.getStatus(),
                winnerIdeaId,
                tiedIdeaIds.stream().sorted().toList(),
                outing.getFinalizationMethod(),
                rounds);
    }
}
