package com.familyapp.auth.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.familyapp.auth.api.RegisterRequest;
import com.familyapp.auth.security.JwtService;
import com.familyapp.user.api.UserAccountPort;
import com.familyapp.user.api.UserAccountPort.UserIdentity;

class AuthServiceTest {
    private static final String TEST_SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    private UserAccountPort userAccountPort;
    private BCryptPasswordEncoder passwordEncoder;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userAccountPort = mock(UserAccountPort.class);
        passwordEncoder = new BCryptPasswordEncoder();
        authService = new AuthService(
                userAccountPort,
                passwordEncoder,
                new JwtService(TEST_SECRET, 3600));
    }

    @Test
    void registrationHashesPasswordAndReturnsFamilyClaim() {
        UUID userId = UUID.randomUUID();
        UUID familyId = UUID.randomUUID();
        when(userAccountPort.createFamilyAccount(
                eq("person@example.com"),
                anyString(),
                eq("Alex"),
                eq("Family")))
                .thenReturn(new UserIdentity(userId, "person@example.com", "Alex", familyId));

        AuthSession session = authService.register(new RegisterRequest(
                "person@example.com", "password123", "Alex", "Family"));

        ArgumentCaptor<String> passwordHash = ArgumentCaptor.forClass(String.class);
        verify(userAccountPort).createFamilyAccount(
                eq("person@example.com"),
                passwordHash.capture(),
                eq("Alex"),
                eq("Family"));
        assertNotEquals("password123", passwordHash.getValue());
        assertTrue(passwordEncoder.matches("password123", passwordHash.getValue()));
        assertEquals(userId, session.userId());
        assertEquals(familyId, session.familyId());
        assertEquals(familyId.toString(),
                new JwtService(TEST_SECRET, 3600)
                        .parseToken(session.accessToken())
                        .get("family_id", String.class));
    }
}
