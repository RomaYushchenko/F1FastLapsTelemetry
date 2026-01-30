-- Етап 2.9: laps
-- Джерело: f_1_telemetry_project_architecture.md § 9.6.1

CREATE TABLE IF NOT EXISTS telemetry.laps (
  session_uid          BIGINT      NOT NULL REFERENCES telemetry.sessions(session_uid) ON DELETE CASCADE,
  car_index            SMALLINT    NOT NULL,

  lap_number           SMALLINT    NOT NULL,

  lap_time_ms          INTEGER     NULL,
  sector1_time_ms      INTEGER     NULL,
  sector2_time_ms      INTEGER     NULL,
  sector3_time_ms      INTEGER     NULL,

  is_invalid           BOOLEAN     NOT NULL DEFAULT false,
  penalties_seconds    SMALLINT    NULL,

  started_at           TIMESTAMPTZ NULL,
  ended_at             TIMESTAMPTZ NULL,

  created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),

  PRIMARY KEY (session_uid, car_index, lap_number)
);

CREATE INDEX IF NOT EXISTS idx_laps_session_car ON telemetry.laps(session_uid, car_index, lap_number DESC);
CREATE INDEX IF NOT EXISTS idx_laps_session_time ON telemetry.laps(session_uid, ended_at DESC);
