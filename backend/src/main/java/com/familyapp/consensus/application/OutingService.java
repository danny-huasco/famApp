package com.familyapp.consensus.application;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.familyapp.auth.security.FamilyPrincipal;
import com.familyapp.calendar.api.BookedEventPort;
import com.familyapp.calendar.api.CalendarConflictAssessment;
import com.familyapp.calendar.api.ScheduleConflictPort;
import com.familyapp.calendar.api.SlotConflictLevel;
import com.familyapp.consensus.api.CreateOutingRequest;
import com.familyapp.consensus.api.InvalidOutingRequestException;
import com.familyapp.consensus.api.OutingConflictException;
import com.familyapp.consensus.api.OutingResponse;
import com.familyapp.consensus.api.OutingStatus;
import com.familyapp.consensus.internal.persistence.OutingEntity;
import com.familyapp.consensus.internal.persistence.OutingRepository;
import com.familyapp.event.OutingCreatedEvent;
import com.familyapp.user.api.FamilyMembershipPort;

@Service
public class OutingService {
    private final OutingRepository outingRepository;
    private final FamilyMembershipPort familyMembershipPort;
    private final ScheduleConflictPort scheduleConflictPort;
    private final BookedEventPort bookedEventPort;
    private final ApplicationEventPublisher eventPublisher;

    public OutingService(
            OutingRepository outingRepository,
            FamilyMembershipPort familyMembershipPort,
            ScheduleConflictPort scheduleConflictPort,
            BookedEventPort bookedEventPort,
            ApplicationEventPublisher eventPublisher) {
        this.outingRepository = outingRepository;
        this.familyMembershipPort = familyMembershipPort;
        this.scheduleConflictPort = scheduleConflictPort;
        this.bookedEventPort = bookedEventPort;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public OutingResponse createOuting(FamilyPrincipal principal, CreateOutingRequest request) {
        if (principal == null || request == null) {
            throw new InvalidOutingRequestException("Authenticated user and outing details are required.");
        }
        if (request.startTime() == null || request.endTime() == null
                || !request.endTime().isAfter(request.startTime())) {
            throw new InvalidOutingRequestException("Outing end time must be after its start time.");
        }
        if (request.attendeeIds() == null || request.attendeeIds().isEmpty()
                || request.attendeeIds().stream().anyMatch(Objects::isNull)) {
            throw new InvalidOutingRequestException("At least one valid attendee is required.");
        }

        Set<UUID> attendeeIds = new HashSet<>(request.attendeeIds());
        if (attendeeIds.size() != request.attendeeIds().size()) {
            throw new InvalidOutingRequestException("Outing attendees must be unique.");
        }
        attendeeIds.add(principal.userId());
        Set<UUID> familyMemberIds = familyMembershipPort.findMemberIds(principal.familyId());
        if (!familyMemberIds.containsAll(attendeeIds)) {
            throw new InvalidOutingRequestException("All attendees must belong to your family.");
        }

        CalendarConflictAssessment assessment = scheduleConflictPort.assessConflicts(
                request.startTime(), request.endTime(), attendeeIds);
        if (assessment.level() == SlotConflictLevel.RED) {
            throw new OutingConflictException();
        }

        UUID outingId = UUID.randomUUID();
        OutingEntity outing = new OutingEntity(
                outingId,
                request.title(),
                request.description(),
                principal.familyId(),
                principal.userId(),
                request.startTime(),
                request.endTime());
        attendeeIds.forEach(outing::addAttendee);
        OutingEntity savedOuting = outingRepository.save(outing);
        bookedEventPort.recordBookedEvent(
                outingId,
                request.title(),
                request.startTime(),
                request.endTime(),
                attendeeIds);
        eventPublisher.publishEvent(new OutingCreatedEvent(
                outingId,
                savedOuting.getTitle(),
                attendeeIds,
                assessment.conflictedAttendeeIds()));
        return new OutingResponse(
                savedOuting.getId(),
                savedOuting.getTitle(),
                savedOuting.getDescription(),
                OutingStatus.OPEN,
                savedOuting.getStartTime(),
                savedOuting.getEndTime(),
                savedOuting.getAttendeeIds(),
                assessment.level(),
                assessment.conflictedAttendeeCount(),
                assessment.conflictedAttendeeIds());
    }
}
