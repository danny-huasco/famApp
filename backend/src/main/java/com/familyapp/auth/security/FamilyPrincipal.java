package com.familyapp.auth.security;

import java.util.UUID;

public record FamilyPrincipal(UUID userId, UUID familyId) {}
