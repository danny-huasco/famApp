package com.familyapp.calendar.application;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.familyapp.calendar.api.CalendarConflictAssessment;
import com.familyapp.calendar.api.ScheduleConflictPort;
import com.familyapp.calendar.domain.CalendarConflictClassifier;
import com.familyapp.calendar.internal.persistence.CalendarEventRepository;

@Service
@Transactional(readOnly = true)
public class CalendarConflictService implements ScheduleConflictPort {
    private final CalendarEventRepository calendarEventRepository;
    private final CalendarConflictClassifier classifier;

    public CalendarConflictService(CalendarEventRepository calendarEventRepository) {
        this.calendarEventRepository = calendarEventRepository;
        this.classifier = new CalendarConflictClassifier();
    }

    @Override
    public CalendarConflictAssessment assessConflicts(
            Instant proposedStart,
            Instant proposedEnd,
            Set<UUID> attendeeIds) {
        if (proposedStart == null || proposedEnd == null || !proposedEnd.isAfter(proposedStart)) {
            throw new IllegalArgumentException("Proposed event end must be after its start.");
        }
        if (attendeeIds == null || attendeeIds.isEmpty()
                || attendeeIds.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("At least one valid attendee is required.");
        }

        Set<UUID> conflicts = calendarEventRepository.findConflictedAttendeeIds(
                attendeeIds, proposedStart, proposedEnd);
        return new CalendarConflictAssessment(
                classifier.classify(attendeeIds.size(), conflicts.size()),
                attendeeIds.size(),
                conflicts.size(),
                conflicts);
    }
}
