package com.familyapp.calendar.internal.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import com.familyapp.calendar.api.AvailabilityWindow;

@Entity
@Table(
        name = "user_availability",
        indexes = @Index(
                name = "idx_availability_outing_user",
                columnList = "outing_id_fk,user_id_fk"))
public class UserAvailabilityEntity {
    @Id
    private UUID id;

    @Column(name = "user_id_fk", nullable = false)
    private UUID userId;

    @Column(name = "outing_id_fk", nullable = false)
    private UUID outingId;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Column(name = "is_tentative", nullable = false)
    private boolean tentative;

    protected UserAvailabilityEntity() {}

    public UserAvailabilityEntity(
            UUID id,
            UUID userId,
            UUID outingId,
            Instant startTime,
            Instant endTime,
            boolean tentative) {
        if (startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("Availability end must be after its start.");
        }
        this.id = id;
        this.userId = userId;
        this.outingId = outingId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.tentative = tentative;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOutingId() {
        return outingId;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public boolean isTentative() {
        return tentative;
    }

    public AvailabilityWindow toWindow() {
        return new AvailabilityWindow(startTime, endTime, tentative);
    }
}
