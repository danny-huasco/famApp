package com.familyapp.consensus.application;

import java.util.Optional;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.familyapp.consensus.api.OutingLedgerContext;
import com.familyapp.consensus.api.OutingLedgerPort;
import com.familyapp.consensus.internal.persistence.OutingRepository;

@Service
@Transactional(readOnly = true)
public class OutingLedgerService implements OutingLedgerPort {
    private final OutingRepository outingRepository;

    public OutingLedgerService(OutingRepository outingRepository) {
        this.outingRepository = outingRepository;
    }

    @Override
    public Optional<OutingLedgerContext> findLedgerContext(UUID outingId) {
        return outingRepository.findById(Objects.requireNonNull(outingId))
                .map(outing -> new OutingLedgerContext(
                        outing.getId(),
                        outing.getFamilyId(),
                        outing.getStatus(),
                        outing.getAttendeeIds()));
    }
}
