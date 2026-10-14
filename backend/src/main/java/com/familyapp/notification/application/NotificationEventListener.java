package com.familyapp.notification.application;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import com.familyapp.event.ExpenseAddedEvent;
import com.familyapp.event.OutingCreatedEvent;
import com.familyapp.event.OutingFinalizedEvent;

@Component
public class NotificationEventListener {
    private final NotificationService notificationService;

    public NotificationEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onOutingCreated(OutingCreatedEvent event) {
        notificationService.createOutingNotifications(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onOutingFinalized(OutingFinalizedEvent event) {
        notificationService.createFinalizationNotifications(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onExpenseAdded(ExpenseAddedEvent event) {
        notificationService.createExpenseNotifications(event);
    }
}
