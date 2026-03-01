# Block H — Optional backend and UI (fuel, ERS, positions, auth)

Part of the [Step-by-Step Implementation Plan — New UI & Backend](../../../IMPLEMENTATION_STEP_BY_STEP.md). Steps 25–30.

---

## Gap analysis (current plan vs codebase)

Comparison of the plan with the current implementation and data availability:

| Area | In plan | Current state | Gap |
|------|--------|---------------|-----|
| **B6 Fuel by lap** | "If fuel/ERS data is stored: add GET fuel-by-lap" | **CarStatusRaw** has `fuelInTank` (float); CarStatusProcessor and CarStatusRawWriter persist it. Same merge-by-timestamp approach as `getLapErs` is feasible. | Plan does not state that **fuel data is already stored**. Add sub-steps: define response shape (e.g. `{ lapNumber, fuelPercent }` or `fuelKg`), implement aggregation (e.g. fuel at lap end per lap) in LapQueryService or new service, add endpoint and contract. |
| **B7 ERS by lap** | "ERS deployed/harvested by lap" | **Existing:** `GET .../laps/{lapNum}/ers` returns ERS energy % along lap distance (CarStatusRaw.ersStoreEnergy merged with telemetry). **Missing:** "deployed" and "harvested" **per lap** (amounts or %). F1 25 CarStatus has ersStoreEnergy and ersDeployMode, not explicit "deployed/harvested" per lap. | **Clarify B7 scope:** (A) One value per lap (e.g. ERS store % at lap end) — derivable from existing data; or (B) true "deployed" vs "harvested" amounts per lap — may require F1 25 fields or delta logic. Plan should list both options and recommend (A) for MVP. |
| **Step 25 response shape** | "fuel-by-lap and/or ERS-by-lap" | No contract or DTO defined. | Add to plan: REST contract section for fuel-by-lap (e.g. `FuelByLapDto`: lapNumber, fuelPercent or fuelKg); for ERS-by-lap (e.g. `ErsByLapDto`: lapNumber, ersStorePercentEnd, optional deployedPercent/harvestedPercent if scope (B)). |
| **B9 Multi-car positions** | "If Motion (packet 0) is added: ingest and broadcast" | **Motion is already implemented:** MotionPacketHandler (udp-ingest) publishes to `telemetry.motion`; MotionConsumer writes to **motion_raw** (session_uid, frame_identifier, car_index, world_pos_x/z). **But:** ingest uses `seekToPlayerCar` — only **player car** motion is published. motion_raw has car_index, so DB supports multi-car. | **Gap:** For B9 we must (1) **extend ingest** to publish motion for **all 22 cars** (or one event with array of positions); (2) **processing:** either store all in motion_raw (if ingest sends all) or add new consumer path; (3) **broadcast** positions via WebSocket (new message type) or REST polling. Plan should state: "Motion packet 0 is already ingested for **player car only**; B9 requires extending to all cars + broadcast." |
| **B9 REST/WS contract** | "broadcast multi-car positions via WebSocket or REST" | No message type or response shape defined. | Add: payload shape (e.g. `CarPositionDto`: carIndex, worldPosX, worldPosZ, optional lapDistance); WebSocket message type (e.g. `POSITIONS`) or REST `GET /api/sessions/active/positions`. |
| **B11–B14 Auth and user** | "Auth, user profile, preferences, delete" | No user entity, no auth, no profile/preferences in backend. | **Gaps:** (1) No list of **exact endpoints** (paths, methods, request/response). (2) **Sessions and users:** are sessions tied to a user (e.g. session.user_id)? If yes, GET /api/sessions filters by current user; B14 "delete all my sessions" implies user-scoped sessions. (3) **Token:** JWT in header vs httpOnly cookie vs memory — not decided. (4) **Order:** B11 → B12 → B13 → B14; sub-steps for B11 (register, login, refresh, logout, protect routes). |
| **Block I "Delete All Sessions"** | — | Block I step 35: "Delete All Sessions (confirm + API)". | If auth is implemented, this should call B14 "delete all my sessions" endpoint. If auth is deferred, "Delete All Sessions" may be UI-only (e.g. local list clear) or call a future endpoint. Plan should cross-reference. |

---

## Component reusability

Follow [README § Frontend component reusability](README.md#frontend-component-reusability-all-blocks). **Strategy View (Step 26):** reuse same DataCard and chart patterns as Block D. **Live Track Map (Step 28):** reuse "No active session" and position display pattern. **Login/Register/Settings (Step 30):** use Shadcn **Form**, **Input**, **Button**, **Switch**; do not duplicate form layout or danger-zone UI; reuse AlertDialog/Tooltip pattern from Block I where applicable.

---

## Decisions recorded

Decisions confirmed for implementation:

| # | Topic | Decision |
|---|--------|----------|
| **H1** | **B7 ERS-by-lap scope** | **One value per lap** — ERS store % at lap end (same pattern as fuel-by-lap). No deployed/harvested per lap in MVP. |
| **H2** | **B9 Multi-car positions** | **Implement now** — extend Motion ingest to all 22 cars; add WebSocket (or REST) for positions; Live Track Map shows real positions. |
| **H3** | **Sessions and user** | **Global sessions** — GET /api/sessions returns all sessions (no filter by user). **Optional: mark the current user** who is driving a session so the UI can show "Your session" or highlight it (e.g. optional `ownerUserId` or `driverUserId` on session, set when we know the linked user; main service stays agnostic of auth, but can store an optional marker for UX). |
| **H4** | **Auth: token storage (UI)** | **Store JWT in cookies** (httpOnly when possible); auth service sets cookie on login; browser sends automatically. |
| **H5** | **Auth: timing** | **Defer to the end** — implement steps 29–30 **last** in the migration so the app can be tested without auth. After all other blocks, add auth and wire UI. |
| **H6** | **Auth: architecture** | **Auth is a separate microservice.** Register, login, refresh, logout, user profile (B12), preferences (B13), delete account/sessions (B14) live in a dedicated auth service. The main telemetry-processing-api-service does **not** contain auth logic; it remains focused on sessions, laps, live data. When auth is on: main API may accept JWT (e.g. validated by gateway or auth service) only for optional features (e.g. marking "current user" on session); session list stays global. |

---

## Steps

| Step | Layer | Task | Depends on | Deliverable |
|------|--------|------|------------|-------------|
| **25** | Backend | **B6, B7 (optional).** If fuel/ERS data is stored: add GET /api/sessions/{uid}/fuel-by-lap and/or ERS deployed/harvested by lap. Otherwise document as future. | — | Fuel/ERS-by-lap endpoints if data available. |
| **26** | New UI | Strategy View: if B6/B7 implemented, wire fuel and ERS-by-lap charts to API; else leave mock. | 16, 25 | Strategy View fuel/ERS from API when available. |
| **27** | Backend | **B9.** Extend Motion ingest to all 22 cars; broadcast multi-car positions via WebSocket or REST. | — | Live positions for all cars. |
| **28** | New UI | Live Track Map: show real car positions from WebSocket/REST. | 22, 27 | Live Track Map with real positions. |
| **29** | Backend | **B11–B14 (deferred to last).** Implement in a **separate auth microservice**: register, login, refresh, logout; user profile (GET/PATCH); preferences (GET/PATCH); delete sessions/account. Main telemetry API stays unchanged; optional: accept JWT or session owner id for "Your session" marker. | — | Auth microservice (done last). |
| **30** | New UI | **Deferred to last.** Login/Register → auth service; store token in cookies; protect /app routes. Settings: profile/preferences via auth service; **theme persistence** (localStorage + apply class on html); danger zone calls auth service delete endpoints. | 29 | Login, Register, Settings wired to auth service; theme persists. |

---

## Detailed changes

| Step | Where | Concrete changes |
|------|--------|------------------|
| 25 | Backend | If fuel/ERS stored: GET /api/sessions/{uid}/fuel-by-lap?carIndex=, GET .../ers-by-lap or similar ([BACKEND_FEATURES_FOR_NEW_UI.md](../../../BACKEND_FEATURES_FOR_NEW_UI.md) §1.5); else document as future. |
| 26 | New UI | Strategy View: if B6/B7 exist, add getFuelByLap / getErsByLap, wire charts; else leave mock. |
| 27 | Backend | Ingest: publish motion for all 22 cars; processing: store and broadcast positions (WebSocket or REST). |
| 28 | New UI | Live Track Map: show real positions from WS/REST. |
| 29 | Backend | **Separate auth microservice:** B11–B14 (register, login, refresh, logout, profile, preferences, delete). Main API: no auth logic; optional session.ownerUserId for "Your session" when auth exists. |
| 30 | New UI | **Last step of migration.** Login/Register → auth service; token in httpOnly cookie; protect /app; Settings → auth service; **theme:** Theme select → save to localStorage (key `theme`: dark\|light\|system), apply class on `<html>` for Tailwind dark mode; danger zone → auth service. |

---

## Testing

| Step | Scope | What to add/update |
|------|--------|--------------------|
| 25, 27, 29 | Backend | Unit tests for new services/controllers (fuel/ERS, positions, auth/profile/preferences); 85% coverage; TestData for new entities. |
| 26, 28, 30 | New UI | Manual or e2e for Strategy fuel/ERS, Track Map positions, Login/Register/Settings. |

---

## Documentation updates

| Doc | Updates |
|-----|--------|
| [rest_web_socket_api_contracts_f_1_telemetry.md](../../../project/rest_web_socket_api_contracts_f_1_telemetry.md) | Fuel/ERS, positions (if any), auth and user endpoints. |
| [BACKEND_FEATURES_FOR_NEW_UI.md](../../../BACKEND_FEATURES_FOR_NEW_UI.md) | Mark B6–B7, B9, B11–B14 as implemented or deferred. |
| [NEW_UI_DOCS.md](../../../NEW_UI_DOCS.md) | §8: auth, persisted settings, danger zone when wired. |

---

## Detailed implementation sub-steps

Use this for a precise, step-by-step implementation order. Each sub-step is a single, verifiable task.

### Step 25 — Backend B6/B7 Fuel and ERS by lap (optional)

| # | Sub-step | Deliverable |
|---|----------|-------------|
| 25.1 | **Decision (H1):** B7 = one value per lap (ERS store % at lap end). Already recorded above. | — |
| 25.2 | Update REST contract: GET /api/sessions/{sessionUid}/fuel-by-lap?carIndex= (default 0). Response: array of FuelByLapDto (lapNumber, fuelPercent and/or fuelKg — define unit in contract). 404 if session not found; 200 and [] if no data. | Contract § fuel-by-lap |
| 25.3 | REST contract: GET /api/sessions/{sessionUid}/ers-by-lap?carIndex=. Response: array of ErsByLapDto (lapNumber, ersStorePercentEnd). One value per lap. 404/200/[] as above. | Contract § ers-by-lap |
| 25.4 | telemetry-api-contracts: add FuelByLapDto, ErsByLapDto. | DTO classes |
| 25.5 | LapQueryService (or new FuelErsByLapService): resolve session; for fuel: get lap boundaries (from lap table), for each lap take CarStatusRaw.fuelInTank at lap end (nearest timestamp to lap end); normalize to % if max fuel known, or return raw. Return list ordered by lapNumber. | Fuel-by-lap logic |
| 25.6 | Same service: for ERS-by-lap, same pattern — CarStatusRaw.ersStoreEnergy at lap end, convert to % (ERS_MAX_ENERGY_J). Single value per lap only. | ERS-by-lap logic |
| 25.7 | Controller: GET fuel-by-lap and GET ers-by-lap (e.g. LapController or SessionController). Thin; carIndex default 0. | Endpoints |
| 25.8 | TestData: fuel/ERS by lap scenario (session with laps and car_status_raw rows). Unit tests for service; MockMvc for 200/404/empty. Verify 85% coverage. | Tests |
| 25.9 | If fuel/ERS data is **not** available or product defers: document in BACKEND_FEATURES_FOR_NEW_UI.md as "deferred"; skip 25.2–25.8. | Doc only |

### Step 26 — New UI Strategy View fuel/ERS (optional)

| # | Sub-step | Deliverable |
|---|----------|-------------|
| 26.1 | api/types.ts: FuelByLapDto, ErsByLapDto (match backend). api/client.ts: getFuelByLap(sessionId, carIndex), getErsByLap(sessionId, carIndex). | API client |
| 26.2 | StrategyView: if B6/B7 implemented (e.g. feature flag or API availability check), fetch getFuelByLap and getErsByLap when session/car available. Loading and error states. | Fetch |
| 26.3 | StrategyView: Fuel Consumption chart — use API data (lapNumber, fuelPercent); replace mock fuelConsumption. | Fuel chart |
| 26.4 | StrategyView: ERS Deployment & Harvesting chart — use API data (lapNumber, ersStorePercentEnd; optional deployed/harvested if present). Replace mock ersDeployment. | ERS chart |
| 26.5 | If B6/B7 not implemented: keep mock data for fuel/ERS charts; no API calls. | Fallback |

### Step 27 — Backend B9 Multi-car live positions

| # | Sub-step | Deliverable |
|---|----------|-------------|
| 27.1 | **Decision (H2):** B9 in scope — implement now. | — |
| 27.2 | REST/WS contract: define payload for positions. E.g. CarPositionDto: carIndex, worldPosX, worldPosZ (optional: lapDistance). WebSocket message type e.g. POSITIONS on same topic /topic/live/{sessionId}; or REST GET /api/sessions/active/positions. | Contract |
| 27.3 | udp-ingest-service: MotionPacketHandler — instead of seekToPlayerCar only, **loop over all 22 cars** in packet; parse each CarMotionData; publish 22 events (or one event with list of MotionDto + carIndex). Kafka topic may stay telemetry.motion with carIndex in payload. | Ingest: all cars |
| 27.4 | telemetry-processing-api-service: MotionConsumer already writes by car_index; ensure it handles all car indices (no filter). motion_raw will then have all cars. | Processing |
| 27.5 | For **live** positions: either (A) read latest motion_raw per car for active session and broadcast at interval (e.g. LiveDataBroadcaster), or (B) new consumer that broadcasts on each Motion event. Define source of "current positions" (e.g. latest frame per car). | Broadcast source |
| 27.6 | Implement broadcast: e.g. new message type POSITIONS in WebSocket (list of CarPositionDto) or REST GET /api/sessions/active/positions returning same list. | Broadcast |
| 27.7 | Controller (if REST): GET /api/sessions/active/positions or GET /api/sessions/{id}/positions for active only. Service: resolve active session, get latest positions from state or motion_raw. | REST option |
| 27.8 | TestData: multi-car motion scenario. Unit tests for position aggregation/broadcast logic; 85% coverage. | Tests |
| 27.9 | Update BACKEND_FEATURES: B9 implemented. | Doc |

### Step 28 — New UI Live Track Map positions (optional)

| # | Sub-step | Deliverable |
|---|----------|-------------|
| 28.1 | If B9 deferred: Live Track Map keeps static/mock positions; no API/WS. | No change |
| 28.2 | api/types.ts: CarPositionDto (carIndex, worldPosX, worldPosZ). If WebSocket: subscribe to POSITIONS message type; if REST: poll GET .../positions at interval (e.g. 2–5 s) when on Live Track Map with active session. | API/WS client |
| 28.3 | LiveTrackMap: replace static drivers array with state from API or WS. Map worldPosX/worldPosZ to canvas coordinates using track layout bounds. | Real positions on map |
| 28.4 | Loading/empty state when no position data yet. | UX |

### Step 29 — Backend B11–B14 Auth and user (deferred to last; separate microservice)

**Done last** in the migration so the app can be tested without auth. Implement in a **dedicated auth microservice**; main telemetry-processing-api-service does not contain auth logic.

| # | Sub-step | Deliverable |
|---|----------|-------------|
| 29.1 | **Decisions (H3–H6):** Global sessions; optional session owner marker; token in cookie; auth = separate microservice. | — |
| 29.2 | **Auth microservice** — REST contract: POST /api/auth/register, POST /api/auth/login (set httpOnly cookie), POST /api/auth/refresh, POST /api/auth/logout. Response: accessToken (and set-cookie), refreshToken, expiresIn. | Contract § auth |
| 29.3 | **Auth microservice** — REST contract: GET/PATCH /api/users/me (profile), GET/PATCH /api/users/me/preferences, DELETE /api/users/me/sessions, DELETE /api/users/me (delete account). | Contract § user |
| 29.4 | **Auth microservice** — DB: user, user_preferences tables. No session table in auth service; "delete my sessions" may call main API with auth context or main API exposes optional DELETE with user scope when JWT present. | Schema (auth service) |
| 29.5 | **Auth microservice** — JWT, BCrypt, register/login/refresh/logout; set httpOnly cookie on login. | Auth flow |
| 29.6 | **Main telemetry API** — no auth logic. Optional: add session.owner_user_id (nullable) so that when a session is "claimed" by the current user (e.g. via auth service or link), UI can show "Your session". GET /api/sessions remains global (all sessions). | Main API: optional owner marker only |
| 29.7 | **Auth microservice** — profile and preferences services + endpoints. | Profile & preferences |
| 29.8 | **Auth microservice** — delete my sessions (coordinate with main API if needed), delete account. | B14 endpoints |
| 29.9 | TestData and unit tests for auth microservice; 85% coverage. | Tests |

### Step 30 — New UI Login/Register/Settings (deferred to last)

**Done last.** UI calls **auth microservice** (different base URL or gateway). Token stored in **cookies** (httpOnly); auth service sets cookie on login.

| # | Sub-step | Deliverable |
|---|----------|-------------|
| 30.1 | api/client: base URL for auth service; postRegister, postLogin (credentials; cookie set by auth service), postRefresh, postLogout; getProfile, patchProfile; getPreferences, patchPreferences; deleteAllMySessions, deleteAccount. Use credentials: 'include' for cookies. | API client (auth service) |
| 30.2 | Auth context: track "isAuthenticated" (e.g. from cookie presence or GET /api/users/me); clear on logout. Optional: refresh token before expiry. | Token / auth state |
| 30.3 | Login page: form → postLogin (auth service); on success (cookie set), redirect to /app. Register: same. Show validation and API errors. | Login/Register |
| 30.4 | Route guard: protect /app/* — if not authenticated, redirect to Login. Optional: validate with auth service GET /api/users/me. | Protected routes |
| 30.5 | Settings: load profile and preferences from auth service; Profile/Preferences sections → PATCH to auth service; toast on success. | Settings API |
| 30.6 | Settings danger zone: "Delete all my sessions" → confirm → auth service DELETE; "Delete account" → confirm → auth service DELETE → clear auth, redirect to Landing. | Danger zone |
| 30.7 | Logout: postLogout (auth service clears cookie), redirect to Landing. Wire to User menu (Block I). | Logout |
| 30.8 | **Theme persistence.** Settings Theme select (Dark/Light/System): on change save to localStorage (e.g. key `theme`, values `dark`\|`light`\|`system`). On app load read and apply: set `class="dark"` or `class="light"` on `<html>` (or use Tailwind dark mode strategy). Apply immediately on change. No backend. | Theme persists and applies |
| 30.9 | **Optional:** When authenticated, show "Your session" or highlight on session list for sessions where session.ownerUserId === current user (if main API exposes ownerUserId). | "Your session" marker |

---

## Implementation checklist

Use this to track what is done and what is left. Mark with `[x]` when completed.

### Step 25 — Backend B6/B7 Fuel and ERS by lap

- [x] 25.1 — Decision B7 scope (H1) recorded
- [x] 25.2 — REST contract: fuel-by-lap endpoint and FuelByLapDto
- [x] 25.3 — REST contract: ers-by-lap endpoint and ErsByLapDto
- [x] 25.4 — telemetry-api-contracts: FuelByLapDto, ErsByLapDto
- [x] 25.5 — Service: fuel-by-lap aggregation (lap end from CarStatusRaw)
- [x] 25.6 — Service: ers-by-lap aggregation (lap end, %; optional deployed/harvested)
- [x] 25.7 — Controller: GET fuel-by-lap, GET ers-by-lap
- [x] 25.8 — TestData + unit tests + MockMvc; verify 85% coverage
- [ ] 25.9 — If deferred: document in BACKEND_FEATURES only

### Step 26 — New UI Strategy View fuel/ERS

- [x] 26.1 — api/types + api/client: getFuelByLap, getErsByLap
- [x] 26.2 — StrategyView: fetch fuel/ERS when B6/B7 available; loading/error
- [x] 26.3 — StrategyView: Fuel Consumption chart from API
- [x] 26.4 — StrategyView: ERS Deployment & Harvesting chart from API
- [x] 26.5 — If B6/B7 not implemented: keep mock

### Step 27 — Backend B9 Multi-car positions

- [x] 27.1 — Decision B9 in scope (H2)
- [x] 27.2 — REST/WS contract: positions payload and message type or GET
- [x] 27.3 — udp-ingest: MotionPacketHandler publish all 22 cars
- [x] 27.4 — Processing: MotionConsumer accepts all car indices
- [x] 27.5 — Define and implement broadcast source (latest positions)
- [x] 27.6 — WebSocket POSITIONS or REST GET positions
- [x] 27.7 — Controller/service for positions (if REST)
- [ ] 27.8 — TestData + unit tests; 85% coverage
- [x] 27.9 — BACKEND_FEATURES B9 implemented/deferred

### Step 28 — New UI Live Track Map positions

- [x] 28.1 — If B9 deferred: no change
- [x] 28.2 — api/types + subscribe/poll for positions
- [x] 28.3 — LiveTrackMap: real positions from API/WS, map to canvas
- [x] 28.4 — Loading/empty state

### Step 29 — Backend Auth and user (B11–B14) — deferred to last; separate auth microservice

- [ ] 29.1 — Decisions H3–H6 recorded
- [ ] 29.2 — Auth microservice: REST contract auth endpoints (register, login with cookie, refresh, logout)
- [ ] 29.3 — Auth microservice: REST contract profile, preferences, delete sessions/account
- [ ] 29.4 — Auth microservice: DB user, user_preferences
- [ ] 29.5 — Auth microservice: JWT, BCrypt, login sets httpOnly cookie
- [ ] 29.6 — Main API: optional session.owner_user_id only; no auth logic; GET /api/sessions global
- [ ] 29.7 — Auth microservice: profile and preferences endpoints
- [ ] 29.8 — Auth microservice: delete my sessions, delete account
- [ ] 29.9 — TestData + unit tests for auth microservice; 85% coverage

### Step 30 — New UI Login/Register/Settings — deferred to last

- [ ] 30.1 — api/client: auth service base URL; auth and user/preferences/delete; credentials: 'include'
- [ ] 30.2 — Auth context: isAuthenticated (cookie or GET /users/me)
- [ ] 30.3 — Login and Register pages → auth service + redirect
- [ ] 30.4 — Route guard for /app
- [ ] 30.5 — Settings: profile and preferences from auth service
- [ ] 30.6 — Settings danger zone: auth service DELETE
- [ ] 30.7 — Logout → auth service, redirect to Landing
- [ ] 30.8 — Theme persistence: Theme select → localStorage; apply class on html on load and on change
- [ ] 30.9 — Optional: "Your session" marker when session.ownerUserId === current user

### Step 31 — Documentation

- [x] REST contract: fuel-by-lap, ers-by-lap (if 25 done)
- [x] REST contract: positions (if 27 done)
- [ ] REST contract: auth, user profile, preferences, delete (if 29 done)
- [x] BACKEND_FEATURES_FOR_NEW_UI.md: B6, B7, B9, B11–B14 marked implemented or deferred
- [ ] NEW_UI_DOCS.md: §8 auth, settings, danger zone (if 30 done)
- [ ] telemetry_processing_api_service.md: new controllers/services (if any)

### Step 32 — Git Commit
- [ ] Add git commit with understanding message

---

## Cross-references

- **Order of steps:** Implement **25 → 26 → 27 → 28** in block order. Implement **29 → 30 last** (after all other blocks) so the app can be tested without auth; then add the auth microservice and wire the UI.
- **Block I step 35 (Delete All Sessions):** When auth (B14) is implemented, "Delete All Sessions" in Settings calls the auth service `DELETE /api/users/me/sessions`. If auth is not yet implemented, Block I may show a placeholder or document "requires auth service".
- **Strategy View entry:** Strategy is opened from Session Details (Block D); fuel/ERS charts (step 26) use the same session id and carIndex context.
- **Live Track Map layout:** Track layout comes from Block F (B8); step 28 only adds **positions** (B9) on top of that layout.
- **Auth microservice:** Separate codebase/service; main telemetry-processing-api-service does not implement register/login/profile/preferences. Optional integration: main API may add nullable `session.owner_user_id` for "Your session" when auth exists.
- **Theme persistence:** Implemented in **Block H step 30** (with other Settings); not in Block I. See Block I "Decisions recorded" I5.
