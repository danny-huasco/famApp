package com.familyapp.calendar.domain;

import com.familyapp.calendar.api.SlotConflictLevel;

public class CalendarConflictClassifier {
    public SlotConflictLevel classify(int attendeeCount, int conflictedAttendeeCount) {
        if (attendeeCount <= 0 || conflictedAttendeeCount < 0
                || conflictedAttendeeCount > attendeeCount) {
            throw new IllegalArgumentException("Conflict assessment attendee counts are invalid.");
        }
        if (conflictedAttendeeCount == 0) {
            return SlotConflictLevel.GREEN;
        }
        return (long) conflictedAttendeeCount * 2 <= attendeeCount
                ? SlotConflictLevel.ORANGE
                : SlotConflictLevel.RED;
    }
}
