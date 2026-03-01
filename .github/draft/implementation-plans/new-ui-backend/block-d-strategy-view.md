# Block D — Strategy View (backend B4, B5 + UI)

Part of the [Step-by-Step Implementation Plan — New UI & Backend](../../../IMPLEMENTATION_STEP_BY_STEP.md). Steps 14–16.

---

## Gap analysis (current plan vs codebase)

Comparison of the plan with the current implementation and backend data:

| Area | In plan | Current state | Gap |
|------|--------|----------------|-----|
| **Strategy View context (sessionUid, carIndex)** | Plan says "getPitStops(sessionUid, carIndex)", "getStints(sessionUid, carIndex)" | Route is `/app/strategy` with no session or car params | **Resolved:** Strategy is opened **from Session Details**: route `/app/sessions/:id/strategy`; link/tab "Strategy" on Session Details; carIndex from session.playerCarIndex or selector. |
| **Pit duration (pitDurationMs)** | PitStopDto includes pitDurationMs | LapData packet has `pitStopTimerInMs` in UDP ingest but it is **not persisted** (Lap and TyreWearPerLap have no pit duration) | **Gap:** MVP can derive inLapTimeMs, outLapTimeMs, compoundIn/compoundOut from laps + tyre-wear; pitDurationMs can be **null** in response, or we add persistence of pit stop duration from LapData in a later step. Plan should state: pitDurationMs optional (null if not available). |
| **Compound display (API vs UI)** | PitStopDto compoundIn, compoundOut; StintDto compound | Tyre-wear API returns compound as **Integer** (F1 25 code, e.g. 16=C5, 18=C3) | **Gap:** Define whether pit-stops/stints return compound as **code** (Integer, UI maps to "Soft"/"Medium") or **display string** (e.g. "soft"). Recommend: same as tyre-wear (Integer code); UI keeps a small map code → label. |
| **Stint "degradation" column (UI)** | StintDto: stintIndex, compound, startLap, lapCount, avgLapTimeMs optional | UI table has "Degradation" column (High/Medium/Low) | **Resolved:** Backend adds optional **degradationIndicator** (e.g. "high"\|"medium"\|"low" or null). When data is present, UI shows it; when null, UI shows "—". Field allows future derivation from lap-time trend. |
| **Overview cards (Strategy View)** | Not specified | Cards: "Total Pit Stops (2)", "Fuel Strategy (Optimal)", "Tyre Management (3 stints)", "ERS Efficiency (94%)" — all mock | **Gap:** Derive "Total Pit Stops" and "Tyre Management (N stints)" from API; "Fuel Strategy" and "ERS Efficiency" stay mock until B6/B7. |
| **Tyre Degradation chart** | Not in B4/B5 | Mock "performance %" by lap | **Out of scope for Block D:** Keep mock; optional future bind to stint/lap data. |
| **Timeline total laps** | Not specified | Hardcoded `(stop.lap / 30) * 100` for pit marker position | **Gap:** Use session totalLaps (from getSession or summary) or max lap from data for timeline scale. |
| **Loading and error states (Strategy View)** | Not explicit | No loading/error/retry for pit-stops/stints | **Gap:** Add loading state, error state with Retry, and empty state (no pit stops / no stints). |
| **Backend: data source for pit detection** | "Detection from laps + tyre compound" | Laps in `lap` table (lapTimeMs, no compound); compound in `tyre_wear_per_lap` | **OK:** Pit = lap where compound changes (vs previous lap). In-lap = lap before change; out-lap = lap of change. Stints = consecutive laps with same compound. Service will join laps + tyre_wear_per_lap (or query both and merge in memory). |
| **Backend: compound code → display** | — | No shared REST DTO for compound display name | **OK:** UI can use a static map (e.g. 16→"C5", 18→"C3", 7→"Inter", 8→"Wet" or "Soft"/"Medium"/"Hard" per game). Optional: backend adds compoundDisplayName in DTO later. |

---

## Decisions

### Resolved (recommended)

| # | Topic | Decision |
|---|--------|----------|
| 1 | **Pit duration** | PitStopDto.pitDurationMs is **optional (null)** in MVP. Backend derives pit stops from compound change; inLapTimeMs/outLapTimeMs from Lap table; compoundIn/compoundOut from TyreWearPerLap. If LapData pitStopTimerInMs is persisted later, pitDurationMs can be filled. |
| 2 | **Compound in API** | PitStopDto and StintDto use **compound as Integer** (F1 25 code), consistent with existing tyre-wear API. UI maps code to display label (e.g. Soft/Medium/Hard/Inter/Wet). |
| 3 | **Overview cards** | "Total Pit Stops" and "Tyre Management (N stints)" are derived from API (pitStops.length, stints.length). "Fuel Strategy" and "ERS Efficiency" remain mock until B6/B7. |
| 4 | **Timeline scale** | Strategy View gets totalLaps from getSession(sessionId) (or from session context when opened from Session Details) and uses it for pit timeline scale; fallback to max(lap from pit stops) if totalLaps missing. |
| 5 | **D1 — Strategy View entry** | **Strategy is opened from Session Details.** Route `/app/sessions/:id/strategy`. Add link or tab "Strategy" on Session Details page. StrategyView reads session id from route params; carIndex from session.playerCarIndex (or car selector if needed). |
| 6 | **D2 — Stint degradation** | Backend adds optional **degradationIndicator** in StintDto (e.g. "high"\|"medium"\|"low" or null). When backend provides it (e.g. later derived from lap-time trend), UI shows it; when null, UI shows "—". |

---

## Component reusability

Follow [README § Frontend component reusability](README.md#frontend-component-reusability-all-blocks). Use **DataCard** for overview cards and strategy panels; Shadcn **Table**, **Skeleton**; shared **formatLapTime** and compound code → label map (reuse or align with Session Details / Driver Comparison). Reuse the same loading/error/empty + Retry pattern as in other blocks.

---

## Steps

| Step | Layer | Task | Depends on | Deliverable |
|------|--------|------|------------|-------------|
| **14** | Backend | **B4 — Pit stops.** Define PitStopDto. Implement detection (e.g. from laps + tyre compound change or pit status); persist or compute on demand. Add GET /api/sessions/{sessionUid}/pit-stops?carIndex=. Update REST contract and tests. | — | Pit stops endpoint available. |
| **15** | Backend | **B5 — Stints.** Define stint DTO (stintIndex, compound, startLap, lapCount, optional avgLapTimeMs). Derive from laps + compound. Add GET /api/sessions/{sessionUid}/stints?carIndex= (or extend pace). Update contract and tests. | — | Stints endpoint available. |
| **16** | New UI | Strategy View: add API client getPitStops(sessionUid, carIndex), getStints(sessionUid, carIndex). **Entry from Session Details:** route `/app/sessions/:id/strategy`, link/tab on Session Details; carIndex from session. Replace mock pit stop timeline and stint comparison with API data; degradation column shows API value or "—" when null. Overview cards: pit count and stint count from API; fuel/ERS mock. Loading/error/empty states. Keep fuel/ERS charts as mock if B6/B7 not implemented. | 4, 14, 15 | Strategy View shows real pit stops and stints. |

---

## Detailed changes

| Step | Where | Concrete changes |
|------|--------|------------------|
| 14 | Backend | **Contract:** [REST contract](../../../project/rest_web_socket_api_contracts_f_1_telemetry.md) — new § for GET /api/sessions/{sessionUid}/pit-stops?carIndex=, PitStopDto (lapNumber, inLapTimeMs, pitDurationMs **optional null**, outLapTimeMs, compoundIn, compoundOut as Integer); **Service:** PitStopService or extend SessionQueryService/LapQueryService; detection from laps + tyre_wear_per_lap (compound change between consecutive laps → pit on out-lap); **Controller:** new endpoint; **Layering:** mapper entity/DTO→PitStopDto, no logic in controller. **TestData:** add pitStop-related constants/factories. |
| 15 | Backend | **Contract:** new § GET /api/sessions/{sessionUid}/stints?carIndex=, StintDto (stintIndex, compound as Integer, startLap, lapCount, avgLapTimeMs optional, **degradationIndicator optional** — "high"\|"medium"\|"low" or null; when null, UI shows "—"); **Service:** derive from laps + tyre_wear (consecutive same compound = one stint); **Controller:** new endpoint; **TestData:** add stint factories; tests. |
| 16 | New UI | **api/client.ts:** getPitStops(sessionUid, carIndex), getStints(sessionUid, carIndex); **api/types.ts:** PitStopDto, StintDto (with optional degradationIndicator). **Routing:** add route `/app/sessions/:id/strategy`; add link or tab "Strategy" on Session Details. **StrategyView:** read id from params, carIndex from session; fetch pit-stops and stints; render pit stop timeline and stint comparison from API; degradation column: show API value or "—" when null. Overview cards: Total Pit Stops = pitStops.length, Tyre Management = stints.length; fuel/ERS cards mock. **States:** loading, error + Retry, empty. Timeline scale from session totalLaps. Compound: UI map from Integer code to display string. Fuel/ERS charts stay mock. |

---

## Detailed implementation sub-steps

Use this for a precise, step-by-step implementation order. Each sub-step is a single, verifiable task.

### Step 14 — Backend B4 Pit stops

| # | Sub-step | Deliverable |
|---|----------|-------------|
| 14.1 | Update REST contract: new section for GET /api/sessions/{sessionUid}/pit-stops?carIndex= (default 0). Response: array of PitStopDto. PitStopDto: lapNumber (int), inLapTimeMs (Integer), pitDurationMs (Integer, optional null), outLapTimeMs (Integer), compoundIn (Integer, F1 code), compoundOut (Integer). 404 if session not found; 200 and [] if no pit stops. | Contract § pit-stops |
| 14.2 | In telemetry-api-contracts: add PitStopDto (lapNumber, inLapTimeMs, pitDurationMs, outLapTimeMs, compoundIn, compoundOut). | PitStopDto class |
| 14.3 | Add PitStopMapper (or static methods): build PitStopDto from (inLap, outLap, compoundIn, compoundOut). inLapTimeMs = inLap.getLapTimeMs(), outLapTimeMs = outLap.getLapTimeMs(), pitDurationMs = null. lapNumber = outLap lap (out-lap number). | PitStopMapper |
| 14.4 | Add PitStopQueryService (or extend LapQueryService): resolve session by id; load laps and tyre_wear_per_lap for session+car; detect compound changes; for each change build one PitStopDto (in-lap = lap before change, out-lap = lap after); return list ordered by lapNumber. Logging: debug at entry. | PitStopQueryService |
| 14.5 | Add GET /api/sessions/{sessionUid}/pit-stops in SessionController or new PitStopController; query param carIndex (default 0). Call service, map to DTO list, return. Thin controller. | Controller endpoint |
| 14.6 | TestData: add constants/factory for pit stop scenario (e.g. two laps with different compounds, expected one pit). | TestData |
| 14.7 | Unit tests: PitStopMapper, PitStopQueryService (with mocked LapRepository, TyreWearPerLapRepository). MockMvc: pit-stops returns 200 and list; 404 for unknown session. Run verify for 85% coverage. | Tests |

### Step 15 — Backend B5 Stints

| # | Sub-step | Deliverable |
|---|----------|-------------|
| 15.1 | Update REST contract: new section for GET /api/sessions/{sessionUid}/stints?carIndex=. Response: array of StintDto. StintDto: stintIndex (int), compound (Integer), startLap (int), lapCount (int), avgLapTimeMs (Integer optional), degradationIndicator (optional, "high"\|"medium"\|"low" or null; UI shows "—" when null). 404 if session not found; 200 and [] if no laps. | Contract § stints |
| 15.2 | In telemetry-api-contracts: add StintDto (stintIndex, compound, startLap, lapCount, avgLapTimeMs, degradationIndicator optional). | StintDto class |
| 15.3 | Add StintMapper: build StintDto from (stintIndex, compound, startLap, lapCount, list of laps for avg). avgLapTimeMs = average of valid lap times in stint; null if none. degradationIndicator = null in MVP (can be derived later from lap-time trend); UI shows "—" when null. | StintMapper |
| 15.4 | Add StintQueryService (or extend LapQueryService): resolve session; load laps + tyre_wear for session+car; group consecutive laps with same compound into stints; for each stint build StintDto. Order by stintIndex. | StintQueryService |
| 15.5 | Add GET /api/sessions/{sessionUid}/stints in controller; carIndex default 0. Thin controller. | Controller endpoint |
| 15.6 | TestData: add stint scenario (e.g. laps 1–5 compound 18, 6–10 compound 16). | TestData |
| 15.7 | Unit tests: StintMapper, StintQueryService. MockMvc: stints returns 200 and list; 404 for unknown session. Verify 85% coverage. | Tests |

### Step 16 — New UI Strategy View

| # | Sub-step | Deliverable |
|---|----------|-------------|
| 16.1 | **api/types.ts:** Add PitStopDto (lapNumber, inLapTimeMs, pitDurationMs, outLapTimeMs, compoundIn, compoundOut). Add StintDto (stintIndex, compound, startLap, lapCount, avgLapTimeMs?, degradationIndicator?). | Types |
| 16.2 | **api/client.ts:** getPitStops(sessionId, carIndex), getStints(sessionId, carIndex). Use same base URL and error/toast behaviour as other methods. | API client |
| 16.3 | **Routing — Strategy from Session Details:** Add route `/app/sessions/:id/strategy`. On Session Details page add link or tab "Strategy" that navigates to this route. StrategyView reads session id from route params; load session (for totalLaps and playerCarIndex), then carIndex = session.playerCarIndex (or add car selector if needed). | Session/car context |
| 16.4 | **StrategyView:** When session/car available, call getSession (if totalLaps needed), getPitStops(sessionId, carIndex), getStints(sessionId, carIndex). Loading state while fetching. Error state with Retry. Empty state: "No pit stops" / "No stint data" when list is empty. | Fetch and states |
| 16.5 | **StrategyView:** Replace pitStops mock with API data. Timeline: use totalLaps from session (or max lap from data) for scale; render one marker per pit (lapNumber). Table: one row per pit with inLap (format ms→"m:ss.fff"), outLap, pitTime (pitDurationMs or "—"), tyre change (compoundIn → compoundOut using UI compound map). | Pit stop UI |
| 16.6 | **StrategyView:** Replace stintComparison mock with API data. Table: Stint, Avg Lap Time (avgLapTimeMs formatted), Laps, **Degradation:** show degradationIndicator from API when present; when null, show "—". Compound label from UI map. | Stint comparison UI |
| 16.7 | **StrategyView:** Overview cards: "Total Pit Stops" = pitStops.length; "Tyre Management" = stints.length + " stints". Fuel Strategy and ERS Efficiency cards stay mock. | Overview cards |
| 16.8 | **Compound map:** Add small util or constant: F1 compound code (Integer) → display string (e.g. 16→"C5", 18→"C3", 7→"Inter", 8→"Wet", or "Soft"/"Medium"/"Hard" as per game). Use in pit table and stint table. | Compound labels |

---

## Testing

| Step | Scope | What to add/update |
|------|--------|-------------------|
| 14–15 | Backend | **PitStop:** service/mapper unit tests with TestData; **Stint:** service/mapper unit tests; **Controller:** MockMvc for pit-stops and stints (200, 404, empty list). Run `mvn -pl telemetry-processing-api-service verify` for 85% coverage. |
| 16 | New UI | Manual or e2e: open Strategy (with session context per D1); verify pit stop timeline and stint table show API data; loading/error/empty states; overview cards show correct counts. |

---

## Documentation updates

| Doc | Updates |
|-----|--------|
| [rest_web_socket_api_contracts_f_1_telemetry.md](../../../project/rest_web_socket_api_contracts_f_1_telemetry.md) | New sections: pit-stops and stints endpoints, PitStopDto and StintDto. |
| [BACKEND_FEATURES_FOR_NEW_UI.md](../../../BACKEND_FEATURES_FOR_NEW_UI.md) | Mark B4, B5 as implemented when done. |
| [.github/project/telemetry_processing_api_service.md](../../../project/telemetry_processing_api_service.md) | If new service/controller: add to architecture. |

---

## Implementation checklist

Use this to track what is done and what is left. Mark items when completed.

### Step 14 — Backend B4 Pit stops

- [ ] 14.1 — REST contract: pit-stops endpoint and PitStopDto
- [ ] 14.2 — telemetry-api-contracts: PitStopDto class
- [ ] 14.3 — PitStopMapper (or equivalent)
- [ ] 14.4 — PitStopQueryService (detection from laps + compound)
- [ ] 14.5 — Controller: GET pit-stops
- [ ] 14.6 — TestData: pit stop scenario
- [ ] 14.7 — Unit tests + MockMvc; verify 85% coverage
- [ ] 14.doc — Update BACKEND_FEATURES (B4 implemented), telemetry_processing_api_service if new components

### Step 15 — Backend B5 Stints

- [ ] 15.1 — REST contract: stints endpoint and StintDto
- [ ] 15.2 — telemetry-api-contracts: StintDto class
- [ ] 15.3 — StintMapper
- [ ] 15.4 — StintQueryService (derive from laps + compound)
- [ ] 15.5 — Controller: GET stints
- [ ] 15.6 — TestData: stint scenario
- [ ] 15.7 — Unit tests + MockMvc; verify 85% coverage
- [ ] 15.doc — BACKEND_FEATURES (B5 implemented)

### Step 16 — New UI Strategy View

- [ ] 16.1 — api/types.ts: PitStopDto, StintDto
- [ ] 16.2 — api/client.ts: getPitStops, getStints
- [ ] 16.3 — Route `/app/sessions/:id/strategy` + link/tab "Strategy" on Session Details
- [ ] 16.4 — StrategyView: fetch, loading, error, empty states
- [ ] 16.5 — Pit stop timeline and table from API
- [ ] 16.6 — Stint comparison table from API
- [ ] 16.7 — Overview cards: pit count and stint count from API
- [ ] 16.8 — Compound code → display string map in UI

### Documentation

- [ ] REST contract: pit-stops and stints sections
- [ ] BACKEND_FEATURES_FOR_NEW_UI.md: B4, B5 marked implemented
- [ ] (Optional) NEW_UI_DOCS: Strategy View section if needed

### Git Commit
- [ ] Add git commit with understanding message
- 
---

## Out of scope for Block D

- **Tyre Degradation chart** — remains mock (performance % by lap).
- **Fuel consumption and ERS deployment charts** — remain mock until B6/B7.
- **Pit duration (pitDurationMs)** — optional null in MVP; can be added when LapData pit stop timer is persisted.
- **Stint degradationIndicator derivation** — field is in StintDto and UI shows "—" when null; actual derivation (e.g. from lap-time trend) can be implemented later.
