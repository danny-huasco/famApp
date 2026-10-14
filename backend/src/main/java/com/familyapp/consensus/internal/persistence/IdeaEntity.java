package com.familyapp.consensus.internal.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "outing_ideas")
public class IdeaEntity {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "outing_id_fk", nullable = false)
    private OutingEntity outing;

    @Column(name = "proposer_id_fk", nullable = false)
    private UUID proposerId;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected IdeaEntity() {}

    public IdeaEntity(
            UUID id,
            OutingEntity outing,
            UUID proposerId,
            String title,
            String description,
            Instant createdAt) {
        this.id = id;
        this.outing = outing;
        this.proposerId = proposerId;
        this.title = title.trim();
        this.description = description == null ? null : description.trim();
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOutingId() {
        return outing.getId();
    }

    public UUID getProposerId() {
        return proposerId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
