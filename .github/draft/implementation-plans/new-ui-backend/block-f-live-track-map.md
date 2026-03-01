# Block F — Live Track Map (backend B8 + UI)

Part of the [Step-by-Step Implementation Plan — New UI & Backend](../../../IMPLEMENTATION_STEP_BY_STEP.md). Steps 21–22.

---

## Current state and gaps analysis

### What exists today

| Area | Current state |
|------|----------------|
| **TrackController** | Exists: `GET /api/tracks/{trackId}/corner-maps/latest?trackLengthM=` — returns corner map (start/end/apex distance per corner), not 2D layout. |
| **Track corner data** | `track_corner_maps`, `track_corners` — corners by lap distance (for session detail graphs). No polyline/outline coordinates. |
| **REST contract** | No section for `GET /api/tracks/{trackId}/layout`; only corner-maps. |
| **LiveTrackMap (UI)** | Static SVG path and hardcoded "Silverstone Circuit"; mock driver positions; no API calls; no active-session or trackId from backend. |
| **Block A (API layer)** | Plan includes `getTrackName(trackId)` and tracks constant; session DTO has `trackId`. Step 4 delivers session list and `getActiveSession`. |
| **B9 (live positions)** | Not in Block F scope; driver positions stay mock until B9. |

### Gaps not fully covered in the original plan

| # | Gap | Detail | Plan addition |
|---|-----|--------|----------------|
| 1 | **Data source: files vs DB** | Plan says "files or new table" but does not pick one. Option A: one JSON per track in resources; Option B: `track_layout` table. | **Resolved:** Option B — new table `track_layout` (data loaded from files via migration or import script). |
| 2 | **Response format and units** | "points array" or GeoJSON — exact schema missing. Need coordinates + optional bounds for viewBox. | **Resolved:** Response includes `points` and optional `bounds: { minX, minY, maxX, maxY }` for client viewBox. |
| 3 | **404 behaviour** | When `trackId` is valid (e.g. F1 track enum) but no layout file/row exists — return 404. When `trackId` is invalid (unknown) — 404 as well. No "list of tracks with layout" in scope for MVP. | Document: GET layout returns 404 if no layout for trackId; UI handles 404 (message or fallback). |
| 4 | **Where trackId comes from (UI)** | Plan says "when track is known (e.g. from active session)". Live Track Map is a Live page — need same pattern as Live Overview: getActiveSession() first; if no active session, show "No active session". trackId = activeSession.trackId. | Add: LiveTrackMap depends on Block C (getActiveSession); fetch layout only when active session exists and has trackId. |
| 5 | **Track name in card** | UI currently shows hardcoded "Silverstone Circuit". Should use session or track display name (e.g. getTrackName(trackId) from Block A). | Add: LiveTrackMap card title = getTrackName(trackId) or sessionDisplayName/track from session. |
| 6 | **Sector markers and start line** | Current mock has sector circles and START line. Layout API returns only points (track shape). | **Resolved:** Option B — keep static sector/start markers in UI for visual reference (not from API). |
| 7 | **Scaling and viewBox** | API points may be in arbitrary units. UI must scale to SVG viewBox (e.g. compute bounding box, preserve aspect ratio). | Add: UI step to compute viewBox from points (or contract specifies normalized 0–1 so viewBox="0 0 1 1" and scale). |
| 8 | **telemetry-api-contracts** | If DTO is shared (e.g. TrackLayoutResponseDto), it may live in telemetry-api-contracts. Confirm whether this module exists and is used for track endpoints. | Add: Define DTO (in contract doc + in backend); if contracts module exists, add there and use from service. |
| 9 | **Controller placement** | Same base path `/api/tracks` — add new method to existing TrackController (recommended) rather than a new controller. | Add: Extend TrackController with GET `/{trackId}/layout`. |
| 10 | **Logging and layering** | TrackLayoutService (or similar): no parsing of UDP; read-only. Follow logging policy: DEBUG at entry, WARN if trackId null or resource missing. | Add: Logging in service and controller; thin controller. |

---

## Component reusability

Follow [README § Frontend component reusability](README.md#frontend-component-reusability-all-blocks). Reuse the **"No active session"** state component or pattern from Block C (Live Overview / Live Telemetry); use **DataCard** for the map container and **getTrackName(trackId)** from Block A. Use Shadcn **Skeleton** for loading; do not duplicate track-name or empty-state logic.

---

## Resolved decisions

| # | Topic | Decision |
|---|--------|----------|
| 1 | **Data source** | **New table** `track_layout` (e.g. track_id, points JSONB, optional version). Data loaded via migration or import script from source files. |
| 2 | **Response format** | `{ trackId: number, points: [{ x, y }], bounds?: { minX, minY, maxX, maxY } }`. Optional `bounds` for client viewBox so UI can scale without computing from points. |
| 3 | **Sector markers / start line** | **Option B:** Keep static sector/start markers in UI for visual reference (not from API). |
| 4 | **When layout 404** | **Option A:** Show inline message "Track layout not available for this track" and keep empty map or placeholder (no generic oval fallback). |
| 5 | **404** | GET /api/tracks/{trackId}/layout returns 404 when no layout exists for trackId. No separate "list of layouts" endpoint in MVP. |
| 6 | **Controller** | Add GET `/{trackId}/layout` to existing TrackController. |
| 7 | **Driver positions** | Remain mock until B9; no change in Block F. |
| 8 | **Track name in UI** | Use getTrackName(trackId) from Block A (or session display name); remove hardcoded "Silverstone Circuit". |

---

## Steps

| Step | Layer | Task | Depends on | Deliverable |
|------|--------|------|------------|-------------|
| **21** | Backend | **B8 — Track layout.** Add GET /api/tracks/{trackId}/layout returning 2D coordinates (points array) and optional bounds. Data from new table `track_layout` (migration + import from files). Define response format in REST contract. Implement TrackLayoutService + extend TrackController. | — | Track layout endpoint available. |
| **22** | New UI | Live Track Map: getTrackLayout(trackId) in API client. Resolve trackId from active session (Block C). Fetch layout when active session present; draw track from points (SVG path); loading/error/404 states. Track name from getTrackName(trackId). Driver positions stay mock. | 4, 9, 21 | Live Track Map draws track from API; positions static; no active session handled. |

---

## Detailed changes (substeps)

### Step 21 — Backend B8 Track layout

| Id | Where | Concrete change |
|----|--------|------------------|
| 21.1 | REST contract | In [rest_web_socket_api_contracts_f_1_telemetry.md](../../../project/rest_web_socket_api_contracts_f_1_telemetry.md) add § **3.5.4 Track layout (2D map)**. Document: `GET /api/tracks/{trackId}/layout`. Path param: `trackId` (short/int). Response 200: body `{ trackId: number, points: [{ x: number, y: number }], bounds?: { minX, minY, maxX, maxY } }`; optional `bounds` for client viewBox. Response 404: no layout for this track. |
| 21.2 | DTO | Define `TrackLayoutResponseDto`: trackId, points (list of {x, y}), optional bounds (minX, minY, maxX, maxY). If telemetry-api-contracts module exists, add there; else in service module. |
| 21.3 | DB migration | Add migration for table `telemetry.track_layout` (e.g. track_id SMALLINT PK, points JSONB not null, optional version). |
| 21.4 | Data import | Import script or migration data: load layout points (and compute/store bounds if desired) from source files into `track_layout` for at least one track. |
| 21.5 | TrackLayoutService | New service: method `getLayout(trackId)`: query repository by track_id; map entity to DTO (include bounds from DB or compute from points). Return Optional of DTO. Log DEBUG at entry; WARN if trackId null or not found. |
| 21.6 | TrackController | Add `GET /{trackId}/layout`: call TrackLayoutService.getLayout(trackId); return 200 + body or 404. Log DEBUG. |
| 21.7 | Tests | TrackLayoutServiceTest: file read or repo mock; present/missing trackId; 404 case. TrackController MockMvc: GET layout 200 and 404. Per [unit_testing_policy.md](../../../project/unit_testing_policy.md); 85% coverage for new classes. |
| 21.8 | Documentation | BACKEND_FEATURES_FOR_NEW_UI.md: mark B8 implemented. Optional: telemetry_processing_api_service.md if new service. |

### Step 22 — New UI Live Track Map

| Id | Where | Concrete change |
|----|--------|------------------|
| 22.1 | API types | In api/types.ts: add `TrackLayoutResponseDto`: `{ trackId: number, points: { x: number, y: number }[], bounds?: { minX, minY, maxX, maxY } }`. |
| 22.2 | API client | Add `getTrackLayout(trackId: number): Promise<TrackLayoutResponseDto | null>`. GET `/api/tracks/${trackId}/layout`. On 404 return null or throw; on error toast and throw. Align with existing client error handling. |
| 22.3 | Active session and trackId | LiveTrackMap: on mount (or when entering page), use same pattern as Live Overview — getActiveSession(). If no active session, show "No active session" state (reuse Live state message or similar). If active session exists, trackId = activeSession.trackId. |
| 22.4 | Fetch layout | When trackId is set, call getTrackLayout(trackId). Loading state while fetching. On success: store points (and bounds if present) for drawing. On 404: show inline message "Track layout not available for this track" and empty map or placeholder (Option A). On other errors: toast + inline error. |
| 22.5 | Draw track from points | Replace hardcoded SVG path with path built from API points. Use response `bounds` when present for viewBox; otherwise compute bounding box from points. Set SVG viewBox to preserve aspect ratio. Build `<path d={pointsToPath(points)} />`. Keep stroke/fill styling consistent. |
| 22.6 | Sector/start | Keep static sector markers and start line in UI for visual reference (Option B); not from API. |
| 22.7 | Track name | Card title: use getTrackName(trackId) from Block A (or session display name). Remove "Silverstone Circuit" hardcode. |
| 22.8 | Driver positions | Leave as mock (same as now); no B9 integration. |
| 22.9 | Connection / lap info | If Live page shows connection status and lap number, keep from WebSocket/snapshot (Block C); no change required for layout. |

---

## Testing

| Step | Scope | What to add/update |
|------|--------|--------------------|
| 21 | Backend | TrackLayoutService unit tests (file read or repository mock); TrackController MockMvc GET /api/tracks/{trackId}/layout (200 with body, 404). Run `mvn -pl telemetry-processing-api-service verify` for 85% if new classes counted. |
| 22 | New UI | Manual or e2e: open Live Track Map with active session; track shape loads from API; loading and 404 states; track name from session/trackId. Without active session: "No active session" shown. |

---

## Documentation updates

| Doc | Updates |
|-----|--------|
| [rest_web_socket_api_contracts_f_1_telemetry.md](../../../project/rest_web_socket_api_contracts_f_1_telemetry.md) | New § 3.5.4 (or equivalent) Track layout endpoint, request/response, 404. |
| [BACKEND_FEATURES_FOR_NEW_UI.md](../../../BACKEND_FEATURES_FOR_NEW_UI.md) | Mark B8 implemented when done. |
| [.github/project/telemetry_processing_api_service.md](../../../project/telemetry_processing_api_service.md) | If new TrackLayoutService: add to architecture. |

---

## Implementation checklist

Use this checklist to track progress. Mark `[ ]` as `[x]` when done.

### Step 21 — Backend B8 Track layout

- [ ] **21.1** — REST contract: new § GET /api/tracks/{trackId}/layout, response with points + optional bounds, 404
- [ ] **21.2** — DTO: TrackLayoutResponseDto (trackId, points, bounds optional)
- [ ] **21.3** — DB migration: table telemetry.track_layout (track_id, points JSONB, optional version)
- [ ] **21.4** — Data import: populate track_layout from source files for at least one track
- [ ] **21.5** — TrackLayoutService: getLayout(trackId), query repo, map to DTO, logging
- [ ] **21.6** — TrackController: GET /{trackId}/layout, 200/404
- [ ] **21.7** — Unit tests: TrackLayoutService, MockMvc controller; 85% coverage
- [ ] **21.8** — BACKEND_FEATURES_FOR_NEW_UI.md B8 implemented; optional telemetry_processing_api_service.md

### Step 22 — New UI Live Track Map

- [ ] **22.1** — api/types.ts: TrackLayoutResponseDto (trackId, points, bounds?)
- [ ] **22.2** — api/client: getTrackLayout(trackId), error/404 handling
- [ ] **22.3** — LiveTrackMap: getActiveSession, "No active session" state when no session
- [ ] **22.4** — LiveTrackMap: fetch layout; on 404 show "Track layout not available" (inline)
- [ ] **22.5** — LiveTrackMap: draw track from points; use bounds from API when present for viewBox
- [ ] **22.6** — Sector/start: keep static markers (Option B)
- [ ] **22.7** — Card title: getTrackName(trackId) or session display name
- [ ] **22.8** — Driver positions: remain mock (no B9)
- [ ] **22.9** — Connection/lap from Block C unchanged

### Step 23 — Documentation

- [ ] REST contract: Track layout § added
- [ ] BACKEND_FEATURES_FOR_NEW_UI.md: B8 marked implemented

### Step 24 — Git Commit
- [ ] Add git commit with understanding message

---

## Out of scope for Block F

- **B9 — Live positions of all cars:** Driver positions on map stay mock; B9 is in Block H (optional).
- **Sector boundaries from API:** Corner-maps returns distance-based corners, not 2D; sector/start markers on map are kept as static UI elements (Option B).
- **List of tracks with layout:** No GET /api/tracks that returns which trackIds have layout; 404 per trackId is sufficient for MVP.
- **Editing or uploading layout from UI:** Backend only serves layout; data comes from files or DB import.
