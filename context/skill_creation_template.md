# Skill: [Skill Name / Identifier]

## Trigger Pattern
Specify the explicit command, prompt keyword, or agent condition that activates this skill.
* *Example:* `invoke-skill --name=<skill_name>` or "When encountering [specific error/pattern]"

## Objective
Provide a concise, 1-2 sentence description of what this skill achieves and why it exists.

## Problem Context & Value
- **Problem Solved**: Briefly describe the failure mode, code quality issue, or manual toil this skill eliminates.
- **Value Generated**: Explain how this reduces token consumption, prevents bugs, or enforces project architecture.

## Execution Workflow
Define the step-by-step instructions the agent MUST execute when this skill is triggered:

1. **Phase 1: Input Validation & Context Inspection**
   - Identify target files, parameters, or environment conditions needed before proceeding.
   - Verify that required dependencies or domain boundaries exist.

2. **Phase 2: Core Processing / Scaffolding**
   - Step-by-step code generation, transformation, or analysis instructions.
   - Specify file structures, naming conventions, and package paths.

3. **Phase 3: Automated Verification & Testing**
   - Run compilation checks, formatters, or unit test executions to validate changes.

## Constraints & Guardrails
List explicit rules, forbidden patterns, and safety boundaries:
- **Rule 1**: [e.g., Do not touch files outside of the target module]
- **Rule 2**: [e.g., Maintain strict null safety / financial precision]
- **Rule 3**: [e.g., Do not introduce external dependencies without approval]

## Output Specification
Define the exact expected output format (e.g., specific file tree, precise code diff, or structured report).