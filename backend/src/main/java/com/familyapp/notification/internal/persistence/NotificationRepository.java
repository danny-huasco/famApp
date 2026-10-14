package com.familyapp.notification.internal.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.familyapp.notification.api.NotificationEventType;

public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {
    boolean existsByEventTypeAndSourceEventIdAndUserId(
            NotificationEventType eventType,
            UUID sourceEventId,
            UUID userId);

    List<NotificationEntity> findAllByUserIdOrderByCreatedAtDescIdDesc(UUID userId);
}
