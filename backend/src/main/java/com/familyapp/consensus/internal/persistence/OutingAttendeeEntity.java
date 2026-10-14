package com.familyapp.consensus.internal.persistence;

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
        name = "outing_attendees",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_outing_attendee",
                columnNames = {"outing_id_fk", "user_id_fk"}))
public class OutingAttendeeEntity {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "outing_id_fk", nullable = false)
    private OutingEntity outing;

    @Column(name = "user_id_fk", nullable = false)
    private UUID userId;

    protected OutingAttendeeEntity() {}

    OutingAttendeeEntity(OutingEntity outing, UUID userId) {
        this.id = UUID.randomUUID();
        this.outing = outing;
        this.userId = userId;
    }

    UUID getUserId() {
        return userId;
    }
}
