package com.familyapp.calendar.internal.persistence;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "calendar_event_attendees",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_calendar_event_attendee",
                columnNames = {"outing_id_fk", "user_id_fk"}))
public class CalendarEventAttendeeEntity {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "outing_id_fk", nullable = false)
    private CalendarEventEntity event;

    @Column(name = "user_id_fk", nullable = false)
    private UUID userId;

    protected CalendarEventAttendeeEntity() {}

    CalendarEventAttendeeEntity(UUID id, CalendarEventEntity event, UUID userId) {
        this.id = id;
        this.event = event;
        this.userId = userId;
    }

    UUID getUserId() {
        return userId;
    }
}
