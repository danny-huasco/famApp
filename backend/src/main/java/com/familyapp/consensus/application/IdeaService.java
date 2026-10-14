package com.familyapp.consensus.application;

import java.time.Instant;
import java.util.UUID;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.familyapp.auth.security.FamilyPrincipal;
import com.familyapp.consensus.api.CreateIdeaRequest;
import com.familyapp.consensus.api.IdeaResponse;
import com.familyapp.consensus.api.InvalidOutingRequestException;
import com.familyapp.consensus.api.OutingNotFoundException;
import com.familyapp.consensus.api.OutingStateException;
import com.familyapp.consensus.api.OutingStatus;
import com.familyapp.consensus.internal.persistence.IdeaEntity;
import com.familyapp.consensus.internal.persistence.IdeaRepository;
import com.familyapp.consensus.internal.persistence.OutingEntity;
import com.familyapp.consensus.internal.persistence.OutingRepository;

@Service
public class IdeaService {
    private final OutingRepository outingRepository;
    private final IdeaRepository ideaRepository;

    public IdeaService(OutingRepository outingRepository, IdeaRepository ideaRepository) {
        this.outingRepository = outingRepository;
        this.ideaRepository = ideaRepository;
    }

    @Transactional
    public IdeaResponse submitIdea(
            FamilyPrincipal principal,
            UUID outingId,
            CreateIdeaRequest request) {
        OutingEntity outing = findOutingForUpdate(principal, outingId);
        if (outing.getStatus() != OutingStatus.OPEN) {
            throw new OutingStateException("Ideas can only be submitted while the outing is open.");
        }
        if (!outing.getAttendeeIds().contains(principal.userId())) {
            throw new InvalidOutingRequestException("Only outing attendees can submit ideas.");
        }
        if (request == null || request.title() == null || request.title().isBlank()) {
            throw new InvalidOutingRequestException("Idea title is required.");
        }

        IdeaEntity idea = ideaRepository.save(new IdeaEntity(
                UUID.randomUUID(),
                outing,
                principal.userId(),
                request.title(),
                request.description(),
                Instant.now()));
        outing.recordChange();
        return new IdeaResponse(
                idea.getId(),
                idea.getOutingId(),
                idea.getProposerId(),
                idea.getTitle(),
                idea.getDescription(),
                idea.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public List<IdeaResponse> listIdeas(FamilyPrincipal principal, UUID outingId) {
        OutingEntity outing = findOuting(principal, outingId);
        if (!outing.getAttendeeIds().contains(principal.userId())) {
            throw new InvalidOutingRequestException("Only outing attendees can view ideas.");
        }
        return ideaRepository.findAllByOuting_IdOrderByCreatedAtAscIdAsc(outingId).stream()
                .map(idea -> new IdeaResponse(
                        idea.getId(),
                        idea.getOutingId(),
                        idea.getProposerId(),
                        idea.getTitle(),
                        idea.getDescription(),
                        idea.getCreatedAt()))
                .collect(Collectors.toUnmodifiableList());
    }

    private OutingEntity findOuting(FamilyPrincipal principal, UUID outingId) {
        validatePrincipal(principal, outingId);
        return outingRepository.findByIdAndFamilyId(outingId, principal.familyId())
                .orElseThrow(OutingNotFoundException::new);
    }

    private OutingEntity findOutingForUpdate(FamilyPrincipal principal, UUID outingId) {
        validatePrincipal(principal, outingId);
        return outingRepository.findByIdAndFamilyId(outingId, principal.familyId())
                .orElseThrow(OutingNotFoundException::new);
    }

    private void validatePrincipal(FamilyPrincipal principal, UUID outingId) {
        if (principal == null || outingId == null) {
            throw new InvalidOutingRequestException("Authenticated user and outing ID are required.");
        }
    }
}
