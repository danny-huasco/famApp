# Technical Decisions: Consensus and Outing Foundation

- **Date:** 2026-10-13
- **Request:** Continue implementation with consensus/outing creation, enforcing the agreed calendar conflict policy, and add the plan's idea and ranked-choice voting foundation.
- **Scope:** Add family-scoped outing creation with explicit attendees and the calendar conflict guard, then implement idea submission and ranked-choice voting. If a runoff remains tied, leave the outing OPEN for another vote; also offer a separate, explicit coin-flip action that selects among only the tied finalists.

## Context

The application already exposes `FamilyPrincipal` to authenticated requests, stores users with a family foreign key, and has `UserAccountPort` as the public boundary for creating family accounts. Calendar exposes `ScheduleConflictPort`; its implementation checks distinct attendee conflicts over half-open time intervals and returns green/orange/red. Calendar event persistence accepts an outing UUID, title, interval, and attendee IDs, but its repository is internal to Calendar. The project plan specifies outing states, ideas, ranked votes, and ranked-choice tallying, but no consensus code exists yet. Notifications are not implemented, so this step must not claim to send them.

## Process and Decisions

### 1. Defining consensus boundaries
- **Decision:** Keep outing/idea/vote persistence in `com.familyapp.consensus` and let Consensus depend on Calendar's public conflict port rather than Calendar persistence internals.
- **Why:** This preserves the stated module boundary while allowing outing creation to enforce the already-approved calendar policy.
- **Alternatives and tradeoffs:** Writing Calendar tables directly from Consensus would couple modules through persistence details and duplicate Calendar's responsibility. A Calendar write port may be needed later for scheduled-event synchronization; this implementation will use the existing Calendar event persistence through a narrow public Calendar operation if required.
- **Impact:** Outing creation and booked-event recording must be atomic so a red slot or failed outing write never leaves an orphan calendar event.

### 2. Scoping outing creation
- **Decision:** Use the authenticated family principal for outing ownership and require explicit attendee IDs to be members of that family; include the creator in the attendee list.
- **Why:** These constraints prevent cross-family access and align voting with the user-approved explicit-attendee scheduling rule.
- **Alternatives and tradeoffs:** Letting callers supply a family ID would trust client-controlled tenant context. Reading the user repository directly from Consensus would violate module boundaries.
- **Impact:** Outing creation is authenticated and family-scoped. Idea, vote, and ranked-choice behavior remain outstanding within the Consensus phase.

### 3. Defining tied-vote behavior
- **Decision:** If ranked-choice elimination ends with tied finalists, keep the outing OPEN so attendees can submit updated rankings. Provide coin flip only through a separate, explicit user action, and limit its candidates to the current tied finalists.
- **Why:** The user requested another chance to vote and selected an optional coin-flip action as an additional fun way to resolve the tie.
- **Alternatives and tradeoffs:** Automatically choosing from a tie would finalize without user intent. Requiring the user to choose a winner manually would no longer be a coin flip.
- **Impact:** Normal finalization will not close a tied outing. The explicit coin-flip endpoint will record its chosen idea and mark the outing finalized; its randomness is intentionally nondeterministic.

### 4. Connecting an outing to calendar persistence
- **Decision:** Add a Calendar-owned public write port for recording a booked event, and invoke it from the transactional Consensus creation service only after conflict assessment permits the interval.
- **Why:** Calendar owns booked-event persistence; Consensus owns outing creation. A narrow API keeps repositories private, while a shared Spring transaction ensures the outing and its calendar record commit or roll back together.
- **Alternatives and tradeoffs:** Directly injecting `CalendarEventRepository` would make Consensus depend on Calendar internals. Publishing an asynchronous event would separate the writes and allow an outing without a corresponding booked event if the listener fails; that is not acceptable for initial scheduling correctness.
- **Impact:** Red conflicts throw a domain error before either record is saved; green/orange conflicts are allowed. The assessment response carries conflicted attendee IDs for later notification integration, but this step does not send notifications.

### 5. Validating explicit attendee membership
- **Decision:** Expose family membership through a small public User module port. Require every requested attendee to belong to the authenticated family and include the creator.
- **Why:** The JWT principal is the tenant authority; IDs from a request body alone cannot establish family membership.
- **Alternatives and tradeoffs:** Reading `UserRepository` from Consensus would violate module boundaries. Requiring the client to send a family ID would be unsafe and redundant.
- **Impact:** The caller cannot schedule an outing for another family or omit themselves from the outing's explicit attendee set.

### 6. Implementing and checking the outing flow
- **Decision:** Persist an outing and its booked Calendar event within one transaction after conflict assessment permits it. Return the assessment, including orange-conflict attendee IDs.
- **Why:** Green and orange are allowed by the agreed rules; red must create neither record. Transactional service integration plus a public Calendar write port preserves module ownership and consistency.
- **Alternatives and tradeoffs:** Notification delivery is not wired yet, so orange conflict details are returned rather than silently discarded; event notifications remain for the notification phase.
- **Impact:** `POST /api/outings` requires authentication, validates attendee family membership, returns HTTP 201 for green/orange and a structured HTTP 409 for red. The initial outing status is `OPEN` to allow later idea and vote submissions.

### 7. Implementing ranked-choice ballots
- **Decision:** Persist one ordered ranking per attendee and outing, replacing that attendee's prior ranking on resubmission. For finalization, repeatedly allocate each ballot to its highest-ranked remaining idea and eliminate all candidates tied at the lowest count in a round. A sole remaining candidate wins; a tie that cannot be reduced leaves the outing OPEN.
- **Why:** The master plan asks for ranked-choice tallying and the user explicitly wants another chance to vote when the runoff is tied.
- **Alternatives and tradeoffs:** Eliminating an arbitrary single lowest candidate would create a hidden ID/order-based tiebreak. Eliminating tied-low candidates together is reproducible, though it can remove more than one idea in a round.
- **Impact:** The vote result is calculated from persisted rankings, not cached tally fields. A separate opt-in coin-flip action may finalize only among the tied finalists and will record that finalization method.

### 8. Protecting concurrent consensus changes
- **Decision:** Use optimistic versioning on outings and update the outing timestamp when ideas or ballots change, rather than requiring a pessimistic row lock.
- **Why:** The first integration run showed Hibernate's PostgreSQL-flavored pessimistic lock clause (`FOR NO KEY UPDATE`) is not supported by the project's H2 PostgreSQL compatibility test mode. An optimistic version check protects concurrent edits without a dialect-specific lock query.
- **Alternatives and tradeoffs:** Removing concurrency protection would let a simultaneous ballot and finalization silently overwrite each other's intent. Pessimistic locking would require test dialect-specific configuration or native SQL.
- **Impact:** Conflicting concurrent idea/vote/finalization transactions may fail with an optimistic-lock exception rather than overwrite one another. The backend tests will verify normal sequential behavior; a specific concurrent stress test remains follow-up work.

## Verification

- `.\backend\gradlew.bat -p backend test --no-daemon` — passed all 27 backend tests, including auth, availability, conflict assessment, outing creation, persistence, ranked ballot replacement, runoff, unresolved tie, and explicit coin-flip tests.
- Workspace diagnostics for the new Consensus module, Calendar write port, User membership port, and related tests — no errors reported.
- Hibernate emitted the already-observed H2 teardown warning for `set client_min_messages = WARNING`; the schema-drop cleanup continued and did not fail the test task.
- An earlier run failed because null-checking Mockito matchers eagerly threw and left matchers registered; tests were corrected to use `verifyNoInteractions` and scoped null suppression for matcher stubbing.
- An integration run exposed Hibernate's PostgreSQL-flavored pessimistic lock syntax as unsupported by H2. The lock query was replaced with optimistic versioning, and the subsequent full suite passed.
- H2 integration tests confirmed an allowed outing and its Calendar event persist, red conflicts save neither, ballots replace correctly, tied ranked-choice finalization leaves the outing open, and the explicit coin flip selects only one of the tied finalists and finalizes with the `COIN_FLIP` method. PostgreSQL production behavior was not exercised in this test run.

## Outcome and Follow-up

The Consensus foundation now supports authenticated family-scoped outings, ideas, ranked ballots, ranked-choice finalization, and the user-selected tied-result behavior. The creator is included in the explicit attendee list; red conflicts are rejected before persistence; green/orange conflicts save the outing and calendar booking atomically. A tied result leaves the outing OPEN for new rankings, and a separate explicit action can choose among only the tied finalists by coin flip. The status enum includes the plan's DRAFT/OPEN/FINALIZED/COMPLETED values, but this initial flow creates outings as OPEN and does not yet implement DRAFT/COMPLETED transitions. Normal creation notices and orange-conflict notices remain deferred until the notifications/event-listener phase. No Git commit or push was made.
