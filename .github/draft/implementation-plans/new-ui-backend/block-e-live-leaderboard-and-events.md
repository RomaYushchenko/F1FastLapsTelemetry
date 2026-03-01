# Block E — Live Overview leaderboard and events (backend B2, B3 + UI)

Part of the [Step-by-Step Implementation Plan — New UI & Backend](../../../IMPLEMENTATION_STEP_BY_STEP.md). Steps 17–20.

---

## Current state and gaps analysis

### What exists today

| Area | Current state |
|------|----------------|
| **Packet Event (3) ingest** | Implemented: `EventPacketParser`, `EventPacketHandler`, `EventEventBuilder` → Kafka `telemetry.event`. Plan 08 done. |
| **EventConsumer + EventProcessor** | Implemented: consume `telemetry.event`, idempotency, `EventProcessor.process()` — **only logs** DRSD/SCAR/RTMT (no persistence). |
| **Session events persistence** | **Not implemented:** no `session_events` table, no entity/repository, EventProcessor does not persist. |
| **GET /api/sessions/…/events** | **Not implemented:** not in REST contract or controller. |
| **Leaderboard** | **Not implemented:** no REST endpoint, no service to aggregate multi-car leaderboard. |
| **SessionRuntimeState** | Has `lastCarPositionByCarIndex`, per-car `snapshots` (single-car snapshot used for WebSocket). No “all cars” leaderboard aggregation. |
| **LapRepository** | `findBySessionUidOrderByCarIndexAscLapNumberAsc` — can provide last lap per car for finished laps. |
| **LiveOverview (UI)** | Mock `leaderboard` and `events` arrays; no API client calls. Depends on Block A (API layer) and Block C (live). |

### Gaps not fully covered in the original plan

| # | Gap | Detail | Plan addition |
|---|-----|--------|----------------|
| 1 | **Lap number for session_events** | EventDto has `penaltyLapNum` for PENA; FTLP has only `vehicleIdx` + `lapTime` (no lap number in F1 packet). For timeline we need “Lap 24 – Fastest Lap”. | **Decision:** Derive lap in EventProcessor from session runtime state (current lap for session or for vehicleIdx) when persisting (Option A). |
| 2 | **session_events schema and ordering** | Plan says “session_uid, lap, event_code, car_index, detail” but not: PK, frame_id for ordering, detail format (JSON vs columns), created_at. | Define: id (PK), session_uid, frame_id (for order), lap (nullable), event_code, car_index (nullable), detail (JSONB/text), created_at. Order by (lap, frame_id). |
| 3 | **EventProcessor → persistence** | EventProcessor currently only logs. No call to repository/writer. | Add SessionEventWriter (or persist in processor via repository); EventProcessor must receive session state or lap resolver to set lap when missing in payload. |
| 4 | **Leaderboard data source** | “LapData for all car indices” — positions from `SessionRuntimeState.lastCarPositionByCarIndex`; last lap time + sectors from DB (last completed lap per car) or from runtime. **Gap** field: leader’s last lap time minus car’s last lap time; if no lap yet, show “—” or “LEAD”. | Service: (1) resolve active session; (2) get all car indices with position from state; (3) for each car load last lap from LapRepository (max lap_number); (4) compute gap to leader; (5) sort by position. |
| 5 | **Leaderboard: driver label and tyre** | UI shows driver (e.g. VER) and tyre (S/M/H). Backend has only carIndex. Tyre comes from CarStatus (per-car snapshot). | **Decision:** Backend driver table so UI gets ready driver labels; tyre (compound) from CarStatus snapshot for all cars — include in leaderboard DTO (Option A). |
| 6 | **REST vs WebSocket for leaderboard** | Plan says “Alternatively extend WebSocket with leaderboard message”. | **Decision:** WebSocket push when data changes (no constant polling); REST for initial load and fallback. |
| 7 | **Events API: fromLap / toLap** | Filter by lap range. Need default (all events) and pagination/limit. | Contract: `fromLap`, `toLap` optional; response ordered by lap then frame; optional `limit` to cap result size. |
| 8 | **Event detail for UI** | UI shows event type, driver, detail (e.g. “1:24.532” for FTLP, “5s Time Penalty” for PENA). Backend must return display-ready or structured detail. | Persist detail as JSON (EventDto fields); REST DTO: eventCode, lap, carIndex, detail (object or display string). Mapper or DTO builder for human-readable strings (e.g. penalty type → “5s Time Penalty”). |

### Component reusability

Follow [README § Frontend component reusability](README.md#frontend-component-reusability-all-blocks). Use **DataCard** for leaderboard and event timeline panels; Shadcn **Table** for leaderboard rows; shared **formatLapTime** for lap/sector times. Reuse the same loading/error/skeleton pattern as in Live Overview (Block C).

---

### Resolved decisions

| # | Topic | Decision |
|---|--------|----------|
| 1 | **Leaderboard updates** | WebSocket push when data changes (no constant polling). UI and backend are not loaded with repeated REST polls; leaderboard updates only when LapData/position/snapshot changes. |
| 2 | **Driver names** | Backend has a **driver (participant) table**; leaderboard and events API return ready driver labels. UI works with backend data and does minimal mapping (e.g. fallback only if driver null). |
| 3 | **Tyre compound in leaderboard** | **Option A:** Include compound in leaderboard from CarStatus snapshot for each car (iterate all cars' status in state). |
| 4 | **FTLP lap number** | **Option A:** Derive lap in EventProcessor from session runtime state (current lap for session or for vehicleIdx) when persisting. |

---
## Steps

| Step | Layer | Task | Depends on | Deliverable |
|------|--------|------|------------|-------------|
| **17** | Backend | **B2 — Live leaderboard.** Add driver (participant) table; GET /api/sessions/active/leaderboard returning position, driverLabel, carIndex, compound, gap, lastLapTimeMs, sector1/2/3. Source: state + LapRepository + CarStatus snapshots. WebSocket push when leaderboard data changes (no polling). REST for initial load. Contract and tests. | — | Leaderboard via REST + WS push; driver table. |
| **18** | New UI | Live Overview leaderboard: getLeaderboard() for initial load; subscribe to WebSocket leaderboard updates. Replace mock table with API/WS data. Use driverLabel from backend (fallback only if null). | 9, 17 | Live Overview shows real leaderboard, updates on change. |
| **19** | Backend | **B3 — Session events.** Ingest Packet Event (3) in udp-ingest-service; produce to Kafka. In processing: persist session_events (session_uid, lap, event_code, car_index, detail). Add GET /api/sessions/{sessionUid}/events?fromLap=&toLap=. Update contract and tests. Optionally push events via WebSocket. | — | Session events endpoint (and optionally WS) available. |
| **20** | New UI | Live Overview event timeline: add getSessionEvents(sessionUid). Replace mock event timeline with API data. Optionally subscribe to event push via WebSocket. | 9, 19 | Live Overview shows real session events. |

---

## Detailed changes

| Step | Where | Concrete changes |
|------|--------|------------------|
| 17 | Backend | **Driver table:** entity + repository for driver/participant (e.g. carIndex or team/driver id → display label). **Contract:** GET /api/sessions/active/leaderboard — response: list (position, driverLabel, carIndex, compound, gap, lastLapTimeMs, sector1/2/3); **Service:** aggregate from state + LapRepository + CarStatus snapshots (all cars); resolve driverLabel from driver table; **Controller:** new endpoint; **WebSocket:** broadcast leaderboard message when LapData/position/snapshot changes (no polling). |
| 18 | New UI | getLeaderboard() for initial load; WebSocket subscription for leaderboard updates; LiveOverview: replace mock table with API/WS data; use driverLabel from backend (fallback if null). |
| 19 | Backend | **udp-ingest:** Packet Event (3) already implemented (plan 08). **processing:** add session_events table + entity + repository; persist in EventProcessor (lap from payload or session state, detail as JSON); **Contract:** GET /api/sessions/{sessionUid}/events?fromLap=&toLap=; **Controller + Service + Mapper;** optional WS push. Layering: persistence in processor/writer, REST in controller + service. |
| 20 | New UI | getSessionEvents(sessionUid, fromLap?, toLap?) in client; LiveOverview: event timeline from API; optional WS subscription. |

---

## Testing

| Step | Scope | What to add/update |
|------|--------|--------------------|
| 17 | Backend | Driver table/repository tests; LeaderboardQueryService (multi-car, gap, compound, driverLabel); LeaderboardController MockMvc; WebSocket broadcaster for leaderboard; TestData; 85% coverage. |
| 19 | Backend | **udp-ingest:** Packet Event parser tests; **processing:** SessionEventsProcessor + repository tests; SessionEventsController MockMvc; add session_events to TestData if needed. |
| 18, 20 | New UI | Manual or e2e: leaderboard and event timeline show real data. |

---

## Documentation updates

| Doc | Updates |
|-----|--------|
| [rest_web_socket_api_contracts_f_1_telemetry.md](../../../project/rest_web_socket_api_contracts_f_1_telemetry.md) | Leaderboard and session events endpoints, DTOs; driver/participant API; WebSocket LEADERBOARD and optional session-events push. |
| [BACKEND_FEATURES_FOR_NEW_UI.md](../../../BACKEND_FEATURES_FOR_NEW_UI.md) | Mark B2, B3 implemented. |

---

## Detailed implementation plan (sub-tasks)

Use this section for a precise, ordered list of work items. Dependencies between steps 17–20 are preserved.

### Step 17 — Backend: Live leaderboard (B2)

| Order | Sub-task | Where | Notes |
|-------|----------|--------|-------|
| 17.1 | Driver (participant) table and API | Backend | Entity + repository for driver/participant (e.g. session or global: carIndex/teamId → short display label "VER", "HAM"). Populate from game data or config; GET /api/sessions/active/drivers or resolve in leaderboard service. |
| 17.2 | Define REST contract | `rest_web_socket_api_contracts_f_1_telemetry.md` | `GET /api/sessions/active/leaderboard`. Response: array of `LeaderboardEntryDto`: position, carIndex, driverLabel, compound (S/M/H), gap, lastLapTimeMs, sector1/2/3. WebSocket: message type LEADERBOARD with same payload, sent when data changes. |
| 17.3 | Add LeaderboardEntryDto (and response wrapper) | `telemetry-api-contracts` or service module | DTO: position, carIndex, driverLabel, compound, gap, lastLapTimeMs, sector1/2/3. |
| 17.4 | LeaderboardQueryService | `telemetry-processing-api-service` | Resolve active session; state (positions, CarStatus snapshots for all cars → compound); last lap per car from LapRepository; gap to leader; resolve driverLabel from driver table; sort by position; build list. |
| 17.5 | LeaderboardController | Same service | GET /api/sessions/active/leaderboard; return 200 + body or 204 if no active session. |
| 17.6 | WebSocket leaderboard push | Same service | On LapData/position/snapshot change for active session: build leaderboard, broadcast LEADERBOARD message to live subscribers (reuse LiveDataBroadcaster or new channel). |
| 17.7 | Unit tests | Same service | Driver table/repository; LeaderboardQueryService (multi-car, gap, compound, driverLabel); Controller; TestData. |
| 17.8 | Logging | Service + controller | DEBUG at entry; WARN if no active session. |

### Step 18 — New UI: Live Overview leaderboard

| Order | Sub-task | Where | Notes |
|-------|----------|--------|-------|
| 18.1 | getLeaderboard() in API client | `f1-telemetry-web-platform` | GET /api/sessions/active/leaderboard; return typed list. Handle 204 → empty list; toast on error. |
| 18.2 | Types for leaderboard | Same | LeaderboardEntry: position, carIndex, driverLabel, compound, gap, lastLapTimeMs, sector1/2/3. |
| 18.3 | WebSocket: subscribe to leaderboard updates | Same | When on Live and subscribed to live topic: handle LEADERBOARD message; update leaderboard state (replace list). No polling. |
| 18.4 | LiveOverview: initial load + WS | LiveOverview.tsx | Initial: getLeaderboard() when on Live (and active session); then rely on WebSocket for updates. |
| 18.5 | Replace mock leaderboard table | LiveOverview.tsx | Render from state (API/WS data); format lastLap (mm:ss.SSS); sectors; gap; compound; driverLabel (fallback to `Car ${carIndex}` if null). |
| 18.6 | Loading and error states | Same | Loading skeleton; error + retry or toast. |

### Step 19 — Backend: Session events (B3)

| Order | Sub-task | Where | Notes |
|-------|----------|--------|-------|
| 19.1 | DB migration: session_events table | `telemetry-processing-api-service` | Columns: id (PK), session_uid, frame_id (for ordering), lap (nullable), event_code, car_index (nullable), detail (JSONB or TEXT), created_at. Index: (session_uid, lap, frame_id). |
| 19.2 | SessionEvent entity | Same | JPA entity mapping to session_events. |
| 19.3 | SessionEventRepository | Same | findBySessionUidOrderByLapAscFrameIdAsc; optional: findBySessionUidAndLapBetween. |
| 19.4 | Resolve lap for event | EventProcessor or new writer | For each event: lap = from EventDto (penaltyLapNum for PENA, etc.); for FTLP derive from session current lap in state (LapAggregator or SessionRuntimeState — current lap for session or for vehicleIdx). Inject lap resolver into EventProcessor. |
| 19.5 | SessionEventWriter or persist in EventProcessor | Same | Build SessionEvent from EventDto + sessionUid + frameId + resolved lap; set detail as JSON of relevant EventDto fields; save. Call from EventProcessor after existing switch. |
| 19.6 | REST contract: GET /api/sessions/{sessionUid}/events | Contract doc | Query: fromLap, toLap optional; limit optional. Response: array of SessionEventDto (lap, eventCode, carIndex, detail object, createdAt). |
| 19.7 | SessionEventDto (REST) and mapper | Same | Entity → DTO; detail as object or display string per event type. |
| 19.8 | SessionEventsController + Service | Same | GET /api/sessions/{sessionUid}/events; service loads from repository with filters; map to DTOs. |
| 19.9 | Unit tests | Same | SessionEventWriter/Processor persistence; repository; controller MockMvc; TestData session_events. |
| 19.10 | (Optional) WebSocket push on new event | Later | EventProcessor after persist → notify broadcaster; WS message type for new session event. |

### Step 20 — New UI: Live Overview event timeline

| Order | Sub-task | Where | Notes |
|-------|----------|--------|-------|
| 20.1 | getSessionEvents(sessionUid, fromLap?, toLap?) in API client | `f1-telemetry-web-platform` | GET /api/sessions/{sessionUid}/events with query params; return typed list. |
| 20.2 | Types for session events | Same | SessionEvent: lap, eventCode, carIndex, detail, createdAt. |
| 20.3 | LiveOverview: fetch events for active session | LiveOverview.tsx | When active session exists, call getSessionEvents(activeSessionId or sessionUid). |
| 20.4 | Replace mock event timeline | LiveOverview.tsx | Render list from API; map eventCode to icon/label (FTLP → "Fastest Lap", PENA → "Penalty", etc.); show lap, driver (from carIndex map), detail. |
| 20.5 | Format detail per event type | Same | FTLP: lap time from detail; PENA: penalty text; SCAR: safety car status; etc. |
| 20.6 | Loading and error states | Same | Skeleton; error + retry. |
| 20.7 | (Optional) WebSocket subscription for new events | Later | If WS push implemented, subscribe and append new events to list. |

---

## Implementation checklist

Use this checklist to track progress: replace `[ ]` with `[x]` when an item is done. Optionally move completed items into a “Done” section at the bottom so remaining work stays visible at a glance.

### Step 17 — Backend: Live leaderboard

- [x] 17.1 — Driver (participant) table + repository; resolve driverLabel for leaderboard/events
- [x] 17.2 — REST contract GET /api/sessions/active/leaderboard + WebSocket LEADERBOARD message type
- [x] 17.3 — LeaderboardEntryDto (position, carIndex, driverLabel, compound, gap, sector times)
- [x] 17.4 — LeaderboardQueryService (state + LapRepository + CarStatus snapshots; gap; driver lookup)
- [x] 17.5 — LeaderboardController
- [x] 17.6 — WebSocket leaderboard push on LapData/position/snapshot change
- [x] 17.7 — Unit tests (driver table, service, controller); TestData
- [x] 17.8 — Logging (DEBUG/WARN)

### Step 18 — New UI: Leaderboard

- [x] 18.1 — getLeaderboard() in API client
- [x] 18.2 — Leaderboard types (driverLabel, compound, etc.)
- [x] 18.3 — WebSocket: subscribe to leaderboard updates; update state on LEADERBOARD message
- [x] 18.4 — LiveOverview: initial load getLeaderboard() + WS for updates
- [x] 18.5 — Replace mock table; show driverLabel (fallback if null), compound, times, sectors, gap
- [x] 18.6 — Loading and error states

### Step 19 — Backend: Session events

- [x] 19.1 — DB migration: session_events table
- [x] 19.2 — SessionEvent entity
- [x] 19.3 — SessionEventRepository (find by session, optional lap range)
- [x] 19.4 — Lap resolution for events (from payload or session state)
- [x] 19.5 — Persist in EventProcessor (SessionEventWriter or repository call)
- [x] 19.6 — REST contract GET /api/sessions/{sessionUid}/events
- [x] 19.7 — SessionEventDto (REST) and mapper
- [x] 19.8 — SessionEventsController + Service
- [x] 19.9 — Unit tests (persistence, repository, controller); TestData
- [ ] 19.10 — (Optional) WebSocket push for new events

### Step 20 — New UI: Event timeline

- [x] 20.1 — getSessionEvents() in API client
- [x] 20.2 — Session event types in frontend
- [x] 20.3 — LiveOverview: fetch events for active session
- [x] 20.4 — Replace mock event timeline with API data
- [x] 20.5 — Format detail per event type (FTLP, PENA, SCAR, …)
- [x] 20.6 — Loading and error states
- [ ] 20.7 — (Optional) WebSocket subscription for new events

### Step 21 — Documentation and closure

- [x] Update [rest_web_socket_api_contracts_f_1_telemetry.md](../../../project/rest_web_socket_api_contracts_f_1_telemetry.md) with leaderboard and events endpoints and DTOs
- [x] Update [BACKEND_FEATURES_FOR_NEW_UI.md](../../../BACKEND_FEATURES_FOR_NEW_UI.md): mark B2 and B3 as implemented
- [ ] Update plan 08 (08-packet-event-ingest-and-processing.md) if session_events persistence is added: note “session_events table and REST events endpoint implemented in Block E”

### Step 22 — Git Commit
- [ ] Add git commit with understanding message