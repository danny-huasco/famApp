# Technical Decisions: Flutter STOMP Voting Client

- **Date:** 2026-10-13
- **Request:** Implement the mobile STOMP service for authenticated live voting updates, including reconnect behavior and topic streams.
- **Scope:** Add a Flutter service that connects to the backend WebSocket endpoint, authenticates STOMP `CONNECT` with the current JWT, subscribes to per-outing vote topics, decodes tally messages, and exposes connection/error/message streams.

## Context

The backend configures the STOMP endpoint at `/ws`, publishes to `/topic/outings/{outingId}/votes`, and authenticates the bearer token from the STOMP `CONNECT` frame's native headers. It authorizes vote-topic subscriptions against outing membership and rejects client `SEND` frames. The Flutter project already includes `stomp_dart_client` and a secure `AuthTokenStore`. The STOMP package supports configurable reconnect delay, `beforeConnect`, native STOMP connect headers, callbacks, and subscriptions.

## Process and Decisions

### 1. Matching backend connection and authorization
- **Decision:** Convert the configured HTTP(S) API base URL to WS(S), connect at `/ws`, and load the current JWT in `beforeConnect` into the STOMP native `Authorization` header before each connection attempt.
- **Why:** The backend authenticates STOMP `CONNECT` headers rather than relying on a WebSocket handshake header, and token refresh/clearing must be reflected on reconnect.
- **Alternatives and tradeoffs:** Supplying the token only as a WebSocket handshake header does not match the backend interceptor. Hardcoding the token at service construction would leave stale credentials after a refresh.
- **Impact:** A missing token is surfaced and prevents an initial connection; a later reconnect without a token is stopped rather than attempting an unauthenticated session.

### 2. Managing reconnects and subscriptions
- **Decision:** Use the STOMP package's reconnect support, retain requested outing streams across reconnects, and resubscribe them when the broker reconnects. Keep the service read-only for voting updates.
- **Why:** The library already owns reconnect timing and transport lifecycle; re-subscribing in the successful-connect callback restores subscriptions after it creates a replacement handler.
- **Alternatives and tradeoffs:** Implementing a second retry loop could conflict with the library's own reconnect timer. Resubscribing only on initial connect would leave existing listeners stale after a dropped connection.
- **Impact:** Subscriptions are keyed per outing and cleaned up when their streams are canceled or the service is disposed. The service does not expose a STOMP `SEND` method.

### 3. Parsing and verification
- **Decision:** Decode the backend's ballot count and UUID-keyed first-preference counts into an immutable typed tally model; inject a STOMP-client factory so tests can drive lifecycle callbacks without opening a live socket.
- **Why:** A typed model validates the JSON boundary once, while an injected factory allows deterministic coverage of authentication headers, reconnect subscription restoration, and message parsing.
- **Alternatives and tradeoffs:** Passing untyped maps to UI code defers malformed payload handling. A live broker test would require starting and authenticating against the Spring application and database, which is not necessary for unit verification of the client lifecycle.
- **Impact:** Malformed vote payloads and connection errors are surfaced on an error stream. A live end-to-end STOMP handshake remains a deployment/integration verification item.

## Verification

- `Set-Location .\mobile\family_outing_mobile; dart format lib\models\vote_tally.dart lib\network\stomp_voting_service.dart test\network\stomp_voting_service_test.dart` — completed.
- `Set-Location .\mobile\family_outing_mobile; flutter analyze` — passed with no issues.
- `Set-Location .\mobile\family_outing_mobile; flutter test` — passed all 12 tests, including token refresh on reconnect, outing-topic restoration, tally parsing, malformed payload rejection, and missing-token behavior.
- Live Spring broker handshake was not run; the service lifecycle and headers were exercised through an injected STOMP client in unit tests.

## Outcome and Follow-up

The mobile service now authenticates STOMP connections from secure token storage, reconnects through the STOMP package, restores active vote-topic subscriptions, and exposes typed tally, connection-state, and error streams. Native device networking and the live Spring broker handshake remain integration checks for a configured Android/iOS environment.
