# Implementation Status and Coding Instructions

**Snapshot:** 2026-10-15  
**Branch:** `staging`  
**Purpose:** Exact handoff instructions for completing the Family Outing Planner. This is a coding checklist, not a product proposal.

## 1. Progress at a glance

The [master plan](./master-plan.md) contains **25 numbered implementation steps**.

| Status | Count | Share |
|---|---:|---:|
| Fully complete | 18 | 72% |
| Partial / integration verification outstanding | 2 | 8% |
| Not started | 5 | 20% |

**Counting rule:** only count a plan step as complete if its implementation and required verification are both established. Do not count files merely existing as completion. The 72% is a milestone count, not an estimate of remaining engineering hours.

### Fully complete (18)

- **0.1–0.3:** Editor/tool setup, repository conventions, and database configuration/connectivity.
- **1.1–1.4:** Java/Spring backend foundation, configuration, family-scoped authentication, and auth/security tests.
- **2.1–2.4:** Calendar service and conflict rules, outing/idea/ranked voting, Ledger allocation/settlements, and backend integration suite.
- **3.1–3.3:** Transactional application events, STOMP broker security/broadcasts, and recipient-scoped notifications.
- **4.1–4.3:** Flutter project/dependencies, Hive persistence, secure Dio/JWT client, and idempotent offline mutation queue.
- **5.1:** Hive-backed Dashboard. On 2026-10-14, `flutter analyze` passed and all 16 then-current Flutter tests passed, including the Dashboard test after moving fixture writes out of the widget-test fake-async zone.

### Partial (2)

- **4.4 — Mobile STOMP:** typed client, token auth, reconnect, and unit tests exist. A live handshake against the Spring `/ws` endpoint has not been demonstrated.
- **5.2 — Availability and calendar overlap:** backend API, mobile response models/repository, availability editor/overlap screen, and tests are in progress. The API integration test passed; the mobile full test/analyzer pass predates the newly added availability UI. Its screen test was edited after an initial compile failure, but was not rerun.

### Not started (5)

- **5.3:** Idea pitching and ranked-choice voting mobile screens, ballot submission, and live tally binding.
- **5.4:** Expense entry, outing expense list, and settlement screens.
- **6.1:** Backend containerization and local container verification.
- **6.2:** Production deployment and production database configuration.
- **6.3:** Full production/staging end-to-end smoke test, including offline recovery.

## 2. Stop/verification boundary

The user explicitly stopped testing. No tests or builds should be started as part of this handoff. At the time this document was written, there were **no active PowerShell build/test shell sessions**.

Latest known validation:

- Backend: the focused Calendar availability integration test passed after H2 was configured for UTC timestamps. The full backend suite also passed after those Calendar changes; a test-count summary was not recalculated for that run.
- Flutter before the new availability screen/repository tests: analysis passed and all 16 tests passed, including the Dashboard.
- Flutter availability repository/screen: latest changes have **not** completed analysis or tests. An initial targeted screen-test compile failed because Flutter Material symbols and shared fixture constants were missing; those source imports/constants were corrected, but the test was intentionally not rerun after the stop request.
- No live STOMP handshake, native Android/Windows build, production migration, or deployment test has been completed.

When implementation resumes, begin at **Step A below**. Do not assume the availability UI passes until the exact checks in A.5 pass.

## 3. Coding sequence

Follow the sequence below in order. Do not skip acceptance checks or begin Phase 6 while a prior feature's focused checks fail.

### Step A — Finish Calendar availability (plan Step 5.2)

#### A.1 Inspect the current implementation before editing

Read these files and preserve their current boundaries:

- Backend service/controller/contracts:
  - `backend/src/main/java/com/familyapp/calendar/application/AvailabilityService.java`
  - `backend/src/main/java/com/familyapp/calendar/api/AvailabilityController.java`
  - all `Availability*` and `AttendeeAvailability*` records in `calendar/api/`
  - `backend/src/main/java/com/familyapp/consensus/api/OutingAvailabilityPort.java`
  - `backend/src/main/java/com/familyapp/consensus/application/OutingAvailabilityService.java`
- Mobile:
  - `mobile/family_outing_mobile/lib/models/availability_overview.dart`
  - `mobile/family_outing_mobile/lib/network/calendar_availability_repository.dart`
  - `mobile/family_outing_mobile/lib/screens/availability_setup_screen.dart`
  - `mobile/family_outing_mobile/test/network/calendar_availability_repository_test.dart`
  - `mobile/family_outing_mobile/test/screens/availability_setup_screen_test.dart`
- Test support:
  - `backend/src/test/java/com/familyapp/calendar/application/AvailabilityControllerIntegrationTest.java`
  - `backend/src/test/resources/application-test.yml`

Do not revert the Dashboard widget-test change. Its fixture setup belongs in `setUp`, outside the `testWidgets` fake-async callback, because Hive writes otherwise stall the test.

#### A.2 Finish backend contract behavior

The intended endpoints are:

```text
GET /api/outings/{outingId}/availability
PUT /api/outings/{outingId}/availability
```

The overview JSON shape is:

```json
{
  "outingId": "UUID",
  "myWindows": [
    {
      "id": "UUID",
      "userId": "UUID",
      "startTime": "ISO-8601 instant",
      "endTime": "ISO-8601 instant",
      "tentative": false
    }
  ],
  "attendees": [
    {
      "userId": "UUID",
      "windows": []
    }
  ],
  "commonFreeSlots": [
    {
      "startTime": "ISO-8601 instant",
      "endTime": "ISO-8601 instant",
      "tentative": false
    }
  ]
}
```

The `PUT` request is:

```json
{
  "windows": [
    {
      "startTime": "ISO-8601 instant",
      "endTime": "ISO-8601 instant",
      "tentative": false
    }
  ]
}
```

Required implementation and checks:

1. Derive the authenticated user and family exclusively from `FamilyPrincipal`. Do not accept a `userId` from the request body or query string.
2. For both endpoints, verify the outing exists, belongs to the principal's family, and includes the principal as an attendee. Return the repository's normal not-found response for unauthorized outings to avoid revealing their existence.
3. Reject null windows and any interval whose end is not strictly after its start. Treat an empty `windows` list as an explicit request to clear the caller's availability for that outing.
4. Replace only the caller's existing windows. Keep delete + insert + overview read in one transaction.
5. Recompute `commonFreeSlots` with the existing `CommonFreeSlotCalculator`; do not duplicate overlap logic in the REST controller.
6. Keep idempotency on the `PUT` route. Same key/same request must replay the original response; same key/different request must conflict.
7. Keep timestamps as UTC instants. Preserve `spring.jpa.properties.hibernate.jdbc.time_zone: UTC` and the test datasource's H2 UTC setting; do not weaken timestamp assertions to local wall-clock time.
8. Extend integration coverage for: own attendee succeeds; another-family user gets not found; same-family non-attendee gets not found; bad interval is rejected without deleting old records; empty list clears only the caller's records; a multi-attendee overlap matches the existing tentative propagation semantics; idempotent replay does not duplicate records.

#### A.3 Finish mobile repository and model behavior

1. Keep JSON parsing strict: fail explicitly for missing/incorrect fields or invalid/reversed timestamps.
2. Keep request timestamps serialized as UTC ISO-8601 strings.
3. In `CalendarAvailabilityRepository`, preserve clear response handling:
   - 2xx overview body -> typed `AvailabilityOverview`
   - local queue response `202` with `queued: true` -> `AvailabilitySaveResult.queued`
   - missing or malformed body -> explicit error, not an empty success-shaped overview
4. Test GET path/response parsing, PUT method/body, queued `202`, and malformed API data with the recording Dio adapter.
5. Do not persist attendee overviews as Hive records until a cache invalidation/refresh policy is defined. The current availability overview should use the server response.

#### A.4 Finish screen behavior and product wiring

1. Keep the screen injectable through `CalendarAvailabilityRepository`; do not construct credentials or a production base URL in the widget.
2. On entry, show loading, then saved personal windows and server-calculated group slots. Show an explicit retry state on load failure.
3. Let the caller add a dated interval, select a start/end time, mark it confirmed or tentative, and remove a draft interval.
4. If the chosen end time is not later than the chosen start, interpret it as crossing midnight and set its end date to the following day.
5. Enable Save only when the draft differs from the last server result. While saving, disable edits and show progress.
6. On successful server save, replace the local draft and overlap display with the returned overview. On local `202`, say “saved on this device and queued to sync”; do not claim that the backend has committed it.
7. Surface save failures visibly and retain the user's draft.
8. Add navigation from an outing card/detail destination to `AvailabilitySetupScreen`, passing the selected outing ID/title and a repository built from the app's configured authenticated `DioClient`.
9. Do not wire a screen to an unconfigured Dio instance. If the authenticated app shell is not ready, keep an explicitly named debug-preview route separate from production navigation and record the integration as pending.

#### A.5 Resume checks (only after testing is authorized)

Run from PowerShell:

```powershell
Set-Location .\mobile\family_outing_mobile
dart format lib\screens\availability_setup_screen.dart lib\models\availability_overview.dart lib\network\calendar_availability_repository.dart test\screens\availability_setup_screen_test.dart test\network\calendar_availability_repository_test.dart
flutter analyze
flutter test test\screens\availability_setup_screen_test.dart test\network\calendar_availability_repository_test.dart test\widget_test.dart
flutter test
```

Then run:

```powershell
.\backend\gradlew.bat -p backend test --tests com.familyapp.calendar.application.AvailabilityControllerIntegrationTest --no-daemon
.\backend\gradlew.bat -p backend test --no-daemon
git diff --check
```

If a test stalls, isolate that test, add temporary diagnostic output only as needed, find the blocking operation, remove diagnostics, rerun the isolated case, and only then rerun the suite. Do not leave diagnostic `print`/`System.out` calls in committed code.

**Step A is complete only when** backend integration tests pass, Flutter analysis is clean, focused and full Flutter tests pass, the screen is reachable from an outing flow with configured Dio, and `git diff --check` passes.

### Step B — Connect app-level authentication and data flow

The screens must be usable through real application services, not only direct widget construction.

1. Add a login/register screen for the existing `/api/auth/register` and `/api/auth/login` endpoints.
2. Construct one app-scoped `SecureAuthTokenStore`, authenticated `DioClient`, `ConnectivityNetworkStatus`, and `StompVotingService`; make their lifecycle explicit and dispose stream/network resources with the app session.
3. Read the API base URL from a build-time/environment configuration value. Fail visibly if it is missing or invalid; do not commit a local/private URL or a credential.
4. On login/register success, persist only the returned JWT in secure storage and move to the authenticated app shell.
5. On 401, clear the token and route to login through the injected unauthorized callback.
6. On logout, clear the token, disconnect STOMP, dispose user-scoped services, and clear or appropriately isolate user-specific cached data and pending requests. Never replay a prior user's queued writes under a different account.
7. Add a current-user session model based on the authenticated response; do not decode unverified client-side JWT claims for authorization decisions.
8. Add widget/unit tests for login success/failure, token restore, unauthorized redirect, logout, and user-bound queue lifecycle.

**Step B is complete only when** a clean app launch can log in against the test backend, subsequent authenticated API calls use the saved token, and logout/401 cannot leak one user's queue or cache into another session.

### Step C — Build outing list/detail and idea pitching (plan Step 5.3 foundation)

The backend exposes outing creation and idea/vote endpoints, but there is no mobile outing repository or idea UI.

1. Add typed request/response mapping for outings and ideas. Validate response IDs, status values, dates, and lists; surface malformed data.
2. Add an outing repository using the app-scoped authenticated Dio client. Store successful outing responses in the typed Hive outings box so the dashboard can render offline.
3. Add a dashboard refresh/loading/error state and an explicit refresh action. Define how remote records replace/merge cached records; do not silently delete unrelated cached entries.
4. Add an outing create flow that collects title, description, start/end instants, and attendee IDs. Do not submit family/user IDs that are not sourced from an authorized roster.
5. Add an outing detail route containing the date, attendee roster, ideas, voting status, and navigation to availability/ledger.
6. Add idea submission/listing using `POST /api/outings/{outingId}/ideas` and `GET /api/outings/{outingId}/ideas`.
7. Reuse the backend's red/orange/green conflict response. Prevent red-slot submission; for orange, show the conflict warning and require explicit confirmation before creating the outing.
8. Tests: outing/idea repository contract tests; offline Hive render; form validation; API errors; navigation; conflict-level behavior.

**Step C is complete only when** a user can create an outing, open its detail, add/list ideas, and see cached outings without network access.

### Step D — Build ranked-choice voting UI (plan Step 5.3 completion)

1. Render the outing's ideas as an ordered list.
2. Allow attendees to reorder their ranking with accessible drag handles plus up/down controls for platforms where drag-and-drop is difficult.
3. Submit the ordered UUID list to `POST /api/outings/{outingId}/votes` with the configured idempotent Dio client.
4. Keep the UI state clear: unsent draft ranking, submitting, saved, offline queued, and failed.
5. Connect `StompVotingService.subscribeVotes(outingId)` only while the detail/voting screen is active; cancel the stream when the screen is disposed or the outing changes.
6. Display connection state and latest first-preference count/ballot count. Treat this tally as informational; do not expose individual ranked ballots.
7. Add finalize and explicit tie-break actions. Do not call tie-break unless the current result is tied; when ranked choice reports a tie, leave the outing open and explain that another ballot or explicit coin flip is needed.
8. Tests: ranking reorder and submission payload; live tally updates; reconnect state; cancellation; no UI finalization on unresolved tie; explicit tie-break path.
9. Live integration check: connect two test clients as attendees to the same outing; vote from client A and verify client B receives the committed tally; verify a non-attendee subscription is rejected.

**Step D is complete only when** the mobile flow passes unit/widget tests and the authenticated two-client test against the backend broker succeeds.

### Step E — Build expense and settlement UI (plan Step 5.4)

1. Add typed expense request/response/settlement mapping.
2. Create an expense form with description, decimal amount text input, payer from the outing attendee roster, and selected share participants from the same roster.
3. Parse input directly into a decimal-safe representation; never convert user money to `double`. Validate positive amount and at most two fractional digits before submission.
4. Submit to `POST /api/outings/{outingId}/expenses` with idempotency enabled.
5. On success, update the Hive expense cache without replacing other outing caches. On local `202`, label the expense pending sync and do not present it as server-confirmed.
6. List cached/server expenses and show payer, split participants, and timestamp.
7. Load `GET /api/outings/{outingId}/settlements`; display each debtor-to-creditor transfer and exact two-decimal amount. Do not recompute backend settlement results using floating-point UI math.
8. Add an offline cache view and a visibly pending marker; reconcile it when queued writes complete.
9. Tests: validation; request payload; idempotency; exact-cent display; settlement rendering; queue-to-confirmed reconciliation; malformed API response.

**Step E is complete only when** a user can add an expense, see its shares, and render the backend's settlement output correctly for the three-person test fixture and at least one odd-cent split.

### Step F — Finish Step 4.4 live STOMP verification

The client and unit tests exist; integration verification remains.

1. Start the backend against the isolated test database/profile with a known test account and outing.
2. Obtain a JWT by registering/logging in through the API; do not put it in source or logs.
3. Run the Flutter app against the local backend using the machine-accessible host for the chosen target (Chrome can use `localhost`; an Android emulator generally needs its host-loopback alias).
4. Subscribe as an outing attendee; confirm `CONNECTED`, then submit a ballot from another attendee and confirm a tally arrives after commit.
5. Attempt an unrelated topic and a non-attendee topic subscription; confirm the server rejects both.
6. Stop/restart the backend or interrupt the socket. Confirm reconnect, reauthentication with the current token, subscription restoration, and visible connection state.
7. Remove/expire the token and confirm reconnect stops and the app enters its unauthorized flow.
8. Record exact endpoint, target, and result without recording the JWT itself.

Do not mark Step 4.4 fully complete until this live authorized/rejected/reconnect sequence passes.

### Step G — Production readiness and deployment (Phase 6)

#### G.1 Database and backend readiness

1. Replace production `ddl-auto: update` with a versioned migration process. Add migrations for all current entities/indexes and test upgrade from an empty schema plus the previous deployed schema.
2. Disable SQL statement logging in production.
3. Verify PostgreSQL timestamp handling remains UTC and amounts remain `NUMERIC(12,2)`.
4. Configure database pooling/timeouts and health checks for the intended hosting platform.
5. Decide idempotency record retention. The window must exceed the maximum supported offline replay lifetime; implement cleanup only after documenting that policy.
6. Verify allowed CORS/WebSocket origins, HTTPS/WSS, secret provisioning/rotation, error responses, and no secret values in logs.

#### G.2 Container and deployment

1. Add/verify `backend/Dockerfile` using Java 21 and the Gradle wrapper.
2. Build the image locally and run it against a disposable test database, not production data.
3. Verify startup health, authentication, one outing creation, one availability update, one expense, and clean shutdown.
4. Deploy first to the configured staging environment. Configure staging-only DB credentials and JWT secret in the platform secret manager.
5. Run migrations and verify rollback/recovery procedures before production.
6. Deploy production only after staging smoke checks pass and the user approves deployment.
7. Never commit `.env`, keys, JWTs, or populated database dumps.

#### G.3 Full end-to-end acceptance

On staging, run this exact sequence with two or more test family accounts:

1. Register a family and authenticate two attendees.
2. Create an outing and confirm both attendees belong to it.
3. Add availability for both, verify a matching overlap, and verify a non-overlap case.
4. Submit ideas, submit ranked ballots, and observe an updated tally from a second client.
5. Exercise a tie; confirm it stays open until another ballot or explicit coin flip.
6. Finalize and verify calendar lock, ledger initialization, and attendee notifications.
7. Add an expense with an odd-cent amount, verify deterministic share allocation and settlement totals.
8. Toggle the device offline; read the cached dashboard and queue a mutation.
9. Restore connectivity; verify the queue replays once, keeps the same idempotency key, and reconciles its state.
10. Repeat a request after simulating a lost response; verify there is no duplicate server record.
11. Check logs/database for errors and confirm no cross-family reads.

**Phase 6 is complete only when** the production smoke test passes and the app's staging and production configuration are demonstrably isolated.

## 4. Best live-UI workflow while coding

Use **Flutter Web in Chrome** as the default fast feedback target because Chrome is installed and the Flutter web app has already started successfully. It avoids blocking UI work on Android SDK or Visual Studio installation.

### 4.1 First visual iteration before authenticated app wiring

1. Add a clearly named debug-only preview entry point or route, guarded by `kDebugMode` and a build-time `DEMO_MODE` define. Do not select demo mode in release builds.
2. Seed deterministic demo outings, availability, ideas, and expenses with fixed UUIDs/times/values. Use a test `CalendarAvailabilityRepository`/Dio adapter or repository interface; do not insert demo data into Neon or a production API.
3. Include screen states in the preview: loading, empty, populated, failed, tentative overlap, confirmed overlap, offline queued, and unauthorized.
4. Launch from `mobile/family_outing_mobile`:

```powershell
flutter run -d chrome --dart-define=DEMO_MODE=true
```

5. Keep the terminal attached; use hot reload for widget/style edits. Use hot restart only when changing app initialization, dependency registration, or static configuration.
6. Resize Chrome to the target phone viewport (start at 390×844 CSS pixels), then also check a tablet/desktop width. Verify scrolling, keyboard avoidance, date/time picker dialogs, accessible labels, text scaling, and empty/error state layouts.
7. Never let demo mode silently fall back to production services. If the demo fixture fails, show/report the failure explicitly.

### 4.2 Real API iteration

After Step B app-level auth and repositories are wired:

1. Start the backend on a local port using the test profile and H2 or a dedicated disposable local PostgreSQL database.
2. Use a test account and test outing only.
3. Configure the Flutter API base URL through a local `--dart-define` or ignored developer configuration. Do not add the URL/credentials to tracked source.
4. Launch Flutter Web against the local backend:

```powershell
flutter run -d chrome --dart-define=API_BASE_URL=http://localhost:8080
```

5. Register/login, then exercise each screen using the API contract in Sections A–E. Inspect browser/network/server logs for HTTP status and response shape; redact bearer tokens before sharing logs.
6. Use browser DevTools network throttling/offline mode to check loading/error/offline UI. For deterministic offline-write tests, prefer automated Dio adapter tests rather than relying only on browser network simulation.
7. For WebSocket verification, check the browser WebSocket frames for `/ws` and the expected `/topic/outings/{id}/votes` subscription. Verify the JWT is sent in the STOMP `CONNECT` frame's native headers, not query parameters.
8. Reset only disposable test data between runs; never clear or mutate production data for UI testing.

### 4.3 Native checks (after platform setup)

- Android: install/accept Android SDK command-line tools/licenses only when the user has approved that setup; use an emulator or device, with the correct host URL for the emulator.
- Windows desktop: install the Visual Studio C++ workload/components only when approved.
- Native UI checks are additional platform checks, not a replacement for the Chrome/API and test feedback loop.

## 5. Durable documentation and repository hygiene

1. Before each agent-initiated implementation slice, read the technical-decision documentation skill and create/update a dated decision record before editing application files.
2. After each slice, record actual commands/results and limitations; never write “passed” for a command that was not run.
3. Keep `.env` and variants out of Git. Keep `.env.example` secret-free.
4. Work on `staging` for testing. Reserve `main` for production.
5. Do not stage, commit, or push unless explicitly asked. Before any eventual commit, review the staged file list and ensure it excludes credentials and generated output.

## 6. Source documents

- [Master implementation plan](./master-plan.md)
- [Engineering system guide](./engineering-system-guide.md)
- [Calendar conflict rules](./calendar-conflict-rules.md)
- [Calendar availability implementation decisions](./technical-decisions/2026-10-14-calendar-availability-workflow.md)
- [Ledger precision and settlement decisions](./technical-decisions/2026-10-13-ledger-expense-settlements.md)
- [Offline queue and idempotency decisions](./technical-decisions/2026-10-13-idempotent-offline-requests.md)
- [Flutter STOMP client decisions](./technical-decisions/2026-10-13-flutter-stomp-client.md)
- [Full technical decision history](./technical-decisions/)
