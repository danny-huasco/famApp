# Calendar Availability and Event Conflicts

This document refines the calendar and outing behavior in [the master plan](./master-plan.md).

## Availability and booked events

- Keep members' free-time availability separate from booked calendar events.
- An outing's explicit attendee list defines who is included in its schedule assessment; do not assume every family member is attending.
- A booked event conflicts with a proposed event when their time intervals overlap for an attendee.
- The availability heatmap counts attendees with one or more overlapping booked events, not the number of conflicting event records.

## Heatmap colors

For `N` attendees and `C` attendees with a conflict during the proposed interval:

- **Green:** `C = 0`
- **Orange:** `0 < C` and `2 * C <= N` (including exactly half)
- **Red:** `2 * C > N`

The conflict count is calculated per attendee, so several overlapping events for one person still count as one conflicted attendee.

## Creating an outing

- Green and orange time slots may be created.
- Red time slots are rejected; an outing must not be created in that slot.
- On successful creation, send the normal “A new event was created” notification to all attendees.
- For an orange slot, also send “You have an event conflict” to each attendee with one or more overlapping booked events.
- A red-slot rejection creates no event and therefore sends no event-created notifications.

The existing `UserAvailability.is_tentative` flag describes free-time availability and is not itself a booked-event conflict or a trigger for conflict notifications.
