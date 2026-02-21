# Implementation plan: UI notifications (toast / popup messages)

**Topic:** Frontend — global popup (toast) notifications for errors and background messages.  
**Purpose:** Allow users to see errors and other messages that occur in the background (REST failures, WebSocket errors, success feedback) without losing context.  
**Status:** decisions resolved; ready for implementation.

**Related docs:** [react_spa_ui_architecture.md](../../project/react_spa_ui_architecture.md) § 7 (error handling), [ui_ux_specification.md](../../project/ui_ux_specification.md) (design tokens).

---

## 1. Current state

| Aspect | What exists today |
|--------|-------------------|
| **Error display** | Inline only: each page keeps local state (`errorMessage`, `editError`, `traceError`, etc.) and renders a block or paragraph (e.g. `status === 'error' && <p className="text-error">…</p>`). |
| **REST errors** | `api/client.ts`: failed requests throw `HttpError(status, message, body)`. Callers (pages) catch and set local error state; no global feedback. |
| **WebSocket / Live** | `useLiveTelemetry`: `errorMessage`, `connectionMessage` stored in hook state; `LiveDashboardPage` passes them to `LiveStateMessage` (card with error text and optional retry). No toast; user must be on Live page to see connection errors. |
| **Success feedback** | e.g. Session List: after PATCH display name, modal closes and list refetches; no explicit “Display name updated” toast. |
| **UI stack** | React 19, Vite, React Router; no toast library in `package.json`. Styling: CSS variables in `index.css` (`--error`, `--warning`, `--success`, `--bg-surface`, `--radius-md`, etc.). |

**Conclusion:** There is no global notification layer. Adding a toast system will give a single, consistent place for background errors and messages across all pages and data sources (REST, WebSocket, user actions).

---

## 2. Goals

1. **Global popup (toast) notifications** — non-blocking messages that appear in a fixed area (bottom-right), auto-dismiss after a few seconds, and can be closed manually.
2. **Error visibility** — show REST and WebSocket errors in toasts so the user sees them even when navigating or when the error happens in the background (e.g. WebSocket disconnect while on Session List).
3. **Other messages** — success (e.g. “Display name updated”), warning (e.g. “Connection lost. Live data may be outdated.”), info (e.g. “Session ended”) using the same toast component with different variants.
4. **No breaking change to existing UX** — pages can keep inline error blocks for primary load failures (e.g. “Failed to load sessions” with Retry); toasts complement them for background/secondary errors and for success feedback so the plan does not force removal of existing inline messages.

---

## 3. Out of scope (for this plan)

- **i18n** — messages can stay in English; i18n is a separate concern.
- **Sound / desktop notifications** — optional future enhancement.
- **Persistence of toast history** — only in-memory queue; no “notification center” or persisted log in this plan.

---

## 4. Design and UX

### 4.1 Toast variants

| Variant | Use case | Visual (proposal) |
|--------|----------|-------------------|
| **error** | REST 4xx/5xx, WebSocket error, validation error | Border/icon `--error`, background `--bg-surface`, text `--text-primary` |
| **warning** | Connection lost, reconnect, deprecated action | Border/icon `--warning` |
| **success** | Display name updated, session saved | Border/icon `--success` |
| **info** | Session ended, generic info | Border/icon `--text-secondary` or `--accent` |

All use design tokens from [ui_ux_specification.md](../../project/ui_ux_specification.md) (§ 3) and existing `index.css` variables.

### 4.2 Placement and behaviour

- **Position:** Bottom-right of viewport. Offset from edges (e.g. `bottom: var(--space-5)`, `right: var(--space-5)`).
- **Stacking:** New toasts stack below (or above) previous; newest can appear at top of stack. Max height for container with scroll if many toasts.
- **Auto-dismiss:** e.g. 5–6 seconds for success/info; 8–10 seconds for error/warning (configurable). Countdown optional.
- **Dismiss:** Close button on each toast; optional “Dismiss all”.
- **Accessibility:** `role="status"` for success/info, `role="alert"` for error/warning; focus not trapped; avoid excessive `aria-live` poling (e.g. `polite` for status, `assertive` for errors).

### 4.3 Message content

- **Single line preferred;** support short second line if needed (e.g. “Connection lost. Live data may be outdated.”).
- **No HTML** in message (plain text or pre-defined snippets) to avoid XSS and keep implementation simple.

---

## 5. Technical approach

### 5.1 Option A: Lightweight library (recommended)

- **Library:** e.g. **sonner** (small, headless-friendly, React 18+ compatible) or **react-hot-toast**.
- **Pros:** Accessible, consistent behaviour, less custom code. **Cons:** extra dependency.
- **Integration:** Wrap app in provider; call `toast.error()`, `toast.success()`, etc. from API client, hooks, or pages. Style with CSS variables to match `--error`, `--warning`, `--success`, `--bg-surface`, `--radius-md`.

### 5.2 Option B: Custom toast (Context + portal)

- **Implementation:** React Context holding array of toasts (id, variant, message, createdAt); provider in `App.tsx`; portal rendering a fixed div; `useToast()` hook that exposes `addToast({ variant, message })`; auto-remove by timer; CSS for placement and variants.
- **Pros:** no new dependency; full control. **Cons:** more code to write and maintain (accessibility, edge cases).

**Decision:** Use Option A — ready-made library (sonner). Sonner is widely used and theming via CSS variables is straightforward.

---

## 6. Implementation plan (stages)

### Stage 1: Toast infrastructure

**Goal:** Add toast mechanism and render toasts globally; no integration with REST/WS yet.

**Steps:**

1. **Add dependency**  
   - `npm install sonner` in `ui`.

2. **Mount provider and container**  
   - In `App.tsx` (or `main.tsx`): render `<Toaster position="bottom-right" richColors={false} />` (no wrapper needed; Toaster is self-contained). Style via `toastOptions` / CSS so colours use `--error`, `--warning`, `--success`, `--bg-surface`, `--radius-md`, `--text-primary`, `--border` from `index.css`.

3. **Styling**  
   - Ensure toasts use design tokens (background, border, radius, shadow).  
   - Optional: add small CSS in `index.css` or a dedicated `toast.css` for overrides (e.g. `.toast-error { border-left: 4px solid var(--error); }`).

4. **Manual check**  
   - From a temporary button or `useEffect`, call `toast.error('Test error')`, `toast.success('Test success')`, etc. and confirm appearance and position.

**Deliverables:** Toasts visible when triggered programmatically; styling aligned with app; no regression on existing pages.

**Dependencies:** None.

---

### Stage 2: Integration with REST (API client / pages)

**Goal:** Show a toast when REST requests fail (and optionally on success for mutating calls).

**Steps:**

1. **Central toasts in API client**  
   - In `api/client.ts`, inside `requestJson` (and `getActiveSession`), before throwing: call `toast.error(message)` for non-404 responses. For **404**, do **not** show a toast (suppress to avoid duplication with the page’s inline “Session not found” message).  
   - So: every REST failure except 404 shows one toast; page keeps inline error state for Retry and context.

2. **Success toasts for mutations**  
   - In `SessionListPage`, after successful `updateSessionDisplayName`, call `toast.success('Display name updated')` (before closing modal and refetching).  
   - Other mutations (if any) later: same pattern.

3. **Avoid duplicate toasts**  
   - Client shows one toast per failure (except 404); page keeps inline error state for Retry.

**Deliverables:** Failed REST calls show an error toast; successful PATCH display name shows success toast; no double toasts by design.

**Dependencies:** Stage 1.

---

### Stage 3: Integration with WebSocket / Live

**Goal:** Push WebSocket connection errors and connection-related messages to toasts so the user sees them even when not on the Live page.

**Steps:**

1. **Use toast in `useLiveTelemetry`**  
   - Wherever `setState(..., status: 'error', errorMessage: ..., connectionMessage: ...)` is called (e.g. `onStompError`, `onWebSocketError`, `/user/queue/errors`, `getActiveSession` failure), also call `toast.error(errorMessage)` or `toast.warning(connectionMessage)` so the message appears in the toast stack.  
   - Prefer one toast per logical event to avoid flooding.

2. **Connected / disconnected / session ended**  
   - **When connected:** show an info toast when live connection is established (e.g. “Live telemetry connected”).  
   - **When disconnected or session ended:** show an info toast when the user was connected and then disconnects or when the session ends (e.g. “Disconnected from live feed”, “Session ended”). So the user sees feedback both on connect and on disconnect/end, including when they are on another page.  
   - Do **not** show a toast for initial “No active session” (first load or poll without prior connection).

3. **Debounce / dedupe**  
   - If the same error fires repeatedly (e.g. reconnect loop), consider debouncing or deduplicating (e.g. same message within 3 s = one toast). Can be a follow-up; MVP can allow multiple toasts.

**Deliverables:** WebSocket/connection errors and, if desired, session-ended/disconnect messages appear as toasts; Live page continues to show inline state as today.

**Dependencies:** Stage 1.

---

### Stage 4: Page-level integration (optional refinements)

**Goal:** Use toasts for secondary actions and avoid duplicate or conflicting messages.

**Steps:**

1. **Session List**  
   - Edit display name: already covered in Stage 2 (success toast).  
   - Load sessions failure: keep inline error + Retry; toast from client will also show (one error toast is acceptable).

2. **Session Detail**  
   - Main session/laps load failure: keep inline error + Retry; toast from client will also show for non-404 (404 suppressed).  
   - **Trace / ERS load failure:** show a toast **in addition to** the existing inline error (e.g. `toast.error(traceError)` when trace or ERS fails) so the user sees it even if they scroll away.

3. **Live**  
   - Keep `LiveStateMessage` for full context (error + retry). Toasts from Stage 3 give global visibility; no need to remove inline content.

4. **Documentation**  
   - In code or README: when to use toast vs inline (e.g. “Primary load error: inline + toast; success of mutation: toast; background/WS error: toast; trace/ERS: inline + toast.”).

**Deliverables:** Clear split between inline and toast; no regressions; trace/ERS errors show both inline and toast.

**Dependencies:** Stages 1–3.

---

### Stage 5: Documentation and acceptance

**Goal:** Document behaviour and update project docs so future work is consistent.

**Steps:**

1. **Update react_spa_ui_architecture.md**  
   - In § 7 (error handling), add a short subsection: “Global notifications (toast)”. Describe that errors and important messages are shown in a toast (position, variants, auto-dismiss); REST errors and WebSocket errors are surfaced there; pages may still show inline errors for primary load with Retry.

2. **Optional: ui_ux_specification.md**  
   - Add a line under “Reusable components” or “Behaviour”: toast variants (error, warning, success, info), placement, and that they use design tokens from § 3.

3. **Acceptance checklist**  
   - [ ] Toast appears for REST failure (e.g. stop backend, reload sessions → error toast).  
   - [ ] Toast appears for WebSocket error (e.g. on Live, stop backend → error toast; optional: same when on another page and WS dies).  
   - [ ] Success toast after updating session display name.  
   - [ ] Toasts use app colours and do not break layout.  
   - [ ] Toasts auto-dismiss and can be closed manually.  
   - [ ] No regression: existing inline error and retry behaviour still works.

**Deliverables:** Docs updated; checklist passed.

**Dependencies:** Stages 1–4.

---

## 7. File and structure changes (summary)

| Area | Files to add or change |
|------|-------------------------|
| **Dependency** | `ui/package.json` — add e.g. `sonner` (or none if custom). |
| **App root** | `ui/src/App.tsx` — wrap with provider; render `<Toaster />` (or custom container). |
| **Styles** | `ui/src/index.css` (or `toast.css`) — overrides for toast appearance using design tokens. |
| **REST** | `ui/src/api/client.ts` — optional: on throw, call `toast.error(message)` (or keep in pages only). |
| **WebSocket** | `ui/src/ws/useLiveTelemetry.ts` — in error/connection paths, call `toast.error` / `toast.warning` / `toast.info`. |
| **Pages** | `ui/src/pages/SessionListPage.tsx` — after successful PATCH, `toast.success('Display name updated')`. Optionally SessionDetailPage for trace/ERS. |
| **Docs** | `.github/project/react_spa_ui_architecture.md` — § 7 add “Global notifications (toast)”. Optional: `ui_ux_specification.md`. |

---

## 8. Decisions (resolved)

| # | Decision | Choice |
|---|----------|--------|
| 1 | **Library vs custom** | Use a ready-made library (sonner). |
| 2 | **Toast position** | Bottom-right. |
| 3 | **REST: where to trigger toasts** | Central: in `requestJson` / `getActiveSession`; show one toast per failure; pages keep inline for Retry. |
| 4 | **404 toasts** | Suppress 404 — do not show a toast to avoid duplication with the page’s inline “Session not found” message. |
| 5 | **WebSocket connect / disconnect / session ended** | Show a toast when the user has connected (e.g. “Live telemetry connected”) and when they disconnect or the session ends (e.g. “Disconnected from live feed”, “Session ended”). |
| 6 | **Trace/ERS load errors** | Show a toast in addition to the existing inline error (both inline and toast). |

---

## 9. References

- **Error handling (UI):** [react_spa_ui_architecture.md](../../project/react_spa_ui_architecture.md) § 7.  
- **Design tokens:** [ui_ux_specification.md](../../project/ui_ux_specification.md) § 3; `ui/src/index.css` (`:root`).  
- **REST client and errors:** `ui/src/api/client.ts`, `ui/src/api/types.ts` (`HttpError`, `ApiErrorBody`).  
- **WebSocket state and errors:** `ui/src/ws/useLiveTelemetry.ts`, `ui/src/ws/types.ts` (`WsErrorMessage`).  
- **Existing error UI:** `LiveStateMessage.tsx`, `SessionListPage.tsx` (errorMessage, editError), `SessionDetailPage.tsx` (errorMessage, traceError, ersStatus), `LiveDashboardPage.tsx`.
