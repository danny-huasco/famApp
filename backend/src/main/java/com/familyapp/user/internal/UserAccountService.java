package com.familyapp.user.internal;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.familyapp.user.api.EmailAlreadyRegisteredException;
import com.familyapp.user.api.UserAccountPort;
import com.familyapp.user.internal.persistence.FamilyEntity;
import com.familyapp.user.internal.persistence.FamilyRepository;
import com.familyapp.user.internal.persistence.UserEntity;
import com.familyapp.user.internal.persistence.UserRepository;

@Service
@Transactional(readOnly = true)
public class UserAccountService implements UserAccountPort {
    private final UserRepository userRepository;
    private final FamilyRepository familyRepository;

    public UserAccountService(UserRepository userRepository, FamilyRepository familyRepository) {
        this.userRepository = userRepository;
        this.familyRepository = familyRepository;
    }

    @Override
    @Transactional
    public UserIdentity createFamilyAccount(
            String email,
            String passwordHash,
            String firstName,
            String familyName) {
        String normalizedEmail = normalizeEmail(email);
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new EmailAlreadyRegisteredException();
        }

        UUID familyId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        FamilyEntity family = familyRepository.saveAndFlush(
                new FamilyEntity(familyId, familyName.trim(), Instant.now()));
        UserEntity user = userRepository.saveAndFlush(
                new UserEntity(userId, normalizedEmail, passwordHash, firstName.trim(), family));
        family.setCreatedBy(user);
        familyRepository.saveAndFlush(family);

        return new UserIdentity(user.getId(), user.getEmail(), user.getFirstName(), family.getId());
    }

    @Override
    public Optional<UserCredential> findCredentialsByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(normalizeEmail(email))
                .map(user -> new UserCredential(
                        user.getId(),
                        user.getEmail(),
                        user.getPasswordHash(),
                        user.getFamily().getId()));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
