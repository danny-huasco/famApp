package com.familyapp.user.api;

import java.util.Optional;
import java.util.UUID;

public interface UserAccountPort {
    UserIdentity createFamilyAccount(
            String email,
            String passwordHash,
            String firstName,
            String familyName);

    Optional<UserCredential> findCredentialsByEmail(String email);

    record UserIdentity(UUID userId, String email, String firstName, UUID familyId) {}

    record UserCredential(UUID userId, String email, String passwordHash, UUID familyId) {}
}
