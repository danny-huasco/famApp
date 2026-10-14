package com.familyapp.consensus.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateIdeaRequest(
        @NotBlank @Size(max = 180) String title,
        @Size(max = 2000) String description) {}
