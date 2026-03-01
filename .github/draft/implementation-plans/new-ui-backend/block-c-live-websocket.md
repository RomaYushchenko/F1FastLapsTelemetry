# Block C — Live (WebSocket + existing snapshot)

Part of the [Step-by-Step Implementation Plan — New UI & Backend](../../../IMPLEMENTATION_STEP_BY_STEP.md). Steps 9–13.

**Backend:** WebSocket is already implemented (STOMP at `/ws/live`, subscribe/unsubscribe, 10 Hz snapshot, SESSION_ENDED, ERROR to `/user/queue/errors`). No backend work in this block.

**Dependency:** Block C requires **Block A step 1** (API layer: `api/config.ts` with `VITE_WS_URL`, `getActiveSession()` in client). Step 13 also needs `getSessions({ limit, offset })` from Block A.

---

## Gap analysis (plan vs codebase)

Comparison of the plan with the current **f1-telemetry-web-platform** and backend contract:

| Area | In plan | Current state | Gap |
|------|--------|----------------|-----|
| **WebSocket deps** | sockjs-client, @stomp/stompjs | Not in package.json | Add in Step 9. |
| **Vite config** | `global: 'globalThis'` for sockjs | vite.config has no define/global | Add in Step 9 (see sub-steps). |
| **useLiveTelemetry** | Port from old UI | No `src/ws/` in new UI | Create hook + types; poll getActiveSession; SUBSCRIBE/UNSUBSCRIBE; handle SNAPSHOT/SESSION_ENDED/ERROR. |
| **API/WS config** | — | New UI has no `api/` folder yet | Block A step 1 adds `VITE_WS_URL` and getActiveSession; Block C assumes they exist. |
| **AppLayout status** | From useLiveTelemetry | Hardcoded `useState('live')` | Replace with live status from hook; map status → Live/Waiting/No Data/Error. |
| **Live Overview** | Real snapshot in "Your Telemetry" | All mock (leaderboard, events, telemetry values) | Feed snapshot into "Your Telemetry"; leaderboard/events stay mock; add "no active session" state. |
| **Live Telemetry** | Snapshot → charts | All mock chart data | Feed snapshot into current-value widgets and/or rolling buffer for charts; tyre temps / fuel — see Decisions. |
| **Dashboard** | getSessions(limit: 5) | Mock "Previous Sessions" | Call getSessions({ limit: 5 }); loading/error/empty; optional "Last session" card. |
| **Snapshot contract** | speed, gear, RPM, throttle, brake, ERS, … | [REST/WS contract §4.5.1](../../../.github/project/rest_web_socket_api_contracts_f_1_telemetry.md) | Contract has **no tyre temperatures** and **no fuel** in SNAPSHOT. Backend WsSnapshotMessage has no tyre/fuel fields. |
| **Toasts / Bell** | Connect/disconnect toasts → Bell | Block A adds notify + Bell | Use same notify helper (from Block A) for WS connect/disconnect/error so they appear in Bell. |
| **Reconnect** | Auto-reconnect when session appears again | Old UI: on disconnect, no auto-reconnect | Implement auto-reconnect: when disconnected (or after SESSION_ENDED), re-poll getActiveSession; when active session appears again, connect. |
| **Live Track Map** | — | Page exists | Block C does **not** include Live Track Map (Block F). No WebSocket on track map in this block. |

---

## Additional gaps (identified during analysis)

These items are **not** fully covered in the original plan and should be added or clarified:

| # | Gap | Detail | Recommendation |
|---|-----|--------|----------------|
| 1 | **Single WebSocket connection** | New UI uses `useLiveTelemetry()` in AppLayout, LiveOverview, LiveTelemetry (and optionally Dashboard). Each hook call creates its own STOMP client and effect → **multiple connections** when several components mount. Old UI had only one consumer (LiveDashboardPage). | Use **one** live data source: either a **LiveTelemetryProvider** that holds STOMP state and exposes it via React context, and `useLiveTelemetry()` reads from context; or call the hook only in AppLayout and pass `{ status, session, snapshot }` down via context. Otherwise we get 2–4 connections per user session. |
| 2 | **Session.id = topic id** | Backend broadcasts to `/topic/live/{topicId}` where `topicId` is **public_id** (from `SessionQueryService.getTopicIdForSession`). `GET /api/sessions/active` returns `Session` with `id` (string). | Ensure **Block A** types define `Session.id` as the same string the backend uses (public_id). In useLiveTelemetry, use `session.id` when subscribing and in topic path. No extra mapping if API contract is aligned. |
| 3 | **Notify (Bell) from hook** | Plan says "toasts on connect/disconnect/error via notify (Bell)". The hook currently (old UI) uses raw `toast.*`. | Hook must call **notify** (Block A’s helper: `notify.info`, `notify.error`, `notify.warning`) when emitting connect/disconnect/error so items appear in Bell. Use `useNotify()` from context if available, else fallback to `toast.*`. |
| 4 | **Rolling buffer for charts** | **Resolved:** **Configurable** via Time Range dropdown (e.g. 10 / 30 / 60 s). Buffer max length = selected seconds × 10 (10 Hz). x-axis from `snapshot.timestamp` or relative time (s). |
| 5 | **Format helpers** | Live Overview shows lap time, delta, sector. Block A adds `formatLapTime(ms)`. | Use `formatLapTime` (and optional `formatSector`) from Block A (e.g. `api/format.ts` or `utils/format.ts`) for current lap time, best lap, delta, and sector display. |
| 6 | **WS base URL** | SockJS needs base URL; endpoint is `/ws/live`. | `WS_URL` in config = base (e.g. `http://localhost:8080`). Connect to `${WS_URL}/ws/live`. Document in .env.example. |
| 7 | **Badge for disconnected vs error** | **Resolved:** **disconnected** → show **"Disconnected"**; **error** → show "Error". StatusBadge: use appropriate variant for Disconnected (e.g. warning). |

---

## Component reusability

Follow [README § Frontend component reusability](README.md#frontend-component-reusability-all-blocks). Use existing **DataCard** (variant `live` for live panels), **StatusBadge** (Live/Waiting/No Data/Disconnected/Error), **TelemetryStat** for "Your Telemetry" values; Shadcn **Skeleton** and chart primitives. Reuse or extract a single **"No active session"** empty state component for Live Overview, Live Telemetry, and later Live Track Map (Block F) to avoid duplicated markup.

---

## Decisions (final)

| # | Topic | Decision |
|---|--------|----------|
| 1 | **Tyre temperatures** | In Block C keep "Tyre Temperatures" as **mock** so UI is ready. **Do not forget:** real data is planned in a separate follow-up — [block-c-follow-up-live-snapshot-tyre-fuel.md](block-c-follow-up-live-snapshot-tyre-fuel.md). Backend will add tyre temps to snapshot; UI will then replace mock with snapshot data. |
| 2 | **Fuel** | In Block C keep fuel in ERS & Fuel as **mock** (or "—"). **Do not forget:** same follow-up plan — [block-c-follow-up-live-snapshot-tyre-fuel.md](block-c-follow-up-live-snapshot-tyre-fuel.md). Backend will add fuel to snapshot; UI will then use real data. |
| 3 | **Reconnect** | **Auto-reconnect:** when status becomes `disconnected` (network drop or SESSION_ENDED), start polling getActiveSession again (e.g. every 4 s). When an active session appears again, connect automatically. So when the session ends and a new one starts (or connection drops and comes back), the UI reconnects without user action. |
| 4 | **Context vs hook** | Use a single hook `useLiveTelemetry()`; ensure all behavior (connect, disconnect, reconnect, SNAPSHOT/SESSION_ENDED/ERROR, cleanup) works correctly. No context unless needed later. |
| 5 | **Toasts** | Keep toasts for connect/disconnect/error and route them through Block A’s notify helper so they appear in Bell. |
| 6 | **Single WebSocket connection** | Use a **LiveTelemetryProvider** at app root (e.g. wrapping routes under `/app`) that holds the STOMP client, getActiveSession polling, and auto-reconnect logic. Expose state via React context. `useLiveTelemetry()` becomes a consumer of this context so that AppLayout, LiveOverview, LiveTelemetry (and Dashboard) all share one connection. |
| 7 | **Badge when disconnected** | When status is **disconnected** (e.g. after SESSION_ENDED or network drop), show **"Disconnected"** in the header badge (distinct from "No Data"). Use StatusBadge variant that fits (e.g. warning). |
| 8 | **Rolling buffer length** | **Configurable:** use the existing "Time Range" dropdown on Live Telemetry (e.g. 10 / 30 / 60 s). Buffer max length = selected seconds × 10 (points at 10 Hz). |
| 9 | **After SESSION_ENDED — snapshot** | **Keep the last snapshot visible** until a new session connects. Do not clear `snapshot` on SESSION_ENDED; optionally show a "Session ended" overlay or caption so the user sees that data is stale. |

---

## Steps

| Step | Layer | Task | Depends on | Deliverable |
|------|--------|------|------------|-------------|
| **9** | New UI | WebSocket live: add dependencies (sockjs-client, @stomp/stompjs). In vite.config add `global: 'globalThis'` for sockjs. Implement live telemetry logic (SockJS /ws/live, STOMP SUBSCRIBE/UNSUBSCRIBE, getActiveSession polling, SNAPSHOT/SESSION_ENDED/ERROR); **LiveTelemetryProvider** for single connection; **useLiveTelemetry()** reads from context. **Auto-reconnect:** when disconnected or after SESSION_ENDED, re-poll getActiveSession; when active session appears again, connect. Use notify for toasts (Bell). | Block A step 1 | useLiveTelemetry() + LiveTelemetryProvider; single connection; auto-reconnect; toasts in Bell. |
| **10** | New UI | AppLayout: replace hardcoded connection status with status from useLiveTelemetry (connected / waiting / no-data / error). Show Live / Waiting / No Data / Error badge; toasts on connect/disconnect/error via notify (Bell). | 9 | Header shows real connection status. |
| **11** | New UI | Live Overview: feed session and snapshot from useLiveTelemetry. Replace "Your Telemetry" panel with real snapshot (speed, gear, RPM, throttle, brake, ERS, etc.). Keep leaderboard and event timeline as mock. Show "no active session" state with link to sessions. | 9 | Live Overview shows real player telemetry; leaderboard/events mock. |
| **12** | New UI | Live Telemetry: feed WebSocket snapshot into charts/widgets (speed, throttle/brake, RPM, gear; ERS from snapshot; tyre temps and fuel mock). "No active session" state when needed. Optional: Pause/Play to freeze/resume live chart updates. | 9 | Live Telemetry shows real-time data from WebSocket. |
| **13** | New UI | Dashboard: replace mock "Previous Sessions" with getSessions({ limit: 5 }). Loading/error/empty; optional "Last session" card from first item. | Block A step 1 | Dashboard shows recent sessions from API. |

---

## Detailed changes

| Step | Where | Concrete changes |
|------|--------|------------------|
| 9 | New UI | **package.json:** add sockjs-client, @stomp/stompjs; **vite.config.ts:** add `define: { global: 'globalThis' }`; **New:** `src/ws/types.ts` (WsSnapshotMessage, WsSessionEndedMessage, WsErrorMessage, WsServerMessage); **New:** core logic (e.g. `src/ws/liveTelemetryCore.ts` or inside provider): SockJS, STOMP SUBSCRIBE/UNSUBSCRIBE, `/topic/live/${sessionId}` (use `session.id`), `/user/queue/errors`; poll getActiveSession when no active session; **auto-reconnect:** when status is disconnected or after SESSION_ENDED, re-poll getActiveSession; when active session appears, connect. **New:** `LiveTelemetryProvider` that holds this state and exposes it via context; **New:** `useLiveTelemetry()` that reads from that context (single connection for the app). Use notify (Block A) for connect/disconnect/error so Bell shows them. Port behaviour from [ui/src/ws/useLiveTelemetry.ts](../../../ui/src/ws/useLiveTelemetry.ts). |
| 10 | AppLayout | Use useLiveTelemetry(); map status → badge: connected → Live, loading-active-session/connecting → Waiting, no-active-session → No Data, **disconnected → Disconnected**, error → Error. Toasts for connect/disconnect/error via notify (Block A) so they appear in Bell. |
| 11 | LiveOverview | useLiveTelemetry(); "Your Telemetry" values from snapshot (speedKph, gear, engineRpm, throttle, brake, ersEnergyPercent, etc.); leaderboard and event timeline mock or empty; when status no-active-session or session null → "No active session" + link to /app/sessions. |
| 12 | LiveTelemetry | useLiveTelemetry(); snapshot → current values and **configurable** rolling buffer (Time Range dropdown: 10/30/60 s) for charts (speed, throttle, brake, RPM, gear, ERS); tyre temps and fuel mock. "No active session" when session null. Optional: isPaused to stop appending to buffer. **After SESSION_ENDED:** keep last snapshot visible (do not clear); optional "Session ended" overlay. |
| 13 | Dashboard | getSessions({ limit: 5, offset: 0 }); loading/error/empty states; list or cards; optional "Last session" card using first item. |

---

## Detailed implementation sub-steps

Use this for a precise, step-by-step implementation order. Each sub-step is a single, verifiable task.

### Step 9 — WebSocket and useLiveTelemetry (single connection via Provider)

| # | Sub-step | Deliverable |
|---|----------|-------------|
| 9.1 | Add dependencies: `sockjs-client`, `@stomp/stompjs` (versions compatible with React 18). | package.json |
| 9.2 | In `vite.config.ts` add `define: { global: 'globalThis' }` so SockJS works in browser (or equivalent per [sockjs-client docs](https://github.com/sockjs/sockjs-client)). | vite.config.ts |
| 9.3 | Create `src/ws/types.ts`: interfaces WsSnapshotMessage (type, timestamp, speedKph, gear, engineRpm, throttle, brake, drs, currentLap, currentSector, currentLapTimeMs, bestLapTimeMs, deltaMs, ersEnergyPercent, ersDeployActive, etc. per contract §4.5.1), WsSessionEndedMessage, WsErrorMessage, WsServerMessage union. | src/ws/types.ts |
| 9.4 | Ensure API config exposes WS URL: in `src/api/config.ts` (Block A) export `WS_URL` (base URL, e.g. `http://localhost:8080`). SockJS endpoint = `${WS_URL}/ws/live`. If Block A did not add it, add it in this step. | config has WS_URL |
| 9.5 | Create core live telemetry logic (e.g. inside a custom hook used only by the provider, or `src/ws/liveTelemetryState.ts`): (1) Build STOMP client with SockJS(`${WS_URL}/ws/live`), reconnectDelay 0, no debug in prod. (2) State: status, session, snapshot, sessionEnded, errorMessage, connectionMessage. (3) On mount: getActiveSession(); if null, set no-active-session and start polling (e.g. 4 s); when session appears, connect. (4) On connect: subscribe to `/topic/live/${session.id}` (session.id = topic id from API), `/user/queue/errors`; SNAPSHOT → set snapshot; SESSION_ENDED → set sessionEnded, status disconnected, **notify**; **do not clear snapshot** (keep last frame visible per Decision 9); ERROR → set error, **notify**. (5) Publish SUBSCRIBE to `/app/subscribe` with { type: 'SUBSCRIBE', sessionId: session.id, carIndex: session.playerCarIndex ?? 0 }. (6) On disconnect/error: set status, **notify**; **auto-reconnect:** when status is disconnected (or after SESSION_ENDED), start polling getActiveSession again; when active session appears, connect. (7) Cleanup: UNSUBSCRIBE, unsubscribe topics, deactivate client, clear poll timer. Use **notify** (from Block A context) for connect/disconnect/error so Bell shows them; fallback to toast if notify not available. | Core logic |
| 9.6 | Create `LiveTelemetryProvider`: holds the above state and effect (single STOMP connection). Mount it so it wraps the part of the tree that contains AppLayout and all Live pages (e.g. in `routes.tsx` wrap the route element that uses AppLayout with `<LiveTelemetryProvider>`). Export context type and provider component. | LiveTelemetryProvider |
| 9.7 | Create `useLiveTelemetry()`: hook that returns `useContext(LiveTelemetryContext)`. So AppLayout, LiveOverview, LiveTelemetry all get the same state from one connection. | useLiveTelemetry.ts |
| 9.8 | Export useLiveTelemetry, LiveTelemetryProvider, and types from `src/ws/` (e.g. index.ts). | Hook, provider, types usable |

### Step 10 — AppLayout connection status

| # | Sub-step | Deliverable |
|---|----------|-------------|
| 10.1 | In AppLayout, call useLiveTelemetry() and read status (and optional session/snapshot if needed for header). | useLiveTelemetry in AppLayout |
| 10.2 | Replace hardcoded connectionStatus state with mapping: status connected → Live, loading-active-session or connecting → Waiting, no-active-session → No Data, **disconnected → Disconnected**, error → Error. | Real status in header |
| 10.3 | Render badge and icon from mapped status (Live / Waiting / No Data / **Disconnected** / Error). Use appropriate variant for Disconnected (e.g. warning). | Badge reflects real state |
| 10.4 | Use Block A notify helper for toasts on connect, disconnect, and WebSocket error so they appear in Bell. If hook currently uses raw toast, refactor hook to accept optional notify callback or use a shared notify from context. | Toasts + Bell for WS events |

### Step 11 — Live Overview

| # | Sub-step | Deliverable |
|---|----------|-------------|
| 11.1 | In LiveOverview, call useLiveTelemetry(); get session, snapshot, status. | useLiveTelemetry in LiveOverview |
| 11.2 | When status is no-active-session or session is null: show "No active session" message and link to /app/sessions; hide or collapse "Your Telemetry" and session info. | No active session state |
| 11.3 | When session and snapshot exist: replace "Your Telemetry" panel with values from snapshot: speed (speedKph), gear, RPM (engineRpm), throttle, brake, ERS (ersEnergyPercent), optional delta (deltaMs), current lap/sector (currentLap, currentSector). Use format helpers (e.g. formatLapTime) if needed. | Your Telemetry from snapshot |
| 11.4 | Session info (track, lap, session time, weather): use session when available; track from getTrackName(session.trackId) or session; lap from snapshot.currentLap. Keep placeholders for data not in API/snapshot. | Session info from session/snapshot |
| 11.5 | Leaderboard and Event timeline: keep mock data or empty placeholders (real data in Block E). | Leaderboard/events mock |

### Step 12 — Live Telemetry

| # | Sub-step | Deliverable |
|---|----------|-------------|
| 12.1 | In LiveTelemetry, call useLiveTelemetry(); get session, snapshot, status. | useLiveTelemetry in LiveTelemetry |
| 12.2 | When no active session: show "No active session" state (message + link to sessions); hide or disable charts. | No active session state |
| 12.3 | When snapshot exists: feed snapshot into current-value displays and a **configurable rolling buffer** for charts. Buffer length from **Time Range** dropdown (10 / 30 / 60 s → 100 / 300 / 600 points at 10 Hz); x-axis from snapshot timestamp or relative time (s). Map: speedKph → speed chart, throttle/brake → throttle/brake chart, engineRpm → RPM, gear → gear chart, ersEnergyPercent → ERS. | Snapshot → charts/widgets |
| 12.4 | Tyre temperatures and fuel: keep mock data (snapshot has no tyre/fuel yet). Show placeholder or "—" with tooltip "Live tyre/fuel coming soon". **Follow-up:** replace with real data per [block-c-follow-up-live-snapshot-tyre-fuel.md](block-c-follow-up-live-snapshot-tyre-fuel.md). | Tyre/fuel mock; follow-up plan exists |
| 12.5 | **SESSION_ENDED:** do **not** clear snapshot; keep last snapshot visible. Optional: show "Session ended" overlay or caption so user sees data is stale. | Last frame stays visible |
| 12.6 | Optional: Pause/Play button sets local isPaused; when paused, do not append new snapshots to chart buffer (charts freeze). | Pause/Play for live charts |

### Step 13 — Dashboard recent sessions

| # | Sub-step | Deliverable |
|---|----------|-------------|
| 13.1 | In Dashboard, add state: sessions (Session[]), loading, error. Call getSessions({ limit: 5, offset: 0 }) on mount (or via useEffect). | getSessions(5) on Dashboard |
| 13.2 | Handle loading (skeleton/spinner), error (message + Retry), empty (no sessions message). | Loading/error/empty |
| 13.3 | Replace mock "Previous Sessions" list with real sessions; link each to `/app/sessions/${session.id}`. | Real session list |
| 13.4 | Optional: "Last session" card or highlight using first item from list. | Optional last-session card |

---

## Precise implementation order (what to do in sequence)

Use this list to implement Block C in a strict order. Each line is one concrete task.

1. **Step 9 — WebSocket and Provider**  
   1.1. Add sockjs-client and @stomp/stompjs to package.json; run install.  
   1.2. Add `define: { global: 'globalThis' }` to vite.config.ts.  
   1.3. Create src/ws/types.ts (WsSnapshotMessage, WsSessionEndedMessage, WsErrorMessage, WsServerMessage).  
   1.4. Ensure src/api/config.ts exports WS_URL (Block A or add here).  
   1.5. Implement core live telemetry logic (state + effect: getActiveSession, poll when no session, STOMP connect, SUBSCRIBE with session.id, SNAPSHOT/SESSION_ENDED/ERROR, disconnect, auto-reconnect via poll, cleanup). Use notify for toasts.  
   1.6. Create LiveTelemetryProvider that holds this logic and provides state via context.  
   1.7. Create useLiveTelemetry() that returns useContext(LiveTelemetryContext).  
   1.8. Mount LiveTelemetryProvider in app (e.g. in routes or App.tsx so it wraps /app layout).  
   1.9. Export provider, hook, and types from src/ws/.

2. **Step 10 — AppLayout**  
   2.1. In AppLayout, call useLiveTelemetry(); read status (and session/snapshot if needed).  
   2.2. Map status → display: connected → Live, loading-active-session | connecting → Waiting, no-active-session → No Data, **disconnected → Disconnected**, error → Error.  
   2.3. Render header badge and icon from mapped status (Live / Waiting / No Data / Disconnected / Error).  
   2.4. Ensure WS connect/disconnect/error go through notify so Bell shows them (already in 1.5 if hook uses notify).

3. **Step 11 — Live Overview**  
   3.1. In LiveOverview, call useLiveTelemetry(); get session, snapshot, status.  
   3.2. No active session: show "No active session" + link to /app/sessions; hide/collapse Your Telemetry.  
   3.3. Your Telemetry panel: from snapshot — speedKph, gear, engineRpm, throttle, brake, ersEnergyPercent, deltaMs, currentLap, currentSector; use formatLapTime where needed.  
   3.4. Session info: track (getTrackName(session.trackId)), lap (snapshot.currentLap), session time/weather placeholders.  
   3.5. Leaderboard and Event timeline: keep mock or empty (Block E later).

4. **Step 12 — Live Telemetry**  
   4.1. In LiveTelemetry, call useLiveTelemetry(); get session, snapshot, status.  
   4.2. No active session: show "No active session" + link; hide/disable charts.  
   4.3. **Configurable rolling buffer:** Time Range dropdown (10 / 30 / 60 s); buffer max = selected seconds × 10. On each SNAPSHOT append point; feed buffer to Recharts.  
   4.4. Tyre temps and fuel: mock or "—" with tooltip "Live tyre/fuel coming soon".  
   4.5. After SESSION_ENDED: keep last snapshot visible (do not clear); optional "Session ended" overlay.  
   4.6. Optional: Pause/Play stops appending to buffer so charts freeze.

5. **Step 13 — Dashboard**  
   5.1. Call getSessions({ limit: 5, offset: 0 }) on mount.  
   5.2. Loading / error / empty states.  
   5.3. Replace mock "Previous Sessions" with real list; links to /app/sessions/:id.  
   5.4. Optional: "Last session" card from sessions[0].

6. **Documentation and follow-up**  
   6.1. Update NEW_UI_DOCS.md §4/§5/§8: WebSocket, useLiveTelemetry, remove "connection status is fake".  
   6.2. Remember follow-up: [block-c-follow-up-live-snapshot-tyre-fuel.md](block-c-follow-up-live-snapshot-tyre-fuel.md) for real tyre/fuel in snapshot.

---

## Implementation checklist

**How to use:** Mark completed items with `[x]`. Use the **Progress summary** to see at a glance what is done vs left to do.

### Progress summary

| Step | Description | Done | Total | Status |
|------|--------------|------|-------|--------|
| 9 | WebSocket + Provider + useLiveTelemetry | 0 | 8 | ⬜ Not started |
| 10 | AppLayout connection status | 0 | 4 | ⬜ Not started |
| 11 | Live Overview | 0 | 5 | ⬜ Not started |
| 12 | Live Telemetry | 0 | 6 | ⬜ Not started |
| 13 | Dashboard recent sessions | 0 | 4 | ⬜ Not started |
| — | Documentation & follow-up | 0 | 2 | ⬜ Not started |
| **Total** | | **0** | **29** | |

*(Update the numbers in "Done" and "Status" as you complete items below.)*

---

### Step 9 — WebSocket and useLiveTelemetry (single connection)

- [ ] 9.1 Add sockjs-client and @stomp/stompjs to package.json; run install
- [ ] 9.2 Add `define: { global: 'globalThis' }` to vite.config.ts
- [ ] 9.3 Create src/ws/types.ts (WsSnapshotMessage, WsSessionEndedMessage, WsErrorMessage, WsServerMessage)
- [ ] 9.4 Ensure WS_URL in api/config.ts (Block A or add here)
- [ ] 9.5 Implement core live telemetry logic (state + STOMP, getActiveSession, poll, SUBSCRIBE with session.id, SNAPSHOT/SESSION_ENDED/ERROR, auto-reconnect, cleanup; use notify for toasts)
- [ ] 9.6 Create LiveTelemetryProvider; wrap app with it
- [ ] 9.7 Create useLiveTelemetry() that reads from context
- [ ] 9.8 Export provider, hook, and types from src/ws/

### Step 10 — AppLayout connection status

- [ ] 10.1 Use useLiveTelemetry() in AppLayout; read status
- [ ] 10.2 Map hook status to display (live / waiting / no-data / error)
- [ ] 10.3 Render badge and icon from real status
- [ ] 10.4 WS connect/disconnect/error go through notify (Bell) — covered in 9.5 if hook uses notify

### Step 11 — Live Overview

- [ ] 11.1 Use useLiveTelemetry() in LiveOverview; get session, snapshot, status
- [ ] 11.2 "No active session" state with link to /app/sessions
- [ ] 11.3 "Your Telemetry" panel from snapshot (speed, gear, RPM, throttle, brake, ERS, delta, lap, sector; formatLapTime)
- [ ] 11.4 Session info from session/snapshot (track, lap, placeholders)
- [ ] 11.5 Leaderboard and Event timeline left as mock/empty

### Step 12 — Live Telemetry

- [ ] 12.1 Use useLiveTelemetry() in LiveTelemetry; get session, snapshot, status
- [ ] 12.2 "No active session" state when needed
- [ ] 12.3 Configurable rolling buffer (Time Range 10/30/60 s) → speed, throttle/brake, RPM, gear, ERS charts
- [ ] 12.4 Tyre temps and fuel mock or "—" with tooltip
- [ ] 12.5 After SESSION_ENDED: keep last snapshot visible; optional "Session ended" overlay
- [ ] 12.6 Optional: Pause/Play freezes chart updates

### Step 13 — Dashboard recent sessions

- [ ] 13.1 Dashboard calls getSessions({ limit: 5, offset: 0 })
- [ ] 13.2 Loading, error, empty states
- [ ] 13.3 "Previous Sessions" list from API with links to session detail
- [ ] 13.4 Optional: "Last session" card from first item

### Documentation and follow-up

- [ ] NEW_UI_DOCS.md §4/§5/§8: document WebSocket, useLiveTelemetry; remove "connection status is fake"
- [ ] **Follow-up (later):** Real tyre/fuel per [block-c-follow-up-live-snapshot-tyre-fuel.md](block-c-follow-up-live-snapshot-tyre-fuel.md)

### Optional

- [ ] UI unit or e2e tests for useLiveTelemetry and live pages

---

## Follow-up (do not forget)

After Block C, **tyre temperatures** and **fuel** in the Live UI must be switched from mock to real snapshot data. A dedicated plan keeps this from being forgotten:

- **[block-c-follow-up-live-snapshot-tyre-fuel.md](block-c-follow-up-live-snapshot-tyre-fuel.md)** — Backend: extend WebSocket snapshot contract and WsSnapshotMessage with tyre temps (e.g. tyresSurfaceTempC) and fuelRemainingPercent; New UI: use these fields in Live Overview and Live Telemetry instead of mock.

---

## Testing

Optional UI unit/e2e for useLiveTelemetry and live pages.

---

## Documentation updates

[NEW_UI_DOCS.md](../../../NEW_UI_DOCS.md) — §4/§5/§8: document WebSocket, useLiveTelemetry; remove "No WebSocket" / "connection status is fake".
