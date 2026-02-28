# Phase 4 Follow-up: Corner Detection Improvements

This folder contains **deferred items** from [Plan 13 — Session Summary Speed/Corner Graph](../implementation-plans/13-session-summary-speed-corner-graph.md) **Phase 4**.

Phase 4 (Motion ingest) is **done**: Motion packets are parsed, published to `telemetry.motion`, stored in `motion_raw`, and merged into corner points in `getCorners()`. The segmenter still uses **steer-only** logic.

The documents below describe what remains to improve corner detection using motion data:

| Document | Goal | Prerequisite |
|----------|------|--------------|
| [01-combined-segmenter.md](01-combined-segmenter.md) | Use steer **or** G-force lateral so high-speed light turns are detected | Motion in pipeline (done) |
| [02-curvature-yaw-corner-detection.md](02-curvature-yaw-corner-detection.md) | Curvature (world X/Z) and yaw-rate methods; optional weighted combined detector | Motion in pipeline (done) |

Reference: [speed-graf-deep-research-report.md](../speed-graf-deep-research-report.md) (§ corner detection methods, combined production detector).
