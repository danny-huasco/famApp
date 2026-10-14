package com.familyapp.notification.internal.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import com.familyapp.notification.api.NotificationEventType;

@Entity
@Table(
        name = "notifications",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_notification_event_recipient",
                columnNames = {"event_type", "source_event_id", "user_id_fk"}))
public class NotificationEntity {
    @Id
    private UUID id;

    @Column(name = "outing_id_fk", nullable = false)
    private UUID outingId;

    @Column(name = "user_id_fk", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 40)
    @Enumerated(EnumType.STRING)
    private NotificationEventType eventType;

    @Column(name = "source_event_id", nullable = false)
    private UUID sourceEventId;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected NotificationEntity() {}

    public NotificationEntity(
            UUID id,
            UUID outingId,
            UUID userId,
            NotificationEventType eventType,
            UUID sourceEventId,
            String title,
            String message) {
        this.id = id;
        this.outingId = outingId;
        this.userId = userId;
        this.eventType = eventType;
        this.sourceEventId = sourceEventId;
        this.title = title;
        this.message = message;
        this.read = false;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getOutingId() {
        return outingId;
    }

    public UUID getUserId() {
        return userId;
    }

    public NotificationEventType getEventType() {
        return eventType;
    }

    public UUID getSourceEventId() {
        return sourceEventId;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public boolean isRead() {
        return read;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
