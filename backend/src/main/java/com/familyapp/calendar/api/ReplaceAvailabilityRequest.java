package com.familyapp.calendar.api;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record ReplaceAvailabilityRequest(
        @NotNull List<@NotNull @Valid AvailabilityWindowRequest> windows) {}
