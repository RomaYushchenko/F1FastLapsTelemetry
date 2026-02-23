# Follow-up: Combined Segmenter (Steer OR G-Force Lateral)

## Goal

Improve corner detection so that **high-speed light turns** (low steer, high lateral G) are recognised as corners, not missed. Today only `|steer| > steerOn` starts a corner; when motion data is available, also treat `|gForceLateral| > gLatOn` as corner entry.

**Why:** Steer-based detection can merge such turns with straights; G-lateral better captures load in fast corners (see [speed-graf-deep-research-report.md](../speed-graf-deep-research-report.md) § G-lateral method).

## Current State

- **SteerBasedCornerSegmenter** uses hysteresis: `|steer| > steerOn` → in corner, `|steer| < steerOff` → exit. Apex = min speed in segment.
- **LapQueryService.getCorners()** already loads motion for the lap and builds **Point**(distanceM, speedKph, steer, **gForceLateral**). The fourth field is set when motion exists for that frame but is **not used** by the segmenter yet.

## What Is Needed

### 1. Corner signal (combined criterion)

- Define **corner signal** per point:
  - If `gForceLateral != null`: `inCorner = (|steer| > steerOn) OR (|gForceLateral| > gLatOn)` (with hysteresis: exit when both below off thresholds).
  - If `gForceLateral == null`: keep current behaviour (steer only).
- Configurable thresholds:
  - `f1.corner.g-lat-on` (e.g. 0.3–0.5 G) and `f1.corner.g-lat-off` (e.g. 0.2 G), with hysteresis to avoid flicker.

### 2. Implementation options

**Option A — extend SteerBasedCornerSegmenter**

- In `detect(List<Point> points)`:
  - For each point, compute `inCornerCandidate = (Math.abs(p.steer()) > steerOn) || (p.gForceLateral() != null && Math.abs(p.gForceLateral()) > gLatOn)`.
  - Exit when `(Math.abs(steer) < steerOff) && (gLat is null or |gLat| < gLatOff)`.
- Reuse existing segment length check and apex = min speed in segment.
- Add `@Value` for `gLatOn` / `gLatOff` with defaults; if motion is absent for the whole lap, behaviour equals current steer-only.

**Option B — new CombinedCornerSegmenter**

- New class that accepts the same `Point` type and implements the combined logic; inject it in **LapQueryService** (e.g. via strategy or config) so that when motion is present we use Combined, otherwise SteerBased. More code but keeps SteerBased unchanged.

Recommendation: **Option A** (extend existing segmenter) to avoid duplication and a single place for thresholds.

### 3. Tests

- Unit tests: points with low steer but high |gForceLateral| form one segment; points with both below thresholds do not; hysteresis (gLatOff < gLatOn) closes segment correctly.
- Reuse **SteerBasedCornerSegmenterTest** style; add cases with `Point(..., gForceLateral)`.

### 4. Configuration

- In `application.yml` (or profile): document `f1.corner.g-lat-on`, `f1.corner.g-lat-off`. Optional: feature flag `f1.corner.use-combined-signal=true` to fall back to steer-only if needed.

### 5. Documentation

- Plan 13 § 7.3: note that “combined” criterion (steer OR gLat) is implemented in the segmenter.
- This file: update “Current State” to “Implemented” once done.

## Acceptance

- For a lap where motion data exists and some points have high |gForceLateral| but low |steer|, the API `GET .../laps/{lapNum}/corners` returns segments that include those turns.
- No change to REST contract or DB schema; only backend segmenter logic and config.

## Dependencies

- Motion ingest and `motion_raw` (Phase 4) — **done**.
- Points in getCorners already carry optional `gForceLateral` — **done**.
