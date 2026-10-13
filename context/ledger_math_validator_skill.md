# Skill: Ledger Math Validator

## Trigger Pattern
Invoke when developing, modifying, or testing monetary calculations, debt simplification algorithms, expense splits, or transaction ledger logic.

## Objective
Guarantee 100% financial precision, prevent floating-point rounding bugs, and generate unit tests for complex cash flow settlements.

## Calculation Requirements
1. **Immutable Monetary Representation**
   - Java: ALWAYS use `java.math.BigDecimal`. Set explicit scale (e.g., `2`) and rounding mode (`RoundingMode.HALF_EVEN` / Banker's Rounding).
   - PostgreSQL: Use `NUMERIC(12, 2)` / `DECIMAL(12, 2)`.
   - Dart: Represent currency as integer cents (`int`) or double-precision decimals formatted explicitly via integer math before display. NEVER perform raw floating-point arithmetic (`0.1 + 0.2`) on financial amounts.

2. **Min-Cash-Flow Debt Simplification Algorithm Verification**
   - Verify that total net balance across all family members sums to EXACTLY zero (`sum(net_balances) == 0.00`).
   - Validate that the output transaction list minimizes the total number of transfers required among participants $N$ to at most $N-1$ transactions.
   - Handle edge cases: zero debts, equal balances, and odd splits (e.g., \$100 divided 3 ways results in \$33.33, \$33.33, and \$33.34 assigned explicitly to a deterministic member).

3. **Automated Test Generation**
   - Generate JUnit 5 unit tests verifying ledger edge cases:
     - Multi-person complex bill splits.
     - Fractional penny handling.
     - Concurrent expense logging assertions.

## Constraints & Guardrails
- Reject any pull request or code edit that uses `float` or `double` primitives for monetary values in Java domain logic.
- Ensure all transaction settlement outputs include explicit `payer_id`, `payee_id`, and `amount` attributes.