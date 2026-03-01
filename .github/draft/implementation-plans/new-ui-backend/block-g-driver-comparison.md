# Block G — Driver Comparison (optional backend B10 + UI)

Part of the [Step-by-Step Implementation Plan — New UI & Backend](../../../IMPLEMENTATION_STEP_BY_STEP.md). Steps 23–24.

---

## 1. Gap analysis (what the current plan misses)

| Gap | Description | Recommendation |
|-----|-------------|----------------|
| **Driver/car selection** | UI needs a list of cars (or car indices) for the session to populate "Driver A" / "Driver B" dropdowns. Backend does not expose participants. | **Resolved:** Extend Session DTO with `participants` list. See § 2 and § 4. |
| **Trace and speed-trace require lap number** | Comparison overlay needs trace/speed for one or two laps (best lap per driver or user-selected). | **Resolved:** B10 includes trace and speed-trace. Default: best lap of each pilot; optional params for user-selected laps. See § 2. |
| **Time delta along distance** | UI shows "Time Delta (Driver A − Driver B)" along distance. Backend has no such endpoint. | Compute time-delta curve on the client from two speed-trace arrays (interpolate to same distance grid if needed). |
| **Session list on Driver Comparison page** | Page has a "Session" selector. | Use `getSessions()` (Block A); session selector bound to GET /api/sessions. |
| **Validation rules** | Comparison endpoint must reject invalid input. | 400 when carIndexA == carIndexB; 404 when session missing or no data for a car. |
| **Sector comparison data source** | Sector deltas (S1/S2/S3). | Use summary A + summary B (bestSector1/2/3Ms). |
| **B10 response shape (trace/speed)** | Which lap(s) for trace/speed. | **Resolved:** Two reference laps (one per car). See § 2. |

---

## Component reusability

Follow [README § Frontend component reusability](README.md#frontend-component-reusability-all-blocks). Use **DataCard** for summary cards and chart containers; Shadcn **Select**, **Table**, chart components; shared **formatLapTime** and compound/sector formatting. Reuse the same session selector and loading/error pattern as in other pages; prefer shared chart wrappers if they exist from Session Details (pace, trace, speed).

---

## 2. Decisions (resolved)

1. **Driver/car list — extend Session DTO with participants**
   - **SessionDto** is extended with an optional field **`participants`**: array of `{ carIndex: number, displayLabel?: string }` (or a small `SessionParticipantDto`).
   - **Data source:** Participants = car indices that have at least one lap or session summary for this session (so the list is correct and consistent with comparison data). `displayLabel` can be derived from `session_finishing_positions` (e.g. "P1", "P2") or a fallback "Car {carIndex}".
   - **Forward compatibility:** In earlier plans a **drivers table** is planned (e.g. Block E — leaderboard). The participants DTO shape must allow later adding a driver id or name from that table (e.g. `driverId?: string`, `displayLabel` filled from drivers table when available). For now, ensure the list is built from actual session data (laps/summary) and labels are consistent; when the drivers table exists, the same DTO can be populated with real driver names.
   - **Where returned:** Include `participants` in **GET /api/sessions/{id}** (session detail). For **GET /api/sessions** (list), participants may be omitted for performance unless needed for list UI.
   - UI uses `session.participants` from the selected session (after loading session by id) to populate Driver A / Driver B dropdowns.

2. **B10 includes trace and speed-trace — Yes**
   - Comparison endpoint returns `traceA`, `traceB`, `speedTraceA`, `speedTraceB` in addition to laps, summary, pace for both cars.

3. **Reference laps: default best lap of each pilot; then user can select laps**
   - **Default (no params):** Server uses **best lap of each pilot** — `referenceLapNumA = summaryA.bestLapNumber`, `referenceLapNumB = summaryB.bestLapNumber`. Response includes `referenceLapNumA`, `referenceLapNumB` (the lap numbers actually used), and trace/speed for those laps.
   - **Optional query params:** `referenceLapNumA`, `referenceLapNumB`. When provided, server uses these laps for trace and speed-trace (allows comparing any lap of A vs any lap of B).
   - **UI behaviour:** Initially load comparison **without** reference-lap params (backend uses best lap each). Show **two lap selectors** (one for Driver A, one for Driver B), populated from `lapsA` and `lapsB`. When the user changes a selection, refetch comparison with `referenceLapNumA` and/or `referenceLapNumB`. So users first see "best vs best", then can choose different laps per pilot if needed.

---

## 3. Steps (overview)

| Step | Layer | Task | Depends on | Deliverable |
|------|--------|------|------------|-------------|
| **23** | Backend | **B10 (optional) — Comparison API.** Add `GET /api/sessions/{sessionUid}/comparison?carIndexA=&carIndexB=` with optional `referenceLapNumA`, `referenceLapNumB`. When omitted, use best lap from summary for each car. Response: laps, summary, pace for both; `referenceLapNumA`, `referenceLapNumB`, traceA/B, speedTraceA/B for those laps. Extend **SessionDto** with `participants` (car indices with data + optional displayLabel; forward-compatible with future drivers table). Include participants in GET /api/sessions/{id}. Contract, validation (400/404), tests. | — | Comparison endpoint; SessionDto.participants. |
| **24** | New UI | Driver Comparison: session selector (getSessions); load session by id for participants; Driver A/B selectors from `session.participants`; getComparison(sessionUid, carIndexA, carIndexB, referenceLapNumA?, referenceLapNumB?). Initial load without ref-lap params (best vs best); two lap selectors (from lapsA, lapsB) to allow choosing different laps; on change refetch with ref-lap params. Replace mock with API data. Compute time-delta from speed-traces on client. | Block A (4), (23) | Driver Comparison uses real data; best-vs-best by default; optional lap pick per driver. |

---

## 4. Detailed plan (backend — Step 23)

| # | Where | Task | Notes |
|---|--------|------|-------|
| 23.1 | Contract | Update [rest_web_socket_api_contracts_f_1_telemetry.md](../../../project/rest_web_socket_api_contracts_f_1_telemetry.md): (1) **Session:** extend SessionDto with optional `participants?: SessionParticipantDto[]` where each has `carIndex`, `displayLabel?` (and optionally `driverId?` for future drivers table). Document: participants = car indices that have at least one lap or summary for this session; include in GET /api/sessions/{id}; may be omitted in GET /api/sessions list. (2) **Comparison:** add § 3.x **Comparison**. `GET /api/sessions/{sessionUid}/comparison?carIndexA=&carIndexB=&referenceLapNumA=&referenceLapNumB=` (reference params optional). When omitted, server uses best lap from summary for each car. Response: `sessionUid`, `carIndexA`, `carIndexB`, `lapsA`, `lapsB`, `summaryA`, `summaryB`, `paceA`, `paceB`, `referenceLapNumA`, `referenceLapNumB`, `traceA`, `traceB`, `speedTraceA`, `speedTraceB`. Errors: 400 if carIndexA == carIndexB or missing car params; 404 if session not found or no data for a car. | Participants forward-compatible with drivers table. |
| 23.2 | telemetry-api-contracts | Add `SessionParticipantDto` (carIndex, displayLabel optional). Add to SessionDto field `participants`. Add `ComparisonResponseDto` with all fields above (and nested types for laps/summary/pace/trace/speed). | |
| 23.3 | Service | **Participants:** In `SessionQueryService.getSession(id)` (or a dedicated method invoked when building session detail), load participants: distinct car indices that have laps or session_summary for this session; displayLabel from session_finishing_positions (e.g. "P1") or fallback "Car {carIndex}". When drivers table exists later, resolve displayLabel from it. Set `sessionDto.setParticipants(list)`. **Comparison:** Add `ComparisonQueryService`: resolve session; validate carIndexA != carIndexB; load laps, summary, pace for both cars; determine reference laps (from params or summaryA.bestLapNumber / summaryB.bestLapNumber); load getLapTrace and getSpeedTrace for (session, referenceLapNumA, carIndexA) and (session, referenceLapNumB, carIndexB). Return DTO. | Participants data must be correct (cars with data in session). |
| 23.4 | Mapper | Add or use mapper: entity/list → comparison DTO; session → participants list (entity or repo result → SessionParticipantDto). | |
| 23.5 | Controller | GET /api/sessions/{id}: ensure response includes participants (from service). Add comparison endpoint: validate params, call ComparisonQueryService, return DTO or 400/404. | Thin: validate → service → return. |
| 23.6 | Logging | DEBUG at entry (sessionUid, carIndexA, carIndexB, referenceLapNumA, referenceLapNumB); WARN before 400/404. | Per [logging-policy.mdc](../../../.cursor/rules/logging-policy.mdc). |
| 23.7 | TestData | Add TestData helpers for two cars (laps/summary/pace for carIndex 0 and 1); participants list for a session. | |
| 23.8 | Tests | Unit tests: SessionQueryService (or participants loader) returns correct participants for session; ComparisonQueryService and controller (happy path, default best lap, custom reference laps, 400 same car, 404 no session, 404 no data for car). 85% coverage. | Per [unit_testing_policy.mdc](../../../.cursor/rules/unit-testing-policy.mdc). |

---

## 5. Detailed plan (New UI — Step 24)

| # | Where | Task | Notes |
|---|--------|------|-------|
| 24.1 | API layer | Add `getComparison(sessionUid, carIndexA, carIndexB, referenceLapNumA?, referenceLapNumB?)`. Types: `ComparisonResponseDto` (lapsA/B, summaryA/B, paceA/B, referenceLapNumA/B, traceA/B, speedTraceA/B), `SessionParticipantDto` (carIndex, displayLabel?). Ensure `getSessions()`, `getSession(id)` (returns session with participants), `getSessionLaps`, `getSessionSummary`, `getPace`, `getLapTrace`, `getLapSpeedTrace` exist (Block A). | |
| 24.2 | DriverComparison page | **Session selector:** Load session list via `getSessions()`; store selected `sessionUid` in state. When session selected, load session detail via `getSession(sessionUid)` to get **participants** for dropdowns. | |
| 24.3 | DriverComparison page | **Driver A / Driver B selectors:** Populate from `session.participants` (from getSession response). Store `carIndexA`, `carIndexB` in state. Display `participant.displayLabel` or fallback "Car {carIndex}". | |
| 24.4 | DriverComparison page | **Fetch comparison data:** Call `getComparison(sessionUid, carIndexA, carIndexB, referenceLapNumA?, referenceLapNumB?)`. **Initial load:** no reference params (backend returns best lap of each pilot). Use TanStack Query (or equivalent) keyed by sessionUid, carIndexA, carIndexB, referenceLapNumA, referenceLapNumB; loading/error states. | |
| 24.5 | DriverComparison page | **Lap selectors (two):** Dropdown "Lap" for Driver A (options from `lapsA`), dropdown "Lap" for Driver B (options from `lapsB`). Default selection: show "Best lap" or the current `referenceLapNumA` / `referenceLapNumB` from response. When user changes selection: set state and **refetch** comparison with `referenceLapNumA` and/or `referenceLapNumB` set to selected lap numbers. | So user can compare best vs best first, then e.g. lap 5 of A vs lap 7 of B. |
| 24.6 | DriverComparison page | **Summary cards:** Bind to `summaryA` / `summaryB` (best lap, avg lap, position). Use finishing position from session or summary where available. | |
| 24.7 | DriverComparison page | **Lap time comparison chart:** Data from `lapsA` / `lapsB` or pace A/B. Replace mock. | |
| 24.8 | DriverComparison page | **Sector delta comparison:** Use summary A/B `bestSector1Ms`, `bestSector2Ms`, `bestSector3Ms`; compute deltas; show bars + delta labels. | |
| 24.9 | DriverComparison page | **Speed and throttle overlay:** Use `speedTraceA`, `speedTraceB`, `traceA`, `traceB` from comparison response (for the current reference laps). Bind to charts. | |
| 24.10 | DriverComparison page | **Time delta along distance:** Compute on client from `speedTraceA` and `speedTraceB`: interpolate to same distance grid if needed; segment time = Δdistance / speed; cumulative time for A and B; delta = timeA − timeB. Display in "Time Delta" chart. | |
| 24.11 | DriverComparison page | Remove all mock data; handle empty state (no laps, no participants, no session). Toasts for API errors (per UI rules). | |
| 24.12 | Testing | Manual or e2e: select session, select two drivers from participants, verify initial view (best vs best), change lap selectors, verify overlay and time delta; summary, lap chart, sector deltas. | |

---

## 6. Testing

| Step | Scope | What to add/update |
|------|--------|--------------------|
| 23 | Backend | Comparison service and controller unit tests; TestData for two cars; 400 (same car), 404 (no session, no data); 85% coverage. |
| 24 | New UI | Manual or e2e: Driver Comparison with real session and two car indices; verify all charts and selectors. |

---

## 7. Documentation updates

| Doc | Updates |
|-----|--------|
| [rest_web_socket_api_contracts_f_1_telemetry.md](../../../project/rest_web_socket_api_contracts_f_1_telemetry.md) | SessionDto: add optional `participants` (SessionParticipantDto: carIndex, displayLabel?); include in GET /api/sessions/{id}. New § Comparison: endpoint, query params (referenceLapNumA/B optional), response shape, errors. |
| [BACKEND_FEATURES_FOR_NEW_UI.md](../../../BACKEND_FEATURES_FOR_NEW_UI.md) | Mark B10 implemented when done; note SessionDto.participants for Driver Comparison (forward-compatible with future drivers table). |
| [NEW_UI_DOCS.md](../../../NEW_UI_DOCS.md) | Note Driver Comparison uses real API (getSession with participants, getComparison); default best lap vs best lap; optional lap selectors per driver. |

---

## 8. Checklist (track progress)

Use this checklist to mark what is done. Update the file as you complete each item.

### Backend (Step 23) — B10 + SessionDto.participants

- [ ] **23.1** — Contract: SessionDto extended with `participants` (§ Session); comparison endpoint and response shape (§ Comparison); referenceLapNumA/B optional, default best lap each
- [ ] **23.2** — DTO: `SessionParticipantDto`; `SessionDto.participants`; `ComparisonResponseDto` (with referenceLapNumA/B, traceA/B, speedTraceA/B)
- [ ] **23.3** — Service: participants loaded in session detail (cars with laps/summary; displayLabel from finishing position or fallback); `ComparisonQueryService` (best lap default, optional ref laps)
- [ ] **23.4** — Mapper: entity/list → comparison DTO; session → participants list
- [ ] **23.5** — Controller: GET /api/sessions/{id} returns session with participants; GET comparison endpoint, validation, 400/404
- [ ] **23.6** — Logging: DEBUG entry (including ref lap params); WARN on validation/404
- [ ] **23.7** — TestData: two-car data; participants list for session
- [ ] **23.8** — Unit tests: participants in session detail; ComparisonQueryService and controller (default best lap, custom ref laps, 400, 404); 85% coverage

### New UI (Step 24)

- [ ] **24.1** — API: `getComparison(sessionUid, carIndexA, carIndexB, referenceLapNumA?, referenceLapNumB?)` and types; getSession returns participants
- [ ] **24.2** — Session selector: load list; on select load getSession(sessionUid) for participants
- [ ] **24.3** — Driver A/B selectors: from `session.participants`, displayLabel or "Car N"
- [ ] **24.4** — Fetch comparison (initial: no ref-lap params → best vs best); TanStack Query keyed by ref laps
- [ ] **24.5** — Two lap selectors (Driver A, Driver B); on change refetch with referenceLapNumA/B
- [ ] **24.6** — Summary cards: real summary A/B
- [ ] **24.7** — Lap time comparison chart: real laps/pace
- [ ] **24.8** — Sector delta comparison: real best sectors, deltas
- [ ] **24.9** — Speed and throttle overlay: from comparison response (traceA/B, speedTraceA/B)
- [ ] **24.10** — Time delta chart: computed from speedTraceA/B on client
- [ ] **24.11** — Mocks removed; empty and error states; toasts
- [ ] **24.12** — Manual/e2e: session → two drivers → best vs best → change laps → verify charts

### Documentation

- [ ] Contract updated (Session participants; Comparison §)
- [ ] BACKEND_FEATURES_FOR_NEW_UI.md updated (B10 done; SessionDto.participants)
- [ ] NEW_UI_DOCS.md updated (Driver Comparison real data)
