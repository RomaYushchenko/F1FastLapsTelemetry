# New UI Documentation (f1-telemetry-web-platform)

## 1. Architecture Overview

The new UI is a **React SPA** built with:

- **Build:** Vite 6, React 18 (peer), TypeScript (implicit via .tsx)
- **Routing:** `react-router` 7 with `createBrowserRouter` and `RouterProvider` (data-router pattern)
- **Styling:** Tailwind CSS 4 (`@tailwindcss/vite`), theme variables in `src/styles/theme.css`
- **Components:** Radix UI primitives + Shadcn-style wrappers in `src/app/components/ui/`
- **Charts:** Recharts (LineChart, BarChart, etc.)
- **State:** Local component state only; no global store (Redux/Zustand)
- **Data:** Session History and Session Details use the **REST API** (see §5). Live Overview and Live Telemetry use **WebSocket** (SockJS/STOMP) for real-time snapshot data (see §5.5). Dashboard uses REST for recent sessions. **Driver Comparison** uses real API: `getSessions()`, `getSession(sessionUid)` for participants (Driver A/B dropdowns), `getComparison(sessionUid, carIndexA, carIndexB, referenceLapNumA?, referenceLapNumB?)`; default is best lap vs best lap; two lap selectors allow comparing any lap of A vs any lap of B. Strategy View uses REST for pit stops and stints.

Entry flow: `index.html` → `src/main.tsx` → `App.tsx` → `RouterProvider(router)`.

---

## 2. Folder Structure

| Path | Description |
|------|-------------|
| `src/app/` | Application shell: routes, layout, pages, shared components |
| `src/app/App.tsx` | Root component; only renders `RouterProvider` |
| `src/app/routes.tsx` | Route definitions and lazy component imports |
| `src/app/components/AppLayout.tsx` | Main layout: header, sidebar, outlet for child routes |
| `src/app/components/` | App-specific components: DataCard, StatusBadge, TelemetryStat, figma/ImageWithFallback |
| `src/app/components/ui/` | Reusable UI primitives (button, card, select, dialog, etc.) — Shadcn/Radix based |
| `src/app/pages/` | One file per route: Landing, Login, Register, Dashboard, LiveOverview, LiveTelemetry, LiveTrackMap, SessionHistory, SessionDetails, DriverComparison, StrategyView, Settings |
| `src/styles/` | index.css, tailwind.css, theme.css, fonts.css |
| `vite.config.ts` | Vite config; alias `@` → `./src`; Tailwind + React plugins |
| `index.html` | Single HTML entry |
| `postcss.config.mjs` | PostCSS (if used by Tailwind) |
| `guidelines/` | Design/guidelines (Guidelines.md) |
| `README.md`, `ATTRIBUTIONS.md` | Project readme and attributions |

---

## 3. Components and Purpose

### Layout & shell

- **AppLayout** — Fixed header (logo, connection status badge, notifications, user), collapsible sidebar (nav groups: Overview, Live, Analysis, Settings), main content area with `<Outlet />`.

### App-level components

- **DataCard** — Card container with optional title, variant (default, live, error), noPadding.
- **StatusBadge** — Badge for connection state: active, warning, error.
- **TelemetryStat** — Label + value + optional unit; variants (performance, neutral, warning), sizes (small, medium, large).
- **figma/ImageWithFallback** — Image with fallback handling.

### UI primitives (src/app/components/ui/)

Used across pages: accordion, alert, alert-dialog, aspect-ratio, avatar, badge, breadcrumb, button, calendar, card, carousel, chart, checkbox, collapsible, command, context-menu, dialog, drawer, dropdown-menu, form, hover-card, input, input-otp, label, menubar, navigation-menu, pagination, popover, progress, radio-group, resizable, scroll-area, select, separator, sheet, skeleton, slider, sonner (toast), switch, table, tabs, textarea, toggle, toggle-group, tooltip, use-mobile, utils (cn).

---

## 4. State Management and Data Flow

- **State:** React `useState` (and local hooks) only. No global store. Notification list for the Bell is held in a small store (`src/notificationStore.ts`) with subscribe/add/markAsRead.
- **Data flow:** Top-down via props. Pages own their state (e.g. SessionHistory: sessions, loading, error, offset; SessionDetails: session, laps, summary, chart data).
- **Server data:** Session History and Session Details load from REST (see §5). Other pages still use mock data.
- **Live data:** A single WebSocket connection is managed by **LiveTelemetryProvider** (wraps `/app` routes). **useLiveTelemetry()** exposes status, session, snapshot, and optional sessionEnded/errorMessage. AppLayout shows real connection status (Live / Waiting / No Data / Disconnected / Error). Connect/disconnect/error toasts go through **notify** so they appear in the Bell.

---

## 5. Data Flow: API Layer, Toaster, Session ID

### 5.1 API layer (f1-telemetry-web-platform)

- **Location:** `src/api/` (config, types, client, sessionId, format).
- **`config.ts`:** Reads `VITE_API_BASE_URL` and `VITE_WS_URL` from env; defaults `http://localhost:8080` and `ws://localhost:8080/ws`. **getWsLiveEndpoint()** returns the SockJS URL for live telemetry (`{base}/ws/live`).
- **`types.ts`:** Session, Lap, SessionSummary, HttpError, ApiErrorBody, and chart DTOs (PacePoint, PedalTracePoint, ErsPoint, SpeedTracePoint, LapCorner, TyreWearPoint) aligned with the REST contract.
- **`client.ts`:** Base URL from config; `requestJson<T>(path, init?)` (fetch, parse JSON; on !ok parse message, call `notify.error(message)` for status !== 404, throw `HttpError`). Methods: `getSessions({ limit?, offset? })`, `getSession(id)`, `getSessionLaps(id, carIndex)`, `getSessionSummary(id, carIndex)`, `getActiveSession()`, `updateSessionDisplayName(id, name)`, and chart endpoints: `getSessionPace`, `getLapTrace`, `getLapErs`, `getLapSpeedTrace`, `getLapCorners`, `getSessionTyreWear`.
- **`sessionId.ts`:** `toSessionIdString(id)` (normalize API id to string), `isValidSessionId(id)` (UUID or numeric), `isSessionUuid(id)`. All URLs and API calls use string session IDs from the API.
- **`format.ts`:** `formatLapTime(ms)`, `formatSector(ms)` for display.

### 5.2 Toaster and notifications

- **Toaster:** `<Toaster />` (sonner) is mounted in `App.tsx` (position bottom-right). Non-404 API errors are shown as toasts via `notify.error()` from the client.
- **Notify helper:** `src/notify.ts` — `notify.error()`, `notify.success()`, `notify.warning()`, `notify.info()` each call the corresponding Sonner toast and push the same message into the notification store.
- **Bell (header):** AppLayout Bell button opens a popover with the list of recent notifications from `src/notificationStore.ts` (subscribe, addNotification, markAllAsRead). Unread badge shown when there are unread items. So REST errors and success toasts are visible both as popups and in the Bell list.

### 5.3 Session ID

- **Session IDs from API** are strings (UUID or numeric session_uid). The UI uses them in routes (`/app/sessions/:id`) and in all API calls. Use `isValidSessionId(id)` before calling session endpoints; invalid or missing id shows an inline message and link back to Session History. 404 from getSession is handled with "Session not found" and no toast (per notification policy).

### 5.4 REST endpoints in use (Block A)

- **Sessions:** `GET /api/sessions?limit=&offset=`, `GET /api/sessions/{id}`, `PATCH /api/sessions/{id}` (sessionDisplayName).
- **Laps / summary / charts:** `GET /api/sessions/{id}/laps?carIndex=`, `GET .../summary?carIndex=`, `GET .../pace`, `GET .../tyre-wear`, `GET .../laps/{lapNum}/trace`, `GET .../laps/{lapNum}/ers`, `GET .../laps/{lapNum}/speed-trace`, `GET .../laps/{lapNum}/corners`, `GET /api/sessions/active` (204 → null).

Contracts and DTOs: `.github/project/rest_web_socket_api_contracts_f_1_telemetry.md`.

### 5.5 WebSocket live telemetry (Block C)

- **Location:** `src/ws/` — types (WsSnapshotMessage, WsSessionEndedMessage, WsErrorMessage), **LiveTelemetryProvider**, **useLiveTelemetry()**.
- **Endpoint:** SockJS at `getWsLiveEndpoint()` (e.g. `http://localhost:8080/ws/live`). STOMP over SockJS; subscribe to `/topic/live/{sessionId}` and `/user/queue/errors`; send SUBSCRIBE/UNSUBSCRIBE via `/app/subscribe` and `/app/unsubscribe`.
- **Single connection:** The provider is mounted around the `/app` layout so all Live pages and the header share one STOMP client. Status: live / waiting / no-data / disconnected / error. Auto-reconnect: when disconnected or after SESSION_ENDED, the provider re-polls `getActiveSession()`; when an active session appears again, it connects.
- **Notify:** Connect, disconnect, and WebSocket error messages are sent through **notify** (info/warning/error) so they appear in the Bell and as toasts.

---

## 6. Local Run

- **Prerequisites:** Node (version per package.json; pnpm used per pnpm overrides).
- **Install:** `pnpm install` (or npm/yarn if lockfile present).
- **Dev:** `pnpm dev` (or `npm run dev`) — runs Vite dev server.
- **Build:** `pnpm build` (or `npm run build`).
- **Env:** Optional `.env` with `VITE_API_BASE_URL` and `VITE_WS_URL`; see `.env.example` in the project root. Defaults: `http://localhost:8080` and `ws://localhost:8080/ws`.

---

## 7. Conventions for Adding Features

- **New page:** Add route in `src/app/routes.tsx`, create component in `src/app/pages/`, add nav item in `AppLayout` if under `/app`.
- **New shared component:** Place in `src/app/components/` (app-specific) or `src/app/components/ui/` (generic primitive).
- **Styling:** Use Tailwind and theme variables from `theme.css` (e.g. `bg-background`, `text-text-secondary`, `#00E5FF` accent).
- **Charts:** Use Recharts (already used in LiveTelemetry, SessionDetails, DriverComparison, StrategyView); keep styling consistent (e.g. tooltip/contentStyle dark theme).
- **API calls:** When integrating backend, add a dedicated `api/` or `services/` layer (client, types, error handling, toast on error) and keep pages thin.

---

## 8. Known Limitations and TODO

- **Session list/detail:** Use real REST API (getSessions, getSession, laps, summary, pace, trace, ERS, speed-trace, corners, tyre-wear). Session IDs from API are strings (UUID or session_uid); used in routes and all API calls. Best Lap / Total Time columns show "—" until backend adds bestLapTimeMs/totalTimeMs to list response.
- **No auth:** Login/Register are UI-only (no submit to backend).
- **Live:** WebSocket (Block C) is implemented: LiveTelemetryProvider, useLiveTelemetry(), real connection status in header, snapshot in Live Overview and Live Telemetry (rolling buffer, Time Range 10/30/60 s). Tyre temperatures and fuel in snapshot may be mock or "—" until follow-up (see block-c-follow-up-live-snapshot-tyre-fuel.md).
- **Driver comparison / Strategy:** No backend endpoints for comparison or strategy; currently mock only.
- **Track map:** Static SVG; no real track geometry or live positions from API.
- **Settings:** Form state not persisted; no PATCH or user prefs API.
- **Toasts:** Sonner `<Toaster />` is mounted in App; API client uses `notify` helper so errors (non-404) and success show as toasts and in the header Bell list.
