# Follow-up: Curvature and Yaw-Rate Corner Detection

## Goal

Add **curvature-based** and **yaw-rate-based** corner detection as in the research document, so that:

- Corners can be detected from **geometry** (world X/Z) and **rotation** (yaw) even when steer or G-lateral are noisy or missing.
- **Apex** can be chosen by **max curvature** instead of min speed when using the curvature method.
- Optionally, a **weighted combined** production detector (`wS*|steer| + wG*|gLat| + wK*|kappa_yaw| + wC*curvature`) improves robustness across track types.

**Why:** Research doc ([speed-graf-deep-research-report.md](../speed-graf-deep-research-report.md)) recommends curvature for better apex and yaw-rate for “curvature without coordinates”; combined weighting reduces dependence on a single signal.

## Current State

- Motion data is in the pipeline: **motion_raw** has `g_force_lateral`, `yaw`, `world_pos_x`, `world_pos_z` per (session_uid, frame_identifier, car_index).
- **LapQueryService.getCorners()** merges motion into points; **SteerBasedCornerSegmenter** (and follow-up combined segmenter) use steer and optionally G-lateral. **Curvature and yaw are not computed or used yet.**

## What Is Needed

### 1. Enrich points with derived signals

For each point (aligned by frame: telemetry + motion), compute and attach:

- **Yaw-rate / kappa_yaw**  
  - From `m_yaw` (radians) and lap distance:  
    `kappa_yaw ≈ (yaw[i] - yaw[i-1]) / max(d[i]-d[i-1], ε)` (unwrap yaw if needed).  
  - Segment: `|kappa_yaw| > kappa_on` → corner; `|kappa_yaw| < kappa_off` → straight (with hysteresis).

- **Curvature (from world X/Z)**  
  - From `world_pos_x`, `world_pos_z` and distance: for three consecutive points p0, p1, p2,  
    `angle = angleBetween(p1-p0, p2-p1)`, `arc_length = |p1-p0| + |p2-p1|`,  
    `curvature ≈ angle / max(arc_length, ε)`.  
  - Optional: smooth over a small window (e.g. 3–5 points) to reduce noise.  
  - Segment: `curvature > c_on` (with hysteresis).  
  - Apex in segment: distance at **max curvature** (research doc) instead of min speed.

These can live in a **point DTO** used only inside the processing service (e.g. extended `SteerBasedCornerSegmenter.Point` or a new `CornerDetectionPoint` with optional curvature, kappaYaw, gLat, steer).

### 2. Curvature-based segmenter

- New class (e.g. **CurvatureBasedCornerSegmenter**):
  - Input: list of points with `distanceM`, `speedKph`, `worldPosX`, `worldPosZ` (and optionally steer/gLat for hybrid).
  - Compute curvature per point (and optionally smooth).
  - Hysteresis: enter corner when curvature > c_on, exit when < c_off.
  - Apex = distance at **max curvature** in segment.
  - Min segment length (e.g. 20 m) and min point count as today.

- Requires **world_pos_x/z** on points: already in **MotionRaw**; when building points in getCorners, add them from the motion map (same way as gForceLateral).

### 3. Yaw-rate segmenter (optional)

- New class or same “advanced” segmenter:
  - Input: points with `distanceM`, `yaw` (and optionally speed for apex = min speed in segment).
  - Compute kappa_yaw; hysteresis with kappa_on / kappa_off.
  - Apex: min speed in segment (or keep consistent with curvature segmenter if both are used).

### 4. Combined production detector (optional)

- As in research:  
  `corner_signal = wS*|steer| + wG*|gLat| + wK*|kappa_yaw| + wC*curvature`  
  (weights configurable; normalise so thresholds are in 0–1 or similar).
- One segmenter that uses this scalar signal with on/off hysteresis; apex can be max of this signal or min speed.
- Requires all four signals on the point; use defaults (0) when a signal is missing so existing laps without motion still work.

### 5. Integration with LapQueryService and corner map

- **LapQueryService.getCorners()**:
  - Build points with: distance, speed, steer, gLat, **yaw**, **worldPosX**, **worldPosZ** from telemetry + motion.
  - Option A: Compute curvature and kappa_yaw in service or in a “point builder” and pass enriched points to segmenter.
  - Option B: Segmenter receives raw points and motion map and computes curvature/yaw internally (less transparent, more coupling).
- Choose **which segmenter** to use: e.g. config `f1.corner.algorithm=steer|combined|curvature|production`. Default `combined` (after 01-combined-segmenter.md) or `production` when implemented.
- **Track corner map (Phase 3)** and **lap_corner_metrics**: continue to use segment start/end/apex from the chosen segmenter; apex definition may change (min speed vs max curvature) per algorithm. Optionally store `algorithm_params` in **track_corner_maps** (e.g. algorithm name + thresholds) for traceability.

### 6. Configuration

- New properties, e.g.:
  - `f1.corner.curvature-on`, `f1.corner.curvature-off`
  - `f1.corner.kappa-yaw-on`, `f1.corner.kappa-yaw-off`
  - For combined: `f1.corner.weight-steer`, `f1.corner.weight-glat`, `f1.corner.weight-kappa`, `f1.corner.weight-curvature`
- Document in application.yml and Plan 13.

### 7. Tests

- Unit tests: synthetic points with known curvature (e.g. arc); segmenter returns one segment with apex at max curvature.
- Yaw: synthetic points with monotonic yaw increase over distance; segmenter finds one segment.
- Combined: weighted sum behaves as expected for on/off thresholds.

### 8. Documentation

- Plan 13 § 7.3: curvature and yaw-rate methods implemented; optional combined production detector.
- REST/API: no change to response shape; only backend algorithm and possibly apex semantics (min speed vs max curvature) documented in API or release notes.

## Acceptance

- For laps with motion data, corners can be detected using (a) curvature only, (b) yaw-rate only, or (c) weighted combined signal, selectable by config.
- Apex can be derived from max curvature when using the curvature method.
- Existing laps without motion still work (steer-only or combined steer+gLat; curvature/yaw fall back to no segment when motion missing).

## Dependencies

- Motion ingest and **motion_raw** with **yaw**, **world_pos_x**, **world_pos_z** — **done**.
- Points in getCorners already merged with motion; extend to pass worldPosX/Z and yaw into segmenter or point builder.
- Optional: [01-combined-segmenter.md](01-combined-segmenter.md) done first so “combined” in production detector includes steer + gLat.

## Order of implementation

1. Add **worldPosX**, **worldPosZ**, **yaw** to the point type used in getCorners (from existing motion map).
2. Implement **CurvatureBasedCornerSegmenter** (curvature from X/Z, apex = max curvature).
3. Add **kappa_yaw** computation and optional **YawRateCornerSegmenter** (or integrate into one “advanced” segmenter).
4. Add **combined production detector** (weighted sum) and config to choose algorithm.
5. Wire algorithm selection in **LapQueryService** and document algorithm_params in corner map if desired.
