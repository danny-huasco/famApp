package com.familyapp.calendar.application;

import java.util.Objects;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import com.familyapp.calendar.internal.persistence.CalendarEventRepository;
import com.familyapp.event.OutingFinalizedEvent;

@Component
public class OutingFinalizedCalendarListener {
    private final CalendarEventRepository calendarEventRepository;

    public OutingFinalizedCalendarListener(CalendarEventRepository calendarEventRepository) {
        this.calendarEventRepository = calendarEventRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void lockBookedEvent(OutingFinalizedEvent event) {
        calendarEventRepository.findById(Objects.requireNonNull(event.outingId()))
                .orElseThrow(() -> new IllegalStateException(
                        "Cannot finalize outing without its calendar booking."))
                .lock();
    }
}
