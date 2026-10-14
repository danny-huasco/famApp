package com.familyapp.ledger.internal.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "outing_ledgers")
public class OutingLedgerEntity {
    @Id
    @Column(name = "outing_id_fk")
    private UUID outingId;

    @Column(name = "initialized_at", nullable = false)
    private Instant initializedAt;

    protected OutingLedgerEntity() {}

    public OutingLedgerEntity(UUID outingId) {
        this.outingId = outingId;
        this.initializedAt = Instant.now();
    }
}
