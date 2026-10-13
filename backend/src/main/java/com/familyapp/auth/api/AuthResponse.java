package com.familyapp.auth.api;

import java.util.UUID;

import com.familyapp.auth.application.AuthSession;

public record AuthResponse(String accessToken, String tokenType, UUID userId, UUID familyId) {
    public static AuthResponse from(AuthSession session) {
        return new AuthResponse(session.accessToken(), "Bearer", session.userId(), session.familyId());
    }
}
