# Skill: Spring Boot Module Generator

## Trigger Pattern
Invoke when the user requests a new backend module, domain package, or feature boundary (e.g., `generate-module --name=<module_name>`).

## Objective
Scaffold a decoupled, domain-driven package structure inside the Java Spring Boot modular monolith adhering to the project's strict architecture guidelines.

## Execution Workflow
When generating a new module (e.g., `ledger`, `consensus`, `calendar`, `auth`), create the following package hierarchy under `com.familyapp.<module_name>`:

1. **`domain/`**
   - JPA Entities (`@Entity`) with strictly defined `@Id` and immutable auditing fields (`createdAt`).
   - All financial/monetary fields MUST use `BigDecimal` with explicit scale/precision definitions (e.g., `@Column(precision = 10, scale = 2)`).
   - Spring Data JPA Repositories extending `JpaRepository`.

2. **`service/`**
   - Service interface (`<ModuleName>Service.java`) defining public business methods.
   - Service implementation (`<ModuleName>ServiceImpl.java`) annotated with `@Service` and `@Transactional(readOnly = true)`.
   - Write operations MUST be explicitly annotated with `@Transactional`.

3. **`controller/`**
   - REST Controller (`<ModuleName>Controller.java`) annotated with `@RestController` and `@RequestMapping("/api/v1/<module_name>")`.
   - Data Transfer Objects (`*Request.java`, `*Response.java`) with Java 21 `record` types to ensure immutability.
   - Inject user/family context directly from JWT claims.

4. **`events/`**
   - Spring Application Events (`*Event.java`) used for asynchronous cross-module communication (e.g., `OutingFinalizedEvent`).

## Constraints & Guardrails
- **No Direct Package Leakage**: Never import classes from another module's `domain/` or `controller/` package directly. Communicate across modules ONLY via public `service/` interfaces or `events/`.
- **Database Partitioning**: Ensure all entities define table mappings aligned with their specific schema boundary.
- **Validation**: Apply `@NotNull`, `@NotBlank`, or `@Positive` annotations on all DTO records.