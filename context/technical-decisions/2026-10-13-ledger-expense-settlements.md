# Technical Decisions: Ledger Expenses and Settlements

- **Date:** 2026-10-13
- **Request:** Continue implementation with the next planned backend module, Ledger.
- **Scope:** Record outing expenses and equal participant shares using exact decimal arithmetic, calculate net balances and settlement transfers, expose authenticated family-scoped APIs, and verify currency/settlement invariants.

## Context

The Consensus module persists outings and explicit attendee sets and enforces authenticated family ownership in its application services. `FamilyPrincipal` provides the authenticated user and family IDs. The project plan defines an Expense with outing, description, amount, and payer, and asks for minimum-cash-flow settlement results. The ledger math validator requires `BigDecimal`, `NUMERIC(12,2)`, HALF_EVEN at currency boundaries, deterministic odd-cent allocation, exact zero-sum balances, and tests that do not claim greedy settlement always globally minimizes transfer count.

## Process and Decisions

### 1. Defining how expenses are split
- **Decision:** Each submitted expense carries the explicit outing-attendee IDs who share it. Split equally in cents; distribute any remaining cents deterministically in ascending UUID order.
- **Why:** Equal split and fractional-cent cases are explicitly called out in the repository's ledger validation guidance. An explicit share-member list supports expenses that apply to only some attendees without inferring participation from every family member.
- **Alternatives and tradeoffs:** Always splitting across every outing attendee is simpler but cannot represent costs paid for only a subset. Custom weighted shares are not specified in the plan and are deferred.
- **Impact:** The payer must be an outing attendee; every share member must be an attendee and each share receives an auditable persisted amount.

### 2. Calculating and returning settlements
- **Decision:** Aggregate each participant's exact amount paid minus amount owed, validate total net zero, and greedily match the largest creditor and debtor until balances are zero. Return payer/payee/amount transfer instructions; do not persist calculated instructions as expense-linked transactions.
- **Why:** Transfers can combine obligations from many expenses, so they do not map accurately to a single source expense. The required API output is a settlement plan derived from the expense ledger.
- **Alternatives and tradeoffs:** Persisting each computed transfer with one `expense_id_fk` would imply a false one-expense provenance and stale plans as new expenses are added. The plan's separate expense-linked Transaction table is deferred until the product defines recorded repayments or per-expense accounting entries.
- **Impact:** The greedy algorithm guarantees valid settlement and terminates in at most N-1 transfers for N non-zero balances, but is not advertised as globally minimum for every arbitrary input.

### 3. Tenant and module boundaries
- **Decision:** Expose outing family, status, and attendee IDs through a Consensus public port; Ledger will not read Consensus persistence entities or repositories directly.
- **Why:** Expense creation needs to validate family scope and attendee membership while preserving the established module boundary.
- **Alternatives and tradeoffs:** Direct repository access is fewer lines but couples Ledger to Consensus's schema and internals.
- **Impact:** Ledger APIs are authenticated and only operate on outings in the caller's family.

### 4. Tightening monetary and access invariants
- **Decision:** Validate that the authenticated user is an attendee of the outing for expense listing and settlement reads, and keep calculation operations explicit about scale and non-null values.
- **Why:** Family membership alone should not expose a specific outing's ledger to a non-attendee; explicit currency invariants prevent accidental scale drift as persisted values are aggregated.
- **Alternatives and tradeoffs:** Family-wide access is simpler, but conflicts with the outing's explicit participant model.
- **Impact:** Non-attendees receive the same not-found response as out-of-family callers, reducing outing existence disclosure; ledger amounts remain fixed at two decimal places.

### 5. Checking concurrent expense writes
- **Decision:** Verify parallel expense submissions through the Spring service proxy and then read the committed expense count and aggregate settlement.
- **Why:** Expenses are append-only rows, so independent inserts should not overwrite each other; a concurrency test checks this actual persistence path rather than only the arithmetic helper.
- **Alternatives and tradeoffs:** A thread-only calculator test would not exercise database transactions or repository behavior.
- **Impact:** The test uses an isolated H2 database and asserts that all submitted expenses remain present and aggregate into the expected balance.

## Verification

- `.\backend\gradlew.bat -p backend test --no-daemon` — passed all 37 backend tests across 16 suites, with zero failures. Includes expense allocation, settlement invariants, outing/ledger integration, and concurrent expense submission coverage.
- After adding the event-driven finalization/notification integration coverage, `.\backend\gradlew.bat -p backend test --no-daemon` passed all 38 backend tests across 17 suites with zero failures or errors.
- Workspace diagnostics for the Ledger module, its Consensus port, and ledger tests — no errors reported.
- `git diff --check` — passed with no whitespace errors.
- The H2 test shutdown logged the existing PostgreSQL-specific `set client_min_messages = WARNING` teardown warning; schema cleanup continued and the Gradle test task succeeded.
- Tests ran against H2; no live Neon/PostgreSQL schema migration or runtime check was part of this step.

## Outcome and Follow-up

Expense capture, deterministic equal share allocation, and derived settlement plans are implemented. Persisted repayment transactions, custom weighted splits, and ledger initialization on outing-finalization events remain follow-up work. The greedy settlement output is valid and uses at most N-1 transfers for N non-zero balances; it is not claimed to globally minimize transfer count for every arbitrary balance set. No commit or push was made.
