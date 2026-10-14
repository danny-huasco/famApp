package com.familyapp.ledger.internal.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OutingLedgerRepository extends JpaRepository<OutingLedgerEntity, UUID> {}
