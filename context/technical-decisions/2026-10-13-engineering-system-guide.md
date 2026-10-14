# Technical Decisions: Engineering System Guide

- **Date:** 2026-10-13
- **Request:** Pause feature development and create a durable technical guide that lets an engineer understand and explain the system, starting with its foundations and then covering recent implementation details.
- **Scope:** Add a current-state guide for architecture, module boundaries, request flows, data and money rules, security, mobile behavior, verification, implementation status, and follow-up work. Do not change application behavior.

## Context

The repository contains a phased implementation plan, per-step technical decision records, a Java/Spring backend, a generated Flutter mobile project, local environment configuration, and an uncommitted working tree on `staging`. Backend source and the prior verification records show a modular monolith with JWT family scoping, Calendar, Consensus, Ledger, Notifications, transaction events, STOMP voting updates, and optional idempotency. The mobile source has Hive models, secure Dio/JWT networking, an offline write queue, and a STOMP voting service. A Dashboard screen and widget test were just added, but the Flutter format/analyze/test command had not returned a result when development was paused.

## Process and Decisions

### 1. Separating foundations from the latest work
- **Decision:** Organize the guide from the system's purpose and architecture through backend and mobile fundamentals, then explain recent features, current status, and remaining work.
- **Why:** A new engineer first needs the module and trust-boundary mental model before understanding individual algorithms and interceptors.
- **Alternatives and tradeoffs:** A chronological activity log would preserve implementation history but make it harder to learn how the system works today.
- **Impact:** The guide is a system explanation rather than a replacement for the implementation plan or detailed decision records.

### 2. Keeping the document evidence-based
- **Decision:** Describe only behavior supported by current source, project configuration, test reports, and existing decision records. Explicitly distinguish completed/verified work from pending dashboard verification and unimplemented plan items.
- **Why:** The guide should be safe to use for onboarding and handoff; overstating verification would mislead the next engineer.
- **Alternatives and tradeoffs:** Calling every planned or recently edited feature complete would make the status section shorter but inaccurate.
- **Impact:** Credentials are excluded, live integration gaps are disclosed, and the next engineer can resume from a clear checkpoint.

### 3. Preserving decision history
- **Decision:** Add a separate current-state engineering guide and retain the focused technical-decision records.
- **Why:** The guide provides an integrated explanation, while the per-change records preserve rationale and exact verification for each implementation step.
- **Alternatives and tradeoffs:** Merging all prior records would create a large document that is harder to maintain and would blur current architecture with change history.
- **Impact:** The guide links to the master plan and detailed decision records rather than copying their entire histories.

## Verification

- Documentation-only change; no application build or test is required.
- The guide's 29 local Markdown links were checked and all resolve.
- `git diff --check` — passed.
- The current Flutter dashboard validation result remains unknown; its in-flight format/analyze/test command was not inspected or reported after the pause request.

## Outcome and Follow-up

Created [the engineering system guide](../engineering-system-guide.md), organized from foundational architecture through recent implementation and remaining work. It excludes credentials and distinguishes verified backend/mobile behavior from the unverified Dashboard edit. Feature implementation remains paused.
