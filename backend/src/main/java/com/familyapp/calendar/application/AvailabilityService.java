package com.familyapp.calendar.application;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.familyapp.auth.security.FamilyPrincipal;
import com.familyapp.calendar.api.AvailabilityQueryPort;
import com.familyapp.calendar.api.AvailabilityOverviewResponse;
import com.familyapp.calendar.api.AvailabilityWindowRequest;
import com.familyapp.calendar.api.AvailabilityWindowResponse;
import com.familyapp.calendar.api.AttendeeAvailabilityResponse;
import com.familyapp.calendar.api.CommonFreeSlot;
import com.familyapp.calendar.api.InvalidAvailabilityException;
import com.familyapp.calendar.api.ReplaceAvailabilityRequest;
import com.familyapp.calendar.domain.CommonFreeSlotCalculator;
import com.familyapp.calendar.internal.persistence.UserAvailabilityEntity;
import com.familyapp.calendar.internal.persistence.UserAvailabilityRepository;
import com.familyapp.consensus.api.OutingAvailabilityContext;
import com.familyapp.consensus.api.OutingAvailabilityPort;
import com.familyapp.consensus.api.OutingNotFoundException;

@Service
@Transactional(readOnly = true)
public class AvailabilityService implements AvailabilityQueryPort {
    private final UserAvailabilityRepository availabilityRepository;
    private final CommonFreeSlotCalculator calculator;
    private final OutingAvailabilityPort outingAvailabilityPort;

    public AvailabilityService(
            UserAvailabilityRepository availabilityRepository,
            CommonFreeSlotCalculator calculator,
            OutingAvailabilityPort outingAvailabilityPort) {
        this.availabilityRepository = availabilityRepository;
        this.calculator = calculator;
        this.outingAvailabilityPort = outingAvailabilityPort;
    }

    public List<CommonFreeSlot> findCommonFreeSlots(UUID outingId) {
        List<UUID> attendeeIds = availabilityRepository.findDistinctUserIdsByOutingId(outingId);
        return findCommonFreeSlots(outingId, Set.copyOf(attendeeIds));
    }

    @Override
    public List<CommonFreeSlot> findCommonFreeSlots(UUID outingId, Set<UUID> attendeeIds) {
        if (attendeeIds.isEmpty()) {
            return List.of();
        }

        List<UserAvailabilityEntity> windows =
                availabilityRepository.findAllByOutingIdAndUserIdIn(outingId, attendeeIds);
        var windowsByAttendee = windows.stream()
                .collect(java.util.stream.Collectors.groupingBy(UserAvailabilityEntity::getUserId));
        if (attendeeIds.stream().anyMatch(attendeeId -> !windowsByAttendee.containsKey(attendeeId))) {
            return List.of();
        }

        List<List<com.familyapp.calendar.api.AvailabilityWindow>> attendeeWindows = attendeeIds.stream()
                .map(attendeeId -> windowsByAttendee.get(attendeeId).stream()
                        .map(UserAvailabilityEntity::toWindow)
                        .toList())
                .toList();
        return calculator.calculate(attendeeWindows);
    }

    @Transactional
    public AvailabilityOverviewResponse getOverview(FamilyPrincipal principal, UUID outingId) {
        OutingAvailabilityContext outing = findAuthorizedOuting(principal, outingId);
        List<UserAvailabilityEntity> records =
                availabilityRepository.findAllByOutingIdAndUserIdInOrderByStartTimeAsc(
                        outingId, outing.attendeeIds());
        var windowsByAttendee = records.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        UserAvailabilityEntity::getUserId));
        List<AttendeeAvailabilityResponse> attendees = outing.attendeeIds().stream()
                .sorted()
                .map(attendeeId -> new AttendeeAvailabilityResponse(
                        attendeeId,
                        windowsByAttendee.getOrDefault(attendeeId, List.of()).stream()
                                .map(this::toResponse)
                                .toList()))
                .toList();
        List<AvailabilityWindowResponse> myWindows =
                windowsByAttendee.getOrDefault(principal.userId(), List.of()).stream()
                        .map(this::toResponse)
                        .toList();
        return new AvailabilityOverviewResponse(
                outingId,
                myWindows,
                attendees,
                findCommonFreeSlots(outingId, outing.attendeeIds()));
    }

    @Transactional
    public AvailabilityOverviewResponse replaceMyAvailability(
            FamilyPrincipal principal,
            UUID outingId,
            ReplaceAvailabilityRequest request) {
        OutingAvailabilityContext outing = findAuthorizedOuting(principal, outingId);
        if (request == null || request.windows() == null) {
            throw new InvalidAvailabilityException("Availability windows are required.");
        }
        for (AvailabilityWindowRequest window : request.windows()) {
            if (window == null || window.startTime() == null || window.endTime() == null
                    || !window.endTime().isAfter(window.startTime())) {
                throw new InvalidAvailabilityException(
                        "Every availability window must end after it starts.");
            }
        }

        availabilityRepository.deleteAllByOutingIdAndUserId(outingId, principal.userId());
        List<UserAvailabilityEntity> records = request.windows().stream()
                .map(window -> new UserAvailabilityEntity(
                        UUID.randomUUID(),
                        principal.userId(),
                        outingId,
                        window.startTime(),
                        window.endTime(),
                        window.tentative()))
                .toList();
        availabilityRepository.saveAll(records);
        return getOverview(principal, outingId);
    }

    private OutingAvailabilityContext findAuthorizedOuting(
            FamilyPrincipal principal,
            UUID outingId) {
        if (principal == null || outingId == null) {
            throw new OutingNotFoundException();
        }
        OutingAvailabilityContext outing = outingAvailabilityPort
                .findAvailabilityContext(outingId)
                .orElseThrow(OutingNotFoundException::new);
        if (!outing.familyId().equals(principal.familyId())
                || !outing.attendeeIds().contains(principal.userId())) {
            throw new OutingNotFoundException();
        }
        return outing;
    }

    private AvailabilityWindowResponse toResponse(UserAvailabilityEntity entity) {
        return new AvailabilityWindowResponse(
                entity.getId(),
                entity.getUserId(),
                entity.getStartTime(),
                entity.getEndTime(),
                entity.isTentative());
    }
}
