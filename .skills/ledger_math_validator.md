# Ledger Math Validator

Use these checks when implementing or changing expense splitting and settlement logic:

- Use decimal arithmetic in Java (`BigDecimal`) and round only at explicit currency boundaries with `RoundingMode.HALF_EVEN`.
- Persist amounts as PostgreSQL `NUMERIC(12,2)` and represent mobile amounts as integer cents.
- For each outing, compute each participant's net balance from amounts paid and amounts owed.
- Assert that all net balances sum exactly to `0.00`.
- Generate transfers only between participants with opposite-signed balances; apply each transfer to both balances.
- Assert that every transfer is non-negative, balances are settled to `0.00`, and no participant pays themselves.
- Cover zero balances, one participant, equal splits, fractional-cent rounding, and multiple simultaneous creditors/debtors in tests.
- Do not assume a greedy settlement always has a globally minimal transfer count; validate the required examples and document the algorithm's guarantee.
