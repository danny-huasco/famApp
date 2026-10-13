# Technical Decisions: Agent Decision Documentation Skill

- **Date:** 2026-10-13
- **Request:** Create a project skill that documents the technical decisions and justifications made during every agent-driven change.
- **Scope:** Add the skill, make it apply automatically to agent change workflows, and document the setup.

## Context

The project already contains domain-specific skill notes under `.skills/` and project guidance in `.clinerules`. There was no `.github/copilot-instructions.md`. VS Code project skills use `.github/skills/<skill-name>/SKILL.md`; a skill is discoverable and can be loaded on demand, but by itself does not establish a project-wide requirement for every change.

## Process and Decisions

### 1. Choosing the skill format and location
- **Decision:** Add the skill as `.github/skills/technical-decision-documentation/SKILL.md`.
- **Why:** This is the supported VS Code project skill location and gives the skill a meaningful name and trigger description. Keeping it in the shared project makes it available to agents working in this repository.
- **Alternatives and tradeoffs:** The repository also has `.skills/` markdown notes, but duplicating the new skill in both locations would create two sources of truth. The VS Code skill format is more directly loadable by the agent.
- **Impact:** Existing skill notes remain untouched; this workflow has a single maintained procedure.

### 2. Requiring the workflow for every agent change
- **Decision:** Add an always-on project instruction at `.github/copilot-instructions.md` and add a corresponding requirement to `.clinerules`.
- **Why:** The skill contains the detailed workflow, while project-wide instructions connect that workflow to every change rather than relying only on a model deciding to load an on-demand skill. Updating both project guidance surfaces covers Copilot and the existing Cline guidance.
- **Alternatives and tradeoffs:** Relying on the skill description alone would be simpler, but would not express the project's mandatory, every-change rule as directly.
- **Impact:** Agent-initiated edits, including documentation and configuration changes, must start and maintain a durable decision record.

### 3. Structuring the durable records
- **Decision:** Require one dated, change-specific Markdown record under `context/technical-decisions/` for each agent-initiated change.
- **Why:** Per-change records are easier to review alongside their implementation and avoid mixing unrelated decisions into a growing log. A consistent format helps explain each step and its justification in clear language.
- **Alternatives and tradeoffs:** A single append-only log would keep everything in one file, but would become harder to navigate and review as the project grows.
- **Impact:** Future agent changes need to include a record with actual decisions, tradeoffs, and test outcomes; this setup change is itself recorded here.

### 4. Validating the setup
- **Decision:** Validate this documentation-only change by checking the added file locations and the skill's required metadata and workflow, rather than running application tests.
- **Why:** No application code or runtime behavior changed, so an application build or test run would not verify the requested outcome.
- **Alternatives and tradeoffs:** Running backend or mobile tests would add time without testing the skill or instruction wiring.
- **Impact:** Validation is limited to inspecting the files and confirming the expected guidance is present.

## Verification
- Manually checked the skill frontmatter, required workflow, project-wide Copilot instruction, Cline rule, and this record's path and contents.
- Application tests/build were not run because the change only adds agent documentation and instructions.

## Outcome and Follow-up

The project now has a reusable technical decision documentation skill, and both Copilot and Cline project guidance require agents to follow it for every change. Future changes should create their own dated record in `context/technical-decisions/`.
