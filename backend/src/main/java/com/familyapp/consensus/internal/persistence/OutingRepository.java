package com.familyapp.consensus.internal.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OutingRepository extends JpaRepository<OutingEntity, UUID> {
    Optional<OutingEntity> findByIdAndFamilyId(UUID outingId, UUID familyId);
}
