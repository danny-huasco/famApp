package com.familyapp.ledger.internal.persistence;

import java.math.BigDecimal;
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
@Table(name = "outing_expenses")
public class ExpenseEntity {
    @Id
    private UUID id;

    @Column(name = "outing_id_fk", nullable = false)
    private UUID outingId;

    @Column(nullable = false, length = 180)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "payer_id_fk", nullable = false)
    private UUID payerId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ExpenseShareEntity> shares = new LinkedHashSet<>();

    protected ExpenseEntity() {}

    public ExpenseEntity(
            UUID id,
            UUID outingId,
            String description,
            BigDecimal amount,
            UUID payerId,
            Instant createdAt) {
        this.id = id;
        this.outingId = outingId;
        this.description = description.trim();
        this.amount = amount.setScale(2);
        this.payerId = payerId;
        this.createdAt = createdAt;
    }

    public void addShare(UUID participantId, BigDecimal amountOwed) {
        shares.add(new ExpenseShareEntity(
                UUID.randomUUID(), this, participantId, amountOwed));
    }

    public UUID getId() {
        return id;
    }

    public UUID getOutingId() {
        return outingId;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public UUID getPayerId() {
        return payerId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Set<ExpenseShareEntity> getShares() {
        return Set.copyOf(shares);
    }
}
