# Step-by-Step Implementation Plan — New UI & Backend

This document defines a **single ordered sequence** of implementation steps that combine:

- **UI migration** ([UI_MIGRATION_PLAN.md](UI_MIGRATION_PLAN.md)) — integrating the new UI with the backend and replacing mock data.
- **Backend features** ([BACKEND_FEATURES_FOR_NEW_UI.md](BACKEND_FEATURES_FOR_NEW_UI.md)) — new or changed API required by the new UI.

Each step states **what** to do, **where** (Backend / New UI), **what it depends on**, and **what is delivered**. For each block, **detailed changes**, **testing**, and **documentation updates** are listed so that implementation is traceable and complete.

---

## Documentation references

| Document | Path | Purpose |
|----------|------|---------|
| **REST & WebSocket API contract** | [.github/project/rest_web_socket_api_contracts_f_1_telemetry.md](.github/project/rest_web_socket_api_contracts_f_1_telemetry.md) | Source of truth for endpoints, request/response DTOs, WebSocket message formats. Update before coding new/changed endpoints. |
| **Backend features for new UI** | [BACKEND_FEATURES_FOR_NEW_UI.md](BACKEND_FEATURES_FOR_NEW_UI.md) | List of backend gaps (B1–B14), implementation phases, data sources (e.g. B8 track layout). |
| **UI migration plan** | [UI_MIGRATION_PLAN.md](UI_MIGRATION_PLAN.md) | Old vs new UI comparison, compatibility matrix, gap analysis, phased tasks. |
| **New UI docs** | [NEW_UI_DOCS.md](NEW_UI_DOCS.md) | New UI structure, conventions, known limitations. Update when API/WebSocket/live flows are added. |
| **React SPA UI architecture** | [.github/project/react_spa_ui_architecture.md](.github/project/react_spa_ui_architecture.md) | Screens, layout, data flow, use of REST/WebSocket; reference for global notifications (toast). |
| **Unit testing policy** | [.github/project/unit_testing_policy.md](.github/project/unit_testing_policy.md) | Coverage (85%), TestData, AAA, @DisplayName, AssertJ, Mockito. Backend tests must follow this. |
| **Cursor rule: unit testing** | [.cursor/rules/unit-testing-policy.mdc](.cursor/rules/unit-testing-policy.mdc) | Agent instructions for writing/updating backend unit tests. |
| **Architecture layering** | [.cursor/rules/architecture-layering.mdc](.cursor/rules/architecture-layering.mdc) | Mappers, builders, parsers, processors, services, thin entry points. Backend changes must respect layers. |
| **Logging policy** | [.cursor/rules/logging-policy.mdc](.cursor/rules/logging-policy.mdc) | SLF4J levels, traceId, log messages in entry points and services. |
| **Telemetry API service** | [.github/project/telemetry_processing_api_service.md](.github/project/telemetry_processing_api_service.md) | Service architecture; update if new controllers/services/repositories are added. |
| **Packet Event ingest plan** | [.github/draft/implementation-plans/08-packet-event-ingest-and-processing.md](.github/draft/implementation-plans/08-packet-event-ingest-and-processing.md) | Reference for B3 (session events) — Packet Event (3) ingestion and processing. |

---

## Dependency overview

```
[Foundation]
  Step 1–2   New UI: API layer, Toaster, sessionId, types
  Step 3–4   New UI: Session list (real data), API client (pace, trace, etc.)
  Step 5–6   New UI: Session detail (charts), Edit display name
  Step 7–8   Backend B1 + New UI: Session list filter/sort
  Step 9–13  New UI: WebSocket live, Live Overview/Telemetry, Dashboard recent sessions
  Step 14–16 Backend B4/B5 + New UI: Strategy View (pit-stops, stints)
  Step 17–20 Backend B2/B3 + New UI: Live leaderboard, Session events
  Step 21–22 Backend B8 + New UI: Track layout / Live Track Map
  Step 23–24 Backend B10 (optional) + New UI: Driver Comparison
  Step 25+  Backend B6/B7, B9, Auth/User (optional)
  Step 31–35 New UI: Export, User menu, Test Connection, Diagnostics, Delete All Sessions (Block I)
  Step 36–40 Verification, old UI removal, documentation update (Block J)
```

---

## Steps (in order)

Detailed steps, concrete changes, testing, and documentation updates for each block are in separate files under [.github/draft/implementation-plans/new-ui-backend/](.github/draft/implementation-plans/new-ui-backend/). See [README](.github/draft/implementation-plans/new-ui-backend/README.md) for the list of block files.

### Block A — Foundation and session list/detail (existing API)

**Steps 1–6.** → [block-a-foundation-and-session-list-detail.md](.github/draft/implementation-plans/new-ui-backend/block-a-foundation-and-session-list-detail.md)

API layer, Toaster, sessionId, types; **toast notifications in header Notifications (Bell)** — same events (API errors, success) shown in Sonner and in Bell dropdown; Session History with real data; Session Details load + charts; Edit display name.

---

### Block B — Session list filters (backend B1 + UI)

**Steps 7–8.** → [block-b-session-list-filters.md](.github/draft/implementation-plans/new-ui-backend/block-b-session-list-filters.md)

Backend: GET /api/sessions filter/sort; New UI: Session History filters.

---

### Block C — Live (WebSocket + existing snapshot)
**Steps 9–13.** → [block-c-live-websocket.md](.github/draft/implementation-plans/new-ui-backend/block-c-live-websocket.md) — WebSocket useLiveTelemetry; AppLayout status; Live Overview/Telemetry; Dashboard recent sessions.

---

### Block D — Strategy View (backend B4, B5 + UI)

**Steps 14–16.** → [block-d-strategy-view.md](.github/draft/implementation-plans/new-ui-backend/block-d-strategy-view.md) — Pit-stops, stints; Strategy View.

---

### Block E — Live Overview leaderboard and events (backend B2, B3 + UI)

**Steps 17–20.** → [block-e-live-leaderboard-and-events.md](.github/draft/implementation-plans/new-ui-backend/block-e-live-leaderboard-and-events.md) — Live leaderboard; Session events.

---

### Block F — Live Track Map (backend B8 + UI)

**Steps 21–22.** → [block-f-live-track-map.md](.github/draft/implementation-plans/new-ui-backend/block-f-live-track-map.md) — Track layout; Live Track Map.

---

### Block G — Driver Comparison (optional backend B10 + UI)

**Steps 23–24.** → [block-g-driver-comparison.md](.github/draft/implementation-plans/new-ui-backend/block-g-driver-comparison.md) — Comparison API; Driver Comparison.

---

### Block H — Optional backend and UI (fuel, ERS, positions, auth)

**Steps 25–30.** → [block-h-optional-fuel-ers-positions-auth.md](.github/draft/implementation-plans/new-ui-backend/block-h-optional-fuel-ers-positions-auth.md) — Fuel/ERS, positions, Auth & user.

---

### Block I — Supplementary UI actions (export, user menu, settings)

**Steps 31–35.** → [block-i-supplementary-ui-actions.md](.github/draft/implementation-plans/new-ui-backend/block-i-supplementary-ui-actions.md) — Session Export, User menu dropdown, Test Connection, View Diagnostics, Delete All Sessions. See [MISSING_UI_ACTIONS_ANALYSIS.md](.github/draft/implementation-plans/new-ui-backend/MISSING_UI_ACTIONS_ANALYSIS.md) for the gap analysis.

---

### Block J — Verification, old UI removal, and documentation (final phase)

**Steps 36–40.** → [block-j-verification-old-ui-removal-docs.md](.github/draft/implementation-plans/new-ui-backend/block-j-verification-old-ui-removal-docs.md) — Verify migration parity and new features; remove old UI (`ui/`); update architecture, API contracts, migration plan, and all docs to reflect the new UI as the single front-end.

---

## Summary table (what after what)

| Step | Layer   | Feature / task                          | Depends on |
|------|---------|------------------------------------------|------------|
| 1    | New UI  | API layer, Toaster, sessionId, types     | —          |
| 2    | New UI  | Session list (real data)                 | 1          |
| 3    | New UI  | Session detail (load)                    | 1, 2       |
| 4    | New UI  | API client (pace, trace, ers, etc.)      | 1          |
| 5    | New UI  | Session detail (charts)                 | 3, 4       |
| 6    | New UI  | Edit display name                        | 1, 2       |
| 7    | Backend | B1 — Session list filter/sort           | —          |
| 8    | New UI  | Session History filters + pagination      | 1, 7       |
| 9    | New UI  | WebSocket useLiveTelemetry               | 1          |
| 10   | New UI  | AppLayout connection status              | 9          |
| 11   | New UI  | Live Overview (telemetry from WS)        | 9          |
| 12   | New UI  | Live Telemetry (snapshot to charts)       | 9          |
| 13   | New UI  | Dashboard recent sessions                | 1          |
| 14   | Backend | B4 — Pit stops                           | —          |
| 15   | Backend | B5 — Stints                              | —          |
| 16   | New UI  | Strategy View (pit-stops, stints)         | 4, 14, 15  |
| 17   | Backend | B2 — Live leaderboard                    | —          |
| 18   | New UI  | Live Overview leaderboard                 | 9, 17      |
| 19   | Backend | B3 — Session events                      | —          |
| 20   | New UI  | Live Overview event timeline              | 9, 19      |
| 21   | Backend | B8 — Track layout                        | —          |
| 22   | New UI  | Live Track Map (layout from API)          | 4, 21      |
| 23   | Backend | B10 — Comparison API (optional)          | —          |
| 24   | New UI  | Driver Comparison (real data)             | 4, (23)    |
| 25   | Backend | B6/B7 — Fuel/ERS by lap (optional)        | —          |
| 26   | New UI  | Strategy View fuel/ERS (optional)         | 16, 25     |
| 27   | Backend | B9 — Multi-car positions (optional)     | —          |
| 28   | New UI  | Live Track Map positions (optional)       | 22, 27     |
| 29   | Backend | B11–B14 — Auth & user (optional)          | —          |
| 30   | New UI  | Login/Register/Settings (optional)        | 29         |
| 31   | New UI  | Session Details: Export Data              | 3, 4       |
| 32   | New UI  | AppLayout: User menu dropdown              | 1          |
| 33   | New UI  | Settings: Test Connection (UDP)            | 1          |
| 34   | New UI  | Settings: View Diagnostics                | 1          |
| 35   | New UI  | Settings: Delete All Sessions (confirm + API) | 1, (29) |
| 36   | Verification | Migration parity checklist (UI_MIGRATION_PLAN matrix + gap analysis) | 1–35 |
| 37   | Verification | New features checklist (blocks A–I) | 1–35 |
| 38   | Removal | Remove old UI (`ui/`); update workspaces, Docker, CI to new UI only | 36, 37 |
| 39   | Documentation | Update react_spa_ui_architecture, API contracts, telemetry_processing_api_service | 38 |
| 40   | Documentation | Update UI_MIGRATION_PLAN, IMPLEMENTATION_STEP_BY_STEP, NEW_UI_DOCS, README/runbooks | 38, 39 |

---

## Final state (after Block J)

After completing steps 36–40 (Block J), the migration is complete:

- **Single front-end:** `f1-telemetry-web-platform` is the only UI; the old `ui/` has been removed.
- **Documentation:** Architecture, API contracts, migration plan, and run/README docs describe the system based on the new UI.

See [block-j-verification-old-ui-removal-docs.md](.github/draft/implementation-plans/new-ui-backend/block-j-verification-old-ui-removal-docs.md) for the detailed checklist and doc update list.

---

## How to use this plan

1. **Implement in order** — Complete steps 1–6 to have session list and session detail working with the existing backend; then 7–8 for filters; then 9–13 for live and dashboard.
2. **Backend and UI in parallel** — Steps 7, 14, 15, 17, 19, 21, 23 (backend) have no dependency on the corresponding UI steps; backend can be done first, then the UI step that consumes the API.
3. **Optional steps** — 23–24 (comparison), 25–26 (fuel/ERS), 27–28 (positions), 29–30 (auth/user) can be skipped or deferred; 31–35 (Block I: export, user menu, settings actions) can be implemented after core blocks for a complete UI. The rest gives full parity for sessions, live, strategy (pit/stints), leaderboard, events, and track map layout.
4. **Contract first** — For every new or changed endpoint, update `.github/project/rest_web_socket_api_contracts_f_1_telemetry.md` (and telemetry-api-contracts) before coding so the new UI and backend stay aligned.

---

## Test coverage and documentation checklist

Use this checklist when completing each block so that new functionality is covered by tests and docs are kept up to date.

### Backend (telemetry-processing-api-service, udp-ingest-service)

| Item | Reference | Action |
|------|-----------|--------|
| **85% line coverage** | [.github/project/unit_testing_policy.md](.github/project/unit_testing_policy.md), [.cursor/rules/unit-testing-policy.mdc](.cursor/rules/unit-testing-policy.mdc) | After any new service, controller, mapper, processor: add or update unit tests; run `mvn -pl telemetry-processing-api-service verify`. |
| **TestData** | unit_testing_policy.md §2.2 | New entities/DTOs (e.g. PitStop, Stint, SessionEvent, LeaderboardEntry): add factories/constants to TestData; use them in tests, no inline test data. |
| **AAA + @DisplayName** | unit_testing_policy.md §2.3, §2.4 | Every test: Arrange/Act/Assert; class and method `@DisplayName` (Ukrainian for methods). |
| **AssertJ + Mockito** | unit_testing_policy.md §2.1, §3 | Use `assertThat`, `assertThatThrownBy`; mock dependencies with `@Mock`, `@InjectMocks` or `@Spy`. |
| **Layering** | [.cursor/rules/architecture-layering.mdc](.cursor/rules/architecture-layering.mdc) | New logic: mappers (entity→DTO), builders (params→event), parsers (ByteBuffer→DTO), processors (domain), services (orchestration); keep controllers/consumers thin. |
| **Logging** | [.cursor/rules/logging-policy.mdc](.cursor/rules/logging-policy.mdc) | New entry points and services: `@Slf4j`, DEBUG at method entry, WARN before throw, ERROR in catch. |

### New UI (f1-telemetry-web-platform)

| Item | Reference | Action |
|------|-----------|--------|
| **API and toasts** | [.github/project/react_spa_ui_architecture.md](.github/project/react_spa_ui_architecture.md) §7.4, [.github/draft/implementation-plans/05-ui-notifications-toast.md](.github/draft/implementation-plans/05-ui-notifications-toast.md) | REST/WS errors: `toast.error()`; success after mutations: `toast.success()`; avoid duplicate toasts for same failure. |
| **Notifications (Bell)** | Block A, [05-ui-notifications-toast.md](.github/draft/implementation-plans/05-ui-notifications-toast.md) | Same toast events (API errors, success, WS connect/disconnect) must be shown in the header **Notifications (Bell)** element: Bell opens dropdown/popover with recent notifications; optional unread badge. Implement notification context/store fed when calling `toast.*`; wire Bell in AppLayout. |
| **Types and contract** | [.github/project/rest_web_socket_api_contracts_f_1_telemetry.md](.github/project/rest_web_socket_api_contracts_f_1_telemetry.md) | Keep `api/types.ts` aligned with REST/WS DTOs; when adding endpoints, copy shapes from contract. |
| **Optional tests** | — | sessionId utils, useLiveTelemetry state, critical flows: add unit or e2e tests if the project adopts them. |

### Documentation to update per block

| Document | When to update |
|----------|----------------|
| [.github/project/rest_web_socket_api_contracts_f_1_telemetry.md](.github/project/rest_web_socket_api_contracts_f_1_telemetry.md) | **Every** new or changed REST/WebSocket endpoint: path, params, request/response body, status codes. |
| [BACKEND_FEATURES_FOR_NEW_UI.md](BACKEND_FEATURES_FOR_NEW_UI.md) | When a backend feature B1–B14 is implemented: mark as done and optionally link to step number. |
| [NEW_UI_DOCS.md](NEW_UI_DOCS.md) | When API/WebSocket/live/auth/settings are added: §4 State, §5 API/WS, §8 Known limitations. |
| [.github/project/telemetry_processing_api_service.md](.github/project/telemetry_processing_api_service.md) | When new controllers, services, or repositories are introduced; keep architecture section in sync. |
| [UI_MIGRATION_PLAN.md](UI_MIGRATION_PLAN.md) | When a migration phase or compatibility item is completed: update status in §5 compatibility matrix. |

---

*References: [UI_MIGRATION_PLAN.md](UI_MIGRATION_PLAN.md), [BACKEND_FEATURES_FOR_NEW_UI.md](BACKEND_FEATURES_FOR_NEW_UI.md), [NEW_UI_DOCS.md](NEW_UI_DOCS.md), [.github/project/rest_web_socket_api_contracts_f_1_telemetry.md](.github/project/rest_web_socket_api_contracts_f_1_telemetry.md), [.github/project/unit_testing_policy.md](.github/project/unit_testing_policy.md).*
