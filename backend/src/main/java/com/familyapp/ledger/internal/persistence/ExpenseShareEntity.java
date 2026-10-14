package com.familyapp.ledger.internal.persistence;

import java.math.BigDecimal;
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
        name = "outing_expense_shares",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_expense_participant",
                columnNames = {"expense_id_fk", "participant_id_fk"}))
public class ExpenseShareEntity {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "expense_id_fk", nullable = false)
    private ExpenseEntity expense;

    @Column(name = "participant_id_fk", nullable = false)
    private UUID participantId;

    @Column(name = "amount_owed", nullable = false, precision = 12, scale = 2)
    private BigDecimal amountOwed;

    protected ExpenseShareEntity() {}

    ExpenseShareEntity(UUID id, ExpenseEntity expense, UUID participantId, BigDecimal amountOwed) {
        this.id = id;
        this.expense = expense;
        this.participantId = participantId;
        this.amountOwed = amountOwed.setScale(2);
    }

    public UUID getParticipantId() {
        return participantId;
    }

    public BigDecimal getAmountOwed() {
        return amountOwed;
    }
}
