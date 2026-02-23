-- Motion data for corner detection (G-force lateral, yaw, world position). Join with car_telemetry_raw by (session_uid, frame_identifier, car_index).
-- Plan: 13-session-summary-speed-corner-graph.md Phase 4.

CREATE TABLE IF NOT EXISTS telemetry.motion_raw (
  ts               TIMESTAMPTZ NOT NULL,
  session_uid      BIGINT      NOT NULL,
  frame_identifier INTEGER     NOT NULL,
  car_index        SMALLINT    NOT NULL,

  g_force_lateral  REAL        NULL,
  yaw              REAL        NULL,
  world_pos_x      REAL        NULL,
  world_pos_z      REAL        NULL,

  PRIMARY KEY (ts, session_uid, frame_identifier, car_index)
);

SELECT create_hypertable('telemetry.motion_raw', 'ts', if_not_exists => TRUE);

CREATE INDEX IF NOT EXISTS idx_motion_raw_session_frame
  ON telemetry.motion_raw(session_uid, car_index, frame_identifier);
