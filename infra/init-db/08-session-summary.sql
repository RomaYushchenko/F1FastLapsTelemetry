-- Етап 2.10: session_summary
-- Джерело: f_1_telemetry_project_architecture.md § 9.6.2

CREATE TABLE IF NOT EXISTS telemetry.session_summary (
  session_uid           BIGINT      NOT NULL REFERENCES telemetry.sessions(session_uid) ON DELETE CASCADE,
  car_index             SMALLINT    NOT NULL,

  total_laps            SMALLINT    NULL,
  best_lap_time_ms      INTEGER     NULL,
  best_lap_number       SMALLINT    NULL,

  best_sector1_ms       INTEGER     NULL,
  best_sector2_ms       INTEGER     NULL,
  best_sector3_ms       INTEGER     NULL,

  last_updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),

  PRIMARY KEY (session_uid, car_index)
);

CREATE INDEX IF NOT EXISTS idx_session_summary_best_lap ON telemetry.session_summary(best_lap_time_ms);
