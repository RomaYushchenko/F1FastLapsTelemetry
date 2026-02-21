-- Finishing position per session/car (from LapData carPosition at session end). Plan: 03-session-page.md Etap 3.

CREATE TABLE IF NOT EXISTS telemetry.session_finishing_positions (
  session_uid        BIGINT      NOT NULL REFERENCES telemetry.sessions(session_uid) ON DELETE CASCADE,
  car_index          SMALLINT    NOT NULL,
  finishing_position INTEGER    NOT NULL,

  PRIMARY KEY (session_uid, car_index)
);

CREATE INDEX IF NOT EXISTS idx_session_finishing_positions_session_uid
  ON telemetry.session_finishing_positions(session_uid);
