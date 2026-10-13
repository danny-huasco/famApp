# Environment & Agent Harness Setup Guide
**Project:** Family Outing Planner & Expense Ledger  
**Stack:** Flutter (Dart, Hive, Dio, STOMP) + Java 21 (Spring Boot 3.x, JPA, PostgreSQL/Neon) + Vercel  

---

## 1. VS Code Extension Blueprint

Install the following minimalist, high-impact extensions to give your agent and manual workflow maximum productivity with low memory overhead.

### Core Language & Framework Tools
* **Dart & Flutter** (`Dart-Code.flutter`): Full language support, widget inspectors, and hot-reload tooling.
* **Extension Pack for Java** (`vscjava.vscode-java-pack`): Provides LSP diagnostics, Maven/Gradle execution, and Java 21 runtime support.
* **Spring Boot Extension Pack** (`vmware.vscode-boot-dev-pack`): Autocomplete for `application.properties`/`yml`, bean navigation, and actuator endpoints.
* **PostgreSQL** (`ckpoint.vscode-pg-formatter` or `ms-ossdata.vscode-postgresql`): Direct SQL queries against your Neon database branch without leaving VS Code.

### Agent Harness & Orchestration
* **Roo Code / Cline** (`rooveterinaryinc.roo-cline` or `saoudrizwan.claude-dev`): Agentic extension with direct file system, terminal execution, and diff viewing capabilities.
* **Aider / Terminal CLI**: Terminal-based LLM pair programmer for git-disciplined, token-efficient multi-file edits.

---

## 2. Token & Context Optimization Rules

To prevent LLM token degradation and unnecessary file indexing, configure strict ignore files at your workspace root.

### Create `.aiignore` (or `.cursorignore`) in workspace root:
```gitignore
# Build and Generated Files
/target/
/build/
/.dart_tool/
/.pub-cache/
/*.elemental/

# IDE & Tooling Metadata
/.idea/
/.vscode/
/*.iml
/*.ipr
/*.iws

# Environment & Secrets
.env
*.pem
*.key
*.p12

# Database / Logs
*.log
/db_dumps/

# Heavy Documentation Artifacts
/*.pdf
/*.png
/*.jpg
```

---

## 3. Universal Agent Coding Rules (`.clinerules`)

Save this exact content as `.clinerules` in your workspace root. All modern agentic extensions (Roo Code, Cline, Cursor, Continue) will consume these rules to govern their code generation quality.

```markdown
# AGENT INSTRUCTIONS & ARCHITECTURAL CONSTRAINTS

## Stack Specifications
- **Frontend**: Flutter (Dart) using Hive (local caching), Dio (HTTP client with JWT interceptors), and stomp_dart_client (STOMP WebSockets).
- **Backend**: Java 21 with Spring Boot 3.x (Modular Monolith), Spring Data JPA, PostgreSQL (Neon).
- **Security**: Spring Security + JWT. Enforce 1-to-1 User-to-Family tenant binding (`family_id`).

## Architectural Directives
1. **Modular Boundaries**: Backend code must respect module packages (`com.familyapp.auth`, `calendar`, `consensus`, `ledger`). 
   - No cross-module database queries; inter-module communication must use internal Spring ApplicationEvents or explicit service interfaces.
2. **Financial Precision**: ALWAYS use `BigDecimal` in Java and double-precision decimal types in Dart/SQL for expenses and transaction calculations. NEVER use floating-point primitives (`float`/`double`) for monetary values.
3. **Optimistic UI & Offline-First**: Mobile UI actions (voting, expense entry) must update local Hive cache first, firing background network calls asynchronously.
4. **Zero Unnecessary Dependencies**: Do not introduce third-party libraries without explicit user confirmation.

## Code Quality Standards
- Write modular, readable code over clever or overly dense expressions.
- Include concise Javadoc / Dartdoc on public APIs and complex algorithm handlers (e.g., Min-Cash-Flow debt settlement).
- Maintain 100% strict null safety in Dart.
- Ensure Spring entities implement clear `equals()` and `hashCode()` contracts based on business keys or database IDs.

## Token Economy Rules
- Do NOT generate markdown summaries exceeding 5 lines after completing code edits.
- Focus output strictly on code diffs or direct answers.
- Never output binary build artifacts or giant logs in output buffers.
```

---

## 4. Local Automation & Linting Hooks

Ensure local formatters and linters fix code issues before agent execution or git commits.

### A. Flutter Format Configuration
Run in terminal within the Flutter app root:
```bash
flutter config --enable-custom-devices
dart format .
```

### B. Pre-Commit Verification Script (`.git/hooks/pre-commit`)
Create this shell script to verify code health before any commit is finalized:

```bash
#!/bin/sh
echo "=== Running Pre-Commit Diagnostics ==="

# Check Flutter / Dart formatting & linter
if [ -d "mobile_app" ]; then
    cd mobile_app
    echo "Checking Dart code formatting..."
    dart format --output=none --set-exit-if-changed .
    if [ $? -ne 0 ]; then
        echo "Error: Dart code formatting issues detected. Run 'dart format .' and try again."
        exit 1
    fi
    echo "Running Flutter analyzer..."
    flutter analyze
    if [ $? -ne 0 ]; then
        echo "Error: Flutter static analysis failed."
        exit 1
    fi
    cd ..
fi

# Check Java / Spring Boot compilation
if [ -f "pom.xml" ]; then
    echo "Compiling Java Backend..."
    ./mvnw test-compile
    if [ $? -ne 0 ]; then
        echo "Error: Java backend failed to compile."
        exit 1
    fi
fi

echo "=== All Pre-Commit Checks Passed ==="
exit 0
```
Make the script executable:
```bash
chmod +x .git/hooks/pre-commit
```

---

## 5. Ready-to-Execute Checklist

- [ ] Installed VS Code extensions (Flutter, Java Extension Pack, Roo Code / Cline).
- [ ] Created `.aiignore` in workspace root.
- [ ] Placed `.clinerules` in workspace root.
- [ ] Verified JDK 21 and Flutter SDK are accessible in the integrated VS Code terminal (`java -version` and `flutter --version`).
- [ ] Installed and tested pre-commit git hooks.