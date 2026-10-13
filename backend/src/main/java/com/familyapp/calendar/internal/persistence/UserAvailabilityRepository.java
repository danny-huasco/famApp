package com.familyapp.calendar.internal.persistence;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserAvailabilityRepository
        extends JpaRepository<UserAvailabilityEntity, UUID> {
    @Query("""
            select distinct availability.userId
            from UserAvailabilityEntity availability
            where availability.outingId = :outingId
            """)
    List<UUID> findDistinctUserIdsByOutingId(@Param("outingId") UUID outingId);

    List<UserAvailabilityEntity> findAllByOutingIdAndUserIdIn(
            UUID outingId,
            Set<UUID> userIds);
}
