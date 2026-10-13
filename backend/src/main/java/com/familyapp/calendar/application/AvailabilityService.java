package com.familyapp.calendar.application;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.familyapp.calendar.api.AvailabilityQueryPort;
import com.familyapp.calendar.api.CommonFreeSlot;
import com.familyapp.calendar.domain.CommonFreeSlotCalculator;
import com.familyapp.calendar.internal.persistence.UserAvailabilityEntity;
import com.familyapp.calendar.internal.persistence.UserAvailabilityRepository;

@Service
@Transactional(readOnly = true)
public class AvailabilityService implements AvailabilityQueryPort {
    private final UserAvailabilityRepository availabilityRepository;
    private final CommonFreeSlotCalculator calculator;

    public AvailabilityService(
            UserAvailabilityRepository availabilityRepository,
            CommonFreeSlotCalculator calculator) {
        this.availabilityRepository = availabilityRepository;
        this.calculator = calculator;
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
}
