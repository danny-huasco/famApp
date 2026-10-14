package com.familyapp.consensus.internal.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface IdeaRepository extends JpaRepository<IdeaEntity, UUID> {
    List<IdeaEntity> findAllByOuting_IdOrderByCreatedAtAscIdAsc(UUID outingId);

    Optional<IdeaEntity> findByIdAndOuting_Id(UUID ideaId, UUID outingId);
}
