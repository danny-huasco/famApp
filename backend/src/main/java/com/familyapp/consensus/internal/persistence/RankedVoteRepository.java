package com.familyapp.consensus.internal.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RankedVoteRepository extends JpaRepository<RankedVoteEntity, UUID> {
    List<RankedVoteEntity> findAllByOuting_IdOrderByVoterIdAscRankAsc(UUID outingId);

    @Modifying
    @Query("""
            delete from RankedVoteEntity vote
            where vote.outing.id = :outingId and vote.voterId = :voterId
            """)
    void deleteBallot(
            @Param("outingId") UUID outingId,
            @Param("voterId") UUID voterId);
}
