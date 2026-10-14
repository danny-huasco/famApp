# Technical Decisions: Idempotent Offline Requests

- **Date:** 2026-10-13
- **Request:** Implement the mobile Dio/JWT client and offline mutation queue, with server-side idempotency before replaying queued writes.
- **Scope:** Add authenticated Dio requests, secure JWT storage and 401 handling, persistent queued mutations, network-triggered replay, and backend idempotency-key handling for the existing HTTP mutation endpoints.

## Context

The Flutter app has Dio and Hive storage, but no HTTP client, secure token storage, or persistent request queue. The backend mutation endpoints currently generate server-side IDs and do not accept idempotency keys. Automatically replaying an uncertain request without server support could create duplicate outings or expenses. The user selected backend idempotency keys before automatic replay.

## Process and Decisions

### 1. Designing server idempotency
- **Decision:** Accept optional canonical UUID `Idempotency-Key` headers on mutation endpoints, scoped by authenticated user and operation. In the same database transaction as the mutation, persist a fingerprint of the complete request arguments and the serialized response.
- **Why:** A retry with the same key and same request can return the original response without repeating side effects; reusing a key with different input should be rejected explicitly.
- **Alternatives and tradeoffs:** Client-only de-duplication cannot tell whether the server committed after a response was lost. Making the header optional preserves existing clients that do not yet use the queue.
- **Impact:** Records are durable in the application database and conflict checks use the same transaction as the feature operation. Idempotency records are retained indefinitely for now; retention and pruning need a later policy that remains longer than any queued request.

### 2. Protecting credentials and queued payloads
- **Decision:** Store JWTs in platform secure storage, attach them through a Dio interceptor, and clear the token plus invoke an injected unauthorized callback on HTTP 401. Persist only request method, relative path, JSON body, timestamp, and idempotency key in the Hive queue; never persist the Authorization header.
- **Why:** JWTs should not be stored in an unencrypted Hive box, and queued writes need to survive app restarts without retaining bearer credentials.
- **Alternatives and tradeoffs:** Keeping the token in Hive would be simpler but expose credentials in an ordinary local database. Queueing only in memory would lose pending work when the app exits.
- **Impact:** Replayed requests obtain the current JWT at send time. Clients can inject UI-specific handling for 401 without coupling the networking layer to a particular screen.

### 3. Choosing replay behavior
- **Decision:** Queue mutation requests when the device is offline or a transport failure leaves the outcome unknown; replay in timestamp order using the original idempotency key. Do not queue reads or server responses with an HTTP status.
- **Why:** Server idempotency makes replay safe even if the first attempt committed but its response was lost. Ordering keeps a user's queued mutation sequence predictable.
- **Alternatives and tradeoffs:** Retrying only when the OS reports no network misses server/transport failures; retrying HTTP 4xx/5xx responses can mask validation or application errors.
- **Impact:** A queued request returns a local `202` result with its idempotency key. Failed replays remain persisted, record the failure, stop the current flush, and report through an injected callback.

### 4. Implementing and testing the mobile queue
- **Decision:** Generate and register the pending-request Hive adapter, then test the network layer through a recording Dio adapter and a temporary Hive box. Reject an idempotency key that is already queued with a different method, path, payload, query, or content type; leave an identical queued request unchanged.
- **Why:** The queue needs persistent serialization to survive restarts, and overwriting a queued request with a reused key could silently discard the original mutation before the backend can detect a payload conflict.
- **Alternatives and tradeoffs:** Replacing a request on key collision would be simpler but unsafe; silently accepting it would leave the user with false expectations about which payload will be sent. The queue therefore reports the conflict and preserves the existing item.
- **Impact:** Queue serialization is JSON-based for both encoding and replay, and asynchronous connectivity/storage errors reject the request instead of leaving Dio's interceptor handler unresolved.

## Verification

- `Set-Location .\mobile\family_outing_mobile; dart run build_runner build --delete-conflicting-outputs` — succeeded and generated the pending-request Hive adapter. The generator emitted a compatibility warning because its resolved analyzer language version is older than the Dart SDK language version; generation completed successfully.
- `Set-Location .\mobile\family_outing_mobile; dart format lib\network test\network\dio_client_test.dart` — completed.
- `Set-Location .\mobile\family_outing_mobile; flutter analyze` — passed with no issues.
- `Set-Location .\mobile\family_outing_mobile; flutter test` — passed, including the Hive round-trip and network tests for JWT attachment, 401 handling, offline persistence, key-collision protection, replay, and failed replay retention.
- `.\backend\gradlew.bat -p backend test --no-daemon` — passed for the full backend suite. H2 still logs the known non-fatal PostgreSQL-specific `set client_min_messages = WARNING` shutdown warning.
- `git diff --check` — passed.

## Outcome and Follow-up

The authenticated Dio client now attaches the current secure JWT, reports 401s after clearing the token, and queues/replays offline mutations without persisting authorization headers. The backend returns stored responses for matching idempotent retries and rejects a reused key with different input. Server-side idempotency storage retention and user-facing queue reconciliation remain follow-up work. Android/iOS native device builds and a live STOMP/network integration have not been verified in this step.
