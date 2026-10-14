package com.familyapp.consensus.api;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record SubmitVoteRequest(@NotEmpty List<@NotNull UUID> rankedIdeaIds) {}
