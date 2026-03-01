# New UI Documentation (f1-telemetry-web-platform)

## 1. Architecture Overview

The new UI is a **React SPA** built with:

- **Build:** Vite 6, React 18 (peer), TypeScript (implicit via .tsx)
- **Routing:** `react-router` 7 with `createBrowserRouter` and `RouterProvider` (data-router pattern)
- **Styling:** Tailwind CSS 4 (`@tailwindcss/vite`), theme variables in `src/styles/theme.css`
- **Components:** Radix UI primitives + Shadcn-style wrappers in `src/app/components/ui/`
- **Charts:** Recharts (LineChart, BarChart, etc.)
- **State:** Local component state only; no global store (Redux/Zustand)
- **Data:** Currently **mock/static data** in every page; no API client or WebSocket

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

- **State:** React `useState` (and local hooks) only. No global store.
- **Data flow:** Top-down via props. Pages own their state (e.g. SessionHistory: searchQuery, filtered list; LiveTelemetry: isPaused, selectedDriver, timeRange).
- **Server data:** None. All lists, charts, and session data are hardcoded mock arrays/objects in the page components.
- **Live data:** Connection status in AppLayout is hardcoded (`'live'`); no WebSocket or polling.

---

## 5. API Integrations and Contracts

- **Current:** No API layer. No `fetch`/axios, no env for API base URL.
- **Expected backend (from old UI / project docs):**
  - REST: `GET /api/sessions`, `GET /api/sessions/{id}`, `GET /api/sessions/{id}/laps`, `GET /api/sessions/{id}/summary`, `GET /api/sessions/{id}/pace`, `GET /api/sessions/{id}/tyre-wear`, `GET /api/sessions/{id}/laps/{lapNum}/trace`, `GET /api/sessions/{id}/laps/{lapNum}/ers`, `GET /api/sessions/{id}/laps/{lapNum}/speed-trace`, `GET /api/sessions/{id}/laps/{lapNum}/corners`, `GET /api/sessions/active`, `PATCH /api/sessions/{id}` (sessionDisplayName).
  - WebSocket: SockJS `/ws/live`, STOMP subscribe with `sessionId`/`carIndex`; messages: SNAPSHOT, SESSION_ENDED, ERROR.

Contracts and DTOs are documented in `.github/project/rest_web_socket_api_contracts_f_1_telemetry.md` and implemented in `telemetry-api-contracts` and `telemetry-processing-api-service`.

---

## 6. Local Run

- **Prerequisites:** Node (version per package.json; pnpm used per pnpm overrides).
- **Install:** `pnpm install` (or npm/yarn if lockfile present).
- **Dev:** `pnpm dev` (or `npm run dev`) — runs Vite dev server.
- **Build:** `pnpm build` (or `npm run build`).
- **Env:** No `.env` usage yet; when API is added, use `VITE_API_BASE_URL` and `VITE_WS_URL` (see old UI `src/api/config.ts`).

---

## 7. Conventions for Adding Features

- **New page:** Add route in `src/app/routes.tsx`, create component in `src/app/pages/`, add nav item in `AppLayout` if under `/app`.
- **New shared component:** Place in `src/app/components/` (app-specific) or `src/app/components/ui/` (generic primitive).
- **Styling:** Use Tailwind and theme variables from `theme.css` (e.g. `bg-background`, `text-text-secondary`, `#00E5FF` accent).
- **Charts:** Use Recharts (already used in LiveTelemetry, SessionDetails, DriverComparison, StrategyView); keep styling consistent (e.g. tooltip/contentStyle dark theme).
- **API calls:** When integrating backend, add a dedicated `api/` or `services/` layer (client, types, error handling, toast on error) and keep pages thin.

---

## 8. Known Limitations and TODO

- **No real data:** All screens use mock data; must be replaced with REST/WebSocket integration.
- **No auth:** Login/Register are UI-only (no submit to backend).
- **Session IDs:** New UI uses numeric `id` (e.g. `session.id = 1`) in links; backend uses UUID or session_uid string — must align (use string `id` from API).
- **Live:** No WebSocket; connection status is fake; live widgets need SockJS/STOMP and active-session polling.
- **Driver comparison / Strategy:** No backend endpoints for comparison or strategy; currently mock only.
- **Track map:** Static SVG; no real track geometry or live positions from API.
- **Settings:** Form state not persisted; no PATCH or user prefs API.
- **Toasts:** Sonner is in dependencies but not mounted in `App.tsx` (only in old UI); add `<Toaster />` when adding API error/success toasts.
