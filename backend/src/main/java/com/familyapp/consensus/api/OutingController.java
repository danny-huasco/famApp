package com.familyapp.consensus.api;

import jakarta.validation.Valid;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.familyapp.auth.security.FamilyPrincipal;
import com.familyapp.consensus.application.IdeaService;
import com.familyapp.consensus.application.OutingService;
import com.familyapp.consensus.application.VotingService;
import com.familyapp.idempotency.api.Idempotent;

@RestController
@RequestMapping("/api/outings")
public class OutingController {
    private final OutingService outingService;
    private final IdeaService ideaService;
    private final VotingService votingService;

    public OutingController(
            OutingService outingService,
            IdeaService ideaService,
            VotingService votingService) {
        this.outingService = outingService;
        this.ideaService = ideaService;
        this.votingService = votingService;
    }

    @PostMapping
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.CREATED)
    @Idempotent(operation = "outing.create")
    public OutingResponse createOuting(
            @AuthenticationPrincipal FamilyPrincipal principal,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateOutingRequest request) {
        return outingService.createOuting(principal, request);
    }

    @PostMapping("/{outingId}/ideas")
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.CREATED)
    @Idempotent(operation = "idea.create")
    public IdeaResponse submitIdea(
            @AuthenticationPrincipal FamilyPrincipal principal,
            @PathVariable UUID outingId,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateIdeaRequest request) {
        return ideaService.submitIdea(principal, outingId, request);
    }

    @GetMapping("/{outingId}/ideas")
    public java.util.List<IdeaResponse> listIdeas(
            @AuthenticationPrincipal FamilyPrincipal principal,
            @PathVariable UUID outingId) {
        return ideaService.listIdeas(principal, outingId);
    }

    @PostMapping("/{outingId}/votes")
    @Idempotent(operation = "vote.submit")
    public VoteResponse submitBallot(
            @AuthenticationPrincipal FamilyPrincipal principal,
            @PathVariable UUID outingId,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody SubmitVoteRequest request) {
        return votingService.submitBallot(principal, outingId, request);
    }

    @PostMapping("/{outingId}/finalize")
    @Idempotent(operation = "outing.finalize")
    public RankedChoiceResult finalizeByRankedChoice(
            @AuthenticationPrincipal FamilyPrincipal principal,
            @PathVariable UUID outingId,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey) {
        return votingService.finalizeByRankedChoice(principal, outingId);
    }

    @PostMapping("/{outingId}/tie-break")
    @Idempotent(operation = "outing.tie-break")
    public RankedChoiceResult resolveTieByCoinFlip(
            @AuthenticationPrincipal FamilyPrincipal principal,
            @PathVariable UUID outingId,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey) {
        return votingService.resolveTieByCoinFlip(principal, outingId);
    }
}
