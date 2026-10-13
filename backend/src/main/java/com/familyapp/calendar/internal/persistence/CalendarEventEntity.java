package com.familyapp.calendar.internal.persistence;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "calendar_events")
public class CalendarEventEntity {
    @Id
    @Column(name = "outing_id_fk")
    private UUID outingId;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<CalendarEventAttendeeEntity> attendees = new LinkedHashSet<>();

    protected CalendarEventEntity() {}

    public CalendarEventEntity(UUID outingId, String title, Instant startTime, Instant endTime) {
        if (outingId == null || title == null || title.isBlank()
                || startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("Booked event details are invalid.");
        }
        this.outingId = outingId;
        this.title = title.trim();
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public void addAttendee(UUID attendeeId) {
        if (attendeeId == null) {
            throw new IllegalArgumentException("Booked event attendee is required.");
        }
        if (attendees.stream().anyMatch(attendee -> attendee.getUserId().equals(attendeeId))) {
            throw new IllegalArgumentException("Booked event attendees must be unique.");
        }
        attendees.add(new CalendarEventAttendeeEntity(UUID.randomUUID(), this, attendeeId));
    }
}
