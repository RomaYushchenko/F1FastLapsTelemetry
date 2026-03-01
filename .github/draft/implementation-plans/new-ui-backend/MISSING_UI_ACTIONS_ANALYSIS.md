# Missing UI Actions — Analysis

This document lists **buttons and interactive elements** in the new UI (f1-telemetry-web-platform) that are expected to perform actions but were **not fully covered** in the original implementation plan (IMPLEMENTATION_STEP_BY_STEP.md and blocks A–H). Each item is mapped to either an existing block (with a note to add detail) or a new block.

---

## Summary

| Element | Location | Status | Where addressed |
|--------|----------|--------|------------------|
| **Export Data** | Session Details | Not in plan | Block I (new) |
| **Session list pagination** | Session History (Previous/Next) | API has limit/offset; UI not wired | Block B (add step detail) |
| **User menu (header)** | AppLayout — avatar + "Driver" | No dropdown / Logout | Block I (new) |
| **Test Connection** | Settings — UDP section | No action | Block I (new) |
| **View Diagnostics** | Settings — UDP section | No action | Block I (new) |
| **Delete All Sessions** | Settings — Danger zone | No backend/UI flow | Block I + Block H (optional) |
| **Date Range / More Filters / Reset** | Session History | Partially in B (date range); Reset/More Filters not explicit | Block B (clarify in step 8) |
| **Live Telemetry Pause/Play** | Live Telemetry | Toggle exists; behaviour with snapshot not specified | Block C step 12 (add note) |
| **Forgot password** | Login page | Link to "#" only | Block I or H (optional) |
| **Theme (Dark/Light/System)** | Settings | Select only; no persistence | Block H step 30 (optional) |

---

## 1. Session Details — Export Data

- **UI:** Button "Export Data" (Download icon) in Session Details header.
- **Expected:** Export session data (summary, laps, optionally charts) as CSV or JSON.
- **Gap:** No step in any block for export.
- **Decision:** New **Block I** — Session export (client-side from loaded data or optional backend endpoint).

---

## 2. Session History — Pagination

- **UI:** "Previous" and "Next" buttons; "Showing X of Y sessions".
- **Backend:** REST contract already has `limit` and `offset` for GET /api/sessions. Block B step 8 mentions "limit, offset" in getSessions params.
- **Gap:** Block B does not explicitly say "wire Previous/Next to getSessions(offset, limit)" or "show total count from API".
- **Decision:** **Block B** — Add explicit pagination UI step (wire buttons, total count if API returns it).

---

## 3. AppLayout — User menu

- **UI:** Header button: avatar + label "Driver". No dropdown.
- **Expected:** Dropdown with e.g. Profile/Settings link, Logout (when auth exists).
- **Gap:** Block H step 30 says "Settings: load/save profile" but does not mention header user menu or logout.
- **Decision:** New **Block I** — User menu dropdown (Settings link, optional Logout/Profile when auth is implemented).

---

## 4. Settings — Test Connection (UDP)

- **UI:** Button "Test Connection" in UDP Connection Settings.
- **Expected:** Check whether UDP port is reachable or ingest is receiving (e.g. health/diagnostic endpoint or client-side port check).
- **Gap:** No plan for this action.
- **Decision:** New **Block I** — Test Connection (define behaviour: e.g. GET health or client-side UDP port check; show toast result).

---

## 5. Settings — View Diagnostics

- **UI:** Button "View Diagnostics" in UDP section.
- **Expected:** Open a panel or page with connection/ingest diagnostics (packet count, errors, etc.).
- **Gap:** No plan.
- **Decision:** New **Block I** — View Diagnostics (optional: modal or page with diagnostic info; backend may need a simple diagnostic endpoint).

---

## 6. Settings — Delete All Sessions

- **UI:** Button "Delete All Sessions" in Danger zone.
- **Expected:** Confirm dialog then call backend to delete all sessions for the user (or all sessions if no auth).
- **Gap:** Block H step 29 mentions "delete sessions/account" (B14); step 30 says "danger zone calls delete endpoints". So backend and Settings wiring are optional in H. Not explicitly "Delete All Sessions" vs "Delete Account".
- **Decision:** **Block I** — Explicitly list "Delete All Sessions": confirm dialog, optional DELETE /api/sessions (bulk) or equivalent; wire in Settings when backend exists. Keep "Delete Account" in Block H.

---

## 7. Session History — Date Range, More Filters, Reset

- **UI:** Buttons "Date Range", "More Filters", "Reset".
- **Block B step 8:** Already includes "date range picker" and filter params. "More Filters" can be an extension of the same filters (e.g. extra fields in the same card). "Reset" clears filters and refetches.
- **Decision:** **Block B** — In step 8 detailed changes, add: "Date Range" opens date picker; "Reset" clears filter state and calls getSessions with defaults; "More Filters" optional (expand filters or leave as placeholder).

---

## 8. Live Telemetry — Pause/Play

- **UI:** Pause/Play button toggles `isPaused`; charts currently use mock data.
- **Expected:** When live snapshot is wired (Block C step 12), Pause stops updating charts from stream; Play resumes.
- **Decision:** **Block C** — In step 12, add one line: "Optional: Pause/Play to freeze/resume live chart updates from snapshot."

---

## 9. Login — Forgot password

- **UI:** Link "Forgot password?" to "#".
- **Expected:** Forgot-password flow (email form, backend endpoint).
- **Decision:** **Block I** (or Block H) — Optional: Forgot password page + backend; link from Login. Can be part of Block H step 29/30 if auth is implemented.

---

## 10. Settings — Theme and preferences persistence

- **UI:** Theme select (Dark/Light/System); Units, Data smoothing, Update rate, Auto-save, Record video, Alert toggles.
- **Block H step 30:** "Settings: load/save profile and preferences via API".
- **Gap:** Theme is often client-only (localStorage + CSS). Other prefs need backend (B13) when optional auth/user exists.
- **Decision:** No new block. Block H step 30 already covers "load/save profile and preferences". Add to Block I or H detail: "Theme: persist in localStorage and apply (no backend required); other preferences when B13 is implemented."

---

## Implementation

- **Block B:** Updated to explicitly include pagination UI and Date Range / Reset / More Filters in step 8.
- **Block C:** Step 12 updated to mention Pause/Play for live charts.
- **Block I (new):** New file `block-i-supplementary-ui-actions.md` with steps for: Session Export, User menu, Test Connection, View Diagnostics, Delete All Sessions, and optional Forgot password. Main plan and README updated to include Block I.
