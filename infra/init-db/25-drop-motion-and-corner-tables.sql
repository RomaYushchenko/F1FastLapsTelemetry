-- One-time migration for databases that were created with init-db/18 and 19.
-- Run manually on existing environments; do not mount as default init (fresh installs no longer create these tables).
-- Order: children first (lap_corner_metrics has no FK to track tables; track_corners references track_corner_maps).

DROP TABLE IF EXISTS telemetry.lap_corner_metrics;
DROP TABLE IF EXISTS telemetry.track_corners;
DROP TABLE IF EXISTS telemetry.track_corner_maps;
DROP TABLE IF EXISTS telemetry.motion_raw;
