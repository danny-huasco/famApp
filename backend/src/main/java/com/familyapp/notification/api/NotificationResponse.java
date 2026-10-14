package com.familyapp.notification.api;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID outingId,
        String title,
        String message,
        boolean read,
        Instant createdAt) {}
