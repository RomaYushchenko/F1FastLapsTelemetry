### Driver Comparison Redesign – Change Plan

> This document compares the **new template** (`Downloads/.../DriverComparison.tsx`) with the **current implementation** (`f1-telemetry-web-platform/src/app/pages/DriverComparison.tsx`) and lists all changes needed so the live page looks like your redesign while keeping the real data flow from the backend.

---

## 1. Page header & global layout

- **Title & subtitle**
  - **Current**:  
    - Title: `Driver Comparison`  
    - Subtitle: `Compare telemetry and performance between two cars in a session`
  - **Template**:  
    - Title: `LAP TIME ANALYSIS`  
    - Subtitle: `Detailed telemetry comparison • Turn by turn analysis`
  - **Change**: Update the top header block in the current page to match the template text and typography (same Tailwind classes you already used in the template).
    - Replace the existing `<h1>` + `<p>` with the block from the template (no logic change).

- **Global spacing**
  - **Current**: `div` wrapper with `space-y-6` (same as template).
  - **Change**: Keep `space-y-6` but the content order inside should follow the template structure:
    1. Header block
    2. Comparison settings panel (collapsible)
    3. Two driver info cards
    4. Charts (Speed, Gear, Throttle, Brake, Delta) in the template style

---

## 2. Top controls: from forms to collapsible “Comparison Settings” panel

**Key requirement:** the **driver list must be loaded only after a session is selected**. This is already how the current page behaves (participants come from `getSession(sessionUid)`), and the redesign must keep this behaviour.

### 2.1. Replace `DataCard` form with template-style selection panel

- **Current**:
  - A `DataCard` that contains:
    - Session `Select`
    - Driver A `Select`
    - Driver B `Select`
    - Conditional lap selectors (for A/B) below when `canCompare` is true.
- **Template**:
  - A **collapsible** panel (`showSelectionPanel` state) with:
    - **Collapsed view**: clickable bar showing:
      - “Comparison Settings” label + icon
      - Session name
      - Driver A & B codes with colored dots
    - **Expanded view**: grid of 3 columns:
      - Session list (cards)
      - Driver A list (selectable cards)
      - Driver B list (selectable cards)
    - Selected session details shown at the bottom of expanded panel.

- **Required state additions to current page**:
  - Add `showSelectionPanel` state:
    - `const [showSelectionPanel, setShowSelectionPanel] = useState(false)`
  - Derive display objects (similar to template):
    - `selectedSessionData` – derived from `sessions` and `sessionUid`
    - `driverAInfo`, `driverBInfo` – derived from `participants` and mapping to colors/teams (see below).

### 2.2. Keep existing data flow but re-skin the UI

- **Session selection logic**:
  - **Do not change** how sessions are loaded:
    - Still use `getSessions()` and `sessionUid` state. See [Block G — § 1 and § 4](.github/draft/implementation-plans/new-ui-backend/block-g-driver-comparison.md) and [NEW_UI_DOCS § 5.1, 5.4](NEW_UI_DOCS.md) for existing API usage.
    - Instead of Shadcn `Select`, render the **session cards** from the template using real `sessions` data.
  - **Card behavior**:
    - On click of a session card, call `setSessionUid(session.id)`.
    - Card styles follow template logic for selected vs non-selected:
      - Selected: highlighted border & background
      - Others: neutral; hover styles

- **Participants (Driver A / B) selection logic**:
  - **Keep** the current use of `sessionDetail.participants` to populate options, and keep the **load-after-session pattern**:
    - `getSessions()` is called first (list view only; no participants field required).
    - When the user selects a session card, call `getSession(sessionUid)` to load **that session’s participants** (cars with data in this session), as defined in [rest_web_socket_api_contracts_f_1_telemetry.md — SessionDto.participants](.github/project/rest_web_socket_api_contracts_f_1_telemetry.md) and [Block G — § 2 and § 4](.github/draft/implementation-plans/new-ui-backend/block-g-driver-comparison.md).
  - `participants: SessionParticipantDto[] = sessionDetail?.participants ?? []`
  - Replace Shadcn `Select` controls with **card lists**:
    - For each participant:
      - Render a card styled as in the template’s `drivers.map(...)` block.
      - Click behavior:
        - For Driver A column:
          - If `participant.carIndex !== carIndexB`, set `carIndexA` to that car index.
        - For Driver B column:
          - If `participant.carIndex !== carIndexA`, set `carIndexB` to that car index.
      - Disabled/greyed state when a car is already chosen as the other driver (same logic as template’s “opacity-50 cursor-not-allowed”).
    - Label:
      - Use `p.displayLabel ?? "Car {carIndex}"` in place of template’s hard-coded code/team.
    - **Colors**:
      - Template uses per-driver static colors; current page uses constants:
        - `DRIVER_A_COLOR = "#00E5FF"`
        - `DRIVER_B_COLOR = "#E10600"`
      - For now,:
        - In Driver A cards, use `DRIVER_A_COLOR` as the accent for the selected card.
        - In Driver B cards, use `DRIVER_B_COLOR` as the accent for the selected card.
        - Optional future extension: if you have a driver/team color map elsewhere, you can inject it here for per-driver colors.

- **Lap selectors (reference laps)**:
  - Keep the existing logic and state (`referenceLapNumA`, `referenceLapNumB`, `lapOptionsA`, `lapOptionsB`, and `fetchComparison`).
  - **Visual integration**:
    - Move the two lap selectors into the **expanded** panel’s bottom area, or keep them in a separate `DataCard` right under the panel but styled to align with the redesign.
    - Keep the binding:
      - Options from `lapOptionsA` / `lapOptionsB`
      - Current value as `referenceLapNumA` / `referenceLapNumB` or `comparison.referenceLapNumA/B`

- **Collapsed panel summary row**:
  - When `showSelectionPanel` is `false`, replace the current `DataCard` header with template collapsed row:
    - Session name: from `selectedSessionData` derived as
      - `sessions.find((s) => s.id === sessionUid)` and display `sessionDisplayName || sessionType || id`.
    - Driver codes: from `labelA` and `labelB` (they already reflect participants’ display labels).
    - Small color dots:
      - Use `DRIVER_A_COLOR` for A, `DRIVER_B_COLOR` for B.

---

## 3. Driver summary cards: match template look & enrich with data

### 3.1. Layout change

- **Current**:
  - Two `DataCard`s: `Driver A — {labelA}` and `Driver B — {labelB}`.
  - Each card shows:
    - Best lap (time)
    - Total laps
    - Best lap number
- **Template**:
  - Two **large gradient cards** with:
    - Colored vertical strip on the left
    - Big square with position number
    - First name and large last name
    - Team name
    - Section with:
      - Lap time
      - Delta
      - Top Speed
      - Avg Speed
      - Max Gear

- **Required structural changes**:
  - Replace the two existing `DataCard` contents with the **template card structure**, but:
    - Keep using `DataCard` as container or wrap the template card content directly (your choice, but consistent with rest of app).
  - Add left color strip divs:
    - `style={{ backgroundColor: DRIVER_A_COLOR }}` for Driver A
    - `style={{ backgroundColor: DRIVER_B_COLOR }}` for Driver B

### 3.2. Data mapping (real data instead of mocks)

The template currently fabricates:

- `position`, `name`, `lastName`, `team`, `lapTime`, `delta`, `topSpeed`, `avgSpeed`, `gear`

For the live page, map from `ComparisonResponseDto` and `Session`:

- **Best lap time**:
  - Already used:
    - `formatLapTime(comparison.summaryA.bestLapTimeMs)`
    - `formatLapTime(comparison.summaryB.bestLapTimeMs)`
  - Use in the big “Lap Time” figures on each card.

- **Positions / labels / names**:
  - `SessionSummary` has only `leaderCarIndex` (optional).
  - `participants` have `displayLabel`; that’s not necessarily name, but it is your best unified label.
  - **Change**:
    - For the big name text (first/last), you likely don’t have structured names yet; so:
      - Use `labelA` / `labelB` as a whole “last name” styled large; skip splitting into first/last for now.
      - For position tile:
        - If you have finishing positions for the car in another DTO (not in `ComparisonResponseDto`), you can pass it down; otherwise:
          - Use a simple “A” and “B” or no numeric position until backend exposes it.
    - Team name:
      - Not available in `ComparisonResponseDto` yet; you can omit this row or use a neutral label like `Session` / `Car {carIndex}` until you wire it to a future drivers table.

- **Delta between drivers (card-level)**:
  - You already compute **time delta** along distance in `timeDeltaData`.
  - For a single scalar to show on each card (e.g. “-0.072s”, “+0.072s”), define:
    - `overallDelta = (comparison.summaryA.bestLapTimeMs ?? 0) - (comparison.summaryB.bestLapTimeMs ?? 0)`
  - Display:
    - On Driver A card: `overallDelta` (formatted seconds, sign).
    - On Driver B card: `-overallDelta` so they mirror.
  - Use color logic similar to template:
    - Green when faster (negative value if you stick to A − B), red when slower.

- **Top Speed / Avg Speed / Max Gear**:
  - These are **not** available in the current API types.
  - To avoid faking data:
    - Either:
      - (a) Remove these three mini-stat blocks for now (or mark them as coming soon), **or**
      - (b) Derive approximate values from existing traces:
        - `speedTraceA` / `speedTraceB` → `topSpeed = max(speedKph)`
        - `avgSpeed` from `mean(speedKph)` over trace
        - Gear is not in DTOs at all, so can’t be computed.
    - In change list terms:
      - **Change**: If you want to keep the exact template UI, you must:
        - Add small helper functions (using `comparison.speedTraceA/B`) to compute `topSpeed` and `avgSpeed` for each driver.
        - Hide `Max Gear` block or replace with another metric from summary (e.g. “Best Lap #” or “Total Laps”).

---

## 4. Charts: align to template naming and style

Your current charts already use **real data** and match the backend plan; the main changes are **naming, layout, and styling** to look like the template.

### 4.1. Speed chart

- **Current**: “Speed Overlay” `LineChart` with `speedOverlayData`.
- **Template**: `DataCard` titled `SPEED` with:
  - `AreaChart`
  - Two areas with gradients (driver A/B)
  - X-axis distance (m), Y-axis km/h
  - Custom tooltip with color-coded entries and “Distance: Xm”

- **Change**:
  - Keep `speedOverlayData` (real packet data).
  - Replace `LineChart` markup with template’s `AreaChart` structure:
    - Two `Area` components:
      - `dataKey="driverA"`, stroke `DRIVER_A_COLOR`
      - `dataKey="driverB"`, stroke `DRIVER_B_COLOR`
    - Define gradients `colorSpeedA` and `colorSpeedB` with these colors.
    - X-axis: `dataKey="distance"` with label “DISTANCE (m)” only if you want same bottom labeling as template; template’s SPEED card uses axis-only and reserves the label for DELTA.
  - Implement `CustomTooltip` like in the template, but typed:
    - Re-use the same tooltip component across charts to avoid duplication.

### 4.2. Gear chart

- **Current**: **No gear chart**.
- **Template**: `GEAR` `LineChart`:
  - `dataKey="gearA"` and `gearB`
  - StepAfter lines, gear values 1–8

- **Change**:
  - **Data availability**:
    - `ComparisonResponseDto` does not contain gear data; neither `traceA/B` nor `speedTraceA/B` carry gear.
  - **Options**:
    - (a) **Drop** the gear chart from the redesign for now, or
    - (b) Implement gear chart later when backend exposes gear per point.
  - For this change list:
    - Document that the **GEAR card cannot be implemented with current API**; keep it out of the live UI or show a placeholder state.

### 4.3. Throttle chart

- **Current**: “Throttle Overlay” `LineChart` using `throttleOverlayData` built from `traceA/B`:
  - distance vs throttle(%) for driver A/B.
- **Template**: `THROTTLE` `AreaChart` using `telemetryData`’s `throttleA/B`.

- **Change**:
  - Keep `throttleOverlayData` as the data source (it’s correct live data).
  - Replace `LineChart` with template-like `AreaChart`:
    - Use gradients `colorThrottleA`, `colorThrottleB` based on `DRIVER_A_COLOR` and `DRIVER_B_COLOR`.
    - Y-axis from 0 to 100 with label `%`.
    - X-axis: distance.
    - Tooltip reused from `CustomTooltip`.

### 4.4. Brake chart

- **Current**: **No brake chart** (brake is used only indirectly?).
  - `PedalTracePoint` type has `brake` field; your `throttleOverlayData` currently ignores it.
- **Template**: `BRAKE` `AreaChart`:
  - Uses `brakeA`/`brakeB` (%) with red gradients.

- **Change**:
  - Build new `brakeOverlayData` from `comparison.traceA` and `comparison.traceB`:
    - Similar to `throttleOverlayData`, but map:
      - `driverA = (p.brake ?? 0) * 100`
      - `driverB = (p.brake ?? 0) * 100`
    - Sort by distance.
  - Add a new `DataCard` titled `BRAKE` with template-like `AreaChart`:
    - Two gradients (`colorBrakeA`, `colorBrakeB`) with red shades.
    - Y-axis 0–100 with `%` label.
    - X-axis distance.
    - Shared tooltip.

### 4.5. Delta chart

- **Current**: “Time Delta (labelA − labelB)” `LineChart` using `timeDeltaData` from `computeTimeDeltaFromSpeedTraces`.
- **Template**: `DELTA` `AreaChart`:
  - `dataKey="delta"` 
  - Zero reference line
  - X-axis distance (with `DISTANCE (m)` label)
  - Y-axis “seconds”
  - Tooltip and a text legend “Positive = A faster • Negative = B faster”

- **Change**:
  - Keep `timeDeltaData` logic exactly as-is (it already follows plan).
  - Replace `LineChart` with template-style `AreaChart`:
    - Add gradient `colorDelta` using `DELTA_COLOR`.
    - Add `<ReferenceLine y={0} ... />` as in template.
    - X-axis label: `DISTANCE (m)`.
    - Y-axis label: `seconds`.
  - Update bottom legend text:
    - Template text: `Positive = {driverA.lastName} faster • Negative = {driverB.lastName} faster`
    - Your live version: `Positive = {labelA} ahead · Negative = {labelB} ahead`
    - **Change**: Use template phrasing but with `labelA`/`labelB`.

### 4.6. Lap time & sector charts

These are **not in your template** but already exist and align with the backend plan:

- Lap time comparison chart (`Lap Time Comparison`)
- Sector delta comparison (`Sector Delta Comparison` bar chart + small cards)

You can keep them as-is, but for consistency:

- You may want to:
  - Apply the same tooltip styling as other charts (dark background, border, radius).
  - Ensure font sizes and colors match global design (already close).

---

## 5. Error, loading, and empty states

The template is purely visual and doesn’t cover these scenarios; your current page already does.

- **Keep the following behaviors intact**:
  - `sessionsLoading`, `sessionsError` with `Skeleton` and error message.
  - `sessionDetailLoading` skeletons for driver selectors.
  - “No session selected” `DataCard`.
  - “Fewer than two participants” `DataCard`.
  - `comparisonLoading` skeleton state.
  - `comparisonError` message.
  - `notify.error(...)` for API errors.

- **Visual integration**:
  - Place these states **under** the comparison settings panel but **before** the driver cards & charts, so the error/empty messages are clearly visible.
  - Optionally wrap some of these messages with styling closer to template’s card aesthetic (e.g. using `DataCard` with soft background).

---

## 6. State & logic recap (what stays, what changes)

- **Stays exactly as is**:
  - `getSessions`, `getSession`, `getComparison` calls.
  - `sessions`, `sessionUid`, `sessionDetail`, `carIndexA/B`, `referenceLapNumA/B`.
  - `comparison` data handling.
  - `lapOptionsA/B`, `speedOverlayData`, `throttleOverlayData`, `timeDeltaData`.
  - `computeTimeDeltaFromSpeedTraces` implementation.

- **New/changed state & derived values**:
  - `showSelectionPanel` – for collapsible settings.
  - `selectedSessionData` – derived from `sessions` and `sessionUid` for display in panel.
  - `driverAInfo` / `driverBInfo` (optional):
    - If you decide to introduce an intermediate driver object for styling, it should be derived from:
      - `participants` (carIndex + displayLabel)
      - and fixed/derived colors (DRIVER_A_COLOR / B).
  - Optional helpers to compute:
    - `overallDelta` from `summaryA.bestLapTimeMs` and `summaryB.bestLapTimeMs`.
    - `topSpeed` / `avgSpeed` from `speedTraceA/B` if you decide to keep those metrics.

- **New derived data**:
  - `brakeOverlayData` from `traceA/B` for the BRAKE chart.

---

## 7. Implementation checklist

You can track work using this checklist:

- **Header & layout**
  - [ ] Update title/subtitle text to match template.
  - [ ] Reorder sections: header → comparison panel → driver cards → charts.

- **Comparison settings UI**
  - [ ] Add `showSelectionPanel` state and collapsed/expanded layouts.
  - [ ] Replace session `Select` with session cards, bound to real `sessions`.
  - [ ] Replace driver `Select`s with card grids using `participants`.
  - [ ] Keep lap selectors, but visually integrate them with panel or place them immediately below with template styling.
  - [ ] Collapsed summary row showing selected session + driver labels and color dots.

- **Driver cards**
  - [ ] Replace existing driver summary `DataCard` content with template-style large cards.
  - [ ] Bind lap times to real `summaryA/B.bestLapTimeMs` via `formatLapTime`.
  - [ ] Compute and display scalar delta from best lap times for A and B.
  - [ ] Decide how to handle top speed / avg speed / max gear:
    - [ ] Either hide them, or compute them from `speedTraceA/B` (except gear which is not available).

- **Charts**
  - [ ] `SPEED`: swap to AreaChart with gradients, using `speedOverlayData`.
  - [ ] `GEAR`: skip (no data) or add later when backend supports gear.
  - [ ] `THROTTLE`: switch to AreaChart using `throttleOverlayData` (real throttle).
  - [ ] `BRAKE`: add new AreaChart using `brakeOverlayData` built from `traceA/B.brake`.
  - [ ] `DELTA`: change to AreaChart with gradient, zero reference line, and template-style labels/text using `timeDeltaData`.
  - [ ] Standardize tooltips & axes styling across charts to match template.

- **States & errors**
  - [ ] Preserve all existing error/loading/empty states and toast notifications.
  - [ ] Ensure new layout still visibly surfaces these states.

This set of changes will keep your **backend integration and comparison logic exactly as-is**, while transforming the **visual structure and interaction** of the Driver Comparison page to closely match your redesigned template.

---

## 8. Deep implementation plan (step‑by‑step)

This section turns the redesign into a concrete implementation plan, with cross‑references to existing documentation and contracts.

### 8.1. Data flow and contracts (backend already implemented)

- **APIs and DTOs** (already implemented; no backend changes needed):
  - Session list: `GET /api/sessions?limit=&offset=` returning `Session[]` without `participants` (for performance) — see [REST contracts, § Sessions](.github/project/rest_web_socket_api_contracts_f_1_telemetry.md) and [Backend features — B1](BACKEND_FEATURES_FOR_NEW_UI.md).
  - Session detail: `GET /api/sessions/{id}` returning `SessionDto` with optional `participants?: SessionParticipantDto[]` — see [Block G § 2.1, 23.1–23.3](.github/draft/implementation-plans/new-ui-backend/block-g-driver-comparison.md).
  - Comparison: `GET /api/sessions/{sessionUid}/comparison?carIndexA=&carIndexB=&referenceLapNumA=&referenceLapNumB=` returning `ComparisonResponseDto` — defined in [Block G § 23.1–23.3](.github/draft/implementation-plans/new-ui-backend/block-g-driver-comparison.md) and wired into the UI per [NEW_UI_DOCS § 1, 5.1](NEW_UI_DOCS.md).
- **Frontend API layer**:
  - All types (`Session`, `SessionParticipantDto`, `ComparisonResponseDto`, `SpeedTracePoint`, `PedalTracePoint`) are defined in `src/api/types.ts` — aligned with the REST contracts.
  - `getSessions`, `getSession`, and `getComparison` are implemented in `src/api/client.ts` — see [NEW_UI_DOCS § 5.1](NEW_UI_DOCS.md).

**Conclusion:** all data needed by the redesign is already available; work is limited to refactoring the UI component and adding small helpers.

### 8.2. Session → participants dependency (load drivers after session)

1. **Keep current lifecycle** (as required):
   - On mount, `DriverComparison` calls `getSessions({ limit, offset, sort })` once.
   - When the user clicks on a session card (`selectedSessionData`), set `sessionUid` and:
     - Reset driver selections and lap selectors.
     - Trigger `getSession(sessionUid)` to load `sessionDetail` and `sessionDetail.participants`.
   - Driver A/B selectors are rendered **only** when `sessionDetail` is loaded and `participants.length > 0`.
2. **Explicitly enforce the dependency in UI**:
   - In the JSX structure:
     - Session column (cards) is always active (after sessions are loaded).
     - Driver A/B columns:
       - Show a skeleton or “Select a session first” placeholder when `!sessionUid` or `sessionDetailLoading`.
       - Show card lists only when `participants.length > 0`.
   - This guarantees that the driver list is session‑specific and avoids leaking drivers from another session.
3. **Error cases**:
   - When `getSession(sessionUid)` fails:
     - Clear `sessionDetail`, `participants`, `carIndexA/B`, `referenceLapNumA/B`, `comparison` (already present in current code).
     - Show an inline error message in a `DataCard` and rely on `notify.error` per [NEW_UI_DOCS § 5.2](NEW_UI_DOCS.md).

### 8.3. Component refactor: structure and state

1. **Introduce new UI state and derived values**:
   - `showSelectionPanel: boolean` (default `false`).
   - `selectedSessionData = sessions.find((s) => s.id === sessionUid) ?? null`.
   - Optional `overallDelta`, `topSpeedA/B`, `avgSpeedA/B`, `brakeOverlayData` helpers as described in sections 3 and 4.
2. **Restructure JSX into logical blocks**:
   - `Header` (title/subtitle).
   - `ComparisonSettingsPanel`:
     - Collapsed bar (`!showSelectionPanel`) with session + drivers summary.
     - Expanded content (`showSelectionPanel`) with 3 columns (sessions, driver A, driver B) and lap selectors (either here or directly below).
   - `ErrorAndEmptyStates` (existing cards).
   - `DriverSummaryCards`.
   - `Charts` (Speed, Gear placeholder/skip, Throttle, Brake, Delta, plus existing Lap/Sector charts).
3. **Optionally extract sub‑components** to keep `DriverComparison` readable and aligned with [NEW_UI_DOCS § 7](NEW_UI_DOCS.md):
   - `ComparisonSettingsPanel.tsx` in `src/app/pages/driver-comparison/` or similar.
   - `DriverSummaryCard.tsx` for A/B cards.
   - Shared `DistanceTooltip` component for charts.

### 8.4. Detailed UI implementation steps

1. **Header & layout**
   - Update the header as described in § 1.
   - Ensure the root wrapper remains `<div className="space-y-6">` to keep spacing consistent with other pages (per [react_spa_ui_architecture.md](.github/project/react_spa_ui_architecture.md)).
2. **Selection panel**
   - Implement `showSelectionPanel` state and toggle actions (click on collapsed bar, “Hide” button in expanded view).
   - Replace the top `DataCard` with:
     - A gradient `div` styled as in the template (you can still place content inside `DataCard` if you prefer).
     - Collapsed bar:
       - Show `selectedSessionData`’s display name or a placeholder “Select session”.
       - Show driver A/B labels and colors only when `carIndexA/B` are set.
     - Expanded content:
       - Left column: list of sessions using `sessions` from `getSessions`.
       - Middle/right columns: driver A/B cards built from `participants` in `sessionDetail`.
       - Lap selectors (two `Select`s) bound to `referenceLapNumA/B` and `lapOptionsA/B`.
3. **Driver cards**
   - Replace existing summary cards with template layout.
   - Map data as described in § 3.2, without inventing new backend fields.
   - Use `SessionParticipantDto.displayLabel` and `carIndex` for labels and fallback.
4. **Charts**
   - Port the template’s Recharts structures, but wire them to real data arrays:
     - `speedOverlayData` → `SPEED` AreaChart.
     - `throttleOverlayData` → `THROTTLE` AreaChart.
     - Newly created `brakeOverlayData` → `BRAKE` AreaChart.
     - `timeDeltaData` → `DELTA` AreaChart.
   - Ensure axes labels and tooltip styling follow the existing design guidelines ([NEW_UI_DOCS § 7; react_spa_ui_architecture.md § charts](.github/project/react_spa_ui_architecture.md)).
5. **Empty and loading states**
   - Review all conditional blocks and ensure none are lost during JSX refactor:
     - `sessionsLoading`, `sessionDetailLoading`, `comparisonLoading`.
     - `!sessionUid`, `participants.length < 2`, `!comparison`.
   - Keep toast behaviour intact per [NEW_UI_DOCS § 5.2](NEW_UI_DOCS.md) and [notify.ts description](NEW_UI_DOCS.md#52-toaster-and-notifications).

### 8.5. Testing and verification

- **Manual / E2E scenarios** (align with Block G § 6, 7):
  1. Open Driver Comparison page; verify sessions load via API and collapsed bar shows “Select session”.
  2. Select a session:
     - Driver lists appear only after `getSession(sessionUid)` finishes.
     - Comparison data loads automatically once two distinct drivers are selected.
  3. Change session:
     - Driver selections and comparison reset.
     - New participants and comparison load correctly.
  4. Use lap selectors:
     - Reference laps update and `getComparison` is called with `referenceLapNumA/B`.
     - Charts and driver cards update accordingly.
  5. Error conditions:
     - Simulate failed `getSessions`, `getSession`, or `getComparison` calls (dev tools / network).
     - Verify inline messages and toasts match notification policy.

- **Contract alignment**:
  - Cross‑check all used fields against:
    - [`rest_web_socket_api_contracts_f_1_telemetry.md`](.github/project/rest_web_socket_api_contracts_f_1_telemetry.md) § Sessions and § Comparison.
    - [`BACKEND_FEATURES_FOR_NEW_UI.md`](BACKEND_FEATURES_FOR_NEW_UI.md) § 1.7 B10.
    - [`block-g-driver-comparison.md`](.github/draft/implementation-plans/new-ui-backend/block-g-driver-comparison.md).

Following this plan ensures the redesign is **pixel‑close** to your template, respects the requirement that **drivers are loaded after a session is chosen**, and stays fully consistent with the existing backend contracts and UI architecture docs.


