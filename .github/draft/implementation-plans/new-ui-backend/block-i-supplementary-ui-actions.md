# Block I — Supplementary UI actions (export, user menu, settings)

Part of the [Step-by-Step Implementation Plan — New UI & Backend](../../../IMPLEMENTATION_STEP_BY_STEP.md). Steps 31–35.

This block covers **buttons and elements** in the new UI that should perform actions but were not fully specified in blocks A–H. See [MISSING_UI_ACTIONS_ANALYSIS.md](MISSING_UI_ACTIONS_ANALYSIS.md) for the analysis.

## Component reusability

Follow [README § Frontend component reusability](README.md#frontend-component-reusability-all-blocks). Use Shadcn **DropdownMenu** (user menu), **AlertDialog** (Delete All Sessions), **Tooltip** (disabled state); do not add custom dropdown or dialog primitives. Reuse the same toast and API client patterns for export and Test Connection.

---

## Gap analysis (plan vs current codebase)

Comparison of the original Block I plan with the current UI and API contracts:

| Area | In plan | Current state | Gap added to plan |
|------|--------|---------------|-------------------|
| **Export (Step 31)** | "Export as CSV or JSON from loaded data" | SessionDetails has "Export Data" button with no handler; data is mock. | **Decision I1:** Backend GET /api/sessions/{id}/export?format=csv\|json; New UI **dropdown** ("Export as JSON", "Export as CSV"); on select call API, trigger download. Payload: summary + laps; file name session-{id}-export.{csv\|json}. |
| **User menu (Step 32)** | "Dropdown: Settings, optional Profile/Logout" | AppLayout has plain `<button>` (avatar + "Driver"); no DropdownMenu. | Specify: when auth is not implemented, show only "Settings". When auth exists (Block H): add "Profile" (link to /app/settings or profile page), "Logout" (call logout, redirect). Keyboard: Arrow keys, Enter, Escape (Shadcn DropdownMenu supports this). |
| **Test Connection (Step 33)** | "(a) backend health/diagnostic or (b) client-side" | Settings has "Test Connection" button with no onClick. Backend has `/actuator/health`. | **Decision I2:** Use **existing GET /actuator/health** (same API base URL). Toast success/error. Document in NEW_UI_DOCS §8. |
| **View Diagnostics (Step 34)** | "Modal or page; if no backend, placeholder + link" | "View Diagnostics" button has no onClick. No GET /api/diagnostics in contract. | **Decision I3:** **Separate page** /app/settings/diagnostics. Add route and page; content from GET /api/diagnostics or placeholder + link. Optional backend contract if added. |
| **Delete All Sessions (Step 35)** | "Confirm dialog; call DELETE (bulk) or B14" | "Delete All Sessions" has no handler or AlertDialog. Block H B14 = auth "delete all my sessions". | **Decision I4:** When **no backend**: button **disabled** + Tooltip "Available when account is linked". When backend exists: AlertDialog on click, on confirm call DELETE. Cross-ref Block H step 30. |
| **Forgot password (optional)** | "Optional: Forgot password page + backend" | Login has "Forgot password?" linking to "#". | Add optional sub-step: Forgot password page (email input) + backend endpoint; link from Login. Implement when auth (Block H) is done. |
| **Theme persistence** | — | Settings Theme select (Dark/Light/System) has no persistence; no class/attribute on document. | **In Block H step 30** (with other Settings). Not in Block I. See block-h-optional-fuel-ers-positions-auth.md. |
| **NEW_UI_DOCS §8** | "Document Export, Test Connection, Diagnostics, Delete All" | NEW_UI_DOCS.md §8 is "Known Limitations and TODO"; no subsection for these features. | Add or expand §8: "Supplementary UI actions" — document Export (format, source), Test Connection (chosen behaviour), View Diagnostics (and optional backend), Delete All Sessions (when backend exists). |

---

## Steps

| Step | Layer | Task | Depends on | Deliverable |
|------|--------|------|------------|-------------|
| **31** | New UI + Backend | **Session Export.** Backend: GET /api/sessions/{id}/export?format=csv\|json. New UI: "Export Data" **dropdown** ("Export as JSON", "Export as CSV"); on select call backend, trigger file download. | 3, 4 | User can download session data (JSON or CSV) from Session Details. |
| **32** | New UI | **User menu in header.** In AppLayout, replace the plain user button with a dropdown (e.g. Shadcn DropdownMenu): "Settings" link to /app/settings, optional "Profile" and "Logout" when auth is implemented (Block H). Show same avatar and label; keyboard accessible. | 1 | Header has a working user menu with at least Settings. |
| **33** | New UI | **Settings: Test Connection.** Wire "Test Connection": call **existing** backend endpoint GET /actuator/health (same API base URL). Toast success on 2xx, toast error on failure. Document in NEW_UI_DOCS §8. | 1 | Test Connection button gives user feedback. |
| **34** | New UI | **Settings: View Diagnostics.** Wire "View Diagnostics": navigate to **separate page** /app/settings/diagnostics. Page shows connection/ingest status if backend exposes GET /api/diagnostics; else placeholder + link to UDP instructions. Optional backend: GET /api/diagnostics. | 1, (33) | View Diagnostics opens diagnostics page. |
| **35** | New UI + optional Backend | **Settings: Delete All Sessions.** When backend/B14 exists: onClick opens AlertDialog; on confirm call DELETE (bulk/B14), toast success/error. When no backend: button **disabled** with Tooltip "Available when account is linked". | 1, (29) | Danger zone "Delete All Sessions" wired with confirm (when API) or disabled (when not). |

**Detailed sub-steps and an implementation checklist** are in the sections below. Use the checklist to mark completed items.

Optional (can be folded into Block H or later):

- **Forgot password:** Login page "Forgot password?" link → Forgot password page (email input) → backend endpoint; implement when auth (B11–B14) is done.
- **Theme persistence:** Implemented in **Block H step 30** (with other Settings); not in Block I.

---

## Detailed changes

| Step | Where | Concrete changes |
|------|--------|------------------|
| 31 | Backend + SessionDetails | **Backend:** GET /api/sessions/{id}/export?format=csv\|json (contract + controller + service); response body = CSV or JSON file; Content-Disposition attachment. **New UI:** "Export Data" **dropdown** ("Export as JSON", "Export as CSV"); on select call API, trigger download from response blob. File name: session-{id}-export.{csv\|json}. |
| 32 | AppLayout | Replace plain `<button>` (avatar + "Driver") with DropdownMenu: trigger = same avatar + label; items: "Settings" (Link to /app/settings); when auth (Block H): "Profile", "Logout" (logout + redirect). Use Shadcn DropdownMenu; ensure keyboard accessible. |
| 33 | Settings | "Test Connection" onClick: fetch **GET /actuator/health** (same API base URL), toast.success on 2xx, toast.error on failure. Document in NEW_UI_DOCS.md §8. |
| 34 | Settings + routes | "View Diagnostics" onClick: **navigate to /app/settings/diagnostics** (separate page). Add route and Diagnostics page. Page: if GET /api/diagnostics exists, show packet count, last received, etc.; else placeholder + link to UDP instructions. |
| 35 | Settings | "Delete All Sessions": when backend/B14 exists — onClick opens AlertDialog, on confirm call DELETE and toast. When no backend — button **disabled** with Tooltip "Available when account is linked". |

---

## Testing

| Step | Scope | What to add/update |
|------|--------|--------------------|
| 31–35 | New UI | Manual: export downloads a file; user menu opens and links work; Test Connection and View Diagnostics show expected UI; Delete All Sessions shows confirm and (when API exists) triggers delete. Optional e2e for export and user menu. |

---

## Documentation updates

| Doc | Updates |
|-----|--------|
| [NEW_UI_DOCS.md](../../../NEW_UI_DOCS.md) | §8: document Export Data (format, source), Test Connection behaviour, View Diagnostics (and optional backend), Delete All Sessions (when backend exists). |
| [rest_web_socket_api_contracts_f_1_telemetry.md](../../../project/rest_web_socket_api_contracts_f_1_telemetry.md) | If backend adds export or diagnostics: document GET /api/sessions/{id}/export, GET /api/diagnostics (or similar). |

---

## Detailed implementation sub-steps

Use this for a precise, step-by-step implementation order. Each sub-step is a single, verifiable task.

### Step 31 — Session Export (Backend + Session Details)

| # | Sub-step | Deliverable |
|---|----------|-------------|
| 31.1 | **Backend contract.** Add GET /api/sessions/{id}/export?format=csv\|json. Query param `format`: `csv` or `json`. Response: 200 with body = CSV or JSON; header Content-Disposition: attachment; filename=session-{id}-export.{csv\|json}. 404 if session not found. | Contract § export |
| 31.2 | **Backend payload.** Export = session summary (from session + summary data) + laps array (from getSessionLaps). CSV: one row per lap (lapNumber, time, position, …). JSON: `{ summary: {...}, laps: [...] }`. | Payload shape |
| 31.3 | **Backend implementation.** Controller (e.g. SessionController): GET export, validate format; service builds payload, serializes to CSV or JSON; return with attachment header. Thin controller; logic in service or dedicated ExportService. | Export endpoint |
| 31.4 | **New UI: dropdown.** SessionDetails: replace single "Export Data" button with **DropdownMenu** trigger "Export Data"; items "Export as JSON", "Export as CSV". On select: call GET /api/sessions/{id}/export?format=json or format=csv (use session id from route). | Export dropdown |
| 31.5 | **New UI: download.** On API response: create blob from response, temporary `<a download>` with filename session-{id}-export.{csv\|json}, trigger click. Toast.success on success; toast.error on API error. | Working export download |

### Step 32 — User menu in header (AppLayout)

| # | Sub-step | Deliverable |
|---|----------|-------------|
| 32.1 | **Replace button with DropdownMenu.** In AppLayout header: use Shadcn `DropdownMenu` (from components/ui/dropdown-menu). Trigger = same avatar + "Driver" label. Items: "Settings" (Link to /app/settings). | Dropdown with Settings |
| 32.2 | **Optional: Profile and Logout when auth exists.** When Block H auth is implemented: add dropdown item "Profile" (Link to /app/settings or /app/profile) and "Logout" (onClick: call auth logout, clear token, redirect to / or /login). When auth not implemented: only Settings. Use a flag or auth context to branch. | Profile + Logout when auth |
| 32.3 | **Accessibility.** Ensure DropdownMenu trigger and items are keyboard accessible (Arrow keys, Enter to open/select, Escape to close). Shadcn DropdownMenu supports this by default; verify focus management and aria labels if needed. | Keyboard accessible |

### Step 33 — Settings: Test Connection

| # | Sub-step | Deliverable |
|---|----------|-------------|
| 33.1 | **Use existing endpoint.** Call **GET /actuator/health** (same API base URL as other REST calls, e.g. VITE_API_BASE_URL + '/actuator/health'). Document in NEW_UI_DOCS §8 that Test Connection checks backend reachability via actuator. | Decision documented |
| 33.2 | **Wire button.** Settings "Test Connection" onClick: fetch GET /actuator/health. On 2xx: toast.success("Connection successful" or similar). On error or non-2xx: toast.error("Connection failed" + short reason). Loading state optional (disable button or spinner while fetching). | Test Connection gives feedback |

### Step 34 — Settings: View Diagnostics

| # | Sub-step | Deliverable |
|---|----------|-------------|
| 34.1 | **Separate page.** Add route /app/settings/diagnostics and a Diagnostics page component. "View Diagnostics" button in Settings: onClick **navigate** to /app/settings/diagnostics. | Route + page |
| 34.2 | **Page content.** Diagnostics page: if backend exposes GET /api/diagnostics (or similar), fetch and display (e.g. packet count, last received timestamp, status). If no endpoint or 404: show placeholder message ("Diagnostics available when backend supports it") + link to UDP instructions (same as in Settings UDP section). | View Diagnostics page content |
| 34.3 | **Optional backend contract.** If backend adds diagnostics: document GET /api/diagnostics response (e.g. `{ packetCount, lastReceivedAt, ingestStatus }`) in rest_web_socket_api_contracts. | Contract if backend added |

### Step 35 — Settings: Delete All Sessions

| # | Sub-step | Deliverable |
|---|----------|-------------|
| 35.1 | **When backend/B14 exists.** "Delete All Sessions" onClick: open Shadcn AlertDialog. Title: "Delete all sessions?" (or similar). Description: "This will permanently delete all your recorded telemetry sessions. This action cannot be undone." On confirm: call DELETE (bulk or auth B14); toast.success on 2xx, toast.error on failure. | AlertDialog + API call |
| 35.2 | **When no backend.** "Delete All Sessions" button is **disabled**. Wrap in Shadcn Tooltip (or title): "Available when account is linked". No AlertDialog when disabled. Use a flag or auth/API availability check to switch between enabled (with dialog) and disabled (with tooltip). | Disabled + tooltip when no API |
| 35.3 | **Backend path (when implemented).** Document in contract: e.g. DELETE /api/sessions?scope=all or auth service DELETE /api/users/me/sessions. Block H step 29/30 covers auth service; cross-reference. | Contract when backend exists |

### Optional (Block I or Block H)

| # | Sub-step | Deliverable |
|---|----------|-------------|
| O.1 | **Forgot password.** Login "Forgot password?" → link to /forgot-password. Forgot password page: email input, submit → call backend (when auth exists). Backend: POST /api/auth/forgot-password or similar. Implement when Block H auth is done. | Forgot password flow |

**Theme persistence** is in **Block H step 30** (with other Settings); see block-h-optional-fuel-ers-positions-auth.md.

---

## Implementation checklist

Use this checklist to track what is done and what remains. Mark items with `[x]` when complete. Optional items (O.1, O.2) can be skipped or done later.

### Step 31 — Session Export (Backend + dropdown)

- [ ] 31.1 — Backend contract: GET /api/sessions/{id}/export?format=csv|json, Content-Disposition attachment
- [ ] 31.2 — Backend payload shape (summary + laps); CSV rows and JSON structure
- [ ] 31.3 — Backend implementation: controller + service, serialize CSV/JSON
- [ ] 31.4 — New UI: "Export Data" dropdown ("Export as JSON", "Export as CSV"); call API on select
- [ ] 31.5 — New UI: trigger download from response blob; toast success/error

### Step 32 — User menu (header)

- [ ] 32.1 — AppLayout: DropdownMenu with trigger (avatar + "Driver"), item "Settings"
- [ ] 32.2 — (When auth) Dropdown items "Profile" and "Logout" wired
- [ ] 32.3 — Keyboard accessibility verified (Arrow, Enter, Escape)

### Step 33 — Test Connection

- [ ] 33.1 — Document in NEW_UI_DOCS §8: Test Connection uses GET /actuator/health (same API base URL)
- [ ] 33.2 — "Test Connection" onClick: fetch /actuator/health, toast success/error

### Step 34 — View Diagnostics

- [ ] 34.1 — Route /app/settings/diagnostics + Diagnostics page; "View Diagnostics" navigates to it
- [ ] 34.2 — Diagnostics page content: from GET /api/diagnostics or placeholder + link to UDP instructions
- [ ] 34.3 — (Optional) Backend GET /api/diagnostics and contract documented

### Step 35 — Delete All Sessions

- [ ] 35.1 — When backend/B14 exists: AlertDialog on click; on confirm call DELETE, toast success/error
- [ ] 35.2 — When no backend: button disabled + Tooltip "Available when account is linked"
- [ ] 35.3 — (When backend) Contract updated for bulk delete or auth B14 endpoint

### Step 36 — Documentation

- [ ] NEW_UI_DOCS.md §8 — Supplementary UI actions (Export, Test Connection, Diagnostics, Delete All) described
- [ ] rest_web_socket_api_contracts — Export and/or diagnostics endpoints if added

### Optional

- [ ] O.1 — Forgot password page + link from Login (when auth exists)

**Theme persistence** is in **Block H step 30** (with other Settings); not in Block I.

### Step 37 — Testing (manual or e2e)

- [ ] Export: dropdown → Export as JSON/CSV → file downloads with correct format and content
- [ ] User menu: open dropdown, Settings navigates; (when auth) Logout works
- [ ] Test Connection: success and failure toasts observed (GET /actuator/health)
- [ ] View Diagnostics: navigation to /app/settings/diagnostics; content or placeholder visible
- [ ] Delete All Sessions: when API — confirm dialog and delete; when no API — button disabled with tooltip

### Step 38 — Git Commit
- [ ] Add git commit with understanding message

---

## Decisions recorded

| # | Topic | Decision |
|---|--------|----------|
| **I1** | **Export (Step 31)** | **Backend API + dropdown.** Backend: GET /api/sessions/{id}/export?format=csv\|json. New UI: "Export Data" **dropdown** with two options ("Export as JSON", "Export as CSV"); on select call backend, trigger file download. |
| **I2** | **Test Connection (Step 33)** | Use **existing** endpoint **GET /actuator/health** (same API base URL as other REST calls). Document in NEW_UI_DOCS §8. |
| **I3** | **View Diagnostics (Step 34)** | **Separate page**: navigate to **/app/settings/diagnostics** (not modal). Add route and Diagnostics page component. |
| **I4** | **Delete All Sessions when no backend (Step 35)** | When auth/backend is not implemented: button **disabled** with **Tooltip** "Available when account is linked". When backend exists: AlertDialog on click, on confirm call DELETE. |
| **I5** | **Theme persistence** | Implement in **Block H step 30** (with other Settings); **not in Block I**. See block-h-optional-fuel-ers-positions-auth.md. |
