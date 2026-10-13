package com.familyapp.auth.application;

import java.util.UUID;

public record AuthSession(String accessToken, UUID userId, UUID familyId) {}
