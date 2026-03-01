# Backend Features for New UI — List and Implementation Plan

This document lists **backend features that are not implemented today** but are required or strongly recommended to fully support the new UI (f1-telemetry-web-platform). It is derived from [UI_MIGRATION_PLAN.md](UI_MIGRATION_PLAN.md) and [NEW_UI_DOCS.md](NEW_UI_DOCS.md).

**Reference:** Current REST and WebSocket contracts are in [.github/project/rest_web_socket_api_contracts_f_1_telemetry.md](.github/project/rest_web_socket_api_contracts_f_1_telemetry.md). Existing endpoints (sessions, laps, summary, pace, trace, ers, speed-trace, corners, tyre-wear, sessions/active, PATCH session) are **not** listed here.

---

## 1. List of Backend Features Not Yet Implemented

### 1.1 Session list — filtering and sorting

| ID | Feature | Description | New UI usage |
|----|---------|-------------|--------------|
| B1 | **Query params for GET /api/sessions** | Support filtering and sorting so the Session History page can show filtered results without client-side only logic. | SessionHistory: search (track/session type text), session type filter, sort (date, result, best lap), date range. |

**Status: Implemented in Block B (Step 7).** See [block-b-session-list-filters.md](.github/draft/implementation-plans/new-ui-backend/block-b-session-list-filters.md). Backend now supports `sessionType`, `trackId`, `search`, `sort` (startedAt_asc/desc, finishingPosition_asc, bestLap_asc/desc), `state` (ACTIVE | FINISHED), `dateFrom`, `dateTo`; response includes `X-Total-Count` header.

---

### 1.2 Live Overview — leaderboard (all cars)

| ID | Feature | Description | New UI usage |
|----|---------|-------------|--------------|
| B2 | **Live leaderboard** | For the active session, expose current position, driver/car, gap, last lap time, sector times (S1/S2/S3) for **all** cars (or at least top N). | LiveOverview: leaderboard table (pos, driver, tyre, gap, last lap, S1, S2, S3). |

**Current backend:** WebSocket sends a **single-car** snapshot (player car). There is no REST or WebSocket payload with all cars’ current state.

**Status: Implemented in Block E (Steps 17–18).** See [block-e-live-leaderboard-and-events.md](.github/draft/implementation-plans/new-ui-backend/block-e-live-leaderboard-and-events.md). Backend: `GET /api/sessions/active/leaderboard` (204 if no active session); WebSocket message type `LEADERBOARD` pushed when LapData/position/snapshot changes. Driver labels from `session_drivers` table (fallback "Car N"). New UI: Live Overview leaderboard from API + WS.

---

### 1.3 Live Overview — event timeline

| ID | Feature | Description | New UI usage |
|----|---------|-------------|--------------|
| B3 | **Session events** | Expose session-wide events: fastest lap (FTLP), pit stop, penalty (PENA), safety car (SCAR), etc., with lap and optional driver/detail. | LiveOverview: event timeline (e.g. “Lap 24 – Fastest Lap – VER – 1:24.532”). |

**Current backend:** F1 25 Packet Event (packet 3) carries these events; they are not persisted or exposed via REST/WebSocket in the current design.

**Status: Implemented in Block E (Steps 19–20).** See [block-e-live-leaderboard-and-events.md](.github/draft/implementation-plans/new-ui-backend/block-e-live-leaderboard-and-events.md). Backend: `session_events` table; EventProcessor persists events with lap resolution; `GET /api/sessions/{sessionUid}/events?fromLap=&toLap=&limit=`. New UI: Live Overview event timeline from API.

---

### 1.4 Strategy View — pit stops and stints

| ID | Feature | Description | New UI usage |
|----|---------|-------------|--------------|
| B4 | **Pit stops** | List of pit stops for a session/car: lap number, in-lap time, pit duration, out-lap time, tyre change (compound in/out). | StrategyView: pit stop timeline and table (lap, inLap, pitTime, outLap, tyre change). |
| B5 | **Stints (tyre stints)** | Per-car stints: stint index, compound, start lap, number of laps, optional avg lap time or degradation indicator. | StrategyView: stint comparison table and tyre strategy bar. |

**Obsolete (B4/B5 implemented):** Laps and tyre-wear exist; LapData has pit status. There is no aggregated “pit stops” or “stints” endpoint.

**Status: Implemented in Block D (Steps 14–16).** See [block-d-strategy-view.md](.github/draft/implementation-plans/new-ui-backend/block-d-strategy-view.md). Backend: `GET /api/sessions/{sessionUid}/pit-stops?carIndex=`, `GET /api/sessions/{sessionUid}/stints?carIndex=`; detection from laps + tyre_wear_per_lap (compound change = pit; consecutive same compound = stint). New UI: Strategy View entry from Session Details (`/app/sessions/:id/strategy`), overview cards from API, loading/error/empty states.

---

### 1.5 Strategy View — fuel and ERS by lap

| ID | Feature | Description | New UI usage |
|----|---------|-------------|--------------|
| B6 | **Fuel remaining by lap** | For each lap (or lap end), fuel remaining (e.g. % or mass) for the selected car. | StrategyView: fuel consumption chart (lap vs fuel %). |
| B7 | **ERS deployment/harvesting by lap** | Per-lap ERS “deployed” and “harvested” amounts or percentages for charts. | StrategyView: ERS deployment & harvesting bar chart by lap. |

**Current backend:** CarStatus has fuel and ERS data; we have `GET .../laps/{lapNum}/ers` (energy % along lap distance). There is no fuel-by-lap or “deployed vs harvested” by lap aggregation.

**Status: Implemented in Block H (Steps 25–26).** Backend: `GET /api/sessions/{sessionUid}/fuel-by-lap?carIndex=`, `GET .../ers-by-lap?carIndex=` (one value per lap at lap end). New UI: Strategy View uses API data when available; mock fallback when empty.

---

### 1.6 Live Track Map

| ID | Feature | Description | New UI usage |
|----|---------|-------------|--------------|
| B8 | **Track layout for map** | 2D track outline or centreline (e.g. polyline or simplified geometry) for a given track so the UI can draw the track. | LiveTrackMap: replace static SVG with backend-driven track shape. |
| B9 | **Live positions of all cars** | Real-time positions (e.g. X,Y or distance/sector) for all cars in the active session. | LiveTrackMap: show all cars on the track. |

**Current backend:** Track corner map exists (`GET /api/tracks/{trackId}/corner-maps/latest`) with corners (start/end/apex distance). No Motion (packet 0) in MVP; WebSocket is single-car only.

**Status: B8 implemented in Block F (Steps 21–22).** See [block-f-live-track-map.md](.github/draft/implementation-plans/new-ui-backend/block-f-live-track-map.md). Backend: `GET /api/tracks/{trackId}/layout` returns 2D points and optional bounds from `telemetry.track_layout` table. New UI: Live Track Map fetches layout when active session exists, draws track from API.

**Status: B9 implemented in Block H (Steps 27–28).** Motion ingest extended to all 22 cars; `GET /api/sessions/active/positions` and WebSocket message type **POSITIONS** on `/topic/live/{sessionId}` provide live world positions. New UI: Live Track Map polls positions (or uses WS) and displays real car positions when available; mock fallback when no position data.

#### B8 — Data source for track drawing (coordinates)

Coordinates for drawing the track will be provided separately (e.g. a file or set of files per track). Storage and API access should be designed so the backend can serve this data easily:

- **Option A — Separate files:** One file per track (e.g. JSON, GeoJSON, or CSV) with 2D coordinates (centreline or outline). Files can live in a dedicated folder (e.g. `track-layouts/` or in classpath/resources). Backend reads the file for the requested `trackId` and returns it via `GET /api/tracks/{trackId}/layout` (or similar). Easy to add or update tracks without DB migrations.
- **Option B — New table:** Table (e.g. `track_layout` or `track_geometry`) with columns such as `track_id`, `version`, `geometry` (JSON/PostGIS), or `points` (array of {x,y}). Data is loaded from the same source files once (import/migration), then served via API. Easier to query by `trackId`, versioning, and consistency with other track metadata.

**Recommendation:** Choose one approach and expose a single REST endpoint (e.g. `GET /api/tracks/{trackId}/layout`) so the UI can fetch coordinates by track and draw the map. The exact file format or table schema can be defined when the source file(s) with coordinates are available.

---

### 1.7 Driver Comparison (optional backend)

| ID | Feature | Description | New UI usage |
|----|---------|-------------|--------------|
| B10 | **Comparison endpoint (optional)** | Single endpoint that returns comparison data for two cars in one session (e.g. lap times, sector deltas, speed/trace overlay) to avoid multiple round-trips. | DriverComparison: one request instead of two carIndex calls for session/laps/summary/pace/trace/speed-trace. |

**Status: Implemented in Block G (Steps 23–24).** See [block-g-driver-comparison.md](.github/draft/implementation-plans/new-ui-backend/block-g-driver-comparison.md). Backend: `GET /api/sessions/{sessionUid}/comparison?carIndexA=&carIndexB=&referenceLapNumA=&referenceLapNumB=` (reference params optional; when omitted, server uses best lap from summary for each car). Response: laps, summary, pace, referenceLapNumA/B, traceA/B, speedTraceA/B. **SessionDto.participants:** GET /api/sessions/{id} includes optional `participants` (array of `{ carIndex, displayLabel? }`) — car indices with at least one lap or summary; displayLabel from session_finishing_positions (e.g. "P1") or "Car N". Forward-compatible with future drivers table. New UI: Driver Comparison uses getSessions, getSession(sessionUid) for participants, getComparison for data; best-vs-best by default; optional lap selectors per driver.

---

### 1.8 User, settings, and auth

| ID | Feature | Description | New UI usage |
|----|---------|-------------|--------------|
| B11 | **Authentication** | Login, register, logout, token refresh (e.g. JWT). | Login, Register pages; protected /app routes. |
| B12 | **User profile** | Store and update display name, email, driver number (and optionally avatar). | Settings: profile section. |
| B13 | **User preferences** | Units (metric/imperial), theme, telemetry preferences (smoothing, update rate), alert toggles (penalty, safety car, packet loss). | Settings: appearance and telemetry/alert sections. |
| B14 | **Dangerous actions** | “Delete all my session data” and “Delete account” with confirmation. | Settings: danger zone. |

**Current backend:** No user entity, no auth, no profile or preferences API.

**Status: Deferred (Block H plan).** B11–B14 to be implemented **last** in a **separate auth microservice** (register, login, refresh, logout, profile, preferences, delete sessions/account). Main telemetry API remains unchanged; optional session owner marker when auth exists. See block-h-optional-fuel-ers-positions-auth.md Steps 29–30.

---

## 2. Implementation Plan (Backend)

Implementation is grouped into **phases** by dependency and value. Each phase can be broken into tasks in the service (e.g. telemetry-processing-api-service) and, where needed, ingest (udp-ingest-service) and DB migrations.

---

### Phase 1 — Session list and Strategy (high value, no new packets)

**Goal:** Support Session History filters/sort and Strategy View pit/stints (and optionally fuel/ERS) using data we can derive or already have.

| Task | Feature | Description | Size |
|------|---------|-------------|------|
| 1.1 | B1 – Session list filters/sort | Add optional query params to `GET /api/sessions`: `sessionType`, `trackId`, `search` (text on sessionDisplayName/trackDisplayName/sessionType), `sort` (e.g. startedAt desc/asc, finishingPosition, bestLap), `dateFrom`, `dateTo` (ISO date or timestamp). Implement in `SessionQueryService` and repository (JPA criteria or QueryDSL). Update contract and controller. | M |
| 1.2 | B4 – Pit stops | Define `PitStopDto` (lapNumber, inLapTimeMs, pitDurationMs, outLapTimeMs, compoundIn, compoundOut). Implement detection from laps + tyre compound changes (or lap boundaries + car status); persist in `pit_stops` or compute on demand. Add `GET /api/sessions/{sessionUid}/pit-stops?carIndex=`. | M |
| 1.3 | B5 – Stints | Define stint DTO (stintIndex, compound, startLap, lapCount, optional avgLapTimeMs). Derive from laps + tyre compound (from tyre_wear or lap metadata). Add `GET /api/sessions/{sessionUid}/stints?carIndex=` or extend pace response with stintIndex/compound. | M |
| 1.4 | B6 – Fuel by lap (if data available) | If CarStatus/fuel is stored per frame or per lap: add aggregation and `GET /api/sessions/{sessionUid}/fuel-by-lap?carIndex=`. If not stored, document as “future” and skip. | S–M |
| 1.5 | B7 – ERS by lap (deployed/harvested) | If we can compute “deployed” and “harvested” per lap from existing ERS/car_status data, add endpoint or extend lap response. Otherwise document as future. | S–M |

**Deliverables:** Updated REST contract; controller + service + repository changes; tests. No new Kafka topics or UDP packets required for B1, B4, B5.

---

### Phase 2 — Live Overview (leaderboard + events)

**Goal:** Support Live Overview leaderboard and event timeline. May require ingesting and storing Packet Event (3) and exposing LapData for all cars.

| Task | Feature | Description | Size |
|------|---------|-------------|------|
| 2.1 | B2 – Live leaderboard | Option A: New REST `GET /api/sessions/active/leaderboard` (or `GET /api/sessions/{id}/leaderboard` for active only) returning list of cars with position, driver/carIndex, gap, lastLapTimeMs, sector1/2/3. Source: LapData for all car indices (and session summary for best lap). Option B: Extend WebSocket to broadcast a “leaderboard” message (all cars) in addition to single-car SNAPSHOT. Prefer A for simplicity if polling is acceptable; B if real-time is required. | L |
| 2.2 | B3 – Session events | Ingest Packet Event (3) in udp-ingest-service; produce to Kafka (e.g. `telemetry.event`). In processing: persist in `session_events` (session_uid, lap, event_code, car_index, detail JSON). Add `GET /api/sessions/{sessionUid}/events?fromLap=&toLap=`. Optionally push events over WebSocket for live timeline. | L |

**Deliverables:** Event ingestion (parser + Kafka); event table and repository; leaderboard aggregation; REST (and optionally WS) contract update; tests.

---

### Phase 3 — Live Track Map (optional, depends on Motion)

**Goal:** Enable Live Track Map with real track shape and, if feasible, live positions.

| Task | Feature | Description | Size |
|------|---------|-------------|------|
| 3.1 | B8 – Track layout | **Data source:** Track coordinates for drawing will come from files you provide (e.g. one file per track) or from a new DB table populated from those files (see § 1.6 B8 — Data source). **Backend:** Add endpoint `GET /api/tracks/{trackId}/layout` that returns 2D coordinates (polyline/centreline or outline) — either by reading the track’s file or by querying the new table. This makes it easy to find and serve layout data via API. Define response format (e.g. `{ trackId, points: [{x,y}] }` or GeoJSON) in the REST contract once the source file format is known. | M |
| 3.2 | B9 – Multi-car live positions | Requires Motion (packet 0) ingestion and storage or broadcast. If Motion is added: ingest in udp-ingest-service; broadcast positions (e.g. X,Y or distance) for all cars via WebSocket or REST polling. If Motion is out of scope, document as future and keep UI with static positions. | L (or defer) |

**Deliverables:** Track layout endpoint (and data); if B9 is in scope: Motion pipeline and WebSocket/REST for positions.

---

### Phase 4 — Driver Comparison (optional) and Auth/User (large)

**Goal:** Optional comparison API; then user and auth if product requires it.

| Task | Feature | Description | Size |
|------|---------|-------------|------|
| 4.1 | B10 – Comparison API | Add `GET /api/sessions/{sessionUid}/comparison?carIndexA=&carIndexB=` returning combined structure (e.g. laps for both, summary for both, pace/trace/speed for both) to reduce client round-trips. | M |
| 4.2 | B11 – Auth | Design and implement auth (e.g. JWT): register, login, refresh, logout; secure `/app` and API if needed. | L |
| 4.3 | B12 – User profile | User entity; GET/PATCH profile (display name, email, driver number). | M |
| 4.4 | B13 – User preferences | Store preferences (units, theme, telemetry options, alerts); GET/PATCH. | M |
| 4.5 | B14 – Delete data / account | Endpoints: “delete all my sessions” (and related data); “delete account” (with confirmation). | M |

**Deliverables:** Auth flow; user and preferences tables; REST for profile and preferences; deletion endpoints; contract and security docs.

---

## 3. Summary Table

| ID | Feature | Phase | Depends on |
|----|---------|-------|------------|
| B1 | Session list filter/sort | 1 | — |
| B2 | Live leaderboard | 2 | LapData for all cars (existing or extended) |
| B3 | Session events | 2 | Packet Event (3) ingest + storage |
| B4 | Pit stops | 1 | Laps + tyre compound (or detection logic) |
| B5 | Stints | 1 | Laps + compound |
| B6 | Fuel by lap | 1 | CarStatus/fuel storage (if present) |
| B7 | ERS deployed/harvested by lap | 1 | ERS/CarStatus (if derivable) |
| B8 | Track layout for map | 3 | Coordinate files (per track) or new table (e.g. track_layout) + API |
| B9 | Live positions (all cars) | 3 | Motion packet (0) — optional |
| B10 | Driver comparison endpoint | 4 | — |
| B11 | Auth | 4 | — |
| B12 | User profile | 4 | B11 |
| B13 | User preferences | 4 | B11 |
| B14 | Delete data / account | 4 | B11 |

---

## 4. Recommendations

1. **Implement Phase 1 first** — B1, B4, B5 give immediate value for Session History and Strategy View with no new UDP or Kafka events; B6/B7 only if data is already available.
2. **Phase 2 (B2, B3)** — Enables full Live Overview; B3 requires Packet Event ingestion and event storage.
3. **Phase 3 (B8, B9)** — B8: serve track coordinates from the files you provide or from a new table (see § 1.6); expose `GET /api/tracks/{trackId}/layout`. B9 is optional and depends on adding Motion (packet 0).
4. **Phase 4** — B10 is optional for performance. B11–B14 are a separate product decision (multi-user, accounts); the new UI can keep Login/Register and Settings as UI-only until then.
5. **Contract first** — For each feature, update `.github/project/rest_web_socket_api_contracts_f_1_telemetry.md` (and telemetry-api-contracts DTOs) before implementation so the new UI can align with the same contract.

---

*This document should be updated when backend features are implemented or when new UI requirements appear. Cross-references: [UI_MIGRATION_PLAN.md](UI_MIGRATION_PLAN.md), [NEW_UI_DOCS.md](NEW_UI_DOCS.md), [.github/project/rest_web_socket_api_contracts_f_1_telemetry.md](.github/project/rest_web_socket_api_contracts_f_1_telemetry.md).*
