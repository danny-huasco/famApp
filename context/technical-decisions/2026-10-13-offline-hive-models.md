# Technical Decisions: Offline Hive Models

- **Date:** 2026-10-13
- **Request:** Continue the mobile implementation with local Hive storage models for outings, availability, and expenses.
- **Scope:** Define persistent Hive model schemas, generate adapters, register/open local boxes, initialize storage before the Flutter app starts, and test offline write/read round trips.

## Context

`mobile/family_outing_mobile` is a generated Flutter project with Hive dependencies. It currently has only the Flutter counter example and no application models or cache. Backend identifiers are UUID strings in JSON; backend expense values use decimal money and must not be stored as binary floating point.

## Process and Decisions

### 1. Choosing storage representations
- **Decision:** Persist backend UUIDs as strings, times as `DateTime`, attendee/participant IDs as string lists, expense amounts and share values as decimal strings, and the outing status as its API string.
- **Why:** These values round-trip naturally through JSON and avoid losing cent precision through `double`.
- **Alternatives and tradeoffs:** Storing money as `double` is convenient but can introduce binary rounding errors. Using integer minor units requires a currency/scale contract that the backend does not currently expose.
- **Impact:** The mobile cache preserves backend identifiers and money values without inventing a currency or amount conversion.

### 2. Generating adapters and opening boxes
- **Decision:** Use `hive_generator` and `build_runner` for three explicitly typed adapters and initialize Hive before `runApp`; keep one box per model family.
- **Why:** Generated adapters follow Hive's intended type-safe persistence workflow, while typed boxes reduce accidental cross-model writes.
- **Alternatives and tradeoffs:** Hand-written adapters avoid dev dependencies but add repetitive serialization code that is easy to break when fields evolve.
- **Impact:** Adapter type IDs are fixed and must remain stable; schema migrations will be needed if persisted field meanings or IDs change.

### 3. Keeping generation compatible with the adapter toolchain
- **Decision:** Keep the generated app code within the Dart syntax supported by `hive_generator`'s analyzer dependency rather than forcing an incompatible analyzer upgrade.
- **Why:** The first adapter-generation attempt reported that the generator's analyzer could not parse two Dart 3.13 shorthand expressions in the generated demo app. The fully qualified enum/type expressions are behaviorally identical and remain supported by the current generator.
- **Alternatives and tradeoffs:** Forcing analyzer 14 would conflict with the current Hive generator's supported analyzer range; replacing the generator with hand-written adapters would add avoidable maintenance.
- **Impact:** The starter's two new shorthand expressions will be written explicitly so code generation can scan the complete package.

## Verification

- `dart run build_runner build --delete-conflicting-outputs` — succeeded after replacing the generated demo's two Dart 3.13 shorthand expressions that the selected Hive generator's analyzer could not parse; all three typed adapter files are present.
- `flutter analyze` — passed with no issues.
- `flutter test` — passed both tests, including an offline Hive write/read round trip for outing, availability, and expense data.
- `flutter run -d chrome --no-pub --web-port 8765 --web-hostname 127.0.0.1` — app connected in Chrome with Hive startup enabled; `Invoke-WebRequest http://127.0.0.1:8765` returned HTTP 200. The temporary Flutter process was stopped after verification.
- Expense values and shares round-tripped as decimal strings (`12.34`, `6.17`) rather than binary floating-point values.

## Outcome and Follow-up

Hive adapters and typed boxes now exist for outings, user availability, and expenses, and the app initializes the boxes before displaying its root widget. Cache invalidation/synchronization, model schema migration policy, and repository/API integration remain follow-up work.
