# Block J — Verification, old UI removal, and documentation update

Part of the [Step-by-Step Implementation Plan — New UI & Backend](../../../IMPLEMENTATION_STEP_BY_STEP.md). **Steps 36–40.**

This block is the **final phase** of the migration: (1) verify that all functionality has been migrated to the new UI and that old and new features are implemented; (2) remove the old UI codebase; (3) update all project documentation, architecture diagrams, and references so they describe the system based on the new UI as the single front-end.

**Prerequisites:** Blocks A–I (steps 1–35, and optionally 25–30) must be completed so that the new UI uses real API/WebSocket data and all planned features are in place.

---

## Gaps identified (not in original plan)

The following items were missing or underspecified in the original Block J; they are now part of this plan.

| Gap | Description | Added as |
|-----|-------------|----------|
| **Dockerfile for new UI** | `f1-telemetry-web-platform` has **no Dockerfile**. The old `ui/` has a Dockerfile (Node build + Nginx). Docker Compose cannot switch to the new UI until the new app can be built and served in a container. | Step 38.1–38.2: create Dockerfile (and nginx.conf if needed) for f1-telemetry-web-platform before changing docker-compose. |
| **Exact doc list** | Plan said "any other docs" and "README/runbooks" without listing every file that references `ui/` or "old UI". | § "Documents that reference old UI" below + explicit checklist items in Step 40. |
| **Verification report location** | Plan said "e.g. MIGRATION_VERIFICATION.md or a section in UI_MIGRATION_PLAN". | Fixed: single artefact **MIGRATION_VERIFICATION.md** at repo root; structure defined in Step 36. |
| **Row-by-row parity checklist** | Step 36 referred to "each row" of the Compatibility Matrix but did not list them as tickable items. | Step 36 detailed: one checkbox per matrix row + per gap-analysis item. |
| **Feature-by-feature checklist** | Step 37 listed blocks but not each concrete feature to tick. | Step 37 detailed: one checkbox per feature (Session list real data, Session detail charts, Edit name, etc.). |
| **CI only builds old UI** | `.github/workflows/build.yml` builds and lints **only** `ui/`; there is no job for `f1-telemetry-web-platform`. | Step 38: replace UI job with new UI (install deps, lint, build in `f1-telemetry-web-platform`). |
| **Docker Compose service name** | Root `docker-compose.yml` defines service **`ui`** (context `./ui`, image `f1-telemetry/ui:latest`). | Step 38: rename service (e.g. to `web` or `frontend`), point context to `./f1-telemetry-web-platform`, image to e.g. `f1-telemetry/web:latest`. |
| **README-DOCKER.md** | Explicitly mentions "ui (React + Nginx)" and "Open UI"; must be updated to new front-end name and path. | Step 40: update README-DOCKER.md. |

---

## Decisions (resolved)

| # | Question | Decision |
|---|----------|----------|
| **Q1** | **Route redirects:** Old UI had `/`, `/sessions`, `/sessions/:sessionUid`. New UI has `/`, `/app`, `/app/sessions`, `/app/sessions/:id`. | **No redirects.** Old links are not important; use the new UI and document new URLs only. |
| **Q2** | **Docker image naming:** After removal of `ui/`, how to name the front-end image? | **Rename** so there is no confusion: use e.g. `f1-telemetry/web:latest` and service name `web` (or `frontend`) in docker-compose; do not keep `f1-telemetry/ui:latest`. |

---

## Steps overview

| Step | Layer | Task | Depends on | Deliverable |
|------|--------|------|------------|-------------|
| **36** | Verification | **Migration parity checklist.** Run through the full [UI_MIGRATION_PLAN.md](../../../UI_MIGRATION_PLAN.md) §5 Compatibility Matrix and §6 Gap Analysis: confirm every row is "Done" or "N/A"; document any remaining gaps and either fix them or record as known limitations. | 1–35 | Signed-off checklist: all migrated features working; gaps documented. |
| **37** | Verification | **New features checklist.** Verify that all features from blocks A–I are implemented and working: Session History (real data, filters), Session Details (charts, edit display name), Live (WebSocket, Overview, Telemetry, Track Map), Strategy View, Leaderboard, Events, Export, User menu, Test Connection, Diagnostics, Delete All (when backend exists). Optional: Driver Comparison, fuel/ERS, positions, auth. | 1–35 | Checklist completed; any failing items fixed or deferred with a note. |
| **38** | Removal | **Remove old UI.** Delete the `ui/` directory (or the path where the old SPA lives). Update root-level scripts (e.g. `package.json` workspaces, `docker-compose`, CI) so they no longer build or serve the old UI; point default front-end to `f1-telemetry-web-platform`. Remove or redirect any links/docs that pointed to `ui/`. | 36, 37 | Old UI removed; build and run scripts use new UI only. |
| **39** | Documentation | **Update architecture and API docs.** Update [.github/project/react_spa_ui_architecture.md](../../../project/react_spa_ui_architecture.md): single SPA = `f1-telemetry-web-platform`, directory layout `f1-telemetry-web-platform/src/app/...`, routes and data flow as implemented. Update [.github/project/rest_web_socket_api_contracts_f_1_telemetry.md](../../../project/rest_web_socket_api_contracts_f_1_telemetry.md) if any endpoint or WS message was added/changed and not yet documented. Update [.github/project/telemetry_processing_api_service.md](../../../project/telemetry_processing_api_service.md) if service boundaries or entry points changed. | 38 | Architecture and contract docs describe the current system. |
| **40** | Documentation | **Update migration and plan docs.** In [UI_MIGRATION_PLAN.md](../../../UI_MIGRATION_PLAN.md): set compatibility matrix to "Migration complete"; add a short "Post-migration" section stating that the new UI is the only front-end and `ui/` has been removed. In [IMPLEMENTATION_STEP_BY_STEP.md](../../../IMPLEMENTATION_STEP_BY_STEP.md): add a "Final state" note that steps 36–40 (Block J) complete the migration; optionally add a one-line reference to this block file. In [NEW_UI_DOCS.md](../../../NEW_UI_DOCS.md): remove or rephrase "Known Limitations" that are no longer relevant; ensure §4 State, §5 API/WS, and §8 reflect the production setup. Update any other docs (README at repo root, runbooks, deployment) that referenced the old UI or dual-UI setup. | 38, 39 | All docs and plans reflect single-UI architecture and completed migration. |

---

## Detailed changes

### Step 36 — Migration parity checklist

| Id | Concrete actions |
|----|------------------|
| **36.1** | Create **MIGRATION_VERIFICATION.md** at repo root. Sections: "Step 36 — Parity (Compatibility Matrix)", "Step 36 — Gap Analysis", "Step 37 — New features", "Sign-off". Leave checklists empty to be filled while verifying. |
| **36.2** | For each row in [UI_MIGRATION_PLAN.md](../../../UI_MIGRATION_PLAN.md) §5 Compatibility Matrix, manually verify in the new UI with real backend (or mark N/A). In MIGRATION_VERIFICATION.md, list each matrix row with result: **OK** / **N/A** / **Deferred (reason)**. Update the matrix in UI_MIGRATION_PLAN §5 so Status column shows "Done" or "N/A" for every row. |
| **36.3** | For [UI_MIGRATION_PLAN.md](../../../UI_MIGRATION_PLAN.md) §6 Gap Analysis (A: portable, B: adaptation, C: from scratch), confirm each bullet is implemented in the new UI. In MIGRATION_VERIFICATION.md, list each gap item with **OK** or **Deferred (reason)**. Any deferred item must be recorded as a known limitation in [NEW_UI_DOCS.md](../../../NEW_UI_DOCS.md) §8. |
| **36.4** | Sign-off in MIGRATION_VERIFICATION.md: "Parity verified for scope on &lt;date&gt;; deferred items: &lt;list or none&gt;." |
| Deliverable | MIGRATION_VERIFICATION.md created and filled; matrix and gaps all OK or documented. |

### Step 37 — New features checklist

| Id | Feature / area | What to verify |
|----|----------------|----------------|
| **37.1** | Block A — API layer, Toaster, sessionId, types | API client exists; Toaster mounted; sessionId used in links/calls; types match contract. |
| **37.2** | Block A — Session list (real data) | Session History loads from GET /api/sessions; table shows real sessions; loading/error/empty states. |
| **37.3** | Block A — Session detail (load + charts) | Session Details loads session, laps, summary; pace/trace/ERS/speed/corners/tyre-wear charts use API data. |
| **37.4** | Block A — Edit display name | Edit display name (modal/dialog) calls PATCH; toast on success/error. |
| **37.5** | Block A — Notifications (Bell) | Header Bell shows recent notifications (same events as toasts). |
| **37.6** | Block B — Session list filters + sort | Filters (search, type, sort) and pagination work with backend B1. |
| **37.7** | Block C — WebSocket useLiveTelemetry | Hook/context connects to WS; SUBSCRIBE/UNSUBSCRIBE; SNAPSHOT/SESSION_ENDED/ERROR handled. |
| **37.8** | Block C — AppLayout connection status | Header badge reflects live status (Live/Waiting/No Data/Error). |
| **37.9** | Block C — Live Overview, Live Telemetry, Dashboard recent sessions | Pages consume live snapshot and/or GET /api/sessions/active; recent sessions on Dashboard from API. |
| **37.10** | Block D — Strategy View | Pit-stops and stints from API; Strategy View displays them. |
| **37.11** | Block E — Live leaderboard, Session events | Leaderboard and event timeline use backend B2/B3. |
| **37.12** | Block F — Live Track Map | Track layout from API B8; map renders layout (and positions if B9 implemented). |
| **37.13** | Block G (optional) — Driver Comparison | If implemented: comparison API and Driver Comparison page work. |
| **37.14** | Block H (optional) — Fuel/ERS, positions, auth | If implemented: fuel/ERS by lap, positions, Login/Register/Settings wired. |
| **37.15** | Block I — Export, User menu, Test Connection, Diagnostics, Delete All | Export dropdown works; User menu (Settings, optional Profile/Logout); Test Connection (health); View Diagnostics page; Delete All with confirm or disabled. |
| **37.16** | Record in MIGRATION_VERIFICATION.md | For each 37.1–37.15: mark **OK** or **Deferred (reason)**. Sign-off: "New features verified on &lt;date&gt;." |
| Deliverable | Every feature above tested and recorded; failures fixed or deferred with note. |

### Step 38 — Remove old UI

| Where | Concrete actions |
|-------|------------------|
| **38.1** | **Dockerfile for new UI.** If `f1-telemetry-web-platform/Dockerfile` does not exist: create it (e.g. multi-stage: Node 20 build → copy `dist/` into Nginx image). Use `ui/Dockerfile` and `ui/nginx.conf` as reference; Vite output is `dist/` by default. Add `f1-telemetry-web-platform/nginx.conf` if needed for SPA fallback (e.g. `try_files $uri $uri/ /index.html`). |
| **38.2** | **Verify new UI builds in Docker.** Run `docker build -f f1-telemetry-web-platform/Dockerfile -t f1-telemetry/web:test f1-telemetry-web-platform` and optionally run the container to confirm the app loads. |
| **38.3** | **docker-compose.yml (root).** Replace the `ui` service with a service for the new UI: rename service to **`web`** (see Decisions above), set `context: ./f1-telemetry-web-platform`, `dockerfile: Dockerfile`, image **`f1-telemetry/web:latest`**. Keep port 80 and dependency on `telemetry-processing-api-service`. Remove any reference to `./ui`. |
| **38.4** | **CI (.github/workflows/build.yml).** Remove steps that use `working-directory: ui` (Install UI dependencies, Rollup workaround, Lint UI, Build UI). Add equivalent steps for `f1-telemetry-web-platform`: Set up Node (same version as new UI package.json), Install dependencies (`working-directory: f1-telemetry-web-platform`), Lint (if script exists), Build. Ensure the workflow does not reference `ui/` anywhere. |
| **38.5** | **Root package.json / workspaces.** If the repo has a root `package.json` with workspaces that include `ui`, remove `ui` from the list; keep `f1-telemetry-web-platform` if it is listed. If there is no root package.json or no workspaces, no change. |
| **38.6** | **Delete old UI.** Delete the entire `ui/` directory. Run a quick grep (or search) for remaining references to `ui/` in code and config (excluding this plan and historical docs that describe the migration). Fix any broken references. |
| **38.7** | **Smoke test.** Run full build: backend (Maven) and new UI (npm/pnpm in f1-telemetry-web-platform). Run `docker-compose build` and `docker-compose up -d`; open http://localhost and confirm the new app loads. |
| Deliverable | Old UI removed; Docker and CI build/serve only `f1-telemetry-web-platform`; no broken refs. |

### Step 39 — Update architecture and API docs

| Id | Document | Concrete updates |
|----|----------|------------------|
| **39.1** | [.github/project/react_spa_ui_architecture.md](../../../project/react_spa_ui_architecture.md) | State that the **single React SPA** is **f1-telemetry-web-platform**. Update §2.2 directory structure to `f1-telemetry-web-platform/src/app/` (pages, components, api, ws, etc.). Update routes (§3 or equivalent) to match `routes.tsx` (Landing, Login, Register, /app with Dashboard, live, sessions, comparison, strategy, settings). Update data flow and WebSocket description to match the implemented hook and provider. Remove or replace any references to `ui/`. Use English for new text (see .cursor/rules/english-documentation.mdc). |
| **39.2** | [.github/project/rest_web_socket_api_contracts_f_1_telemetry.md](../../../project/rest_web_socket_api_contracts_f_1_telemetry.md) | Ensure every endpoint and WebSocket message used by the new UI is documented (path, params, request/response, status codes). Add any missing endpoints (e.g. export, diagnostics) added in Blocks I or H. |
| **39.3** | [.github/project/telemetry_processing_api_service.md](../../../project/telemetry_processing_api_service.md) | If new controllers, services, or entry points were added (e.g. export, diagnostics, auth), update the service architecture section and diagram (if any). |

### Step 40 — Update migration and plan docs

| Id | Document | Concrete updates |
|----|----------|------------------|
| **40.1** | [UI_MIGRATION_PLAN.md](../../../UI_MIGRATION_PLAN.md) | In §5 Compatibility Matrix: add a note "As of Block J, migration complete; all rows implemented in the new UI or N/A." Add **§9 Post-migration**: state that f1-telemetry-web-platform is the only front-end, ui/ has been removed, Docker/CI/scripts use the new UI. |
| **40.2** | [IMPLEMENTATION_STEP_BY_STEP.md](../../../IMPLEMENTATION_STEP_BY_STEP.md) | Block J and steps 36–40 are already added; ensure "Final state" subsection is present and accurate. |
| **40.3** | [NEW_UI_DOCS.md](../../../NEW_UI_DOCS.md) | Review §4 State, §5 API/WebSocket, §8 Known Limitations: remove or rephrase limitations that are no longer true; document production setup (VITE_API_BASE_URL, VITE_WS_URL, env vars). |
| **40.4** | [README-DOCKER.md](../../../README-DOCKER.md) | Replace "ui (React + Nginx)" with "web" or "frontend" (f1-telemetry-web-platform). Update "Open UI" and any paths/commands to refer to the new app. Update architecture diagram if it names "ui". |
| **40.5** | Other docs (see § Documents that reference old UI) | Update or remove references to `ui/` or "old UI" in each listed file as needed. |

---

## Documents that reference old UI (update in Step 40)

Use this list to ensure no doc is missed when removing references to `ui/` or "old UI". For each file, either update the text to describe the new UI only or add a one-line note that the old UI has been removed.

| Document | Path | Action in Block J |
|---------|------|-------------------|
| React SPA UI architecture | `.github/project/react_spa_ui_architecture.md` | Step 39.1: single SPA = f1-telemetry-web-platform; directory layout; remove ui/ refs. |
| REST & WebSocket API contract | `.github/project/rest_web_socket_api_contracts_f_1_telemetry.md` | Step 39.2: ensure all endpoints/WS messages documented. |
| Telemetry API service | `.github/project/telemetry_processing_api_service.md` | Step 39.3: update if new controllers/services added. |
| UI Migration Plan | `UI_MIGRATION_PLAN.md` | Step 40.1: matrix complete; §9 Post-migration. |
| Implementation Step-by-Step | `IMPLEMENTATION_STEP_BY_STEP.md` | Step 40.2: Final state present. |
| New UI docs | `NEW_UI_DOCS.md` | Step 40.3: §4, §5, §8 aligned with production. |
| Docker guide | `README-DOCKER.md` | Step 40.4: "ui" → web/frontend; paths/commands for new UI. |
| Implementation progress | `.github/project/IMPLEMENTATION_PROGRESS.md` | Step 40.5: "ui/ module" / Stage 11 — add note that UI has moved to f1-telemetry-web-platform; old ui/ removed. |
| Frontend refinement plan | `.github/project/frontend_refinement_plan_f1_telemetry.md` | Step 40.5: paths like `ui/src/charts/` — add note or update to f1-telemetry-web-platform paths. |
| Draft implementation plans (02, 03, 06, 07, 12) | `.github/draft/implementation-plans/*.md` | Step 40.5: optional; if these are still referenced, add "Historical: referred to old ui/; current UI is f1-telemetry-web-platform" or update paths. |
| Block plans (A–I, README) | `.github/draft/implementation-plans/new-ui-backend/*.md` | No change required (they already target new UI). |

---

## Testing

| Step | Scope | What to do |
|------|--------|------------|
| 36–37 | Verification | Manual (or automated) run-through: session list, filters, session detail, charts, edit name, live WebSocket, live overview/telemetry/track map, strategy view, leaderboard, events, export, user menu, test connection, diagnostics, delete all (if backend present). Optional: smoke e2e for critical paths. |
| 38 | Removal | After deleting old UI: run full build and test (backend + new UI); ensure no broken references; run Docker/CI and confirm they use the new UI only. |
| 39–40 | Documentation | Review updated docs for consistency (no remaining "ui/" or "old UI" as primary); ensure links and code blocks are correct. |

---

## Documentation updates (summary)

| Doc | Updates (Block J) |
|-----|-------------------|
| [UI_MIGRATION_PLAN.md](../../../UI_MIGRATION_PLAN.md) | §5 matrix complete; §9 Post-migration (single UI, old UI removed). |
| [IMPLEMENTATION_STEP_BY_STEP.md](../../../IMPLEMENTATION_STEP_BY_STEP.md) | Block J (steps 36–40); summary table; "Final state" subsection. |
| [NEW_UI_DOCS.md](../../../NEW_UI_DOCS.md) | §4, §5, §8 aligned with production; known limitations updated. |
| [.github/project/react_spa_ui_architecture.md](../../../project/react_spa_ui_architecture.md) | Single SPA = f1-telemetry-web-platform; directory layout; routes; data flow; no ui/ references. |
| [.github/project/rest_web_socket_api_contracts_f_1_telemetry.md](../../../project/rest_web_socket_api_contracts_f_1_telemetry.md) | All used endpoints and WS messages documented. |
| [.github/project/telemetry_processing_api_service.md](../../../project/telemetry_processing_api_service.md) | Controllers/services/entry points up to date. |
| Root README, runbooks, deployment | Single front-end; paths and commands for new UI only. |

---

## Master checklist (track progress)

Copy this checklist into **MIGRATION_VERIFICATION.md** or keep it here; mark each item when done so you can see what remains.

### Step 36 — Migration parity

- [ ] **36.1** Created MIGRATION_VERIFICATION.md at repo root with sections for Step 36, 37, Sign-off.
- [ ] **36.2** Every row of UI_MIGRATION_PLAN §5 Compatibility Matrix verified and marked Done/N/A; results recorded in MIGRATION_VERIFICATION.md.
- [ ] **36.3** Every item in UI_MIGRATION_PLAN §6 Gap Analysis verified; deferred items recorded in NEW_UI_DOCS §8.
- [ ] **36.4** Sign-off for parity written in MIGRATION_VERIFICATION.md.

#### Step 36 — Compatibility Matrix rows (optional; tick when status = Done/N/A)

- [ ] Landing / marketing page
- [ ] Login / Register
- [ ] App layout with sidebar & header
- [ ] Dashboard (overview + recent sessions)
- [ ] Live overview (leaderboard, session info, events)
- [ ] Live telemetry (charts, driver select)
- [ ] Live track map
- [ ] Session list (table, filters)
- [ ] Session detail (summary, laps, charts)
- [ ] Edit session display name
- [ ] Active session + WebSocket live
- [ ] Pace chart
- [ ] Pedal trace / throttle-brake chart
- [ ] ERS chart (lap)
- [ ] Speed trace chart
- [ ] Corners (lap)
- [ ] Tyre wear chart
- [ ] Driver comparison (multi-driver)
- [ ] Strategy view (pit stops, fuel, ERS)
- [ ] Settings (profile, UDP, alerts)
- [ ] REST API client
- [ ] WebSocket + STOMP
- [ ] Session ID (UUID / numeric)
- [ ] Toaster (sonner)

### Step 37 — New features

- [ ] **37.1** Block A — API layer, Toaster, sessionId, types: verified.
- [ ] **37.2** Block A — Session list (real data): verified.
- [ ] **37.3** Block A — Session detail (load + charts): verified.
- [ ] **37.4** Block A — Edit display name: verified.
- [ ] **37.5** Block A — Notifications (Bell): verified.
- [ ] **37.6** Block B — Session list filters + sort: verified.
- [ ] **37.7** Block C — WebSocket useLiveTelemetry: verified.
- [ ] **37.8** Block C — AppLayout connection status: verified.
- [ ] **37.9** Block C — Live Overview, Live Telemetry, Dashboard recent sessions: verified.
- [ ] **37.10** Block D — Strategy View: verified.
- [ ] **37.11** Block E — Live leaderboard, Session events: verified.
- [ ] **37.12** Block F — Live Track Map: verified.
- [ ] **37.13** Block G (optional) — Driver Comparison: verified or N/A.
- [ ] **37.14** Block H (optional) — Fuel/ERS, positions, auth: verified or N/A.
- [ ] **37.15** Block I — Export, User menu, Test Connection, Diagnostics, Delete All: verified.
- [ ] **37.16** All 37.1–37.15 recorded in MIGRATION_VERIFICATION.md; sign-off date added.

### Step 38 — Remove old UI

- [ ] **38.1** Dockerfile (and nginx.conf if needed) for f1-telemetry-web-platform created.
- [ ] **38.2** New UI Docker build tested (image runs, app loads).
- [ ] **38.3** docker-compose.yml updated: service **web**, context = f1-telemetry-web-platform, image f1-telemetry/web:latest.
- [ ] **38.4** .github/workflows/build.yml updated: UI steps use f1-telemetry-web-platform only; no ui/.
- [ ] **38.5** Root package.json workspaces (if any): ui removed.
- [ ] **38.6** ui/ directory deleted; no broken references in code/config.
- [ ] **38.7** Full build + docker-compose smoke test passed.

### Step 39 — Architecture and API docs

- [ ] **39.1** react_spa_ui_architecture.md updated (single SPA, layout, routes, no ui/).
- [ ] **39.2** rest_web_socket_api_contracts_f_1_telemetry.md complete for all used endpoints/WS.
- [ ] **39.3** telemetry_processing_api_service.md updated if new controllers/services.

### Step 40 — Migration and plan docs

- [ ] **40.1** UI_MIGRATION_PLAN.md: §5 complete note; §9 Post-migration added.
- [ ] **40.2** IMPLEMENTATION_STEP_BY_STEP.md: Final state subsection present.
- [ ] **40.3** NEW_UI_DOCS.md: §4, §5, §8 updated; production env documented.
- [ ] **40.4** README-DOCKER.md: ui → web/frontend; commands/paths for new UI.
- [ ] **40.5** Other docs (IMPLEMENTATION_PROGRESS.md, frontend_refinement_plan, draft plans): references to ui/ updated or noted as historical.

### Step 41 — Block J complete

- [ ] All items above checked; MIGRATION_VERIFICATION.md signed off; migration marked complete in UI_MIGRATION_PLAN and IMPLEMENTATION_STEP_BY_STEP.

### Step 428 — Git Commit
- [ ] Add git commit with understanding message