# Technical Decisions: Flutter Mobile Foundation

- **Date:** 2026-10-13
- **Request:** Continue implementation with the mobile application's Flutter foundation.
- **Scope:** Generate a Flutter app under `mobile/family_outing_mobile`, declare the planned local-storage, HTTP, WebSocket, path, and state-management dependencies, and verify the generated project.

## Context

The `mobile/` directory contains only `.gitkeep`; there is no existing Flutter project or `pubspec.yaml`. The Flutter stable CLI is available (Flutter 3.47.7, Dart 3.13.5). The master plan specifies Hive, Dio, STOMP, path provider, and either Provider or Bloc for state management.

## Process and Decisions

### 1. Choosing the project layout and state-management dependency
- **Decision:** Place the Flutter project in `mobile/family_outing_mobile` using the organization ID `com.familyapp`, and select Provider for state management.
- **Why:** This matches the plan's example project name and organization. Provider meets the stated state-management requirement while keeping the first mobile foundation small and approachable.
- **Alternatives and tradeoffs:** Bloc offers more structure for complex workflows but adds concepts and boilerplate before the screens and domain state exist.
- **Impact:** Android/iOS package identifiers are rooted at `com.familyapp`; state management can be expanded later if app complexity warrants it.

### 2. Declaring the planned runtime packages
- **Decision:** Add `hive`, `hive_flutter`, `dio`, `stomp_dart_client`, `provider`, and `path_provider` to the generated project's dependencies.
- **Why:** These are the packages called out by the mobile foundation plan for persistence, networking, live updates, state, and filesystem support.
- **Alternatives and tradeoffs:** Delaying dependencies until each feature is implemented would avoid unused packages temporarily, but would leave the requested Step 4.1 foundation incomplete.
- **Impact:** Flutter resolves the package versions in `pubspec.lock`, keeping subsequent builds reproducible.

## Verification

- `flutter pub get` — completed dependency resolution; the first `flutter pub add` attempt stopped after updating the manifest because Windows Developer Mode was not yet enabled. After the user enabled it, `flutter pub get` completed successfully.
- `flutter analyze` — passed with no issues.
- `flutter test` — passed the generated counter smoke test.
- `flutter run -d chrome --no-pub --web-port 8765 --web-hostname 127.0.0.1` — started successfully; `Invoke-WebRequest http://127.0.0.1:8765` returned HTTP 200. The temporary Flutter process was stopped after the check.
- `flutter doctor -v` — Flutter and Chrome were available. Android development still needs the Android SDK command-line tools and license acceptance; Windows desktop development needs the Visual Studio C++ workload/components. Native Android/Windows app launch was not attempted.

## Outcome and Follow-up

The Flutter app shell is generated at `mobile/family_outing_mobile` with the planned Hive, Dio, STOMP, Provider, and path-provider dependencies. The Chrome web target builds and responds. Offline adapters, API clients, WebSocket service behavior, and UI screens remain later plan steps. No SDKs or packages were installed system-wide; Flutter downloaded the declared Dart packages from the project's pubspec.
