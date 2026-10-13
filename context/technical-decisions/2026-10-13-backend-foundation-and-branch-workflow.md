# Technical Decisions: Backend Foundation and GitHub Branch Workflow

- **Date:** 2026-10-13
- **Request:** Continue the family outing planner implementation, use `staging` for testing and `main` for production, and follow the project's technical-decision documentation skill throughout the work.
- **Scope:** Git branch alignment and CI; Java 21 / Spring Boot backend scaffold; Neon configuration; initial auth and user modules; registration, JWT, and tenant-boundary tests.

## Context

The master plan specifies Java 21, Spring Boot 3.x, Gradle Kotlin DSL, Neon PostgreSQL, JWT authentication, and strict user-to-family binding. The project rules require module communication through public interfaces or Spring events, exact currency arithmetic, a `staging`-to-`main` promotion flow, and this decision-record workflow.

The GitHub remote already had an initial `main` commit. The local checkout was an unborn `master` branch with untracked project files. `.env` contains local connection settings and is ignored by Git. Java 21, Flutter, Dart, Git, and the relevant Java/Flutter/Gradle extensions are available. `psql` 18.6 is installed outside PATH at `C:\\Program Files\\PostgreSQL\\18\\bin\\psql.exe`.

This record was started after the documentation-skill requirement was added during implementation. It captures decisions and verification already completed and will be maintained for the remaining work in this change.

## Process and Decisions

### 1. Aligning the local repository with the requested branch flow

- **Decision:** Fetch the existing `origin/main`, create a local `main` tracking branch from it, and make local `staging` start at the same commit. Preserve the local ignore rules and project files while switching.
- **Why:** This makes ongoing work genuinely based on the production branch rather than leaving the project on an unrelated, unborn local branch. The local implementation is now on `staging`; no remote branch has been pushed or modified.
- **Alternatives and tradeoffs:** Committing the current files before branch alignment would have created an avoidable initial commit before the user requested one. Recreating a remote branch or pushing changes would also exceed the requested local implementation work.
- **Impact:** `staging` and `main` share GitHub's initial commit. Changes remain uncommitted and local until explicitly committed and pushed.

### 2. Recording and testing the branch promotion policy

- **Decision:** Document development on `staging` and promotion of verified changes to production `main` in `.clinerules`. Add a GitHub Actions workflow that runs backend tests on pushes and pull requests targeting either branch.
- **Why:** The project needs both a human-readable promotion rule and an automated check that can serve as a merge gate.
- **Alternatives and tradeoffs:** CI can run tests, but it does not itself configure GitHub branch protection or production/staging deployments. Those require repository settings and deployment configuration and are deferred to the relevant deployment phase.
- **Impact:** The workflow runs `backend/gradlew test` with Java 21 for both branches. No GitHub settings or remote deployments were changed.

### 3. Selecting and generating the backend scaffold

- **Decision:** Pin Spring Boot 3.5.16 with Java 21 and Gradle Kotlin DSL. Use the official Gradle 8.14.3 wrapper files and the planned Spring Web, JPA, Security, WebSocket, PostgreSQL, Lombok, and JJWT dependencies.
- **Why:** Spring Initializr's current metadata no longer lists Boot 3.x, but Maven Central has the Boot 3.5.16 Gradle plugin. Initializr generation failed for both the requested 3.5.16 version (HTTP 400) and the offered Boot 4 releases (HTTP 500 while resolving their BOM). A direct scaffold preserves the plan's specified Spring Boot 3.x stack rather than silently upgrading it.
- **Alternatives and tradeoffs:** Switching to Boot 4 would contradict the master plan. Waiting for Initializr to recover would block progress without changing the intended code. Direct scaffolding means the build files were authored locally rather than emitted by Initializr, but the resulting project remains a standard Gradle Kotlin DSL Spring Boot application.
- **Impact:** `backend/` now has a reproducible Gradle wrapper and Java 21 toolchain. The wrapper JAR is explicitly allowed by `.gitignore` despite the inherited global `*.jar` rule.

### 4. Configuring local Neon access

- **Decision:** Keep datasource credentials in the ignored root `.env`, import it as a properties file from `backend/src/main/resources/application.yml`, and leave schema management at the plan's `ddl-auto: update`.
- **Why:** Spring Boot does not load a root `.env` automatically; the explicit optional import makes ordinary Gradle launches use the local settings without checking credentials into source control. The integration test profile uses H2 instead of the live Neon database.
- **Alternatives and tradeoffs:** Manually exporting credentials for each run works but is repetitive. Running database integration tests against Neon would require CI secrets and risks mixing test data with a managed environment; H2 gives isolated repeatable tests but does not replace a later PostgreSQL-specific verification.
- **Impact:** The psql connectivity query succeeded against Neon. Hibernate also opened a Neon connection during application startup. The initial startup attempt hit an already-occupied port 8080; retrying on an ephemeral port succeeded and responded with HTTP 401 at the protected root path.

### 5. Establishing auth/user module boundaries

- **Decision:** Keep user and family persistence behind `com.familyapp.user.api.UserAccountPort`; make the auth module use only that public API. Registration creates a family and its first user in one transaction, normalizes email, and stores a BCrypt password hash. JWTs contain the user subject and `family_id` claim and are parsed into a `FamilyPrincipal` by a stateless security filter.
- **Why:** This satisfies the plan's family claim and module-isolation rules while keeping repositories and persistence entities inside the user module.
- **Alternatives and tradeoffs:** Letting auth import repositories or JPA entities would couple the modules across the persistence boundary. A registration-only family creation flow follows the plan's registration verification but does not yet implement inviting additional family members.
- **Impact:** Auth exposes register and login routes, validates input, returns generic invalid-credential responses, and rejects malformed bearer tokens. The local ignored `.env` has a generated 256-bit development JWT secret; production must use a separate secret.

### 6. Verifying registration and tenant scoping

- **Decision:** Use an H2-backed Spring test profile for persistence integration tests and test-only JWT key material; add registration and family-scoped repository tests.
- **Why:** Tests remain repeatable and do not depend on private Neon credentials. They verify that registration stores a password hash, creates a family, and that a family-scoped user query excludes another family's user.
- **Alternatives and tradeoffs:** Pointing tests at Neon would more closely match production PostgreSQL, but would make local/CI tests dependent on live credentials and shared data. H2 is intentionally a fast integration layer; PostgreSQL-specific compatibility remains a follow-up check.
- **Impact:** The latest recorded Gradle test run passed 5 tests with 0 failures, 0 errors, and 0 skipped tests.

### 7. Enforcing the family creator relationship

- **Decision:** Replace the creator UUID-only mapping with a JPA one-to-one relationship to the user row. Persist the family first with a temporarily null creator, persist its initial user, then set the creator before the same transaction commits.
- **Why:** The plan names `created_by_id_fk`; a scalar UUID alone would not create a database foreign-key constraint. The transaction makes the relationship complete atomically while retaining the user-to-family foreign key.
- **Alternatives and tradeoffs:** A fully non-null circular pair of foreign keys cannot be inserted in one ordinary transaction without deferred constraints. The chosen mapping gives the creator column a real foreign key and uniqueness constraint; it permits null at the database-schema level during creation, while the registration service always fills it before commit.
- **Impact:** Registration will fail atomically rather than committing a family with a missing or invalid creator. The persistence integration test will verify that the persisted creator is the registering user.

### 8. Making integration-test cleanup respect the foreign keys

- **Decision:** Replace manual deletion in each integration test with a test-managed transaction that rolls back after each test.
- **Why:** After adding the real creator foreign key, the previous cleanup tried to delete users before their owning family rows and correctly failed referential integrity checks. Rollback restores the isolated initial database state without temporarily nulling a valid relationship.
- **Alternatives and tradeoffs:** Explicit ordered deletes would need to clear the creator link, delete users, and then delete families. That adds test-only persistence operations and is less representative than transaction rollback.
- **Impact:** The next test run must confirm both registration and tenant-isolation tests pass with the stronger relationship.

### 9. Revising calendar behavior to match the clarified event-conflict use case

- **Decision:** Do not implement the tentative-slot approval/notification proposal. Treat event attendees as an explicit list, keep free-time availability distinct from already-booked events, and classify a candidate time by the number of attendees with an overlapping booked event: green for zero conflicts, orange for a positive count at or below half of attendees, and red for more than half. Block creation in red; permit orange and send the normal new-event notification to all attendees plus a conflict notification to each attendee with an overlap.
- **Why:** The user clarified that the product problem is a group event conflict heatmap, not a poll over tentative windows. Counting attendees, rather than event rows, avoids one person with several conflicts skewing the color.
- **Alternatives and tradeoffs:** Treating every family member as an attendee would ignore the explicitly requested attendee list. Reusing free-availability rows as booked events would conflate opposite meanings. Permitting red would contradict the user's instruction to lock creation there.
- **Impact:** Calendar scheduling will need a persisted booked-event representation, explicit outing participants, and a conflict check before an outing is created or scheduled. A conflict notification is per conflicted attendee per newly created orange event; a red attempt creates no event and therefore sends no creation notifications. The previous proposed notification-on-calculation dedup behavior is superseded.

### 10. Calculating shared free-time windows

- **Decision:** Keep free-time windows in `UserAvailability`; calculate the intersection across an explicit attendee set when supplied. Preserve the plan's `findCommonFreeSlots(UUID outingId)` entry point as a convenience for users who have submitted windows, while adding an attendee-aware form for outing integration.
- **Why:** A missing free-time row means the system has no confirmed free interval for that attendee, so attendee-aware calculation returns no shared slots rather than silently omitting that person. Availability is not reused as a booked event.
- **Alternatives and tradeoffs:** Treating each availability row as a busy event would reverse its meaning and undermine the clarified conflict heatmap. Inferring all participants from rows alone is insufficient when an attendee has not submitted availability; the explicit form preserves the event's attendee set.
- **Impact:** Per-attendee overlapping windows are unioned before intersecting attendees. Intervals use half-open `Instant` ranges; tentative status is preserved on resulting slots but does not generate notifications. Booked-event conflicts, creation blocking, and conflict notifications remain for the later event/consensus integration.

### 11. Fixing the first calendar compilation check

- **Decision:** Add the missing import for the public `AvailabilityWindow` DTO in the persistence entity that maps database rows into that DTO.
- **Why:** The first Gradle test run caught a compile error because the DTO and persistence entity live in separate packages. No change to the data model or behavior is needed.
- **Alternatives and tradeoffs:** Moving the DTO into the persistence package would blur the public API/persistence boundary. The explicit import preserves the intended module layering.
- **Impact:** Re-run the backend tests after the import fix; the calendar work is not verified until they pass.

### 12. Modeling booked-event conflict assessments

- **Decision:** Store booked calendar events and their attendee IDs inside the calendar module, separate from free-time availability. Expose a public conflict-assessment port that receives the proposed half-open time interval and explicit attendee IDs, and returns the distinct conflicted attendee set plus the green/orange/red classification.
- **Why:** The clarified use case needs to compare a candidate event against existing booked events and must count each attendee once, even if that person has multiple overlapping bookings. A public port lets the future consensus module request a check without importing calendar persistence details.
- **Alternatives and tradeoffs:** Deriving busy events from free-time windows would conflate separate concepts. Counting event rows could incorrectly push a slot into red because one attendee has several existing events. Storing attendee IDs within Calendar avoids cross-module entity imports, but requires the consensus module to publish scheduled-event facts later.
- **Impact:** Conflict assessment will validate a non-empty attendee set, count overlaps using `existing.start < candidate.end && existing.end > candidate.start`, classify zero as green, positive counts at or below half as orange, and greater than half as red. Persisting booked events after a successful schedule is deferred to the event integration step.

### 13. Verifying the conflict implementation

- **Decision:** Add focused tests for the color thresholds and service contract, and correct issues exposed by the first run before considering this step complete.
- **Why:** The first test run caught that checking an immutable Java set with `contains(null)` throws instead of returning false. The check should handle null elements consistently across set implementations, and duplicate event rows must not duplicate an attendee in the stored participant collection.
- **Alternatives and tradeoffs:** Relying only on the database uniqueness constraint would reject duplicate attendee rows late at persistence time. Guarding the collection before adding provides a clearer invariant, while the unique constraint remains as a final integrity guarantee.
- **Impact:** After the fix, validation will cover the empty attendee case, exact-half classification, multiple existing events for one attendee through distinct query semantics, and the repository's half-open time overlap predicate.

## Verification

- `code --list-extensions` — required Java, Flutter, Dart, and Gradle extensions were present; GitLens was subsequently confirmed present.
- `java -version` — Temurin Java 21.0.12.1 was active.
- `flutter --version` — Flutter 3.47.7 / Dart 3.13.5 were available.
- `psql --version` — PostgreSQL 18.6 was available via its full path.
- `psql ... -c 'SELECT current_database() IS NOT NULL AS connected;'` — returned `t` for Neon. The test used process-scoped `PGPASSWORD`; credentials were not printed.
- `.\gradlew.bat build --no-daemon` from `backend/` — passed before auth implementation.
- `.\gradlew.bat test --no-daemon` from `backend/` — passed with 5 tests before tightening the family creator foreign key. The next run after that change failed 2 tests during `@AfterEach` cleanup because it deleted users before families; this is the issue being corrected in step 8, not a registration or tenant-query failure.
- `.\backend\gradlew.bat -p backend test --no-daemon` — final run passed all 16 backend tests, including the calendar color-boundary tests, service validation tests, and H2 repository overlap test. An earlier run exposed a null-check issue with immutable sets; it was fixed. A transient compile failure from a removed test import was also fixed before the passing run.
- Workspace diagnostics for the newly added/changed conflict-assessment and repository test files — no errors reported.
- The H2 repository test passes, but Hibernate logs an unsuccessful PostgreSQL-specific `set client_min_messages = WARNING` command during schema teardown; this does not fail the test task and should be reviewed if test-database dialect configuration is changed.
- Spring Boot startup on `--server.port=0` — Hikari connected to Neon, Hibernate initialized, and the app responded with HTTP 401 at `/` as expected for an unauthenticated request.
- A later startup attempt after adding `.env` import and auth configuration was interrupted when the user requested a pause; it was not counted as a successful check. The test suite does use the dedicated H2 test profile.
- Workspace diagnostics for `backend/` — no errors reported.
- Git status — local `staging` and `main` both point to the fetched initial GitHub commit; `.env` is ignored; the wrapper JAR remains untracked/trackable, not ignored.

## Outcome and Follow-up

The repository is aligned locally with the requested staging/production branch model, has a Java 21 Spring Boot 3.5 backend foundation, Neon configuration, initial authentication and user modules, repeatable registration/tenant tests, and a booked-event conflict assessment port with persisted events and explicit attendees. Conflict assessments use distinct attendee IDs and half-open time overlap; zero conflicts are green, positive conflicts through exactly half are orange, and a majority is red. The GitHub Actions workflow is ready to run when these changes are pushed. No commit, push, branch-protection change, or deployment was performed.

The auth work is still foundational: registration creates a new family for each account, and inviting additional accounts into an existing family is not implemented. Calendar conflict results are not yet connected to consensus outing creation; rejecting red slots, saving successfully scheduled events through that flow, and publishing notifications remain follow-up work. PostgreSQL-backed integration coverage, required GitHub branch protection, and branch-specific staging/production deployment configuration also remain follow-up work in their corresponding plan phases.
