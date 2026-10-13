# Spring Boot Module Generator

When creating a backend module:

- Use the module package specified by the master plan (`com.familyapp.auth`, `user`, `calendar`, `consensus`, or `ledger`).
- Keep persistence entities, repositories, and implementation details inside their owning module.
- Do not import another module's internal types. Cross module calls must use a public interface or Spring ApplicationEvent.
- Put business rules in services and expose behavior through deliberate public interfaces or REST controllers.
- Add focused tests for business rules and tenant isolation where applicable.
- Keep database and API configuration externalized; never commit credentials.
- Represent monetary values with `BigDecimal`, use `RoundingMode.HALF_EVEN` where rounding is required, and map persisted amounts to `NUMERIC(12,2)`.
