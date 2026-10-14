package com.familyapp.idempotency.internal.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyRecordRepository
        extends JpaRepository<IdempotencyRecordEntity, UUID> {
    Optional<IdempotencyRecordEntity> findByUserIdAndOperationAndIdempotencyKey(
            UUID userId,
            String operation,
            String idempotencyKey);
}
