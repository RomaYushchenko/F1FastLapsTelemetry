# Post-MVP and Optional Features — Not Implemented or Deferred

This document lists features that were **not implemented** in the scope of Blocks A–J (MVP migration) or were explicitly marked **optional** / **deferred to post-MVP**. For each feature we give a short description and a brief implementation plan.

**Reference:** [IMPLEMENTATION_STEP_BY_STEP.md](../../IMPLEMENTATION_STEP_BY_STEP.md), [BACKEND_FEATURES_FOR_NEW_UI.md](../../BACKEND_FEATURES_FOR_NEW_UI.md), [block-h-optional-fuel-ers-positions-auth.md](block-h-optional-fuel-ers-positions-auth.md), [NEW_UI_DOCS.md](../../NEW_UI_DOCS.md) §8.

---

## 1. Authentication and user (B11–B14)

**Status:** Deferred. To be implemented **last** in a **separate auth microservice**.

### Description

- **B11 — Authentication:** Login, register, logout, token refresh (e.g. JWT). Login and Register pages exist in the UI but do not call any backend; they are UI-only.
- **B12 — User profile:** Store and update display name, email, driver number (and optionally avatar). Settings → Profile section would call auth service.
- **B13 — User preferences:** Units (metric/imperial), theme, telemetry preferences (smoothing, update rate), alert toggles. Settings form state is not persisted; no GET/PATCH preferences API.
- **B14 — Dangerous actions:** “Delete all my session data” and “Delete account” with confirmation. In the UI, “Delete All Sessions” is **disabled** with tooltip “Available when account is linked”.

**Current state:** No user entity, no auth, no profile or preferences API in telemetry-processing-api-service. Sessions are global (no filter by user). Optional future: session owner marker for “Your session” when auth exists.

### Implementation plan (brief)

| Step | Layer | Task |
|------|--------|------|
| 1 | Auth service (new) | New microservice: register, login, refresh, logout. JWT in httpOnly cookie (per Block H decision H4). |
| 2 | Auth service | User entity; GET/PATCH profile (display name, email, driver number). |
| 3 | Auth service | Preferences entity or key-value; GET/PATCH (units, theme, telemetry options, alerts). |
| 4 | Auth service | Endpoints: “delete all my sessions” (scope by user if sessions get `owner_user_id`), “delete account” (with confirmation). |
| 5 | Main API | Optional: accept JWT (e.g. via gateway or auth service); optional `session.ownerUserId` for UX; **session list stays global** (per H3). |
| 6 | New UI | Login/Register → auth service; store token in cookies; protect `/app` routes. Settings → profile/preferences via auth service; theme persistence (e.g. localStorage + apply class on `<html>`); danger zone → auth service delete endpoints. |

**Docs:** [block-h-optional-fuel-ers-positions-auth.md](block-h-optional-fuel-ers-positions-auth.md) Steps 29–30; [BACKEND_FEATURES_FOR_NEW_UI.md](../../BACKEND_FEATURES_FOR_NEW_UI.md) §1.8.

---

## 2. Diagnostics API

**Status:** Not implemented. UI shows placeholder when endpoint is missing or returns 404.

### Description

Settings → “View Diagnostics” opens `/app/settings/diagnostics`. The Diagnostics page calls **GET /api/diagnostics**. If the endpoint is missing or returns 404, the page shows a placeholder and a link to UDP connection instructions. When implemented, the endpoint could return e.g. packet count, last received timestamp, ingest/processing health.

### Implementation plan (brief)

| Step | Layer | Task |
|------|--------|------|
| 1 | Contract | Add to [rest_web_socket_api_contracts_f_1_telemetry.md](../../.github/project/rest_web_socket_api_contracts_f_1_telemetry.md): GET /api/diagnostics — response shape (e.g. packetCount, lastReceivedAt, optional per-source stats). |
| 2 | Backend | DiagnosticsController (or existing health/diagnostics module); service that aggregates data from ingest state, Kafka consumer lag, or DB (e.g. last packet time). |
| 3 | New UI | Diagnostics page already calls GET /api/diagnostics; when 200, display returned fields instead of placeholder. |

**Doc:** [NEW_UI_DOCS.md](../../NEW_UI_DOCS.md) §8.1.

---

## 3. Session list: Best Lap / Total Time columns

**Status:** Not implemented. Columns show “—” until backend adds these fields to the list response.

### Description

Session History table has columns for Best Lap and Total Time. The REST contract for **GET /api/sessions** (list) does not currently include `bestLapTimeMs` or `totalTimeMs`. The UI shows “—” for these columns. Session detail and summary already expose best lap; the gap is only in the **list** response to avoid extra round-trips.

### Implementation plan (brief)

| Step | Layer | Task |
|------|--------|------|
| 1 | Contract | Extend SessionDto (list response) with optional `bestLapTimeMs` (Long), `totalTimeMs` (Long). Document in REST contract. |
| 2 | Backend | SessionQueryService (or repository): when building list DTOs, join or subquery session_summary (e.g. per player_car_index) to get bestLapTimeMs and totalTimeMs. Include in SessionDto for list. |
| 3 | New UI | SessionHistory: map `session.bestLapTimeMs` / `session.totalTimeMs` to table cells; format with formatLapTime(); remove “—” when present. |

**Doc:** [NEW_UI_DOCS.md](../../NEW_UI_DOCS.md) §8.2.

---

## 4. Theme persistence (Settings)

**Status:** Deferred with Block H step 30. Theme select (Dark/Light/System) has no persistence; no class/attribute on document.

### Description

Settings page has appearance / theme selection. The selected theme is not persisted (no backend, no localStorage). Block H step 30 plans: theme select → save to localStorage (key `theme`: dark | light | system), apply class on `<html>` for Tailwind dark mode. This was deferred together with auth/Settings backend.

### Implementation plan (brief)

| Step | Layer | Task |
|------|--------|------|
| 1 | New UI | On theme change: write to localStorage (`theme`); apply class (e.g. `dark` / `light`) or data attribute on `<html>` so Tailwind dark mode applies. |
| 2 | New UI | On app load: read `theme` from localStorage; set initial theme before first paint if possible to avoid flash. |
| 3 | Optional | If auth and preferences (B13) exist later: persist theme in user preferences and sync with localStorage. |

**Doc:** [block-i-supplementary-ui-actions.md](block-i-supplementary-ui-actions.md) (Theme persistence in Block H step 30).

---

## 5. Delete All Sessions (backend)

**Status:** Not implemented. Button is disabled with tooltip “Available when account is linked”.

### Description

Settings → Danger zone → “Delete All Sessions”. When auth/backend supports bulk delete (B14), the button should open an AlertDialog and on confirm call the delete endpoint. Without auth, the button is disabled. Implementation depends on B11–B14 (auth + “delete all my sessions” endpoint).

### Implementation plan (brief)

| Step | Layer | Task |
|------|--------|------|
| 1 | Auth service | Implement “delete all my sessions” endpoint (scope by current user when sessions are user-scoped, or global if product decides otherwise). |
| 2 | New UI | Enable “Delete All Sessions” when user is logged in; on confirm call auth service endpoint; show success/error toast; refresh session list or redirect. |

**Doc:** [NEW_UI_DOCS.md](../../NEW_UI_DOCS.md) §8.1; [block-h-optional-fuel-ers-positions-auth.md](block-h-optional-fuel-ers-positions-auth.md).

---

## 6. Live snapshot: tyre temperatures and fuel (Block C follow-up)

**Status:** Optional follow-up. Documented in [block-c-follow-up-live-snapshot-tyre-fuel.md](block-c-follow-up-live-snapshot-tyre-fuel.md). Checklist in that file is largely done; any remaining gaps are minor (e.g. commit, or edge cases).

### Description

Live Overview “Your Telemetry” and Live Telemetry “Tyre Temperatures” / “ERS & Fuel” can show real-time tyre temps (FL, FR, RL, RR °C) and fuel level from the WebSocket snapshot. If the snapshot or UI still shows mock or “—” in some places, this follow-up ensures backend sends `tyresSurfaceTempC` and `fuelRemainingPercent` and the UI uses them everywhere.

### Implementation plan (brief)

| Step | Layer | Task |
|------|--------|------|
| 1 | Contract | Ensure snapshot contract and WsSnapshotMessage include optional `tyresSurfaceTempC` (array of 4), `fuelRemainingPercent`. |
| 2 | Backend | SessionRuntimeState.CarSnapshot and WsSnapshotMessageBuilder: map tyre temps and fuel from CarTelemetry/CarStatus into snapshot. |
| 3 | New UI | Live Overview and Live Telemetry: use `snapshot.tyresSurfaceTempC` and `snapshot.fuelRemainingPercent`; remove any remaining mock or “—” for these fields. |

**Doc:** [block-c-follow-up-live-snapshot-tyre-fuel.md](block-c-follow-up-live-snapshot-tyre-fuel.md).

---

## 7. Summary table

| # | Feature | Type | Depends on |
|---|---------|------|------------|
| 1 | Authentication and user (B11–B14) | Deferred (post-MVP) | New auth microservice |
| 2 | Diagnostics API | Not implemented | — |
| 3 | Session list bestLapTimeMs / totalTimeMs | Not implemented | — |
| 4 | Theme persistence | Deferred | Can be done UI-only (localStorage) |
| 5 | Delete All Sessions (backend) | Not implemented | B11–B14 (auth) |
| 6 | Live snapshot tyre/fuel (follow-up) | Optional | Contract + backend snapshot + UI |

---

*This list should be updated when any of these features is implemented or when new deferred items are identified.*
