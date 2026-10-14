# Technical Decisions: Flutter Offline Dashboard

- **Date:** 2026-10-13
- **Request:** Implement the first mobile dashboard screen with a calendar overview, upcoming outings, and a balance summary that renders from Hive offline.
- **Scope:** Replace the generated counter screen with a dashboard backed only by locally persisted Outing and Expense records; add a widget test for cached rendering.

## Context

The Flutter app initializes typed Hive boxes for outings, availability, and expenses before calling `runApp`. The `Outing` model contains a time range and status. `Expense` stores exact decimal strings for amounts and per-user shares, so money calculations must use integer cents. There is no current-user identity/session state in the mobile UI yet, so a personal balance cannot be calculated reliably at this step.

## Process and Decisions

### 1. Rendering offline data
- **Decision:** Build the dashboard directly from Hive box listenables, with no API call or network prerequisite.
- **Why:** The step's verification target is immediate rendering from cached data when offline, and Hive is already the persistence boundary for these models.
- **Alternatives and tradeoffs:** Adding a repository/state-management layer would introduce abstractions before any dashboard mutation or remote synchronization flow exists. Direct box listenables keep the read-only first screen small and automatically refresh it when cached records change.
- **Impact:** The app can render the same dashboard in airplane mode; later repository work can replace the widget's box access without changing persisted schemas.

### 2. Summarizing shared expenses
- **Decision:** Show a family-wide unsettled balance derived from cached expense amounts and shares, using integer cents, rather than presenting a user-specific balance.
- **Why:** The mobile app does not yet expose a trusted current-user ID to the dashboard; claiming an individual balance would be misleading. The family-wide net can be derived from the stored payer and split shares.
- **Alternatives and tradeoffs:** Showing total spending is simpler but is not a balance; decoding user identity from a JWT adds an authentication parsing dependency and risks implying freshness. The chosen summary is clearly labeled as cached group balance.
- **Impact:** The displayed value is the sum of positive net balances among cached expenses. It is an offline snapshot and may not include expense updates absent from Hive.

### 3. Calendar overview and verification
- **Decision:** Show a seven-day strip centered on today, mark days with cached upcoming outings, and test the populated dashboard with a temporary Hive database.
- **Why:** A compact weekly view provides an at-a-glance calendar without adding a calendar package or server dependency.
- **Alternatives and tradeoffs:** A full month grid or heatmap is better suited to the later availability/overlap screen; adding it now would broaden this dashboard step.
- **Impact:** The dashboard previews the next week and lists future outings; richer calendar navigation remains a later UI step.

## Verification

- Pending implementation and Flutter tests.

## Outcome and Follow-up

Pending verification.
