package com.familyapp.user.internal;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.familyapp.user.api.FamilyMembershipPort;
import com.familyapp.user.internal.persistence.UserRepository;

@Service
@Transactional(readOnly = true)
public class FamilyMembershipService implements FamilyMembershipPort {
    private final UserRepository userRepository;

    public FamilyMembershipService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Set<UUID> findMemberIds(UUID familyId) {
        return userRepository.findAllByFamily_Id(familyId).stream()
                .map(user -> user.getId())
                .collect(Collectors.toUnmodifiableSet());
    }
}
