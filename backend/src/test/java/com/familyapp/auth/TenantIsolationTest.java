package com.familyapp.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.familyapp.user.api.UserAccountPort;
import com.familyapp.user.api.UserAccountPort.UserIdentity;
import com.familyapp.user.internal.persistence.FamilyRepository;
import com.familyapp.user.internal.persistence.UserRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TenantIsolationTest {
    @Autowired
    private UserAccountPort userAccountPort;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FamilyRepository familyRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void familyScopedQueryDoesNotReturnAnotherFamiliesUsers() {
        String suffix = UUID.randomUUID().toString();
        UserIdentity userA = register("user-a-" + suffix + "@example.com", "Family A");
        UserIdentity userB = register("user-b-" + suffix + "@example.com", "Family B");

        var usersVisibleToFamilyA = userRepository.findAllByFamily_Id(userA.familyId());

        assertEquals(1, usersVisibleToFamilyA.size());
        assertEquals(userA.userId(), usersVisibleToFamilyA.getFirst().getId());
        assertFalse(usersVisibleToFamilyA.stream()
                .anyMatch(user -> user.getFamily().getId().equals(userB.familyId())));
    }

    private UserIdentity register(String email, String familyName) {
        return userAccountPort.createFamilyAccount(
                email,
                passwordEncoder.encode("secure-password-123"),
                "Member",
                familyName);
    }
}
