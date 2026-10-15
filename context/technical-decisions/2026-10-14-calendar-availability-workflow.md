# Technical Decisions: Calendar Availability Workflow

- **Date:** 2026-10-14
- **Request:** Resume implementation after verifying yesterday's Dashboard work, then continue with the planned availability and calendar-overlap experience.
- **Scope:** Confirm the Dashboard widget/test, expose authenticated Calendar availability read/replace operations for outing attendees, add mobile API/model support and availability/overlap UI, and document results.

## Context

The paused Dashboard source reads Hive directly and has a populated-cache widget test. `flutter analyze` passed on resume, but both the full Flutter test run and an isolated Dashboard widget test stalled after starting that test. The test did not return a failure or success before it was stopped for diagnosis.

The backend already persists `UserAvailabilityEntity` records keyed by outing/user and has `AvailabilityService` plus `CommonFreeSlotCalculator`; it has no Calendar REST controller. The availability calculator intersects each attendee's windows and preserves tentative status. `OutingLedgerPort` already exposes family and attendee IDs but is Ledger-named; a Calendar-specific public Consensus contract is clearer than reusing that API. The mobile app has a Hive `UserAvailability` model and an authenticated Dio client, but no availability repository/screen.

## Process and Decisions

### 1. Verifying the paused Dashboard
- **Decision:** Treat a passing analyzer as source-level validation only; isolate and fix the hanging widget-test path before calling the Dashboard verified.
- **Why:** The full Flutter test process stopped at the Dashboard test, so later tests could not establish the full suite's status. A targeted test gives a clear signal about the changed screen.
- **Alternatives and tradeoffs:** Skipping the widget test would leave the exact paused change unverified; rerunning the same suite without isolating the cause would repeat the stall without better evidence.
- **Impact:** Any timeout or test harness issue will be recorded distinctly from an assertion failure.

### 2. Designing the Calendar API boundary
- **Decision:** Add an authenticated `GET /api/outings/{outingId}/availability` that returns the outing attendee roster, each attendee's saved windows, and server-calculated common free slots; add `PUT` on the same resource to replace only the authenticated attendee's windows.
- **Why:** The UI needs both its caller's editable windows and a group overlap result, while the existing backend calculator already handles intersection and tentative-window rules. A whole-list replacement is straightforward to retry and avoids exposing arbitrary user IDs as write targets.
- **Alternatives and tradeoffs:** Requiring the mobile client to combine separate per-user requests would duplicate authorization and aggregation logic. Reusing a Ledger-specific port would couple Calendar to a contract named for another bounded context.
- **Impact:** Reads/writes are limited to authenticated outing attendees. The backend obtains attendee IDs from a new Consensus-owned Calendar public contract, and does not let the request choose a user ID.

### 3. Keeping writes safe for the existing offline queue
- **Decision:** Make the replacement route a `PUT` mutation and apply the existing optional idempotency mechanism to it; validate every time range and save all windows in the same transaction.
- **Why:** The mobile queue already replays `PUT` requests with the original key. Server-side response replay protects the request if the client loses the response after commit.
- **Alternatives and tradeoffs:** A series of per-window `POST` requests adds partial-update states and more queued writes. Client-only idempotency cannot resolve an uncertain server commit.
- **Impact:** An empty list intentionally clears that caller's availability for the outing; successful updates return the resulting caller-owned windows.

### 4. Implementing the mobile experience
- **Decision:** Keep the HTTP client injectable, add typed availability response/request models and a small repository, let an attendee add/remove dated time windows with an optional tentative flag, and render the backend's common windows in the same screen.
- **Why:** Injected Dio keeps this step testable without inventing login/session UI or hard-coding a device-specific backend address. The server remains the source of truth for group intersection.
- **Alternatives and tradeoffs:** Duplicating the intersection algorithm in the client risks disagreement with backend semantics. A full calendar/heatmap package is broader than the current persisted interval model and is deferred until the exact calendar navigation/grid UX is established.
- **Impact:** The screen supports interval-based group overlap, rather than a time-grid heatmap. App-level authenticated navigation and actual device connectivity remain follow-up integration work.

## Verification

- Pending targeted Dashboard test diagnosis, backend Calendar integration tests, Flutter tests/analyzer, and `git diff --check`.

## Outcome and Follow-up

Pending implementation and validation. Feature work should not be considered complete until the target tests pass and the exact live-integration/platform gaps are recorded.
