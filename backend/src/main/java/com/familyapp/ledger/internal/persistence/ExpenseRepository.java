package com.familyapp.ledger.internal.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseRepository extends JpaRepository<ExpenseEntity, UUID> {
    List<ExpenseEntity> findAllByOutingIdOrderByCreatedAtAscIdAsc(UUID outingId);
}
