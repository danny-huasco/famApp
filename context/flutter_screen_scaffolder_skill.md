# Skill: Flutter Screen Scaffolder

## Trigger Pattern
Invoke when creating new UI screens, flows, or views in the mobile app (e.g., `scaffold-screen --name=<screen_name>`).

## Objective
Generate cross-platform Flutter screens that incorporate Optimistic UI updates, local Hive storage caching, Dio HTTP networking, and STOMP WebSockets.

## Execution Workflow
When creating a screen (e.g., `VotingScreen`, `LedgerScreen`, `OutingDetailsScreen`), scaffold the following layer components:

1. **UI Layer (`lib/views/<feature>/`)**
   - `StatelessWidget` / `StatefulWidget` for screen layouts following clean composition.
   - Separate complex widgets into small, modular sub-components in `lib/views/<feature>/widgets/`.

2. **State Controller (`lib/controllers/<feature>_controller.dart`)**
   - Manages state using a clean reactive pattern (e.g., `ValueNotifier` / `ChangeNotifier`).
   - Implements **Optimistic UI Updates**: 
     1. Instantly update the local state/Hive box.
     2. Render the updated UI state to the user immediately.
     3. Dispatch the background API request via Dio.
     4. Revert state and display a toast alert if the background network request fails.

3. **Data Source Layer (`lib/data/<feature>_repository.dart`)**
   - **Hive Service**: Local persistence for offline read access.
   - **Dio Client**: Network requests using interceptors for JWT Bearer token authentication.
   - **STOMP Client Integration**: Connects to Spring WebSocket topics (e.g., `/topic/outings/{id}/votes`) for real-time live synchronization.

## Constraints & Guardrails
- Maintain 100% strict null safety in Dart.
- Never place raw SQL or HTTP calls directly inside Widget `build()` methods.
- Ensure all screens handle loading, error, and empty states gracefully.