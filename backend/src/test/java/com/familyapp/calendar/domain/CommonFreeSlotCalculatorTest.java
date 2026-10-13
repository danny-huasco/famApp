package com.familyapp.calendar.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.familyapp.calendar.api.AvailabilityWindow;

class CommonFreeSlotCalculatorTest {
    private final CommonFreeSlotCalculator calculator = new CommonFreeSlotCalculator();

    @Test
    void findsOverlapsForAllThreeAttendeesAndPreservesTentativeStatus() {
        List<List<AvailabilityWindow>> availability = List.of(
                List.of(
                        window("09:00", "12:00", false),
                        window("13:00", "17:00", false)),
                List.of(window("10:00", "14:00", false)),
                List.of(
                        window("10:30", "11:30", false),
                        window("13:30", "15:00", true)));

        var slots = calculator.calculate(availability);

        assertEquals(2, slots.size());
        assertEquals(Instant.parse("2026-01-01T10:30:00Z"), slots.get(0).startTime());
        assertEquals(Instant.parse("2026-01-01T11:30:00Z"), slots.get(0).endTime());
        assertTrue(!slots.get(0).tentative());
        assertEquals(Instant.parse("2026-01-01T13:30:00Z"), slots.get(1).startTime());
        assertEquals(Instant.parse("2026-01-01T14:00:00Z"), slots.get(1).endTime());
        assertTrue(slots.get(1).tentative());
    }

    @Test
    void mergesOverlappingWindowsAndUsesConfirmedCoverageWherePossible() {
        List<List<AvailabilityWindow>> availability = List.of(
                List.of(
                        window("09:00", "11:00", false),
                        window("10:30", "12:00", true)),
                List.of(window("10:00", "12:00", false)));

        var slots = calculator.calculate(availability);

        assertEquals(2, slots.size());
        assertEquals(Instant.parse("2026-01-01T10:00:00Z"), slots.get(0).startTime());
        assertEquals(Instant.parse("2026-01-01T11:00:00Z"), slots.get(0).endTime());
        assertTrue(!slots.get(0).tentative());
        assertEquals(Instant.parse("2026-01-01T11:00:00Z"), slots.get(1).startTime());
        assertEquals(Instant.parse("2026-01-01T12:00:00Z"), slots.get(1).endTime());
        assertTrue(slots.get(1).tentative());
    }

    @Test
    void returnsNoSlotsWhenAnAttendeeHasNoAvailability() {
        assertTrue(calculator.calculate(List.of(
                List.of(window("09:00", "10:00", false)),
                List.of())).isEmpty());
    }

    @Test
    void rejectsEmptyOrReversedAvailabilityWindows() {
        assertThrows(IllegalArgumentException.class, () -> new AvailabilityWindow(
                Instant.parse("2026-01-01T10:00:00Z"),
                Instant.parse("2026-01-01T10:00:00Z"),
                false));
    }

    private AvailabilityWindow window(String start, String end, boolean tentative) {
        return new AvailabilityWindow(
                Instant.parse("2026-01-01T" + start + ":00Z"),
                Instant.parse("2026-01-01T" + end + ":00Z"),
                tentative);
    }
}
