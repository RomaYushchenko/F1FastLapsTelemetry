# UI Migration Plan: Old UI → New UI (f1-telemetry-web-platform)

This document is the result of a full analysis of both UI codebases and provides a structured migration plan to integrate the new version of the UI into the project.

---

## 1. Analysis of the New UI (f1-telemetry-web-platform)

### 1.1 Structure

- **Root:** `f1-telemetry-web-platform/` — Vite + React SPA.
- **Entry:** `index.html` → `src/main.tsx` → `src/app/App.tsx` → `RouterProvider(router)`.
- **Routes:** Defined in `src/app/routes.tsx` with `createBrowserRouter`: `/`, `/login`, `/register`, `/app` (layout) with children: index (Dashboard), `live`, `live/telemetry`, `live/track-map`, `sessions`, `sessions/:id`, `comparison`, `strategy`, `settings`.
- **Key dirs:** `src/app/pages/` (one component per route), `src/app/components/` (AppLayout, DataCard, StatusBadge, TelemetryStat, figma), `src/app/components/ui/` (50+ Shadcn/Radix components), `src/styles/` (Tailwind, theme, fonts).

### 1.2 Architecture

- **Approach:** Component-based React; no global state library; routing via React Router 7 (data-router).
- **State:** Local `useState` (and similar) only; no Redux/Zustand/Context for app data.
- **Data:** All screens use **mock/static data** (arrays and objects defined in the page). No API client, no `fetch`, no env for API base.
- **Styling:** Tailwind CSS 4, theme variables in CSS (`theme.css`), Radix + class-variance (e.g. `cn()` in `utils.ts`).

### 1.3 Dependencies (package.json)

- **Runtime:** React 18 (peer), react-router 7, Radix UI (accordion, alert-dialog, avatar, checkbox, dialog, dropdown, etc.), recharts, react-hook-form, date-fns, lucide-react, sonner, motion, next-themes, vaul, etc.
- **Build:** Vite 6, @vitejs/plugin-react, @tailwindcss/vite, tailwindcss 4.
- **Not present:** No REST client (axios/fetch wrapper), no SockJS/STOMP, no TanStack Query.

### 1.4 Entry Points and Config

- **Entry:** `src/main.tsx` (createRoot, App, index.css).
- **Config:** `vite.config.ts` — React + Tailwind plugins, alias `@` → `./src`, assetsInclude for svg/csv.
- **Env:** No `.env` or `import.meta.env` usage for API/WS URLs.

---

## 2. Full List of Features — New UI

| # | Feature | Description | Where implemented |
|---|--------|-------------|-------------------|
| 1 | Landing page | Marketing landing: hero, features, “How it works”, CTA | `src/app/pages/Landing.tsx` |
| 2 | Login | Login form (email, password, remember, forgot link) — UI only | `src/app/pages/Login.tsx` |
| 3 | Register | Registration form — UI only | `src/app/pages/Register.tsx` |
| 4 | App layout | Header (logo, connection badge, notifications, user), sidebar (nav groups), main outlet | `src/app/components/AppLayout.tsx` |
| 5 | Dashboard | Overview: connection status card, last session card, quick compare CTA, previous sessions table (mock) | `src/app/pages/Dashboard.tsx` |
| 6 | Live Overview | Session info cards, leaderboard table, “Your Telemetry” panel, event timeline — mock | `src/app/pages/LiveOverview.tsx` |
| 7 | Live Telemetry | Driver/time range selects, speed, throttle/brake, RPM, gear, tyre temps, ERS & fuel charts — mock | `src/app/pages/LiveTelemetry.tsx` |
| 8 | Live Track Map | Track SVG, driver positions (static), selected driver panel, sector times — mock | `src/app/pages/LiveTrackMap.tsx` |
| 9 | Session History | Filters (search, type, sort), sessions table, pagination — mock | `src/app/pages/SessionHistory.tsx` |
| 10 | Session Details | Summary, lap time evolution, position evolution, tyre strategy, sector heatmap, event timeline — mock | `src/app/pages/SessionDetails.tsx` |
| 11 | Driver Comparison | Driver A/B and session selectors, summary cards, lap time comparison, sector delta, speed/throttle overlay, delta graph — mock | `src/app/pages/DriverComparison.tsx` |
| 12 | Strategy View | Tyre degradation, fuel consumption, ERS deployment, pit stop timeline, stint comparison — mock | `src/app/pages/StrategyView.tsx` |
| 13 | Settings | Profile, appearance (theme), telemetry prefs, alerts, UDP connection instructions, danger zone — UI only | `src/app/pages/Settings.tsx` |
| 14 | Connection status in header | Badge “Live” / “Waiting” / “No Data” (currently hardcoded “live”) | `AppLayout.tsx` |
| 15 | Reusable cards/stats | DataCard, StatusBadge, TelemetryStat | `src/app/components/` |
| 16 | Design system | Tailwind + theme + 50+ UI primitives (button, card, select, dialog, etc.) | `src/app/components/ui/`, `src/styles/` |

---

## 3. Analysis of the Old UI (ui)

### 3.1 Structure

- **Root:** `ui/` — Vite + React SPA.
- **Entry:** `src/main.tsx` → `App.tsx` (BrowserRouter, Routes, Toaster).
- **Routes:** `/` (LiveDashboard), `/sessions` (SessionListPage), `/sessions/:sessionUid` (SessionDetailPage). All under one AppLayout (Header + optional “Back to Sessions” on detail).
- **Key dirs:** `src/pages/`, `src/components/` (Header, AppLayout, Live* widgets), `src/api/` (client, config, types, sessionId), `src/ws/` (useLiveTelemetry, types), `src/charts/` (pace, speed, throttle-brake, ers, tyre-wear, types), `src/constants/` (tracks).

### 3.2 Architecture

- **Approach:** React with REST + WebSocket; routing via react-router-dom (Routes/Route).
- **State:** Local state + one main hook `useLiveTelemetry()` for live data (session, snapshot, status).
- **Data:** REST via `src/api/client.ts` (getSessions, getSession, getSessionLaps, getSessionSummary, getSessionPace, getLapTrace, getLapErs, getLapSpeedTrace, getLapCorners, getSessionTyreWear, getActiveSession, updateSessionDisplayName). WebSocket: SockJS `/ws/live`, STOMP SUBSCRIBE/UNSUBSCRIBE, SNAPSHOT / SESSION_ENDED / ERROR.
- **Env:** `VITE_API_BASE_URL`, `VITE_WS_URL` in `src/api/config.ts`.

### 3.3 Dependencies

- **Runtime:** React 19, react-router-dom 7, recharts 3, @stomp/stompjs, sockjs-client, sonner.
- **Build:** Vite 7, @vitejs/plugin-react, TypeScript, ESLint. No Tailwind (CSS variables + custom classes).

### 3.4 Entry and Config

- **Entry:** `src/main.tsx` (StrictMode, createRoot, index.css, App).
- **Config:** `vite.config.ts` — React plugin, `global: 'globalThis'` for sockjs-client.

---

## 4. Full List of Features — Old UI

| # | Feature | Description | Where implemented |
|---|--------|-------------|-------------------|
| 1 | Live dashboard | Live telemetry widgets (speed, RPM, gear, throttle, brake, DRS, ERS, lap/sector, delta); session bar; connection/session-ended messages | `LiveDashboardPage.tsx`, `useLiveTelemetry.ts`, Live*Widget components |
| 2 | Session list | GET /api/sessions, table with session, type, track, started, ended, place, state; edit display name (modal, PATCH) | `SessionListPage.tsx`, `api/client.ts` |
| 3 | Session detail | GET session, laps, summary, pace, trace, ERS, speed-trace, corners, tyre-wear; lap selector; pace chart; pedal trace; ERS chart; speed chart; corners; tyre wear chart | `SessionDetailPage.tsx`, `api/client.ts`, charts |
| 4 | Active session polling | GET /api/sessions/active; poll when 204 until session appears, then connect WebSocket | `useLiveTelemetry.ts`, `api/client.ts` |
| 5 | WebSocket live | SockJS /ws/live, STOMP SUBSCRIBE with sessionId/carIndex, SNAPSHOT/SESSION_ENDED/ERROR; toasts for connect/disconnect/errors | `useLiveTelemetry.ts`, `ws/types.ts` |
| 6 | Display name edit | Modal with input, PATCH /api/sessions/{id}, toast success/error | `SessionListPage.tsx`, `api/client.ts` |
| 7 | Error handling | HttpError, toast on non-404 API errors, retry on session list/detail | `api/client.ts`, pages |
| 8 | Session ID handling | UUID or numeric session_uid; toSessionIdString, isValidSessionId; links use session id from API | `api/sessionId.ts`, types |
| 9 | Track display names | getTrackName(trackId) fallback when trackDisplayName missing | `constants/tracks.ts` |
| 10 | Layout | Header + main; “Back to Sessions” on session detail | `AppLayout.tsx`, `Header.tsx` |

---

## 5. Compatibility Matrix

As of Block J, migration complete; all rows implemented in the new UI or N/A. See [MIGRATION_VERIFICATION.md](MIGRATION_VERIFICATION.md).

| Feature | In old UI | In new UI | Status |
|---------|-----------|-----------|--------|
| Landing / marketing page | ❌ | ✅ | Done |
| Login / Register | ❌ | ✅ | Done (UI-only until backend auth) |
| App layout with sidebar & header | ❌ | ✅ | Done |
| Dashboard (overview + recent sessions) | ❌ | ✅ | Done |
| Live overview (leaderboard, session info, events) | Partial | ✅ | Done |
| Live telemetry (charts, driver select) | ✅ | ✅ | Done |
| Live track map | ❌ | ✅ | Done |
| Session list (table, filters) | ✅ | ✅ | Done |
| Session detail (summary, laps, charts) | ✅ | ✅ | Done |
| Edit session display name | ✅ | ✅ | Done |
| Active session + WebSocket live | ✅ | ✅ | Done |
| Pace chart | ✅ | ✅ | Done |
| Pedal trace / throttle-brake chart | ✅ | ✅ | Done |
| ERS chart (lap) | ✅ | ✅ | Done |
| Speed trace chart | ✅ | ✅ | Done |
| Corners (lap) | ✅ | ✅ | Done |
| Tyre wear chart | ✅ | ✅ | Done |
| Driver comparison (multi-driver) | ❌ | ✅ | Done / N/A (optional B10) |
| Strategy view (pit stops, fuel, ERS) | ❌ | ✅ | Done |
| Settings (profile, UDP, alerts) | ❌ | ✅ | Done |
| REST API client | ✅ | ✅ | Done |
| WebSocket + STOMP | ✅ | ✅ | Done |
| Session ID (UUID / numeric) | ✅ | ✅ | Done |
| Toaster (sonner) | ✅ | ✅ | Done |

---

## 6. Gap Analysis

### A) Easily portable (minimal changes)

- **Session list table structure** — New UI already has table + filters; swap mock array for `getSessions()` and map API response. Add loading/error/empty states (old UI has these).
- **Session detail layout** — New UI has summary, lap evolution, position, tyre strategy, sector table, events; replace mock with GET session/laps/summary and existing REST endpoints.
- **Pace chart, pedal trace, ERS, speed trace, tyre wear** — Same Recharts patterns; new UI uses mock data. Port by calling same endpoints (pace, trace, ers, speed-trace, tyre-wear) and pass API data into existing chart components.
- **Session ID in URLs** — Use string `id` from API (UUID or session_uid string) in routes and links; new UI currently uses numeric `session.id` in `Link to={/app/sessions/${session.id}}` — change to `session.id` from API and ensure `sessions/:id` matches backend.

### B) Requires adaptation (logic exists, different architecture)

- **Live dashboard** — Old UI: single page with widgets + `useLiveTelemetry`. New UI: split into Live Overview, Live Telemetry, Live Track Map. Need to: (1) add `useLiveTelemetry` (or equivalent) in new UI, (2) feed snapshot into Overview and Telemetry pages, (3) keep layout/sidebar of new UI.
- **Connection status in header** — Old UI: derived from WebSocket state. New UI: hardcoded. Replace with real status from live hook/context when WebSocket is integrated.
- **Edit display name** — Old UI: inline modal in session list. New UI: no modal. Add edit modal/dialog and `updateSessionDisplayName` call; reuse API contract (PATCH, 64 chars, toast).
- **Error handling and toasts** — Old UI: toast on API errors (except 404), retry buttons. New UI: no API yet. Add central API client with toast on error and optional retry; mount Sonner in App.

### C) Missing in new UI (implement from scratch or design)

- **REST API layer** — Create `api/config.ts` (VITE_API_BASE_URL, VITE_WS_URL), `api/types.ts` (Session, Lap, SessionSummary, etc. from backend contracts), `api/client.ts` (fetch wrapper, getSessions, getSession, getSessionLaps, getSessionSummary, getSessionPace, getLapTrace, getLapErs, getLapSpeedTrace, getLapCorners, getSessionTyreWear, getActiveSession, updateSessionDisplayName). Align types with `telemetry-api-contracts` / `rest_web_socket_api_contracts_f_1_telemetry.md`.
- **WebSocket live** — SockJS + STOMP client, subscribe with sessionId/carIndex, handle SNAPSHOT/SESSION_ENDED/ERROR; optional polling for GET /api/sessions/active when no active session. Can port `useLiveTelemetry` from old UI and adapt to new UI routes/layout.
- **Session ID validation and display** — Port `sessionId.ts` (toSessionIdString, isValidSessionId) and use for links and API calls; use `trackDisplayName` / getTrackName fallback as in old UI.
- **Driver comparison (backend)** — New UI has full mock UI; backend may not have comparison endpoints. Either add new API (e.g. multi-car laps/summary) or derive from existing session/lap endpoints and implement in new UI.
- **Strategy view (backend)** — Pit stops, fuel, ERS by lap may need new endpoints or derivation from existing data; implement once API is defined.

---

## 7. Risks and Dependencies

### 7.1 Backend interaction

- **New UI** currently has **no** API or WebSocket integration. All integration work is in the new codebase.
- **Old UI** uses: REST base URL and WS URL from env; session `id` as string (UUID); carIndex from session; query params `carIndex` on laps/summary/pace/trace/ers/speed-trace/corners/tyre-wear.

### 7.2 API comparison

- **Endpoints used by old UI:**  
  GET sessions, session by id, laps, summary, pace, tyre-wear, lap trace, lap ers, lap speed-trace, lap corners; GET sessions/active; PATCH sessions/{id}.
- **New UI:** No calls yet. When adding, use the **same** endpoints and query params to avoid breaking changes. Contract: `.github/project/rest_web_socket_api_contracts_f_1_telemetry.md`.

### 7.3 Breaking changes and compatibility

- **Session id type:** Backend returns `id` (string, UUID). New UI uses numeric `id` in mock (e.g. 1, 2). Switching to real API will break links if not updated to string `id`. Use string everywhere (params, links, API).
- **Response shapes:** Old UI types (`Session`, `Lap`, `SessionSummary`, chart DTOs) match backend. New UI must use the same types (or port from old UI) to avoid runtime mismatches.
- **WebSocket:** Same contract (SockJS /ws/live, STOMP, SUBSCRIBE/UNSUBSCRIBE, SNAPSHOT/SESSION_ENDED/ERROR). Porting the old hook keeps compatibility.

### 7.4 TypeScript / interfaces

- Old UI: `api/types.ts` + `charts/types.ts` align with REST DTOs. New UI has no shared types for API. **Recommendation:** Copy or re-export from a single place (e.g. `api/types.ts`) and use in both API client and pages.

---

## 8. Quality and Technical Debt

### 8.1 Duplication

- **Charts:** Old UI has dedicated chart components (pace, speed, throttle-brake, ers, tyre-wear) with API data. New UI inlines Recharts in pages with mock data. After migration, either reuse old UI chart components in new UI or extract shared chart components in new UI to avoid duplicate chart logic.
- **Session list/detail logic:** Old UI has load/error/retry and edit-display-name logic. New UI has only layout and mock. Logic will live in new UI after migration; avoid copying large blocks without refactoring into hooks/services.

### 8.2 Old UI patterns to avoid carrying over

- **Vite `global: 'globalThis'`** — Only needed for sockjs-client; new UI will need it when adding WebSocket.
- **Single “Live” page** — New UI’s split (Overview / Telemetry / Track map) is better for navigation; keep it and feed one live source (e.g. one hook) into multiple pages.
- **Inline modal in table** — New UI can use existing Dialog/Modal from ui/ for edit display name; no need to replicate inline modal markup.

### 8.3 New UI improvements to keep as best practices

- **Unified design system** — Tailwind + theme + Radix/Shadcn: consistent spacing, colors, components. Keep and extend.
- **Clear route/layout split** — `/app` layout with sidebar and named routes is easier to extend than a flat structure.
- **Centralized UI primitives** — Button, Card, Select, Dialog, etc. in `components/ui/`; use these for any new feature.
- **Consistent chart styling** — Dark tooltip/contentStyle in Recharts; reuse in all chart integrations.

---

## 9. Prioritized Implementation Plan

### Phase 1 — Critical (blocks launch with real backend)

| Task | Description | Priority | Size | Dependencies |
|------|-------------|----------|------|--------------|
| 1.1 | Add API layer in new UI: config (VITE_API_BASE_URL, VITE_WS_URL), types (Session, Lap, SessionSummary, chart DTOs, HttpError), client (fetch wrapper, getSessions, getSession, getSessionLaps, getSessionSummary, getActiveSession, updateSessionDisplayName). Toast on error (non-404); optional retry. | P0 | M | None |
| 1.2 | Mount Toaster (sonner) in new UI App so API errors can show toasts. | P0 | S | 1.1 |
| 1.3 | Session list: replace mock in SessionHistory with getSessions(); loading, error, empty states; links use session.id (string) to `/app/sessions/:id`. | P0 | M | 1.1 |
| 1.4 | Session detail: load session by id from route params; call getSession, getSessionLaps, getSessionSummary; replace mock in SessionDetails with API data; handle 404/invalid id (sessionId validation). | P0 | M | 1.1, 1.3 |
| 1.5 | Session ID handling: port sessionId.ts (toSessionIdString, isValidSessionId); use in session list/detail and API calls. Use trackDisplayName + getTrackName fallback; port tracks constant if needed. | P0 | S | 1.1 |

### Phase 2 — Core functionality

| Task | Description | Priority | Size | Dependencies |
|------|-------------|----------|------|--------------|
| 2.1 | Add GET pace, trace, ers, speed-trace, corners, tyre-wear to API client; wire SessionDetails to these endpoints (lap selector, carIndex from session). | P1 | M | 1.4 |
| 2.2 | Replace mock charts in SessionDetails with real data (pace, lap time/position, pedal trace, ERS, speed, corners, tyre wear). | P1 | M | 2.1 |
| 2.3 | Edit session display name: add modal/dialog in SessionHistory (or session list table), call updateSessionDisplayName, toast success/error, refresh list. | P1 | S | 1.1, 1.3 |
| 2.4 | WebSocket live: port useLiveTelemetry (SockJS/STOMP, getActiveSession, SUBSCRIBE/UNSUBSCRIBE, SNAPSHOT/SESSION_ENDED/ERROR); integrate in new UI (e.g. context or hook consumed by LiveOverview, LiveTelemetry). | P1 | L | 1.1 |
| 2.5 | Live Overview: replace mock leaderboard/session/events with live data (from WebSocket snapshot + session); connection status in header from live status. | P1 | M | 2.4 |
| 2.6 | Live Telemetry: feed WebSocket snapshot into existing charts/widgets; keep driver/time range UI; optional “no active session” state with link to sessions. | P1 | M | 2.4 |
| 2.7 | Dashboard: replace mock “Previous Sessions” with getSessions() (e.g. first N); optional “last session” from first item or dedicated endpoint. | P2 | S | 1.1 |

### Phase 3 — Polish and extra features

| Task | Description | Priority | Size | Dependencies |
|------|-------------|----------|------|--------------|
| 3.1 | Live Track Map: keep static SVG for now; later: real track geometry and positions from API/WebSocket if available. | P3 | L | 2.4 |
| 3.2 | Driver Comparison: if backend adds comparison API, wire selectors and charts; else implement client-side from existing session/lap endpoints (e.g. two sessions or two carIndex). | P3 | L | 2.1 |
| 3.3 | Strategy View: if backend adds strategy/pit/fuel/ERS endpoints, wire charts; else leave mock or derive from existing data. | P3 | L | 2.1 |
| 3.4 | Settings: persist profile/telemetry prefs (requires backend); UDP instructions and danger zone can stay UI-only for now. | P3 | M | — |
| 3.5 | Login/Register: implement auth when backend supports it (no change to API layer until then). | P3 | M | — |

---

## 10. Recommendations

1. **Adopt new UI as the primary front-end** — Use the new app (f1-telemetry-web-platform) as the single SPA: better structure, design system, and routes. Migrate behavior and integrations from the old UI into it, then deprecate the old `ui/` once parity is reached.

2. **Introduce API and WebSocket in one go for “app” routes** — Add the full REST client and session ID handling first (Phase 1), then session list and session detail with real data. Add WebSocket and live flows in Phase 2 so that both “session history” and “live” experiences work against the same backend contracts without regressions.

3. **Reuse types and sessionId utilities from the old UI** — Copy or re-export `api/types.ts`, `api/sessionId.ts`, and chart DTOs from the old UI into the new one (or a shared package) so that request/response shapes stay aligned with the backend and with `rest_web_socket_api_contracts_f_1_telemetry.md`. This reduces contract drift and bugs.

4. **Use a single live data source for all live pages** — Implement one `useLiveTelemetry` (or equivalent) and feed it into Live Overview, Live Telemetry, and (later) Live Track Map. This avoids duplicate WebSocket logic and keeps connection status consistent (e.g. header badge).

5. **Keep Driver Comparison and Strategy as “UI-first” until API exists** — The new UI already has mock screens. Implement full backend integration only when the API is defined; until then, either leave mock data or implement a minimal version using existing endpoints (e.g. two sessions or two car indices) and document limitations in NEW_UI_DOCS.md.

---

---

## 9. Post-migration

As of Block J (steps 36–40):

- **Single front-end:** **f1-telemetry-web-platform** is the only UI. The old **ui/** directory has been removed.
- **Build and run:** Docker Compose, CI (`.github/workflows/build.yml`), and any root-level scripts build and serve only **f1-telemetry-web-platform**. The Docker service is named **web** (image `f1-telemetry/web:latest`).
- **Documentation:** Architecture, API contracts, migration plan, and runbooks describe the system based on the new UI. See [MIGRATION_VERIFICATION.md](MIGRATION_VERIFICATION.md) for the parity and feature checklists.

---

*Document generated from analysis of `ui/` and `f1-telemetry-web-platform/` and aligned with `.github/project/rest_web_socket_api_contracts_f_1_telemetry.md` and project architecture.*
