package com.familyapp.consensus.internal.persistence;

import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import com.familyapp.consensus.api.OutingStatus;
import com.familyapp.consensus.api.FinalizationMethod;
import com.familyapp.consensus.api.OutingStateException;

@Entity
@Table(name = "outings")
public class OutingEntity {
    @Id
    private UUID id;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(name = "family_id_fk", nullable = false)
    private UUID familyId;

    @Column(name = "created_by_id_fk", nullable = false)
    private UUID createdById;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutingStatus status;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    @Column(name = "finalized_idea_id_fk")
    private UUID finalizedIdeaId;

    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(name = "finalization_method", length = 20)
    private FinalizationMethod finalizationMethod;

    @OneToMany(mappedBy = "outing", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<OutingAttendeeEntity> attendees = new LinkedHashSet<>();

    protected OutingEntity() {}

    public OutingEntity(
            UUID id,
            String title,
            String description,
            UUID familyId,
            UUID createdById,
            Instant startTime,
            Instant endTime) {
        this.id = id;
        this.title = title.trim();
        this.description = description == null ? null : description.trim();
        this.familyId = familyId;
        this.createdById = createdById;
        this.status = OutingStatus.OPEN;
        this.startTime = startTime;
        this.endTime = endTime;
        this.updatedAt = Instant.now();
    }

    public void addAttendee(UUID userId) {
        if (attendees.stream().anyMatch(attendee -> attendee.getUserId().equals(userId))) {
            return;
        }
        attendees.add(new OutingAttendeeEntity(this, userId));
    }

    public void recordChange() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getFamilyId() {
        return familyId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public OutingStatus getStatus() {
        return status;
    }

    public FinalizationMethod getFinalizationMethod() {
        return finalizationMethod;
    }

    public void finalizeWith(UUID ideaId, FinalizationMethod method) {
        if (status != OutingStatus.OPEN || ideaId == null || method == null) {
            throw new OutingStateException("Only an open outing can be finalized with a winner.");
        }
        status = OutingStatus.FINALIZED;
        finalizedIdeaId = ideaId;
        finalizationMethod = method;
        updatedAt = Instant.now();
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public Set<UUID> getAttendeeIds() {
        Set<UUID> attendeeIds = new HashSet<>();
        for (OutingAttendeeEntity attendee : attendees) {
            attendeeIds.add(attendee.getUserId());
        }
        return Set.copyOf(attendeeIds);
    }
}
