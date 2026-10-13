package com.familyapp.calendar.internal.persistence;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CalendarEventRepository
        extends JpaRepository<CalendarEventEntity, UUID> {
    @Query("""
            select distinct attendee.userId
            from CalendarEventAttendeeEntity attendee
            join attendee.event event
            where attendee.userId in :attendeeIds
              and event.startTime < :proposedEnd
              and event.endTime > :proposedStart
            """)
    Set<UUID> findConflictedAttendeeIds(
            @Param("attendeeIds") Set<UUID> attendeeIds,
            @Param("proposedStart") Instant proposedStart,
            @Param("proposedEnd") Instant proposedEnd);
}
