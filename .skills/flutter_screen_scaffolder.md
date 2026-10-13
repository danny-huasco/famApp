# Flutter Screen Scaffolder

When creating a mobile screen:

- Follow the existing Flutter project structure and reuse established widgets and navigation patterns.
- Keep HTTP access behind the app's Dio-based data layer and local persistence behind Hive-backed repositories.
- Preserve offline behavior and optimistic updates where the feature requires them; reconcile queued mutations when connectivity returns.
- Represent money as integer cents. Do not use `double` for monetary values or calculations.
- Keep UI state and error/loading/empty states explicit.
- Add focused tests for important screen behavior and data transformations.
