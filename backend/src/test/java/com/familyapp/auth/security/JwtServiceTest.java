package com.familyapp.auth.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class JwtServiceTest {
    private static final String TEST_SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    @Test
    void tokenContainsUserAndFamilyClaims() {
        UUID userId = UUID.randomUUID();
        UUID familyId = UUID.randomUUID();
        JwtService jwtService = new JwtService(TEST_SECRET, 3600);

        var claims = jwtService.parseToken(jwtService.issueToken(userId, familyId));

        assertEquals(userId.toString(), claims.getSubject());
        assertEquals(familyId.toString(), claims.get("family_id", String.class));
    }
}
