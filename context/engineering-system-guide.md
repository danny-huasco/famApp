# Family Outing Planner: Engineering System Guide

This guide describes the system that exists in the repository now. It is written to help an engineer build the mental model first, then follow the main request and data flows, and finally understand which parts are recent or still unfinished.

**Current snapshot:** 2026-10-13  
**Active branch:** `staging`  
**Repository state:** implementation changes are uncommitted and unstaged; nothing has been pushed.  
**Status caveat:** the first Dashboard screen and its widget test were just edited when feature work was paused. Their final format/analyze/test result was not available at handoff.

## 1. What the product does

The Family Outing Planner helps members of one family coordinate a date and activity, vote on ideas, record shared expenses, and calculate who should reimburse whom.

At a high level:

```text
Flutter mobile app
  ├─ renders cached outings and expenses from Hive
  ├─ sends REST requests through Dio with a JWT
  ├─ queues authenticated writes locally when offline
  └─ subscribes to live vote tallies over STOMP/WebSocket
             │ REST + STOMP
             ▼
Spring Boot modular monolith
  ├─ authenticates users and scopes access to their family
  ├─ owns Calendar, Consensus, Ledger, and Notifications modules
  ├─ coordinates related modules through public ports and domain events
  └─ persists application records in PostgreSQL
             │
             ▼
Neon PostgreSQL (intended shared/hosted database)
```

This is currently **one Spring application**, not a collection of microservices. The domain modules have logical package and API boundaries inside that application. They share one configured database connection; module boundaries do not mean separate deployments or separate physical databases.

The phased roadmap remains in the [master implementation plan](./master-plan.md). The guide below describes actual source and test evidence, not every future-plan item as if it were complete.

## 2. Foundations and runtime

### Backend

- **Language/runtime:** Java 21.
- **Framework:** Spring Boot 3.5.16, Gradle Kotlin DSL, Spring MVC, Spring Data JPA, Spring Security, WebSocket/STOMP, Bean Validation, and Spring AOP.
- **Entry point:** [`FamilyAppApplication`](../backend/src/main/java/com/familyapp/FamilyAppApplication.java).
- **Build/dependencies:** [`backend/build.gradle.kts`](../backend/build.gradle.kts).
- **Configuration:** [`application.yml`](../backend/src/main/resources/application.yml).
- **Database:** PostgreSQL driver is included; the configured hosted target is Neon. The local `.env` is ignored by Git and must not be committed.
- **Schema management:** the current application configuration uses Hibernate `ddl-auto: update` and SQL logging. Treat that as current development configuration, not as a production migration strategy; review both settings before production.

The backend reads `JDBC_DATABASE_URL`, `JDBC_DATABASE_USERNAME`, and `JDBC_DATABASE_PASSWORD`. JWT configuration uses `JWT_SECRET` and optional `JWT_EXPIRATION_SECONDS` (defaults to one hour). The secret is expected to be Base64-encoded and to decode to at least 32 random bytes. No secret values belong in this guide or in version control.

Backend tests use H2 in PostgreSQL compatibility mode and create/drop their schema. This gives repeatable integration coverage without requiring Neon for every test. A previous local application startup was recorded as connecting to Neon, but the latest full test run was against H2.

### Mobile

- **Language/runtime:** Flutter 3.47.7 / Dart 3.13.5, with the package requiring Dart `^3.13.5`.
- **Project:** [`mobile/family_outing_mobile`](../mobile/family_outing_mobile/pubspec.yaml), generated with the `com.familyapp` organization.
- **Main dependencies:** Hive/Hive Flutter for local persistence; Dio for HTTP; `stomp_dart_client` for live updates; `flutter_secure_storage` for JWT storage; `connectivity_plus` for network state; Provider is selected as the planned state-management dependency.
- **Startup:** [`main.dart`](../mobile/family_outing_mobile/lib/main.dart) initializes Hive before building the app.

Flutter, Dart, and Chrome were available in the environment. Native Android validation still requires Android SDK command-line tools and accepted licenses; Windows desktop validation still requires the Visual Studio C++ workload/components. A live mobile-to-Spring STOMP handshake has not been performed.

## 3. Backend architecture and trust boundaries

### Authentication and family scoping

Registration creates a family and its initial user account. Passwords are BCrypt-hashed. Login and registration return a signed JWT containing:

- `sub`: the user UUID
- `family_id`: the family UUID
- issued-at and expiration timestamps

The JWT implementation is in [`JwtService`](../backend/src/main/java/com/familyapp/auth/security/JwtService.java), and the stateless HTTP security chain is in [`SecurityConfiguration`](../backend/src/main/java/com/familyapp/auth/security/SecurityConfiguration.java). The request filter validates the bearer token and installs a `FamilyPrincipal` carrying both IDs.

Only `/api/auth/**` and the initial WebSocket endpoint handshake are public in HTTP security; other REST endpoints require authentication. The service layer then enforces the more specific family and outing-attendee rules. A family claim is not by itself permission to read every outing's ledger: Ledger checks both family ownership and outing attendance.

The User module owns user and family persistence. Other modules access family/user facts through public ports rather than importing those repositories.

### Logical modules

| Module | Owns | Main responsibility |
|---|---|---|
| Auth | Authentication API and JWT security | Register, login, issue/validate tokens, expose the authenticated principal |
| User | User/family entities and repositories | Create family accounts and provide membership lookups |
| Calendar | Availability and booked calendar events | Find shared free slots, assess event conflicts, and lock a finalized booking |
| Consensus | Outings, attendees, ideas, ranked ballots | Create outings, collect ideas/ballots, tally votes, and finalize an outing |
| Ledger | Expenses, participant shares, outing-ledger row | Split expense amounts and calculate settlement transfers |
| Notifications | Recipient-scoped notification records | Create notices from events and list the authenticated user's notices |
| Idempotency | Idempotency annotation, aspect, records | Replay a prior mutation response safely when the same request is retried |
| WebSocket | STOMP broker and inbound authorization | Authenticate STOMP connections, authorize vote-topic subscriptions, and broadcast tallies |

Within a module, `api` contains contracts/controllers, `application` contains use-case services, `domain` contains business calculations, and `internal.persistence` contains entities/repositories. The boundaries are enforced by coding practice and public interfaces/events; they are not separate deployable services.

Cross-module calls use small public interfaces such as `FamilyMembershipPort`, `ScheduleConflictPort`, `BookedEventPort`, and `OutingLedgerPort`. Shared Spring event records live in `com.familyapp.event`. This keeps Calendar, Ledger, and Notifications from querying Consensus's internal repositories directly.

## 4. Main domain rules and data

The active relational records are represented by JPA entities owned by the corresponding modules. The principal relationships are:

- A user belongs to one family; a family has members.
- An outing belongs to one family and has an explicit attendee set.
- An outing has ideas; each attendee can submit a ranked ballot over its ideas.
- An outing can have an associated booked calendar event and ledger row.
- Expenses belong to an outing, have one payer, and have participant-specific shares.
- Notifications belong to a recipient and refer to a source event.
- Idempotency records are scoped by user, operation, and idempotency key.

### Calendar and conflicts

Availability windows (`UserAvailability`) describe free-time windows and are distinct from booked calendar events. The availability service computes common free slots only when each attendee has availability records for that outing; if an attendee has no recorded windows, it returns no common slots.

When an outing is proposed, only its explicit attendee list is assessed. A booked event conflicts if its interval overlaps the proposal for an attendee. The count is per attendee, not per conflicting event:

- **Green:** no attendee conflicts.
- **Orange:** at least one but no more than half of attendees conflict (including exactly half).
- **Red:** more than half conflict.

Green and orange proposals are allowed; red proposals are rejected. Orange conflicts generate notices only for affected attendees. The detailed rules are in [`calendar-conflict-rules.md`](./calendar-conflict-rules.md).

### Consensus and ranked voting

Outing statuses currently include `DRAFT`, `OPEN`, `FINALIZED`, and `COMPLETED`. Creation puts an outing in `OPEN`, adds its creator to the attendee set, and requires all attendees to belong to the creator's family. The same transaction records a booked event and publishes an outing-created event.

An attendee submits one ranked ballot for the outing. Submitting again replaces that attendee's previous ballot. Every ranked idea must be unique and belong to the same outing. The ranked-choice calculator counts each ballot's highest-ranked idea that remains in the round; if no majority exists, it eliminates all ideas tied at the current lowest count and transfers those ballots to their next remaining choices.

An unresolved tie does **not** automatically pick a winner: the outing stays open so another ballot can be submitted. An attendee may explicitly request the optional coin-flip tie-break for the current tied finalists. The implementation uses `SecureRandom` and records whether finalization used ranked choice or a coin flip. The main service is [`VotingService`](../backend/src/main/java/com/familyapp/consensus/application/VotingService.java); the tally is [`RankedChoiceTally`](../backend/src/main/java/com/familyapp/consensus/domain/RankedChoiceTally.java).

### Ledger and money precision

Money is never represented as binary floating point:

- Backend amounts use `BigDecimal`, currency scale 2, and database `NUMERIC(12,2)`.
- Inputs are rounded to two decimal places with `RoundingMode.HALF_EVEN`.
- Equal shares are split in integer cents. Any leftover cents are assigned deterministically by participant UUID order.
- The mobile cache stores decimal strings and dashboard calculations use integer cents.

For settlements, the Ledger totals each attendee's amount paid minus amount owed. The net balances must sum to exactly zero. A deterministic greedy algorithm pairs the largest debtors with the largest creditors until all balances are zero; it produces a valid plan in at most N-1 transfers for N non-zero balances. It is **not** claimed to globally minimize transfer count for every possible balance set. Settlement transfers are derived, not persisted as if each transfer belonged to one source expense.

Ledger operations require the caller to be both in the outing's family and an outing attendee. Expense participants and the payer must also be outing attendees. See [`LedgerService`](../backend/src/main/java/com/familyapp/ledger/application/LedgerService.java) and the [Ledger decision record](./technical-decisions/2026-10-13-ledger-expense-settlements.md).

## 5. Important request and event flows

### A. Register or log in

1. The client sends credentials to `POST /api/auth/register` or `POST /api/auth/login`.
2. Auth delegates user/family persistence through the User module's public port.
3. Passwords are hashed/verified with BCrypt.
4. Auth issues the signed user-and-family JWT.
5. The Flutter client stores the JWT in platform secure storage and attaches `Authorization: Bearer …` to later Dio calls.

### B. Create an outing

1. An authenticated caller sends outing details and attendee IDs.
2. Consensus validates time order, unique attendees, and family membership; it adds the creator as an attendee.
3. Calendar assesses overlapping booked events for those attendees. Red conflicts reject the request; green/orange are permitted.
4. Consensus persists the outing and its attendee set, records the booked time in Calendar, and publishes an `OutingCreatedEvent`.
5. Before the transaction commits, the notification listener creates de-duplicated notices: a normal creation notice for each attendee, plus conflict notices for only orange-conflicted attendees.
6. A failure in these synchronous database side effects rolls back the originating transaction.

### C. Vote and receive a live tally

1. An attendee posts a ranked ballot to `POST /api/outings/{outingId}/votes`.
2. Consensus validates it and replaces that user's prior ballot inside the transaction.
3. Consensus publishes a tally event; the WebSocket broadcaster sends it **after commit** to `/topic/outings/{outingId}/votes`.
4. Clients connect to `/ws` with a bearer JWT in the STOMP `CONNECT` native headers.
5. The inbound interceptor authenticates `CONNECT`, allows subscription only to a well-formed outing vote topic for an attendee, and rejects client `SEND` frames.
6. The payload contains the outing ID, ballot count, and first-preference counts. It is a compact live summary, not a ranked-ballot disclosure or a stream of every elimination round.

The broker is an in-process Spring simple broker with `/topic` as its broker prefix and `/app` as its application destination prefix.

### D. Finalize an outing

1. An attendee requests ranked-choice finalization.
2. If the tally produces a winner, Consensus marks the outing finalized and publishes `OutingFinalizedEvent`.
3. If the tally is tied, the result reports the tie and leaves the outing open; an explicit tie-break request is separate.
4. Before commit, Calendar marks the booked event locked, Ledger creates its outing-ledger row, and Notifications stores finalization notices for attendees.
5. All these effects are synchronous database work in the originating transaction, so a listener failure prevents a partial finalization.

### E. Record an expense and calculate settlements

1. An attendee posts an expense to `POST /api/outings/{outingId}/expenses`, identifying the payer and the attendees who share it.
2. Ledger validates membership, rounds the amount to currency scale, divides the cents, and persists the expense and shares.
3. An `ExpenseAddedEvent` causes attendee notifications before commit.
4. `GET /api/outings/{outingId}/expenses` lists records; `GET /api/outings/{outingId}/settlements` derives current net balances and transfers from all outing expenses.

### F. Queue and replay a mobile mutation

1. Dio attaches the JWT from secure storage.
2. For authenticated `POST`, `PUT`, `PATCH`, or `DELETE` requests, the offline interceptor creates an `Idempotency-Key` UUID.
3. If connectivity reports offline, or a transport timeout/error makes the result uncertain, the app stores the method, relative path, JSON body/query, content type, timestamp, and key in Hive. It never stores the `Authorization` header. The client receives a **local 202 queued response**, which means “saved on this device,” not “committed by the server.”
4. On reconnection, requests are replayed oldest first with the same key and the current JWT.
5. The backend's optional idempotency aspect fingerprints the request and stores the response in the same transaction as the mutation. Same key + same user/operation/input returns the stored response; same key with different input produces a conflict.
6. Successful queue entries are deleted. Failed entries remain with an attempt count and a safe error description; the flush stops and reports the error through the injected callback.

Idempotency rows currently have no pruning policy and are retained indefinitely. Queue reconciliation/user controls are not yet in the UI.

## 6. REST and real-time API surface

Implemented controller routes:

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/auth/register` | Create a family account and return a JWT |
| POST | `/api/auth/login` | Authenticate and return a JWT |
| POST | `/api/outings` | Create an outing |
| POST | `/api/outings/{outingId}/ideas` | Add an idea |
| GET | `/api/outings/{outingId}/ideas` | List outing ideas |
| POST | `/api/outings/{outingId}/votes` | Submit/replace a ranked ballot |
| POST | `/api/outings/{outingId}/finalize` | Finalize when ranked choice has a winner |
| POST | `/api/outings/{outingId}/tie-break` | Explicitly resolve a current tie |
| POST | `/api/outings/{outingId}/expenses` | Add an expense |
| GET | `/api/outings/{outingId}/expenses` | List expenses |
| GET | `/api/outings/{outingId}/settlements` | Calculate net balances/transfers |
| GET | `/api/notifications` | List the authenticated user's notifications |

The Calendar availability/conflict behavior is currently a backend service and public-port capability; a dedicated mobile-facing Calendar REST API has not yet been implemented. Notifications currently expose a list endpoint; user-facing read-state mutation/push delivery is not implemented.

## 7. Mobile storage, networking, and current screen

### Hive cache

Hive adapter IDs are persistent schema identifiers and must not be casually renumbered:

| Type ID | Model | Stored values |
|---|---|---|
| 0 | `Outing` | UUID strings, title/status, description, start/end times, attendee IDs |
| 1 | `UserAvailability` | IDs, optional outing ID, time range, tentative flag |
| 2 | `Expense` | IDs, decimal amount/share strings, payer and timestamp |
| 3 | `PendingRequest` | HTTP method/path, JSON body/query, content type, key, queue time, retry metadata |

[`HiveService`](../mobile/family_outing_mobile/lib/data/hive_service.dart) registers generated adapters and opens the typed boxes before the UI starts. Model/schema changes need deliberate Hive migration/version handling.

### HTTP and auth

The networking code is in `lib/network/`:

- `AuthTokenStore` abstracts token storage; `SecureAuthTokenStore` delegates to `flutter_secure_storage`.
- `AuthInterceptor` attaches the bearer token, clears it on 401, and invokes an injected unauthorized callback.
- `DioClient` assembles the interceptors and the persistent request queue.
- `OfflineMutationInterceptor` handles queueable mutations and never queues a request without an Authorization header.
- `OfflineRequestQueue` handles Hive persistence, connectivity-driven replay, ordering, key preservation, and error retention.

The backend currently accepts an optional canonical UUID `Idempotency-Key` on the write controllers covered by the idempotency aspect. Existing clients without the header remain supported.

### STOMP voting client

[`StompVotingService`](../mobile/family_outing_mobile/lib/network/stomp_voting_service.dart) converts an HTTP(S) API URL to WS(S) `/ws`, reads the current token before connection/reconnection, and sets it in the STOMP native `Authorization` header. It exposes connection-state, error, and typed `VoteTally` streams, restores requested outing subscriptions on reconnect, and does not expose client-side STOMP sends.

This client has deterministic unit coverage through an injected STOMP client factory. It has not yet completed a live handshake with the running Spring broker.

### Dashboard checkpoint

The generated Flutter counter screen has been replaced in source by a first dashboard at [`dashboard_screen.dart`](../mobile/family_outing_mobile/lib/screens/dashboard_screen.dart). It reads Hive listenables directly, shows a seven-day outing overview and upcoming outing cards, and computes a family-wide unsettled amount from the cached expense payer/shares using integer cents. It is intentionally a **family-wide cached summary**, not a user-specific balance: the current mobile UI does not yet expose a trusted current-user identity to calculate one.

The dashboard implementation and its populated-cache widget test were edited immediately before the user requested a pause. Their final formatter/analyzer/test command output was not captured. Do not treat this latest screen as validated until that command is rerun and its results are reviewed.

## 8. Tests and verification snapshot

### Backend

The most recent full backend command was:

```powershell
.\backend\gradlew.bat -p backend test --no-daemon
```

It completed successfully. The test report contained **43 tests across 20 suites, 0 failures, 0 errors**. Tests use H2 PostgreSQL mode; this does not validate current Neon migrations/runtime behavior. H2 teardown still logs a known, non-fatal PostgreSQL-specific `set client_min_messages = WARNING` warning.

### Flutter

Before the dashboard edit, the full Flutter checks passed:

```powershell
Set-Location .\mobile\family_outing_mobile
flutter analyze
flutter test
```

At that point, analysis reported no issues and all **12 tests** passed, covering Hive round trips, Dio auth/offline behavior, idempotent replay, and the STOMP client model/lifecycle. The dashboard widget test replaced the generated counter smoke test afterward; rerun both commands before relying on that screen.

The pending-request Hive adapter was generated successfully with:

```powershell
dart run build_runner build --delete-conflicting-outputs
```

The generator emitted an analyzer-language-version compatibility warning, but generation completed. Earlier, the Hive-enabled Flutter web app started in Chrome and returned HTTP 200. No Android/iOS native build, full mobile/backend live integration, or production deployment validation has been completed.

## 9. How to resume tomorrow

Recommended orientation order:

1. Read this guide once for the system map.
2. Read the [master implementation plan](./master-plan.md) to separate completed work from future phases.
3. Trace authentication through [`AuthService`](../backend/src/main/java/com/familyapp/auth/application/AuthService.java), [`JwtService`](../backend/src/main/java/com/familyapp/auth/security/JwtService.java), and `FamilyPrincipal`.
4. Trace outing creation through `OutingService`, then follow its Calendar ports and `OutingCreatedEvent` listener.
5. Read `VotingService` and `RankedChoiceTally`; then compare the backend `/ws` authorization with `StompVotingService`.
6. Read `LedgerService`, `ExpenseShareCalculator`, and `MinCashFlowCalculator` before inspecting the mobile balance summary.
7. First verify the paused dashboard change with `dart format`, `flutter analyze`, and `flutter test`; fix any diagnostics before moving into the availability UI phase.

Detailed rationale and earlier verification are retained in the [technical decision records](./technical-decisions/). Particularly useful records:

- [Backend foundation and branch workflow](./technical-decisions/2026-10-13-backend-foundation-and-branch-workflow.md)
- [Consensus and outing foundation](./technical-decisions/2026-10-13-consensus-outing-foundation.md)
- [Ledger precision and settlement decisions](./technical-decisions/2026-10-13-ledger-expense-settlements.md)
- [Events and notifications](./technical-decisions/2026-10-13-event-notifications-and-finalization.md)
- [STOMP backend authorization](./technical-decisions/2026-10-13-stomp-voting-updates.md)
- [Hive models](./technical-decisions/2026-10-13-offline-hive-models.md)
- [Offline requests and idempotency](./technical-decisions/2026-10-13-idempotent-offline-requests.md)
- [Flutter STOMP client](./technical-decisions/2026-10-13-flutter-stomp-client.md)
- [Flutter dashboard](./technical-decisions/2026-10-13-flutter-offline-dashboard.md)

## 10. Remaining work and known limitations

- Verify and polish the just-added Dashboard after resuming; its last validation result is unknown.
- Implement availability setup and the multi-calendar overlap UI, then connect it to a mobile-facing Calendar API.
- Implement idea pitching and ranked-choice voting screens; bind the existing STOMP stream into the voting UI.
- Implement expense capture, ledger display, and settlement screens.
- Add mobile repositories/data synchronization for reading and updating backend data; current dashboard reads Hive, but it does not fetch/sync data itself.
- Add user-facing login/session state and a trustworthy current-user identity in mobile state before showing personal balances.
- Add queued-write reconciliation UI and decide a safe server idempotency-record retention/pruning policy.
- Verify a live STOMP handshake and full mobile-to-backend flows against a test environment.
- Resolve/confirm platform build prerequisites for Android and Windows desktop.
- Review production schema migrations, SQL logging, HTTPS/CORS/origin configuration, secret provisioning, and deployment topology before Phase 6.
- Add backend Docker packaging, production deployment, and end-to-end smoke verification per the remaining plan.

The active Git branch is `staging` for testing; `main` is reserved for production. Work is not staged, committed, or pushed. Review `git status` and the staged file list before any eventual commit.
