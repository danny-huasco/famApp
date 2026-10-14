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
        name = "outing_votes",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_outing_voter_rank",
                    columnNames = {"outing_id_fk", "user_id_fk", "rank"}),
            @UniqueConstraint(
                    name = "uk_outing_voter_idea",
                    columnNames = {"outing_id_fk", "user_id_fk", "idea_id_fk"})
        })
public class RankedVoteEntity {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "outing_id_fk", nullable = false)
    private OutingEntity outing;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idea_id_fk", nullable = false)
    private IdeaEntity idea;

    @Column(name = "user_id_fk", nullable = false)
    private UUID voterId;

    @Column(name = "rank", nullable = false)
    private int rank;

    protected RankedVoteEntity() {}

    public RankedVoteEntity(
            UUID id,
            OutingEntity outing,
            IdeaEntity idea,
            UUID voterId,
            int rank) {
        this.id = id;
        this.outing = outing;
        this.idea = idea;
        this.voterId = voterId;
        this.rank = rank;
    }

    public UUID getIdeaId() {
        return idea.getId();
    }

    public UUID getVoterId() {
        return voterId;
    }

    public int getRank() {
        return rank;
    }
}
