# Technical Decisions: Implementation Handoff Instructions

- **Date:** 2026-10-15
- **Request:** Stop all active testing and provide only a detailed, unambiguous implementation-progress and next-steps document, including the best way to inspect the live UI.
- **Scope:** Document the completion-count method, current verified/unverified state, exact coding sequence and acceptance checks for remaining mobile and deployment work, and a live-UI validation workflow. Do not make application changes or start tests/builds.

## Context

The master plan contains 25 numbered steps. At handoff, core environment/backend work and the first offline Dashboard are implemented. The Calendar availability API and Flutter screen are in progress. A Flutter analyzer/full test run passed before the latest Calendar UI/repository additions. The Calendar backend API integration test and full backend suite passed after UTC handling was configured. The newly added availability screen test initially failed compilation due to missing Flutter imports and test constants; those source issues were corrected, but no test was rerun after the user explicitly asked to stop testing.

## Process and Decisions

### 1. Defining the progress percentage
- **Decision:** Count only fully implemented milestones whose required behavior and verification are evidenced as complete; count a step requiring a live integration check as partial when only unit coverage exists.
- **Why:** A simple fraction of completed numbered steps is reproducible and avoids estimating effort or treating unfinished code as done.
- **Alternatives and tradeoffs:** A percentage weighted by guessed engineering effort would appear more precise but could not be justified from the plan.
- **Impact:** The document reports 18 of 25 milestones complete (72%), two partial, and five not started, with the counted steps listed explicitly.

### 2. Ordering the work
- **Decision:** Sequence the instructions from finishing the current Calendar step, through voting and ledger UI, app-level authenticated wiring, live UI validation, then deployment.
- **Why:** The mobile features depend on the existing backend contracts, secure auth/network services, and local storage. The steps give a junior engineer concrete file/API/test work without implying the partially built Calendar screen is already integrated into navigation.
- **Alternatives and tradeoffs:** Jumping directly to deployment would conceal missing product screens and mobile data-flow wiring.
- **Impact:** Each coding item has its target files, expected behavior, and acceptance checks; API examples describe contracts already chosen in the current implementation.

### 3. Selecting the live-UI feedback loop
- **Decision:** Prefer Flutter Web in Chrome for fast visual iteration, connected to a local test backend and seeded test data; use an isolated mock-data preview only until authenticated navigation and backend data are ready.
- **Why:** Chrome is already available and the Flutter web app has previously started successfully, while native Android/Windows build prerequisites are unresolved. A local backend/test database lets the engineer exercise the real REST contract without production data.
- **Alternatives and tradeoffs:** A production-connected preview risks changing real family data; a permanent fake repository can hide API mismatches; native device work currently adds setup cost.
- **Impact:** The guide explicitly distinguishes a temporary debug preview from real integration validation and keeps demo fixtures out of production builds.

## Verification

- No tests/builds were run for this documentation-only request, as explicitly requested.
- No active PowerShell test/build shell sessions remained when the request was handled.
- The handoff guide's local links were checked and all resolve.
- `git diff --check` — passed.

## Outcome and Follow-up

Created [the implementation status and coding instructions](../implementation-next-steps.md), including the 18/25 milestone count, exact next implementation sequence, acceptance checks, and Chrome live-UI workflow. No application code was changed and no tests/builds were started. Testing and application implementation remain stopped for this request.
