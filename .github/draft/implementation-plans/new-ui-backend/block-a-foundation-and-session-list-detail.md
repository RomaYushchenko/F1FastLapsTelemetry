# Block A — Foundation and session list/detail (existing API)

Part of the [Step-by-Step Implementation Plan — New UI & Backend](../../../IMPLEMENTATION_STEP_BY_STEP.md). Steps 1–6.

---

## Gap analysis (current plan vs codebase)

Comparison of the plan with the current **f1-telemetry-web-platform** implementation:

| Area | In plan | Current state | Gap |
|------|--------|----------------|-----|
| **API layer** | `api/config.ts`, `api/types.ts`, `api/client.ts` | No `src/api/` folder | Entire API layer missing. |
| **Session ID** | `api/sessionId.ts` (toSessionIdString, isValidSessionId) | Not present | Port from `ui/src/api/sessionId.ts`. |
| **Tracks** | Tracks constant + getTrackName(trackId) | Not present | Port to **`src/constants/tracks.ts`** (constants in separate file per decision). |
| **Toaster** | Mount `<Toaster />` (sonner) in App | Not in App.tsx; sonner is in package.json | Add Toaster to `App.tsx` (or root layout). |
| **Bell (Notifications)** | Dropdown/popover with recent notifications fed from toasts; unread badge | Bell is a static button with hardcoded red dot; no list, no context | Add notification context/store; wire toast calls to push to it; Bell opens popover with list; optional unread badge. |
| **SessionHistory** | getSessions(), loading/error/empty, links with session.id (string) | Mock array; no API; links use numeric id; no loading/error/empty | Replace mock with API; add states; use `session.id` (string) in links. |
| **Session list columns** | — | Table has Track, Date, Session Type, Best Lap, Total Time, Result, Actions | **API** returns only: id, sessionDisplayName, sessionType, trackId, startedAt, endedAt, endReason, state, playerCarIndex, finishingPosition. No bestLapTimeMs or totalTime in list. Use: Track (getTrackName(trackId) or trackDisplayName if API adds it), Date (startedAt), Session Type (sessionType), Result (finishingPosition → P1…), Actions. **Decision:** Keep Best Lap and Total Time columns with "—" placeholder so UI is ready; when backend adds bestLapTimeMs / totalTime to list response, display them without UI changes. |
| **SessionDetails** | getSession, getSessionLaps, getSessionSummary; 404/invalid id; real data | All mock data; no API calls; no guard on id | Add API calls, guard with isValidSessionId, 404 and error states. |
| **Charts API** | getSessionPace, getLapTrace, getLapErs, getLapSpeedTrace, getLapCorners, getSessionTyreWear | Not present | Add in Step 4; chart DTOs in types. |
| **SessionDetails charts** | Lap selector, carIndex; pace, trace, ERS, speed, corners, tyre wear from API | Mock chart data only; no lap selector | Add lap/car selection; wire all chart endpoints; loading/empty for charts. |
| **Edit display name** | Dialog in Session History; PATCH; toast; validation 64 chars | No Edit action in Session History | Add Edit button, Shadcn Dialog, validation, updateSessionDisplayName, toast, refresh. |
| **Format helpers** | — | SessionDetails shows "1:27.451" from mock | Add `formatLapTime(ms)` (and optionally formatSector) for display; can live in `api/format.ts` or `utils/format.ts`. |
| **Retry on error** | Error + retry for list/detail | Not specified | Add explicit "Retry" button/action when list or detail load fails. |
| **Env variables** | VITE_API_BASE_URL, VITE_WS_URL | Not documented in new UI | Add `.env.example` (or doc) with e.g. VITE_API_BASE_URL=http://localhost:8080, VITE_WS_URL=ws://localhost:8080/ws. |

---

## Component reusability

When implementing UI in this block, follow [README § Frontend component reusability](README.md#frontend-component-reusability-all-blocks). Use existing **DataCard**, **StatusBadge**, **TelemetryStat**, **AppLayout**; Shadcn **Dialog**, **Select**, **Table**, **Skeleton** from `components/ui`; shared **formatLapTime** and **getTrackName**. For loading/error/empty states and "Retry", prefer a shared pattern or small component reused across Session History and Session Details.

---

## Decisions (resolved)

| # | Topic | Decision |
|---|--------|----------|
| 1 | **Bell notifications** | **Option A:** Notification context/store; `notify()` helper that calls both Sonner and `context.addNotification()`. Bell shows full list of recent notifications + unread badge. |
| 2 | **Tracks location** | **`src/constants/tracks.ts`** — constants always in a separate file. |
| 3 | **Session list — Best Lap / Total Time** | **Option B:** Keep columns with "—" placeholder so UI is ready; when backend adds bestLapTimeMs / totalTime to list response, data will appear without UI changes. |
| 4 | **Pagination (Session History)** | **Add pagination in Block A:** getSessions(limit, offset); Previous/Next buttons; page state (e.g. offset, pageSize). Display "Showing X–Y" (and "of Z" if backend later returns total count). |

---

## Steps

| Step | Layer | Task | Depends on | Deliverable |
|------|--------|------|------------|-------------|
| **1** | New UI | Add API layer and Toaster. In `f1-telemetry-web-platform`: create `api/config.ts` (VITE_API_BASE_URL, VITE_WS_URL), `api/types.ts` (Session, Lap, SessionSummary, chart DTOs, HttpError — align with old UI / contract), `api/client.ts` (fetch wrapper, getSessions, getSession, getSessionLaps, getSessionSummary, getActiveSession, updateSessionDisplayName). Toast on non-404 errors. Port `api/sessionId.ts` (toSessionIdString, isValidSessionId) and tracks constant for getTrackName. Mount `<Toaster />` (sonner) in App. **Wire header Notifications (Bell):** ensure the same toast events (API errors, success, later WS connect/disconnect) are also shown in the Bell notification element — e.g. Bell opens a dropdown/popover with recent notifications (notification context or store fed when calling `toast.error`/`toast.success`/etc.); optional unread badge. See [05-ui-notifications-toast.md](../05-ui-notifications-toast.md) and [react_spa_ui_architecture.md](../../../project/react_spa_ui_architecture.md) §7.4. | — | API client usable from any page; toasts on errors; toast notifications visible in header Notifications (Bell) element. |
| **2** | New UI | Session list: replace mock in SessionHistory with getSessions({ limit, offset }). Add loading, error, and empty states; **pagination** (Previous/Next, page state; display "Showing X–Y"). Keep Best Lap and Total Time columns with "—" until backend provides data. Use session.id (string from API) in links to `/app/sessions/:id`. Route param `id` (string). | 1 | Session History shows real sessions with pagination; click opens detail. |
| **3** | New UI | Session detail (load): in SessionDetails, read session id from route params; call getSession, getSessionLaps, getSessionSummary. Replace mock summary and lap list with API data. Handle 404 and invalid session id (use isValidSessionId). | 1, 2 | Session Details shows real session, laps, summary. |
| **4** | New UI | Extend API client: add getSessionPace, getLapTrace, getLapErs, getLapSpeedTrace, getLapCorners, getSessionTyreWear (same signatures as old UI). Add chart DTOs to types if not already present. | 1 | All session-detail endpoints callable from UI. |
| **5** | New UI | Session detail (charts): in SessionDetails, add lap selector and carIndex from session; call pace, trace, ers, speed-trace, corners, tyre-wear for selected lap/car. Replace mock charts with real API data (Recharts same as now, data from API). | 3, 4 | Session Details shows pace, pedal trace, ERS, speed, corners, tyre wear. |
| **6** | New UI | Edit display name: add modal/dialog (e.g. Shadcn Dialog) in Session History. On submit call updateSessionDisplayName(sessionId, value); toast success/error; refresh list or update row. Validate length (e.g. 64 chars). | 1, 2 | Users can rename sessions from Session History. |

---

## Detailed changes

| Step | Where | Concrete changes |
|------|--------|------------------|
| 1 | `f1-telemetry-web-platform/` | **New:** `src/api/config.ts` — read `import.meta.env.VITE_API_BASE_URL`, `VITE_WS_URL`; **New:** `src/api/types.ts` — `Session`, `Lap`, `SessionSummary`, pace/trace/ers/speed-trace/corners/tyre-wear DTOs, `HttpError` (align with [REST contract §3](../../../project/rest_web_socket_api_contracts_f_1_telemetry.md)); **New:** `src/api/client.ts` — `fetch` wrapper with base URL, error parsing, toast on non-404 via `toast.error()`, methods: getSessions, getSession, getSessionLaps, getSessionSummary, getActiveSession, updateSessionDisplayName; **New:** `src/api/sessionId.ts` — `toSessionIdString`, `isValidSessionId`; **New or port:** tracks constant + `getTrackName(trackId)`; **Edit:** `src/app/App.tsx` — mount `<Toaster />` (sonner). **Notifications (Bell):** Add a notification list (e.g. React context or store) that receives the same events as toasts (when API client or pages call `toast.error()` / `toast.success()` / `toast.warning()` / `toast.info()`). In **AppLayout**, wire the header Bell button to a dropdown or popover (e.g. Shadcn Popover/DropdownMenu) that displays recent notifications from this list; optional unread badge (e.g. red dot when there are unread items). So toast notifications from the old UI (REST errors, success after PATCH, later WebSocket connect/disconnect) are visible both as Sonner popups and in the new UI’s notification element (Bell). Reference: [05-ui-notifications-toast.md](../05-ui-notifications-toast.md), [react_spa_ui_architecture.md](../../../project/react_spa_ui_architecture.md) §7.4. **Edit:** `vite.config.ts` — no change for Step 1 (global for sockjs in Step 9). |
| 2 | SessionHistory | Replace mock with `getSessions({ limit, offset })`; state: sessions, loading, error, offset, pageSize (e.g. 20). Fetch on mount and when offset changes. **Pagination:** Previous (disabled when offset === 0), Next (disabled when returned length < limit); display "Showing X–Y" (e.g. offset+1 to offset+sessions.length; add "of Z" when backend returns total). Table columns: Track (getTrackName(trackId) or trackDisplayName), Date (startedAt), Session Type, **Best Lap** ("—" or session.bestLapTimeMs when API adds it), **Total Time** ("—" or session total when API adds it), Result (finishingPosition → P1…), Actions. Links: `session.id` (string). Retry on error. |
| 3 | SessionDetails | Read `id` from `useParams()`; guard with `isValidSessionId(id)`; call getSession(id), getSessionLaps(id, carIndex), getSessionSummary(id, carIndex); replace mock summary and lap list with API data; handle 404 (not found message / redirect) and loading/error states. Add `formatLapTime(ms)` (and optionally formatSector) in utils or api for display. Retry button on load error. |
| 4 | API client + types | In `api/client.ts`: add getSessionPace(sessionUid, carIndex), getLapTrace(sessionUid, lapNum, carIndex), getLapErs, getLapSpeedTrace, getLapCorners, getSessionTyreWear; in `api/types.ts`: add chart response types if missing (pace, trace, ers, speed-trace, corners, tyre-wear) per REST contract. |
| 5 | SessionDetails | Lap selector (dropdown/select) and carIndex from session.playerCarIndex; for selected lap/car call pace, trace, ers, speed-trace, corners, tyre-wear; pass API response into existing Recharts (pace chart, pedal trace, ERS, speed, corners, tyre wear); loading/empty state when no lap selected or no data. |
| 6 | SessionHistory | Add Dialog (Shadcn) for edit; "Edit" action opens dialog with current sessionDisplayName; input max length 64; on submit: updateSessionDisplayName(sessionId, value); on success: toast.success, close dialog, refresh list or update row; on error: toast.error; validate blank and length before submit. |

---

## Detailed implementation sub-steps

Use this for a precise, step-by-step implementation order. Each sub-step is a single, verifiable task.

### Step 1 — API layer and Toaster

| # | Sub-step | Deliverable |
|---|----------|-------------|
| 1.1 | Create `src/api/config.ts`: read `import.meta.env.VITE_API_BASE_URL` and `VITE_WS_URL`; export defaults (e.g. `http://localhost:8080`, `ws://localhost:8080/ws`) when env is missing. | config.ts |
| 1.2 | Create `src/api/types.ts`: `Session`, `Lap`, `SessionSummary` (match [REST contract §3](../../../project/rest_web_socket_api_contracts_f_1_telemetry.md)); `ApiErrorBody`, `HttpError` class; chart DTOs: `PacePoint` (lapNumber, lapTimeMs), `PedalTracePoint` (distance, throttle, brake), `ErsPoint` (lapDistanceM, energyPercent), `SpeedTracePoint` (distanceM, speedKph), `LapCorner` (cornerIndex, startDistanceM, endDistanceM, apexDistanceM, entrySpeedKph, apexSpeedKph, exitSpeedKph, durationMs?), `TyreWearPoint` (lapNumber, wearFL, wearFR, wearRL, wearRR, compound?). | types.ts |
| 1.3 | Create `src/api/client.ts`: base URL from config; `requestJson<T>(path, init?)` (fetch, parse JSON, on !ok parse message and call `toast.error(message)` for status !== 404, then throw `HttpError`); implement getSessions(params?: { limit?, offset? }) building query string; getSession, getSessionLaps, getSessionSummary, getActiveSession (204 → null), updateSessionDisplayName. | client.ts (core + session methods) |
| 1.4 | Create `src/api/sessionId.ts`: port `toSessionIdString(id)`, `isValidSessionId(id)` (and optionally `isSessionUuid(id)`) from old UI. | sessionId.ts |
| 1.5 | Create **`src/constants/tracks.ts`** with TRACK_NAMES map and `getTrackName(trackId)` (port from old UI). Constants in separate file per decision. | tracks + getTrackName |
| 1.6 | In `App.tsx`: import and render `<Toaster />` (sonner), e.g. next to `<RouterProvider router={router} />`; ensure sonner styles or theme work with app (position bottom-right). | Toaster in App |
| 1.7 | **Bell:** Add notification context (e.g. `NotificationContext` with list of `{ id, type, message, timestamp, read? }` and `addNotification`, `markAsRead`). Provide at app root (e.g. in App.tsx or layout that wraps RouterProvider). | NotificationContext |
| 1.8 | **Bell:** Add helper (e.g. `notify.error(msg)`, `notify.success(msg)`) that calls both `toast.error(msg)` / `toast.success(msg)` and `context.addNotification(...)`. Wire API client to use this helper instead of raw `toast` for errors (so Bell list stays in sync). Optionally use same helper for success toasts (e.g. after PATCH). | notify helper + client wiring |
| 1.9 | **Bell:** In AppLayout, replace static Bell button with Popover (or DropdownMenu): trigger = Bell icon; content = list of recent notifications from context (newest first, cap e.g. 20); optional unread badge (red dot when there are unread items); "Mark all as read" optional. | Bell popover + unread badge |
| 1.10 | Optional: add `.env.example` with `VITE_API_BASE_URL=...` and `VITE_WS_URL=...`. | .env.example |

### Step 2 — Session list

| # | Sub-step | Deliverable |
|---|----------|-------------|
| 2.1 | SessionHistory: add state `sessions: Session[]`, `loading: boolean`, `error: string | null`, `offset: number`, `pageSize: number` (e.g. 20). | State (including pagination) |
| 2.2 | API client: getSessions(params?: { limit?, offset? }) — build query string `?limit=...&offset=...` (defaults e.g. limit 20, offset 0). | getSessions with limit/offset |
| 2.3 | On mount and when offset/pageSize change: set loading true, call getSessions({ limit: pageSize, offset }), on success set sessions and clear error, on catch set error; set loading false. | Fetch with pagination params |
| 2.4 | Render: if loading → spinner/skeleton; if error → message + "Retry" (refetch with current offset); if !loading && !error && sessions.length === 0 → empty state; otherwise table. | Loading / error / empty / table |
| 2.5 | Table: columns — Track (getTrackName(session.trackId) or session.trackDisplayName), Date (startedAt), Session Type, **Best Lap** (session.bestLapTimeMs != null ? formatLapTime(session.bestLapTimeMs) : "—"), **Total Time** (session.totalTimeMs != null ? format… : "—"), Result (finishingPosition → P1…), Actions. Use `session.id` (string) in `<Link>`. Types: Session may later get optional bestLapTimeMs/totalTimeMs from API. | Table with real fields + "—" placeholders |
| 2.6 | Pagination UI: "Previous" (disabled when offset === 0), "Next" (disabled when sessions.length < pageSize); display "Showing X–Y" (offset+1 to offset+sessions.length). If backend later returns total count, show "of Z". | Previous/Next + "Showing X–Y" |
| 2.7 | Ensure route is `sessions/:id` (already in routes.tsx). | Route param `id` |

### Step 3 — Session detail (load)

| # | Sub-step | Deliverable |
|---|----------|-------------|
| 3.1 | SessionDetails: read `id` from `useParams()`. If !id or !isValidSessionId(id) → show "Invalid session" or redirect to /app/sessions. | Guard invalid id |
| 3.2 | State: session, laps, summary, loading, error. On mount (useEffect with id): if invalid id skip; else set loading true, call getSession(id), getSessionLaps(id, carIndex), getSessionSummary(id, carIndex) (carIndex from session.playerCarIndex after getSession, or 0). | Fetch session + laps + summary |
| 3.3 | Handle 404: when getSession throws HttpError with status 404, show "Session not found" and optional link back to list; do not show toast (per 05-ui-notifications-toast). | 404 handling |
| 3.4 | Replace mock summary block with data from summary + session: Result (finishingPosition), Best Lap (formatLapTime(bestLapTimeMs)), Total Time (derive or leave —), Total Laps, etc. Subtitle: track name (getTrackName(session.trackId) or trackDisplayName), sessionType, startedAt. | Summary and header from API |
| 3.5 | Replace mock lap/sector table with laps from API (lapNumber, lapTimeMs, sector1/2/3, isInvalid, positionAtLapStart). Add format helper `formatLapTime(ms)` (and optionally formatSector) in utils or api. | Lap list from API |
| 3.6 | On load error (non-404): show inline error + "Retry" that re-runs fetch. | Retry on error |

### Step 4 — Chart endpoints and types

| # | Sub-step | Deliverable |
|---|----------|-------------|
| 4.1 | In `api/client.ts`: add getSessionPace(sessionUid, carIndex), getLapTrace(sessionUid, lapNum, carIndex), getLapErs, getLapSpeedTrace, getLapCorners, getSessionTyreWear (same signatures as old UI). Use requestJson; append ?carIndex= when carIndex !== 0. | client chart methods |
| 4.2 | In `api/types.ts`: ensure chart types exist (PacePoint, PedalTracePoint, ErsPoint, SpeedTracePoint, LapCorner, TyreWearPoint) per REST contract. TyreWearPoint: compound as string | number depending on API. | Chart types |

### Step 5 — Session detail (charts)

| # | Sub-step | Deliverable |
|---|----------|-------------|
| 5.1 | SessionDetails: add lap selector (Select or dropdown) — options from laps (lapNumber); add selectedLap state (e.g. first lap with valid time or null). carIndex = session.playerCarIndex ?? 0. | Lap selector + carIndex |
| 5.2 | When selectedLap and session id exist: fetch getSessionPace(id, carIndex), getLapTrace(id, selectedLap, carIndex), getLapErs, getLapSpeedTrace, getLapCorners, getSessionTyreWear. Use separate state or one "chart data" state; handle loading and empty (e.g. []). | Fetch chart data for selected lap |
| 5.3 | Pace chart: bind to pace API response (lapNumber, lapTimeMs → e.g. lap, time in s for axis). Replace mock lapTimeData. | Pace chart from API |
| 5.4 | Pedal trace: bind to trace API (distance, throttle, brake). Replace mock if present. | Trace chart from API |
| 5.5 | ERS chart: bind to ers API (lapDistanceM, energyPercent). | ERS chart from API |
| 5.6 | Speed trace: bind to speed-trace API (distanceM, speedKph). | Speed chart from API |
| 5.7 | Corners: use corners API for markers/labels on speed trace or separate list. | Corners from API |
| 5.8 | Tyre wear: bind to tyre-wear API (lapNumber, wearFL/FR/RL/RR, compound). Replace mock tyre stints where applicable. | Tyre wear chart from API |
| 5.9 | Position evolution: build from laps (lapNumber, positionAtLapStart). Already available from Step 3. | Position chart from laps |
| 5.10 | Loading/empty: when no lap selected or chart returns [], show placeholder or "No data" for that chart. | Loading/empty states for charts |

### Step 6 — Edit display name

| # | Sub-step | Deliverable |
|---|----------|-------------|
| 6.1 | SessionHistory: add "Edit" action per row (e.g. button or menu item "Edit name"). | Edit action |
| 6.2 | State: editDialogOpen (boolean), editSession (Session | null), editName (string). On "Edit" click: set editSession(session), editName(session.sessionDisplayName ?? ''), editDialogOpen true. | Dialog state |
| 6.3 | Shadcn Dialog: title "Edit session name"; input controlled by editName, maxLength 64; Validate: trim, not blank, length ≤ 64. On submit: call updateSessionDisplayName(editSession.id, editName.trim()); on success: toast.success('Display name updated'), close dialog, refetch getSessions() or update local sessions state; on error: toast.error (client already shows toast). | Dialog UI + submit |
| 6.4 | Cancel button closes dialog without submit. | Cancel |

---

## Block A — Checklist

Use this checklist to track progress. Mark items as done when implemented and verified.

### Step 1 — API layer and Toaster

- [ ] 1.1 — `src/api/config.ts` (VITE_API_BASE_URL, VITE_WS_URL)
- [ ] 1.2 — `src/api/types.ts` (Session, Lap, SessionSummary, HttpError, chart DTOs)
- [ ] 1.3 — `src/api/client.ts` (requestJson, getSessions, getSession, getSessionLaps, getSessionSummary, getActiveSession, updateSessionDisplayName)
- [ ] 1.4 — `src/api/sessionId.ts` (toSessionIdString, isValidSessionId)
- [ ] 1.5 — Tracks constant + getTrackName (constants/tracks.ts or api/tracks.ts)
- [ ] 1.6 — `<Toaster />` mounted in App.tsx
- [ ] 1.7 — Notification context (list + addNotification)
- [ ] 1.8 — notify helper and API client wired to push to notification list
- [ ] 1.9 — Bell in AppLayout opens popover with notification list; unread badge (optional)
- [ ] 1.10 — .env.example (optional)

### Step 2 — Session list

- [ ] 2.1 — SessionHistory state (sessions, loading, error, offset, pageSize)
- [ ] 2.2 — getSessions({ limit, offset }) in API client
- [ ] 2.3 — Fetch on mount and when offset/pageSize change
- [ ] 2.4 — Loading, error + Retry, empty state, table render
- [ ] 2.5 — Table columns: Track, Date, Session Type, Best Lap ("—" or value), Total Time ("—" or value), Result, Actions; links use session.id (string)
- [ ] 2.6 — Pagination: Previous/Next, "Showing X–Y"
- [ ] 2.7 — Route param `id` for sessions/:id

### Step 3 — Session detail (load)

- [ ] 3.1 — Guard invalid session id (isValidSessionId)
- [ ] 3.2 — getSession, getSessionLaps, getSessionSummary (carIndex from session or 0)
- [ ] 3.3 — 404 handling (not found message, no toast)
- [ ] 3.4 — Summary and header from API (track name, session type, date)
- [ ] 3.5 — Lap list from API; formatLapTime (and optional formatSector)
- [ ] 3.6 — Retry on load error

### Step 4 — Chart endpoints

- [ ] 4.1 — getSessionPace, getLapTrace, getLapErs, getLapSpeedTrace, getLapCorners, getSessionTyreWear in client
- [ ] 4.2 — Chart DTOs in types (PacePoint, PedalTracePoint, ErsPoint, SpeedTracePoint, LapCorner, TyreWearPoint)

### Step 5 — Session detail (charts)

- [ ] 5.1 — Lap selector + carIndex from session
- [ ] 5.2 — Fetch pace, trace, ers, speed-trace, corners, tyre-wear for selected lap
- [ ] 5.3 — Pace chart from API
- [ ] 5.4 — Pedal trace from API
- [ ] 5.5 — ERS chart from API
- [ ] 5.6 — Speed trace from API
- [ ] 5.7 — Corners from API
- [ ] 5.8 — Tyre wear chart from API
- [ ] 5.9 — Position evolution from laps
- [ ] 5.10 — Loading/empty states for charts

### Step 6 — Edit display name

- [ ] 6.1 — Edit action in Session History row
- [ ] 6.2 — Dialog state (open, session, name)
- [ ] 6.3 — Dialog UI + validation (trim, length ≤ 64) + updateSessionDisplayName + toast + refresh
- [ ] 6.4 — Cancel closes dialog

### Testing (optional)

- [ ] Unit tests for api/sessionId.ts (toSessionIdString, isValidSessionId)
- [ ] E2E or manual: Session History loads and opens Session Details
- [ ] E2E or manual: Session Details shows summary and charts for selected lap
- [ ] E2E or manual: Edit display name flow (open dialog, submit, list updates)

### Documentation

- [ ] NEW_UI_DOCS.md — §5 Data flow (API layer, Toaster, sessionId); §8 Known limitations updated
- [ ] react_spa_ui_architecture.md — align with new UI routes and API usage (if applicable)

### Git Commit
- [ ] Add git commit with understanding message

**How to use the checklist:** Mark completed items by changing `- [ ]` to `- [x]`. Use "Block A — Checklist" as the single place to see what is done and what remains.

---

## Testing

| Step | Scope | What to add/update |
|------|--------|--------------------|
| 1–6 | New UI | **Optional:** unit tests for `api/sessionId.ts` (toSessionIdString, isValidSessionId); **optional:** integration/e2e for Session History load and Session Details load (if project has Playwright/Cypress). No backend code change in Block A — existing backend tests remain. |

---

## Documentation updates

| Doc | Updates |
|-----|--------|
| [NEW_UI_DOCS.md](../../../NEW_UI_DOCS.md) | §5 Data flow: document API layer (config, types, client), Toaster, sessionId; §8 Known limitations: remove "No real data" for sessions/detail once done; add "Session IDs from API (string)". |
| [react_spa_ui_architecture.md](../../../project/react_spa_ui_architecture.md) | If present, align screen descriptions with new UI routes and API usage. |
