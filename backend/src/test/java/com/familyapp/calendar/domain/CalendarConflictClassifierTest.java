package com.familyapp.calendar.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.familyapp.calendar.api.SlotConflictLevel;

class CalendarConflictClassifierTest {
    private final CalendarConflictClassifier classifier = new CalendarConflictClassifier();

    @Test
    void classifiesZeroConflictsAsGreen() {
        assertEquals(SlotConflictLevel.GREEN, classifier.classify(5, 0));
    }

    @Test
    void classifiesExactlyHalfAsOrange() {
        assertEquals(SlotConflictLevel.ORANGE, classifier.classify(4, 2));
    }

    @Test
    void classifiesMoreThanHalfAsRed() {
        assertEquals(SlotConflictLevel.RED, classifier.classify(4, 3));
    }

    @Test
    void rejectsEmptyAttendeeSet() {
        assertThrows(IllegalArgumentException.class, () -> classifier.classify(0, 0));
    }
}
