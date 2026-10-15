package com.familyapp.calendar.api;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.familyapp.auth.security.FamilyPrincipal;
import com.familyapp.calendar.application.AvailabilityService;
import com.familyapp.idempotency.api.Idempotent;

@RestController
@RequestMapping("/api/outings/{outingId}/availability")
public class AvailabilityController {
    private final AvailabilityService availabilityService;

    public AvailabilityController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    @GetMapping
    public AvailabilityOverviewResponse getAvailability(
            @AuthenticationPrincipal FamilyPrincipal principal,
            @PathVariable UUID outingId) {
        return availabilityService.getOverview(principal, outingId);
    }

    @PutMapping
    @Idempotent(operation = "availability.replace")
    public AvailabilityOverviewResponse replaceAvailability(
            @AuthenticationPrincipal FamilyPrincipal principal,
            @PathVariable UUID outingId,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody ReplaceAvailabilityRequest request) {
        return availabilityService.replaceMyAvailability(principal, outingId, request);
    }
}
