package com.familyapp.consensus.application;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.familyapp.consensus.api.OutingMembershipPort;
import com.familyapp.consensus.internal.persistence.OutingRepository;

@Service
@Transactional(readOnly = true)
public class OutingMembershipService implements OutingMembershipPort {
    private final OutingRepository outingRepository;

    public OutingMembershipService(OutingRepository outingRepository) {
        this.outingRepository = outingRepository;
    }

    @Override
    public boolean isAttendee(UUID outingId, UUID familyId, UUID userId) {
        if (outingId == null || familyId == null || userId == null) {
            return false;
        }
        return outingRepository.findByIdAndFamilyId(outingId, familyId)
                .map(outing -> outing.getAttendeeIds().contains(userId))
                .orElse(false);
    }
}
