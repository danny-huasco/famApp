package com.familyapp.notification.application;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.familyapp.auth.security.FamilyPrincipal;
import com.familyapp.event.ExpenseAddedEvent;
import com.familyapp.event.OutingCreatedEvent;
import com.familyapp.event.OutingFinalizedEvent;
import com.familyapp.notification.api.NotificationEventType;
import com.familyapp.notification.api.NotificationResponse;
import com.familyapp.notification.internal.persistence.NotificationEntity;
import com.familyapp.notification.internal.persistence.NotificationRepository;

@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> listForUser(FamilyPrincipal principal) {
        if (principal == null || principal.userId() == null) {
            throw new IllegalArgumentException("Authenticated user is required.");
        }
        return notificationRepository.findAllByUserIdOrderByCreatedAtDescIdDesc(principal.userId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void createOutingNotifications(OutingCreatedEvent event) {
        for (UUID attendeeId : event.attendeeIds()) {
            createIfAbsent(
                    event.outingId(),
                    attendeeId,
                    NotificationEventType.OUTING_CREATED,
                    event.outingId(),
                    "A new event was created",
                    event.title() + " has been added to the family calendar.");
        }
        for (UUID attendeeId : event.conflictedAttendeeIds()) {
            createIfAbsent(
                    event.outingId(),
                    attendeeId,
                    NotificationEventType.OUTING_CONFLICT,
                    event.outingId(),
                    "You have an event conflict",
                    event.title() + " overlaps another booked event.");
        }
    }

    @Transactional
    public void createFinalizationNotifications(OutingFinalizedEvent event) {
        for (UUID attendeeId : event.attendeeIds()) {
            createIfAbsent(
                    event.outingId(),
                    attendeeId,
                    NotificationEventType.OUTING_FINALIZED,
                    event.outingId(),
                    "Outing finalized",
                    event.outingTitle() + " was finalized with "
                            + event.winnerIdeaTitle() + " as the selected idea.");
        }
    }

    @Transactional
    public void createExpenseNotifications(ExpenseAddedEvent event) {
        for (UUID attendeeId : event.attendeeIds()) {
            createIfAbsent(
                    event.outingId(),
                    attendeeId,
                    NotificationEventType.EXPENSE_ADDED,
                    event.expenseId(),
                    "New expense added",
                    "A new expense was added: " + event.description() + ".");
        }
    }

    private void createIfAbsent(
            UUID outingId,
            UUID userId,
            NotificationEventType eventType,
            UUID sourceEventId,
            String title,
            String message) {
        if (!notificationRepository.existsByEventTypeAndSourceEventIdAndUserId(
                eventType, sourceEventId, userId)) {
            notificationRepository.save(new NotificationEntity(
                    UUID.randomUUID(), outingId, userId, eventType, sourceEventId, title, message));
        }
    }

    private NotificationResponse toResponse(NotificationEntity notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getOutingId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.isRead(),
                notification.getCreatedAt());
    }
}
