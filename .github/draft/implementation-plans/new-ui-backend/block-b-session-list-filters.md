# Block B — Session list filters (backend B1 + UI)

Part of the [Step-by-Step Implementation Plan — New UI & Backend](../../../IMPLEMENTATION_STEP_BY_STEP.md). Steps 7–8.

---

## Gap analysis (vs current plan and codebase)

| Gap | Description | Current state | Action in this plan |
|-----|-------------|---------------|---------------------|
| **Total count for pagination** | UI needs "Showing X–Y of Z". Backend currently returns only `List<SessionDto>`. | No total in response. | **Decision:** use `X-Total-Count` header; body remains `[...]`. Implement in Step 7; read header in Step 8. |
| **Search semantics** | "Search" applies to display name, track name, session type. DB has only `session_display_name`, `session_type` (code), `track_id`. | Not specified how search matches track/type. | Backend: resolve search text to (1) LIKE on `session_display_name`, (2) F1SessionType display names → list of codes, (3) F1Track display names → list of track IDs; combine with OR in JPA Spec. Document in contract. |
| **Sort by bestLap** | Session entity has no best lap; it lives in summary/laps. | Plan mentioned as example. | **Decision:** implement in MVP. Backend: join/subquery with best-lap data (e.g. from session summary or laps) for sort=bestLap_asc/desc. |
| **Sort by finishingPosition** | Position is in `session_finishing_positions`, not Session. | Not explicitly in repository design. | Add join with `session_finishing_positions` (player car) in Specification when sort=finishingPosition. |
| **API client in New UI** | SessionHistory currently uses mock data; no `getSessions` call. | Block A introduces API layer and getSessions(limit, offset). | Step 8 assumes Block A done; extend getSessions with full params object. |
| **Session Type / Track filter values** | UI needs options for Session Type and Track dropdowns. | No GET /api/tracks list; session type is enum. | Session Type: use F1SessionType codes (backend) and display names (UI can use same enum or contract). Track: use trackId; UI uses static list from F1 tracks (or optional GET /api/reference/tracks later). |
| **Date range field** | dateFrom/dateTo — filter by startedAt, endedAt, or both? | Not specified. | **Decision:** full filter on both **startedAt** and **endedAt**. Session included if startedAt in [dateFrom, dateTo] and endedAt in [dateFrom, dateTo]; for active sessions (endedAt null) treat as "in range" if startedAt in range. Document in contract. |
| **Default sort and Reset defaults** | What is "default" after Reset? | Not defined. | Define: sort=startedAt_desc, no filters, offset=0, limit=50. Document in UI and contract. |
| **Loading and error states** | Session History must handle loading and API errors. | UI has empty state only. | Step 8: add loading (skeleton/spinner), error state + retry, toast on API error per project rules. |
| **More Filters** | Button exists; scope not defined. | Plan said "optional or placeholder". | **Decision:** add **state** filter (ACTIVE / FINISHED / All). Wire in "More Filters" (expand or inline in same card). |

---

## Component reusability

Follow [README § Frontend component reusability](README.md#frontend-component-reusability-all-blocks). Reuse the same **Table**, **Select**, **Skeleton**, loading/error/empty and pagination patterns as in Block A (Session History). Use Shadcn **Calendar/Popover** for date range; do not duplicate filter UI that exists elsewhere.

---

## Steps

| Step | Layer | Task | Depends on | Deliverable |
|------|--------|------|------------|-------------|
| **7** | Backend | **B1 — Session list filter/sort.** Extend GET /api/sessions with optional params: sessionType, trackId, search, sort (startedAt, finishingPosition, **bestLap**), **state** (ACTIVE/FINISHED), dateFrom, dateTo (filter on **both startedAt and endedAt**). Response: body `[...]` + header **X-Total-Count**. Implement in SessionQueryService and repository (incl. best-lap join for sort). Update REST contract and controller. Add/update tests. | — | GET /api/sessions supports filters, sort (incl. best lap), state, date range, and total count. |
| **8** | New UI | Session History: pass all filter/sort params (search, session type, track, sort incl. **Best Lap**, **state**, dateFrom, dateTo) into getSessions(params). Wire Date Range, Reset, and **More Filters** with **state** (ACTIVE / FINISHED / All). **Requires Block A:** extend getSessions; read **X-Total-Count** header. Pagination: Previous/Next, "Showing X–Y of Z". Loading and error states; remove mock data. | 1, 7 | Session History filters (incl. state), sort (incl. best lap), date range (startedAt + endedAt), and pagination work with backend. |

---

## Detailed changes

| Step | Where | Concrete changes |
|------|--------|------------------|
| 7 | Backend (telemetry-processing-api-service) | **Contract first:** extend [REST contract §3.1.1](../../../project/rest_web_socket_api_contracts_f_1_telemetry.md) with query params: `sessionType`, `trackId`, `search`, `sort` (startedAt_asc/desc, finishingPosition_asc, bestLap_asc/desc), `state` (ACTIVE | FINISHED; optional, omit = all), `dateFrom`, `dateTo` (ISO date; filter: startedAt and endedAt both in range; document null endedAt for active sessions). Total: **X-Total-Count** header; body remains array. **Controller:** bind params; set X-Total-Count; return list. **SessionQueryService:** method with filter + total. **Repository:** JPA Specification with filters, sort (incl. join for finishingPosition and best-lap for bestLap sort). **DTO:** SessionDto unchanged. |
| 8 | New UI | **API client (Block A):** extend getSessions with params (incl. `state?`); build query string; read **X-Total-Count** header. **SessionHistory:** state for filters (search, sessionType, trackId, sort, **state**, dateFrom, dateTo) and pagination; wire search, Session Type, Sort (incl. Best Lap), Track, Date Range, **State** (in More Filters or inline), Reset; Previous/Next; "Showing X–Y of Z"; loading/error; remove mock data; table from SessionDto. |

---

## Testing

| Step | Scope | What to add/update |
|------|--------|--------------------|
| 7 | Backend | **SessionQueryServiceTest:** filter combinations (sessionType, trackId, search, **state**, sort incl. **bestLap**, dateFrom/dateTo on both startedAt and endedAt); pagination and total count; **SessionControllerTest:** query params and **X-Total-Count** header. Follow [unit_testing_policy.md](../../../project/unit_testing_policy.md); 85% coverage. |
| 8 | New UI | Manual or e2e: apply each filter (incl. **state**), sort (incl. **Best Lap**), date range; assert query string and list; pagination Previous/Next and "Showing X–Y of Z"; Reset; loading and error states. |

---

## Documentation updates

| Doc | Updates |
|-----|--------|
| [rest_web_socket_api_contracts_f_1_telemetry.md](../../../project/rest_web_socket_api_contracts_f_1_telemetry.md) | §3.1.1: document query params (sessionType, trackId, search, sort incl. bestLap, state, dateFrom, dateTo), date filter on startedAt and endedAt, X-Total-Count header, examples. |
| [BACKEND_FEATURES_FOR_NEW_UI.md](../../../BACKEND_FEATURES_FOR_NEW_UI.md) | Mark B1 as implemented; add "Implemented in Step 7" reference. |

---

## Detailed task breakdown (implementation order)

Use this list for precise implementation; each item can be ticked in the [Checklist](#implementation-checklist) below.

### Step 7 — Backend (B1)

| # | Task | Details |
|---|------|--------|
| 7.1 | **Contract** | In REST contract §3.1.1: add query params `sessionType`, `trackId`, `search`, `sort` (startedAt_asc/desc, finishingPosition_asc, **bestLap_asc/desc**; default startedAt_desc), **`state`** (ACTIVE | FINISHED; optional = all), `dateFrom`, `dateTo` (ISO date; filter: **startedAt and endedAt** both in range; document null endedAt for active). Total: **X-Total-Count** header only; body remains `[...]`. Keep `limit`, `offset`. |
| 7.2 | **No wrapper DTO** | Response body remains `List<SessionDto>`; total only in **X-Total-Count** header. No SessionListResponseDto. |
| 7.3 | **SessionQueryService** | Add (or overload) method e.g. `listSessions(SessionListFilter filter)` where filter holds sessionType, trackId, search, sort, **state**, dateFrom, dateTo, offset, limit. Return type: list + total (e.g. `SessionListResult` with list and long total). |
| 7.4 | **Search resolution** | In service or dedicated helper: given `search` string, compute (1) session type codes whose F1SessionType display name contains search (case-insensitive), (2) track IDs whose F1Track display name contains search (case-insensitive). Pass to repository spec along with raw search for session_display_name LIKE. |
| 7.5 | **Repository** | JPA Specification: filter by sessionType, trackId, **state** (ACTIVE/FINISHED via runtime state or session fields), session_display_name LIKE, **startedAt and endedAt** both in [dateFrom, dateTo] (null endedAt: include if startedAt in range). Sort: startedAt, join session_finishing_positions for finishingPosition, **join/subquery for best lap** for bestLap_asc/desc. Count query for total. |
| 7.6 | **Controller** | GET /api/sessions: bind all optional params (incl. state); call service; return list with **X-Total-Count** header. |
| 7.7 | **Tests** | SessionQueryServiceTest: filter combinations (sessionType, trackId, search, **state**, date range on both startedAt/endedAt), sort (startedAt, finishingPosition, **bestLap**), pagination, total count. SessionControllerTest: query params and **X-Total-Count** header. Keep 85% coverage. |

### Step 8 — New UI

| # | Task | Details |
|---|------|--------|
| 8.1 | **API client** | Extend getSessions (from Block A): accept params `{ sessionType?, trackId?, search?, sort?, state?, dateFrom?, dateTo?, limit?, offset? }`. Build query string; read **X-Total-Count** header for total. Define TypeScript types for SessionDto. |
| 8.2 | **Session History state** | State for filters: search, sessionType, trackId, sort, **state**, dateFrom, dateTo; pagination: offset, limit, total. Defaults: sort=startedAt_desc, offset=0, limit=50, rest empty. |
| 8.3 | **Wire search** | Search input: controlled value → state; on change or debounce trigger getSessions with current params (reset offset to 0). |
| 8.4 | **Wire Session Type select** | Session Type dropdown: options from F1 session types (All + Race, Qualifying, Practice, Sprint etc.); value → state; onChange refetch with offset=0. |
| 8.5 | **Wire Sort select** | Sort dropdown: options (Date Newest, Date Oldest, Result/Position, **Best Lap**); value maps to sort param; onChange refetch with offset=0. |
| 8.6 | **Wire Track filter** | Track dropdown: options from static list (F1 tracks by id + display name); value → trackId in state; onChange refetch. (Optional: GET /api/reference/tracks if added later.) |
| 8.7 | **Date Range** | "Date Range" button opens date picker (e.g. Shadcn Calendar/Popover); user selects dateFrom and dateTo; set state and refetch with offset=0. |
| 8.8 | **Reset** | "Reset" button: clear all filter state to defaults (no filters, sort=startedAt_desc, offset=0); call getSessions with defaults. |
| 8.9 | **More Filters — State filter** | Add **state** filter (ACTIVE / FINISHED / All) under "More Filters" (expand same card or inline). Wire to state and refetch with offset=0. |
| 8.10 | **Pagination** | Previous: getSessions(..., offset: currentOffset - limit); Next: offset + limit. Disable Previous when offset=0; Next when offset + items.length >= total. Display "Showing X–Y of Z" using total from API. |
| 8.11 | **Replace mock data** | Remove hardcoded sessions array; use data from getSessions response. |
| 8.12 | **Loading state** | While getSessions is in flight: show skeleton or spinner for table body. |
| 8.13 | **Error state** | On getSessions error: show inline error message + Retry button; show toast.error per project rules. |
| 8.14 | **Table columns / links** | Ensure table uses real fields: sessionDisplayName or id, track (trackDisplayName from DTO), sessionType, startedAt, finishingPosition (Result), link to `/app/sessions/{id}`. Align with SessionDto from contract. |

---

## Implementation checklist

Track progress by marking items when done. Copy this block into your working doc or keep it in the file and update `[ ]` → `[x]`.

### Step 7 — Backend

- [x] **7.1** REST contract §3.1.1: query params (incl. state, bestLap sort, dateFrom/dateTo for both startedAt and endedAt) and X-Total-Count header documented
- [x] **7.2** Controller returns list + X-Total-Count header (no wrapper DTO)
- [x] **7.3** SessionQueryService method with filter (incl. state) + total
- [x] **7.4** Search resolution (display name + session type + track name)
- [x] **7.5** Repository Specification (filters incl. state, date on startedAt and endedAt; sort incl. bestLap + count)
- [x] **7.6** SessionController binds params and returns list + X-Total-Count header
- [x] **7.7** SessionQueryServiceTest: filter/sort (incl. bestLap, state)/count cases
- [x] **7.8** SessionControllerTest: query params and response
- [ ] **7.9** JaCoCo 85% coverage for changed code

### Step 8 — New UI

- [x] **8.1** getSessions(params) in API client (incl. state); read X-Total-Count header
- [x] **8.2** Session History filter (incl. state) + pagination state and defaults
- [x] **8.3** Search input wired to state and refetch
- [x] **8.4** Session Type select wired
- [x] **8.5** Sort select wired (incl. Best Lap option)
- [x] **8.6** Track filter wired (static list or API)
- [x] **8.7** Date Range picker wired (dateFrom, dateTo; filter on both startedAt and endedAt)
- [x] **8.8** Reset clears state and refetches
- [x] **8.9** State filter in More Filters (ACTIVE / FINISHED / All) wired
- [x] **8.10** Previous/Next pagination and "Showing X–Y of Z"
- [x] **8.11** Mock data removed; real API data used
- [x] **8.12** Loading state (skeleton/spinner)
- [x] **8.13** Error state + Retry + toast
- [x] **8.14** Table columns match SessionDto and link to session detail

### Step 9 — Documentation

- [x] REST contract §3.1.1 updated
- [x] BACKEND_FEATURES_FOR_NEW_UI.md B1 marked implemented

### Step 10 — Git Commit
- [x] Add git commit with understanding message
---

## Decisions (resolved)

| # | Topic | Decision |
|---|--------|----------|
| 1 | **Total count format** | **B)** Header `X-Total-Count: N`; response body remains `[...]`. |
| 2 | **Sort by best lap** | Implement in MVP. Backend adds join/subquery with best-lap data (e.g. from summary or laps) for sort=bestLap_asc/desc. |
| 3 | **Date range** | Full filter on **both startedAt and endedAt**. Session included if both timestamps fall in [dateFrom, dateTo]; active sessions (endedAt null) included when startedAt in range. |
| 4 | **More Filters** | Add **state** filter (ACTIVE / FINISHED / All). Wire under "More Filters" (expand or inline). |
