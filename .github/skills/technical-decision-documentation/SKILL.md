---
name: technical-decision-documentation
description: "Use before and throughout every agent-initiated project change, including code, tests, documentation, configuration, and build files. Record the technical decisions, justifications, tradeoffs, and verification in a change-specific decision log."
---

# Technical Decision Documentation

## Objective

Explain the technical choices made during each agent-driven change so a junior developer could clearly justify the work to a tech lead. Keep the explanation grounded in the repository and the actual implementation; do not invent requirements, evidence, or alternatives.

## Required Workflow

1. **Inspect the request and project context**
   - Identify the intended behavior and every file or system boundary likely to be affected.
   - Read relevant implementation, tests, project instructions, and existing decision records before choosing an approach.
   - Reuse established project patterns and identify constraints that affect the change.
2. **Start the decision record before implementation**
   - Create `context/technical-decisions/YYYY-MM-DD-<change-slug>.md`, using the current date and a short, descriptive, kebab-case slug.
   - If a record with that name already exists, choose a distinct slug; do not overwrite or merge unrelated records.
   - Record the request, scope, relevant context, and initial decisions before editing implementation files.
3. **Explain each meaningful process step**
   - As work proceeds, record the step, the decision, why it fits the requirement and existing design, and the relevant tradeoffs or alternatives.
   - Explain choices in clear, first-person language appropriate for a junior developer reporting to a tech lead: what I chose, why I chose it, and what it means for the project.
   - Cover investigation and design, implementation, and verification. For a step with no genuine technical choice, say so briefly rather than making one up.
   - Update the record if implementation reveals new constraints or changes an earlier decision.
4. **Verify and close the record**
   - Run the smallest relevant existing tests, build, or lint checks. Record the exact commands and their actual results.
   - If a check cannot or should not be run, state that and why; never imply it passed.
   - Summarize the resulting behavior, important tradeoffs, and any follow-up or known limitation.
5. **Report the record**
   - In the final response, link to the decision record and briefly summarize the key decisions and validation result.

## Decision Record Format

Use this structure, adapting or omitting a subsection only when it genuinely does not apply:

```markdown
# Technical Decisions: <change summary>

- **Date:** YYYY-MM-DD
- **Request:** <what was asked>
- **Scope:** <affected behavior and boundaries>

## Context
<Relevant existing patterns, constraints, and evidence inspected.>

## Process and Decisions

### 1. <Investigation or design step>
- **Decision:** <what I chose>
- **Why:** <how this addresses the requirement and fits the codebase>
- **Alternatives and tradeoffs:** <real alternatives considered and why they were not chosen>
- **Impact:** <relevant effects on behavior, interfaces, data, security, performance, or maintenance>

### 2. <Implementation step>
...

## Verification
- `<exact command>` — <actual result>

## Outcome and Follow-up
<What changed, any limitations, and justified follow-up work.>
```

## Guardrails

- Create one focused record per agent-initiated change, including documentation-only and configuration-only changes.
- Keep the record with the project in `context/technical-decisions/`; do not substitute chat-only explanations for the durable record.
- Be specific and understandable, but do not turn routine edits into inflated design claims.
- Distinguish observed facts from assumptions, and disclose unresolved questions or failed checks.
- Do not include credentials, personal data, or other secrets in decision records.
- Do not modify unrelated files just to produce a record; the record itself is part of the requested change documentation.
