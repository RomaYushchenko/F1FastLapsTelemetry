# Implementation Plan: Session Summary — Speed / Per-Corner Graph

**Source document:** [speed-graf-deep-research-report.md](../speed-graf-deep-research-report.md)  
**Target UI:** Session Summary page — **one graph** for speed analysis (speed vs distance, optionally per-corner metrics).  
**Status:** Draft.

---

## 1. Scope and Goals

- Add **one chart** on the **Session Summary** page that visualises **speed along the lap** (and optionally per-corner entry/apex/exit).
- Data and formulas are defined in the research document; this plan aligns them with the **existing project** (Kafka contracts, REST API, DB, React architecture) and decomposes implementation into phases.

**Out of scope for the “one graph” MVP:** full Corner Dashboard, Corner Detail drawer, Track Map, and separate “Corner Browser” screen. Those can be added later if needed.

---

## 2. Alignment with Project Documentation

### 2.1 What Matches

| Research document | Project |
|-------------------|--------|
| Base axis: **lap distance** (`m_lapDistance`) | `LapDto.lapDistance`, `car_telemetry_raw.lap_distance_m` — already present |
| Speed from CarTelemetry | `CarTelemetryDto.speed`, `car_telemetry_raw.speed_kph` — already stored |
| Steer for corner detection | `CarTelemetryDto.steer`, `car_telemetry_raw.steer` — already stored |
| Sync by frame | `frameIdentifier` in envelope and in `car_telemetry_raw`; idempotency `(sessionUID, frameIdentifier, carIndex)` |
| Session/lap identity | Sessions by `sessionUid` (or public id); laps by `(sessionUid, carIndex, lapNumber)` — no `lapId` UUID in current API |
| Track metadata | `PacketSessionData` → `trackId`, `trackLength`; Session has `trackId`, `trackLengthM` |
| REST style | Query by `sessionUid`, `lapNum`, `carIndex` (see trace, ERS) |

### 2.2 Gaps and Decisions

| Topic | Research document | Current project | Decision |
|-------|-------------------|------------------|----------|
| **Motion data** | PacketMotionData (0): `m_worldPositionX/Z`, `m_gForceLateral`, `m_yaw` for curvature/yaw corner detection | **Not ingested**; no Motion parser/publisher | **Phase 1–2:** use **steer-based** corner detection only (data in `car_telemetry_raw`). **Later phase:** add Motion ingest and curvature/yaw method. |
| **Kafka topics** | New topics: `f1udp.frames.v1`, `f1udp.lap-events.v1`, `f1udp.corner-metrics.v1` | Existing: `telemetry.session`, `telemetry.lap`, `telemetry.carTelemetry` | **No new topics** for MVP. Compute speed profile and (optionally) corner segments from existing `car_telemetry_raw` + lap boundaries. Corner metrics can be written to DB only (no dedicated topic) unless we need replay. |
| **Frame Assembler** | Merges Lap + Motion + Telemetry per frame | Lap and CarTelemetry come separately; no Motion | For steer-only corners we **do not** need a separate assembler: we already have `lap_distance_m` and `steer` in `car_telemetry_raw` per frame. When Motion is added, we can align by `(session_uid, frame_identifier, car_index)`. |
| **Lap identifier in API** | Report uses `lapId` (UUID) in examples | REST uses `sessionUid` + `lapNum` (and `carIndex`) | Keep **existing pattern:** `GET /api/sessions/{sessionUid}/laps/{lapNum}/...` for speed trace and corners. |
| **DB tables** | `telemetry_frames`, `track_corner_maps`, `track_corners`, `lap_corner_metrics`, `lap_corner_profiles` | We have `sessions`, `laps`, `car_telemetry_raw`; no corner tables | **Phase 1:** no new tables (speed trace from `car_telemetry_raw`). **Phase 2+:** add tables only when we persist corner map and per-lap corner metrics. |
| **REST endpoints** | `/api/laps/{lapId}/corners`, `/api/tracks/{trackId}/corner-maps/latest` | No corner endpoints yet | Introduce **session-scoped** first: e.g. `GET /api/sessions/{sessionUid}/laps/{lapNum}/speed-trace`, then `GET .../laps/{lapNum}/corners` (and later track-level corner map if needed). |

### 2.3 References

- **Kafka:** [kafka_contracts_f_1_telemetry.md](../../project/kafka_contracts_f_1_telemetry.md) — topics, envelope, idempotency.  
- **REST:** [rest_web_socket_api_contracts_f_1_telemetry.md](../../project/rest_web_socket_api_contracts_f_1_telemetry.md) — sessions, laps, trace, ERS.  
- **Session Summary:** [04-session-summary-page.md](04-session-summary-page.md) — existing blocks (Summary, Pace, Tyre wear, Lap table, ERS, Pedal trace).  
- **Diagrams:** [telemetry_diagrams_plan.md](../../project/telemetry_diagrams_plan.md) — Session Detail historical charts.  
- **Layering:** [architecture-layering.mdc](../../../.cursor/rules/architecture-layering.mdc) — mappers, builders, processors, services, thin entry points.

---

## 3. Decomposition Overview

| Phase | Deliverable | Backend | Frontend | Depends on |
|-------|-------------|---------|----------|------------|
| **1** | One chart: **speed vs distance** for one selected lap | New endpoint: speed trace from `car_telemetry_raw` | Session Summary: one chart + lap selector | — |
| **2** | **Per-corner metrics** (entry/apex/exit) for one lap | Steer-based corner detection; endpoint `.../corners` | **Chart with corner markers and turn numbers** (labels on graph; optional table below) | Phase 1 |
| **3** | **Stable corner map** per track + persisted metrics | DB: track_corner_maps, track_corners, lap_corner_metrics; track-level API | Optional: corner labels, map version | Phase 2 |
| **4** | **Motion-based** corner detection (curvature/yaw) | Motion ingest, optional Frame Assembler, extended corner algo | — | Motion UDP + ingest |

---

## 4. Phase 1: Speed vs Distance Chart (MVP)

**Goal:** One graph on Session Summary showing **speed (kph) vs lap distance (m)** for a **single selected lap**. No corner model yet.

### 4.1 Backend

1. **Contract (REST)**  
   - Add to `rest_web_socket_api_contracts_f_1_telemetry.md`:
   - `GET /api/sessions/{sessionUid}/laps/{lapNum}/speed-trace?carIndex=0`
   - Response: array of `{ "distanceM": number, "speedKph": number }`, ordered by `distanceM`.
   - Source: `car_telemetry_raw` filtered by `session_uid`, `car_index`, `lap_number`; use `lap_distance_m` and `speed_kph`. If no data, return `[]`.

2. **Processing service**  
   - **LapQueryService** (or dedicated **SpeedTraceQueryService**): method `getSpeedTrace(sessionUid, carIndex, lapNum)`.
   - Resolve session (centralised resolve); optional: resolve lap exists.
   - Query: `CarTelemetryRawRepository` by `session_uid`, `car_index`, `lap_number`; select `lap_distance_m`, `speed_kph`; order by `lap_distance_m`.
   - Filter out nulls; map to DTO list (e.g. `SpeedTracePointDto`: distanceM, speedKph).
   - **Mapper:** entity/row → DTO in mapper package.

3. **REST controller**  
   - New endpoint in **LapController** (or **SessionDetailController**): GET `.../laps/{lapNum}/speed-trace`, query param `carIndex`. Thin: validate, call service, return list.

4. **DTO**  
   - In `telemetry-api-contracts` or processing module: `SpeedTracePointDto` (distanceM, speedKph). If contracts are shared, add to contracts; else keep in processing and document in REST contract.

5. **Tests**  
   - Unit tests for service (with mocked repo) and for controller (MockMvc). Use TestData for session/lap/telemetry samples. Assert 85% coverage per project policy.

### 4.2 Frontend

1. **Session Summary page**  
   - Add one **Speed chart** block (same layout area as Pace / Tyre wear / Pedal trace).
   - **Lap selector:** reuse same lap dropdown as for Pedal trace / ERS (sessionUid + lapNum + carIndex).

2. **Data**  
   - Fetch `GET /api/sessions/{sessionUid}/laps/{lapNum}/speed-trace?carIndex=0` when lap is selected (e.g. via TanStack Query keyed by sessionUid, lapNum, carIndex).

3. **Chart**  
   - X: `distanceM`, Y: `speedKph`. Use existing charting stack (e.g. Recharts). Tooltip: distance, speed. No corner markers in Phase 1.

4. **Docs**  
   - Update [react_spa_ui_architecture.md](../../project/react_spa_ui_architecture.md): Session Detail blocks + new endpoint.  
   - Update [telemetry_diagrams_plan.md](../../project/telemetry_diagrams_plan.md): add “Speed vs distance (Session Summary)” to historical charts.

### 4.3 Acceptance

- User opens Session Detail → selects a lap → sees one chart: speed (kph) vs distance (m) for that lap.
- Empty state: no data for that lap → show empty chart or message (consistent with trace/ERS).

### 4.4 Check

- `mvn -pl telemetry-processing-api-service verify` passes (including coverage).  
- REST contract and UI architecture docs updated.

---

## 5. Phase 2: Per-Corner Metrics (Steer-Based)

**Goal:** Compute **entry / apex / exit speed** per “corner” (segment) for one lap and expose via API; **show corner markers and turn numbers on the speed-vs-distance chart** so each turn is visible on the graph (e.g. labels “1”, “2”, “T1”, “T2” at apex/entry and optional vertical or shaded bands for segment bounds).

### 5.1 Corner Detection (Steer-Only)

- **Input:** Ordered points `(lap_distance_m, speed_kph, steer)` for one lap from `car_telemetry_raw`.
- **Algorithm:** From research doc § “Метод на основі керма (steer threshold)”:
  - Signal `S(d) = |steer|`.
  - Segment: `S(d) > steer_on` → corner start; `S(d) < steer_off` → corner end (hysteresis).
  - Configurable thresholds `steer_on` / `steer_off` (e.g. 0.05 / 0.03 or application properties).
- **Output:** List of segments `[startDistanceM, endDistanceM]`; apex = distance where speed is minimum in segment (or midpoint if no clear min).
- **Placement:** Stateless **CornerSegmenter** (or **SteerBasedCornerDetector**) in processing module; input: list of (distanceM, speedKph, steer); output: list of corner segments (start, end, apex distance). No DB yet.

### 5.2 Metrics per Segment

- For each segment: **entry_speed_kph** = speed at segment start (interpolate or nearest point); **exit_speed_kph** = speed at segment end; **apex_speed_kph** = min speed in segment (or at apex distance).
- **CornerMetricsAggregator** (or in LapQueryService): given lap’s speed trace + segments → list of corner metrics (cornerIndex, startDistanceM, endDistanceM, apexDistanceM, entrySpeedKph, apexSpeedKph, exitSpeedKph, durationMs optional).

### 5.3 API

- **Contract:**  
  - `GET /api/sessions/{sessionUid}/laps/{lapNum}/corners?carIndex=0`  
  - Response: list of corner objects: cornerIndex (1-based), startDistanceM, endDistanceM, apexDistanceM, entrySpeedKph, apexSpeedKph, exitSpeedKph, durationMs (optional).  
  - Implementation: load speed trace → run CornerSegmenter → run metrics aggregation → return DTOs. No persistence required for Phase 2.

- **Optional:**  
  - `GET /api/sessions/{sessionUid}/laps/{lapNum}/corners/{cornerIndex}/profile?bins=50`  
  - Response: resampled speed vs distance within that corner (research doc § JSON profile). Can be Phase 2b if needed for “one graph” (e.g. overlay corner zones on the speed chart).

### 5.4 Frontend

- **On the speed-vs-distance chart (required):** show **corner markers and/or turn numbers** directly on the graph:
  - **Turn numbers** (e.g. 1, 2, 3 … or T1, T2, T3) at the apex (or entry) of each corner segment, so the user can match segments to turn numbers along the lap.
  - **Markers:** vertical lines or shaded bands for entry/apex/exit (or at least apex) so each corner is visually identifiable on the speed curve.
- Implementation: use the same speed-vs-distance chart as in Phase 1; when `.../corners` data is loaded, render reference lines/labels or annotations at `startDistanceM`, `apexDistanceM`, `endDistanceM` with the corner index or label (e.g. “1”, “T1”). Recharts (or the chosen chart lib) supports reference lines and custom labels; ensure corner indices are visible and not overlapping.
- **Optional:** a small **Corner metrics table** below the chart (corner index, entry/apex/exit speed) as supplementary detail.
- Data: fetch `.../corners` when lap is selected; use same lap selector as speed trace.

### 5.5 Tests and Docs

- Unit tests for CornerSegmenter (given synthetic distance/steer/speed, expect segment list) and for aggregation.  
- REST contract and UI docs updated.

---

## 6. Phase 3: Stable Corner Map and Persisted Metrics

**Goal:** Persist **track-level corner map** (per trackId + trackLength) and **per-lap corner metrics** in DB; API can return corner names and stable corner_index; support comparison across laps later.

### 6.1 DB

- **track_corner_maps:** (track_id, track_length_m, version, created_at, algorithm_params jsonb).  
- **track_corners:** (map_id FK, corner_index, start_distance_m, end_distance_m, apex_distance_m, direction, name nullable).  
- **lap_corner_metrics:** (session_uid, car_index, lap_number, corner_index, entry_speed_kph, apex_speed_kph, exit_speed_kph, min/avg/max_speed_kph, duration_ms). Unique (session_uid, car_index, lap_number, corner_index).  
- **lap_corner_profiles:** optional; (lap_id, corner_index, bins, speed_kph real[], distance_m real[]) for resampled profile storage.

Use **session_uid + car_index + lap_number** as lap identity (no new lap_id UUID) to match existing `laps` table.

### 6.2 Corner Map Builder

- **Corner Model Builder** (research doc): Build corner map from first N valid flying laps (e.g. from `car_telemetry_raw` + lap boundaries). Cluster segments by apex distance; stabilize start/end (e.g. median). Store in `track_corner_maps` + `track_corners`. Trigger: on session end or on-demand when first lap for a track is requested.

### 6.3 Persistence of Metrics

- When a lap is finalized (or on-demand when user requests corners for a lap): run corner detection (using track corner map if available, else steer-only segments), compute metrics, **upsert** into `lap_corner_metrics`. Idempotent by (session_uid, car_index, lap_number, corner_index).

### 6.4 API

- `GET /api/sessions/{sessionUid}/laps/{lapNum}/corners` — can now return corner **names** from track_corners and optionally mapVersion.  
- `GET /api/tracks/{trackId}/corner-maps/latest?trackLengthM=` — return current corner map for track (for UI or diagnostics).  
- Document in REST contract; keep session-scoped lap endpoints as primary.

### 6.5 Frontend

- Optional: show corner names (e.g. “T1”, “T2”) on chart or table; “Corner map version” in UI for debugging. No obligation to implement full Corner Dashboard in this plan.

---

## 7. Phase 4: Motion Data and Advanced Corner Detection

**Goal:** Ingest **PacketMotionData** (packet 0); add **m_gForceLateral**, **m_yaw**, **m_worldPositionX/Z** to the pipeline; implement curvature/yaw-based (and combined) corner detection as in the research document.

### 7.1 Ingest

- **UDP:** Motion packet parser (ByteBuffer → MotionDto); handler publishes to Kafka (new topic or existing pattern). See [future_features_ideas.md](../../project/future_features_ideas.md) Motion section.  
- **Kafka:** Either new topic `telemetry.motion` or extend envelope contract; consumer in processing service.  
- **DB:** Either extend `car_telemetry_raw` with columns `g_lat`, `yaw`, `pos_x`, `pos_z` or new table `motion_raw` keyed by (ts, session_uid, frame_identifier, car_index). Choice: avoid duplication; if we merge “frame” server-side, one option is a single “frames” table as in the report; otherwise keep telemetry and motion separate and join by frame in application.

### 7.2 Frame Alignment

- For corner detection that uses both steer and G-lateral/yaw: align by `(session_uid, frame_identifier, car_index)`. No need for a separate “Frame Assembler” service if we join in the processing service when building the list of points for a lap (telemetry + motion from DB or from streams).

### 7.3 Corner Algorithm

- Implement **combined** and **curvature** methods from research doc (§ “Метод curvature по world-координатах”, “Комбінований production детектор”). Use stabilized corner map (Phase 3) and optionally recompute with new algorithm; version corner maps by algorithm_params.

### 7.4 API / UI

- No change to “one graph” scope; backend can use better segments and apex for existing endpoints. Track map (2D polyline from worldPositionX/Z) and Corner Detail overlay can be added in a separate feature.

---

## 8. Implementation Order and Checklist

- **Phase 1** (MVP): Speed trace endpoint + one chart on Session Summary.  
- **Phase 2**: Steer-based corner detection + corners endpoint + markers or table on same page.  
- **Phase 3**: DB corner map + lap_corner_metrics + track corner map API.  
- **Phase 4**: Motion ingest + curvature/yaw corner detection.

**Dependencies:**  
- Phase 1: none.  
- Phase 2: Phase 1 (same chart + data).  
- Phase 3: Phase 2 (segmenter + metrics logic).  
- Phase 4: Motion UDP + Kafka + DB and/or frame alignment.

**Documentation updates (each phase):**  
- [rest_web_socket_api_contracts_f_1_telemetry.md](../../project/rest_web_socket_api_contracts_f_1_telemetry.md) — new endpoints and DTOs.  
- [react_spa_ui_architecture.md](../../project/react_spa_ui_architecture.md) — Session Detail blocks and data flow.  
- [telemetry_diagrams_plan.md](../../project/telemetry_diagrams_plan.md) — new chart type.  
- [documentation_index.md](../../project/documentation_index.md) — link to this plan and to speed-graf report.

---

## 9. Summary

- The **research document** is aligned with the project by: using existing **lap distance** and **car_telemetry_raw** (speed, steer), keeping **sessionUid + lapNum** in REST, and deferring **Motion** and new Kafka topics to later phases.  
- The **one graph** for Session Summary is delivered in **Phase 1** (speed vs distance); **Phase 2** adds per-corner metrics (steer-based) on the same page; Phases 3–4 add persistence and Motion-based detection without changing the “one graph” scope.
