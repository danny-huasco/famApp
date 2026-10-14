package com.familyapp.user.api;

import java.util.Set;
import java.util.UUID;

public interface FamilyMembershipPort {
    Set<UUID> findMemberIds(UUID familyId);
}
