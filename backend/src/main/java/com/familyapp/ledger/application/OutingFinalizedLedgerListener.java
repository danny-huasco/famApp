package com.familyapp.ledger.application;

import java.util.Objects;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import com.familyapp.event.OutingFinalizedEvent;
import com.familyapp.ledger.internal.persistence.OutingLedgerEntity;
import com.familyapp.ledger.internal.persistence.OutingLedgerRepository;

@Component
public class OutingFinalizedLedgerListener {
    private final OutingLedgerRepository outingLedgerRepository;

    public OutingFinalizedLedgerListener(OutingLedgerRepository outingLedgerRepository) {
        this.outingLedgerRepository = outingLedgerRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void initializeLedger(OutingFinalizedEvent event) {
        var outingId = Objects.requireNonNull(event.outingId());
        if (!outingLedgerRepository.existsById(outingId)) {
            outingLedgerRepository.save(new OutingLedgerEntity(outingId));
        }
    }
}
