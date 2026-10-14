package com.familyapp.consensus.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import com.familyapp.auth.security.FamilyPrincipal;
import com.familyapp.calendar.api.BookedEventPort;
import com.familyapp.calendar.api.CalendarConflictAssessment;
import com.familyapp.calendar.api.ScheduleConflictPort;
import com.familyapp.calendar.api.SlotConflictLevel;
import com.familyapp.consensus.api.CreateOutingRequest;
import com.familyapp.consensus.api.InvalidOutingRequestException;
import com.familyapp.consensus.api.OutingConflictException;
import com.familyapp.consensus.api.OutingStatus;
import com.familyapp.consensus.internal.persistence.OutingEntity;
import com.familyapp.consensus.internal.persistence.OutingRepository;
import com.familyapp.event.OutingCreatedEvent;
import com.familyapp.user.api.FamilyMembershipPort;

class OutingServiceTest {
    private OutingRepository outingRepository;
    private FamilyMembershipPort familyMembershipPort;
    private ScheduleConflictPort scheduleConflictPort;
    private BookedEventPort bookedEventPort;
    private ApplicationEventPublisher eventPublisher;
    private OutingService outingService;

    @BeforeEach
    void setUp() {
        outingRepository = mock(OutingRepository.class);
        familyMembershipPort = mock(FamilyMembershipPort.class);
        scheduleConflictPort = mock(ScheduleConflictPort.class);
        bookedEventPort = mock(BookedEventPort.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        outingService = new OutingService(
                outingRepository,
                familyMembershipPort,
                scheduleConflictPort,
                bookedEventPort,
                eventPublisher);
    }

    @Test
    @SuppressWarnings("null")
    void createsOrangeOutingAndRecordsCalendarEventWithExplicitAttendees() {
        UUID userId = UUID.randomUUID();
        UUID familyId = UUID.randomUUID();
        UUID attendeeId = UUID.randomUUID();
        FamilyPrincipal principal = new FamilyPrincipal(userId, familyId);
        Set<UUID> attendees = Set.of(userId, attendeeId);
        CreateOutingRequest request = request(List.of(attendeeId));
        when(familyMembershipPort.findMemberIds(familyId)).thenReturn(attendees);
        when(scheduleConflictPort.assessConflicts(
                request.startTime(), request.endTime(), attendees))
                .thenReturn(new CalendarConflictAssessment(
                        SlotConflictLevel.ORANGE, 2, 1, Set.of(attendeeId)));
        when(outingRepository.save(org.mockito.ArgumentMatchers.any(OutingEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = outingService.createOuting(principal, request);

        assertEquals(OutingStatus.OPEN, response.status());
        assertEquals(attendees, response.attendeeIds());
        assertEquals(SlotConflictLevel.ORANGE, response.conflictLevel());
        assertEquals(Set.of(attendeeId), response.conflictedAttendeeIds());
        verify(bookedEventPort).recordBookedEvent(
                response.id(), request.title(), request.startTime(), request.endTime(), attendees);
        verify(eventPublisher).publishEvent(new OutingCreatedEvent(
                response.id(),
                request.title(),
                attendees,
                Set.of(attendeeId)));
    }

    @Test
    void rejectsRedSlotWithoutSavingOutingOrBookedEvent() {
        UUID userId = UUID.randomUUID();
        UUID familyId = UUID.randomUUID();
        UUID attendeeId = UUID.randomUUID();
        FamilyPrincipal principal = new FamilyPrincipal(userId, familyId);
        Set<UUID> attendees = Set.of(userId, attendeeId);
        CreateOutingRequest request = request(List.of(attendeeId));
        when(familyMembershipPort.findMemberIds(familyId)).thenReturn(attendees);
        when(scheduleConflictPort.assessConflicts(
                request.startTime(), request.endTime(), attendees))
                .thenReturn(new CalendarConflictAssessment(
                        SlotConflictLevel.RED, 2, 2, attendees));

        assertThrows(
                OutingConflictException.class,
                () -> outingService.createOuting(principal, request));

        verifyNoInteractions(outingRepository, bookedEventPort, eventPublisher);
    }

    @Test
    void rejectsAttendeesOutsideAuthenticatedFamily() {
        UUID userId = UUID.randomUUID();
        UUID familyId = UUID.randomUUID();
        UUID foreignAttendeeId = UUID.randomUUID();
        when(familyMembershipPort.findMemberIds(familyId)).thenReturn(Set.of(userId));

        assertThrows(
                InvalidOutingRequestException.class,
                () -> outingService.createOuting(
                        new FamilyPrincipal(userId, familyId), request(List.of(foreignAttendeeId))));

        verifyNoInteractions(scheduleConflictPort, outingRepository, bookedEventPort);
    }

    @Test
    void rejectsInvalidTimeAndDuplicateAttendees() {
        UUID userId = UUID.randomUUID();
        UUID familyId = UUID.randomUUID();
        when(familyMembershipPort.findMemberIds(familyId)).thenReturn(Set.of(userId));

        CreateOutingRequest invalidTime = new CreateOutingRequest(
                "Outing", null, Instant.parse("2026-01-01T11:00:00Z"),
                Instant.parse("2026-01-01T10:00:00Z"), List.of(userId));
        CreateOutingRequest duplicateAttendees = request(List.of(userId, userId));

        assertThrows(InvalidOutingRequestException.class,
                () -> outingService.createOuting(
                        new FamilyPrincipal(userId, familyId), invalidTime));
        assertThrows(InvalidOutingRequestException.class,
                () -> outingService.createOuting(
                        new FamilyPrincipal(userId, familyId), duplicateAttendees));
    }

    private CreateOutingRequest request(List<UUID> attendeeIds) {
        return new CreateOutingRequest(
                "Family outing",
                "A sample outing",
                Instant.parse("2026-01-01T10:00:00Z"),
                Instant.parse("2026-01-01T11:00:00Z"),
                attendeeIds);
    }
}
