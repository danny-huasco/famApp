package com.familyapp.calendar.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

import org.springframework.stereotype.Component;

import com.familyapp.calendar.api.AvailabilityWindow;
import com.familyapp.calendar.api.CommonFreeSlot;

@Component
public class CommonFreeSlotCalculator {
    public List<CommonFreeSlot> calculate(List<List<AvailabilityWindow>> attendeeWindows) {
        if (attendeeWindows.isEmpty() || attendeeWindows.stream().anyMatch(List::isEmpty)) {
            return List.of();
        }

        List<CommonFreeSlot> common = union(attendeeWindows.getFirst());
        for (int i = 1; i < attendeeWindows.size() && !common.isEmpty(); i++) {
            common = intersect(common, union(attendeeWindows.get(i)));
        }
        return List.copyOf(common);
    }

    private List<CommonFreeSlot> union(Collection<AvailabilityWindow> windows) {
        TreeSet<Instant> boundaries = new TreeSet<>();
        for (AvailabilityWindow window : windows) {
            boundaries.add(window.startTime());
            boundaries.add(window.endTime());
        }

        List<Instant> points = new ArrayList<>(boundaries);
        List<CommonFreeSlot> segments = new ArrayList<>();
        for (int i = 0; i < points.size() - 1; i++) {
            Instant start = points.get(i);
            Instant end = points.get(i + 1);
            List<AvailabilityWindow> covering = windows.stream()
                    .filter(window -> !window.startTime().isAfter(start)
                            && !window.endTime().isBefore(end))
                    .toList();
            if (!covering.isEmpty()) {
                boolean tentative = covering.stream().allMatch(AvailabilityWindow::tentative);
                segments.add(new CommonFreeSlot(start, end, tentative));
            }
        }
        return compact(segments);
    }

    private List<CommonFreeSlot> intersect(
            List<CommonFreeSlot> left,
            List<CommonFreeSlot> right) {
        List<CommonFreeSlot> intersections = new ArrayList<>();
        for (CommonFreeSlot first : left) {
            for (CommonFreeSlot second : right) {
                Instant start = first.startTime().isAfter(second.startTime())
                        ? first.startTime()
                        : second.startTime();
                Instant end = first.endTime().isBefore(second.endTime())
                        ? first.endTime()
                        : second.endTime();
                if (end.isAfter(start)) {
                    intersections.add(new CommonFreeSlot(
                            start,
                            end,
                            first.tentative() || second.tentative()));
                }
            }
        }
        return compact(intersections);
    }

    private List<CommonFreeSlot> compact(List<CommonFreeSlot> slots) {
        List<CommonFreeSlot> sorted = slots.stream()
                .sorted(Comparator.comparing(CommonFreeSlot::startTime))
                .toList();
        List<CommonFreeSlot> compacted = new ArrayList<>();
        for (CommonFreeSlot slot : sorted) {
            if (!compacted.isEmpty()) {
                CommonFreeSlot previous = compacted.getLast();
                if (previous.endTime().equals(slot.startTime())
                        && previous.tentative() == slot.tentative()) {
                    compacted.set(compacted.size() - 1, new CommonFreeSlot(
                            previous.startTime(), slot.endTime(), previous.tentative()));
                    continue;
                }
            }
            compacted.add(slot);
        }
        return compacted;
    }
}
