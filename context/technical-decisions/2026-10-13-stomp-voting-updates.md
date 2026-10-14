# Technical Decisions: STOMP Voting Updates

- **Date:** 2026-10-13
- **Request:** Continue the backend implementation with real-time voting tally broadcasts.
- **Scope:** Configure the Spring STOMP broker at `/ws`, broadcast updated first-preference counts at `/topic/outings/{outingId}/votes`, and require authenticated outing attendees to subscribe.

## Context

The backend already includes `spring-boot-starter-websocket`, and ranked ballots are persisted through `VotingService`. Consensus currently publishes framework-neutral application events for related feature modules. JWT authentication is available through `JwtService`; ordinary HTTP endpoints require authentication. Outing membership is family-scoped and can be checked via Consensus-owned persistence without exposing its repository to the WebSocket module.

## Process and Decisions

### 1. Choosing the push payload and delivery point
- **Decision:** Publish a small event containing outing ID, ballot count, and first-preference counts after ballot persistence; send it to the existing plan's per-outing STOMP topic only after the database transaction commits.
- **Why:** The initial real-time view can show current first-choice totals without exposing ballot order or uncommitted updates. Replacing a ballot produces a fresh aggregate from the persisted ballots.
- **Alternatives and tradeoffs:** Broadcasting every ranked-choice elimination round is more detailed but costs extra tally work on each ballot. Sending before commit can show clients state that later rolls back.
- **Impact:** The endpoint is push-only for voting; ballot writes remain on authenticated REST endpoints.

### 2. Restricting subscriptions
- **Decision:** Permit the HTTP WebSocket handshake, authenticate STOMP `CONNECT` using a bearer JWT, allow only the vote-topic subscription shape, and verify the authenticated user is an attendee in the outing's family before accepting it. Reject client `SEND` frames.
- **Why:** The vote topic contains outing-specific data and must not be anonymously readable or writable.
- **Alternatives and tradeoffs:** Reusing only HTTP security may block STOMP clients that put credentials in the CONNECT frame. Allowing arbitrary topic subscriptions would expose future broker topics unintentionally.
- **Impact:** Clients must include `Authorization: Bearer <JWT>` in STOMP CONNECT headers. Only an attendee can subscribe to their outing's tally topic.

## Verification

- `.\backend\gradlew.bat -p backend test --tests com.familyapp.websocket.StompAuthenticationChannelInterceptorTest --tests com.familyapp.websocket.VotingWebSocketBroadcasterTest --no-daemon` — passed all three focused authentication/subscription/broadcast tests.
- `.\backend\gradlew.bat -p backend test --no-daemon` — passed all 41 backend tests across 19 suites with zero failures or errors.
- Workspace diagnostics for WebSocket, event, membership, voting, security, and WebSocket tests — no errors reported.
- `git diff --check` — passed with no whitespace errors.
- Spring application test contexts started the in-process broker. No external CLI or multi-client wire-level STOMP handshake was run.
- H2 teardown emitted the pre-existing PostgreSQL-specific `set client_min_messages = WARNING` warning; Gradle completed successfully.

## Outcome and Follow-up

The in-process STOMP broker is configured at `/ws` with `/topic` broadcasts. JWT-authenticated attendees can subscribe to their outing's vote topic; all client sends and unrelated topic subscriptions are rejected. Each committed ballot (including a replacement) broadcasts the current first-preference counts and ballot count. Broker clustering, SockJS fallback, and real multi-client deployment testing remain follow-up work.
