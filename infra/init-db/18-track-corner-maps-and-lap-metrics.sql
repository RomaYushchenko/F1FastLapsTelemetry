-- Phase 3 (plan 13): track-level corner maps and per-lap corner metrics.
-- See: .github/draft/implementation-plans/13-session-summary-speed-corner-graph.md §6.

-- Track corner map: one row per (track_id, track_length_m, version).
CREATE TABLE IF NOT EXISTS telemetry.track_corner_maps (
  id                BIGSERIAL     PRIMARY KEY,
  track_id          SMALLINT     NOT NULL,
  track_length_m    INTEGER      NOT NULL,
  version           INTEGER      NOT NULL DEFAULT 1,
  created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
  algorithm_params  JSONB        NULL,

  UNIQUE (track_id, track_length_m, version)
);

CREATE INDEX IF NOT EXISTS idx_track_corner_maps_track ON telemetry.track_corner_maps(track_id, track_length_m);

-- Corners belonging to a map (start/end/apex distance, optional name).
CREATE TABLE IF NOT EXISTS telemetry.track_corners (
  map_id              BIGINT      NOT NULL REFERENCES telemetry.track_corner_maps(id) ON DELETE CASCADE,
  corner_index        SMALLINT    NOT NULL,
  start_distance_m    REAL        NOT NULL,
  end_distance_m      REAL        NOT NULL,
  apex_distance_m     REAL        NOT NULL,
  direction           SMALLINT    NULL,
  name                VARCHAR(16) NULL,

  PRIMARY KEY (map_id, corner_index)
);

-- Per-lap corner metrics (idempotent upsert by session_uid, car_index, lap_number, corner_index).
CREATE TABLE IF NOT EXISTS telemetry.lap_corner_metrics (
  session_uid       BIGINT    NOT NULL,
  car_index         SMALLINT  NOT NULL,
  lap_number        SMALLINT  NOT NULL,
  corner_index      SMALLINT  NOT NULL,

  entry_speed_kph   SMALLINT  NULL,
  apex_speed_kph    SMALLINT  NULL,
  exit_speed_kph    SMALLINT  NULL,
  min_speed_kph     SMALLINT  NULL,
  avg_speed_kph     SMALLINT  NULL,
  max_speed_kph     SMALLINT  NULL,
  duration_ms       INTEGER   NULL,

  PRIMARY KEY (session_uid, car_index, lap_number, corner_index)
);

CREATE INDEX IF NOT EXISTS idx_lap_corner_metrics_session ON telemetry.lap_corner_metrics(session_uid, car_index, lap_number);
