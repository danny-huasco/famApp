# Technical Decisions: Outing Events and Notifications

- **Date:** 2026-10-13
- **Request:** Continue implementation into the backend internal-event and notification phase.
- **Scope:** Publish outing creation/finalization events from Consensus, persist creation/conflict/finalization notifications, lock the calendar booking on finalization, and initialize a persisted ledger header without crossing module persistence boundaries.

## Context

Consensus creates the outing and records its calendar booking in one transaction. Calendar conflict assessment identifies explicit conflicted attendee IDs, but notifications have not yet been implemented. The master plan calls for finalization events to initialize the ledger and lock calendar dates, and for notification records to be created for key events. `FamilyPrincipal` contains the authenticated user and family IDs; notification reads must be restricted to the current user.

## Process and Decisions

### 1. Choosing event delivery timing
- **Decision:** Publish Spring application events inside the owning Consensus transaction and handle them synchronously before commit.
- **Why:** Notification rows, ledger initialization, and calendar locking should not silently diverge from the outing operation that triggered them. A listener failure should roll back the originating transaction.
- **Alternatives and tradeoffs:** After-commit/asynchronous listeners can keep request latency low, but they can leave a finalized outing without a locked calendar date or notifications if downstream writes fail. Durable outbox processing is a future scalability option.
- **Impact:** Side effects remain in a single database transaction and can add request latency; notification or listener persistence errors are surfaced by failing the request.

### 2. Scoping notification visibility and content
- **Decision:** Store one notification per recipient and event, with a unique source/event/recipient key, and only list notifications for the authenticated user.
- **Why:** The agreed outing-created notice goes to every attendee, orange conflict notices go only to conflicted attendees, and finalization is relevant to outing attendees. Recipient scoping prevents cross-user notification reads.
- **Alternatives and tradeoffs:** Storing a shared outing-wide notification with client-side filtering risks exposing another attendee's conflict status.
- **Impact:** The database prevents duplicate event notices for a recipient, and unread state can be updated without exposing other users' notifications.

### 3. Implementing module boundaries and event handlers
- **Decision:** Publish immutable outing-created, outing-finalized, and expense-added event records from their owning application services. Keep shared event contracts limited to identifiers, text, and attendee IDs so they do not import feature-module types. Calendar and Ledger consume only the finalized-event contract; Notifications consumes the event records and persists recipient-scoped records. Finalization stores an outing-ledger row and marks the associated Calendar booking locked.
- **Why:** The existing feature modules already communicate through public API contracts. An application event package avoids making Calendar or Ledger depend on Consensus persistence types.
- **Alternatives and tradeoffs:** Calling Calendar and Ledger repositories directly from Consensus would create persistence-level coupling. A durable event broker or transactional outbox would improve recovery and throughput but is beyond the current in-process MVP.
- **Impact:** `GET /api/notifications` lists only the authenticated user's records. Outing creation sends the normal notice to attendees and conflict notices to only the conflicted attendees; finalization and expense creation notify outing attendees.

## Verification
- `.\backend\gradlew.bat -p backend test --tests com.familyapp.notification.application.OutingEventNotificationIntegrationTest --no-daemon` — passed the focused create/finalize/expense lifecycle integration test.
- `.\backend\gradlew.bat -p backend test --no-daemon` — after adding WebSocket updates, passed all 41 backend tests across 19 suites with zero failures or errors.
- Workspace diagnostics for the changed event, notification, Calendar, Consensus, Ledger, and related test files — no errors reported after fixing compiler and null-analysis findings.
- `git diff --check` — passed with no whitespace errors.
- H2 teardown emitted the pre-existing PostgreSQL-specific `set client_min_messages = WARNING` warning. Gradle completed successfully and the test database tables were dropped.
- Validation used H2 and did not exercise a live Neon/PostgreSQL deployment.

## Outcome and Follow-up

Outing creation, ranked-choice/coin-flip finalization, and expense creation now publish transactional events. The related notification, ledger-initialization, and calendar-lock effects commit or roll back with the triggering operation. Notifications are user-scoped and deduplicated by event type, source event, and recipient. External push/email delivery, notification read-state mutation, and an outbox for durable asynchronous dispatch remain follow-up work. No commit or push was made.
