package com.familyapp.consensus.application;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.familyapp.consensus.api.OutingAvailabilityContext;
import com.familyapp.consensus.api.OutingAvailabilityPort;
import com.familyapp.consensus.internal.persistence.OutingRepository;

@Service
@Transactional(readOnly = true)
public class OutingAvailabilityService implements OutingAvailabilityPort {
    private final OutingRepository outingRepository;

    public OutingAvailabilityService(OutingRepository outingRepository) {
        this.outingRepository = outingRepository;
    }

    @Override
    public Optional<OutingAvailabilityContext> findAvailabilityContext(UUID outingId) {
        return outingRepository.findById(outingId)
                .map(outing -> new OutingAvailabilityContext(
                        outing.getFamilyId(),
                        outing.getAttendeeIds()));
    }
}
