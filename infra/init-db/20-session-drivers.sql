-- Session drivers: car_index -> display label (e.g. "VER", "HAM") for leaderboard and events.
-- Block E — Live leaderboard. Populate from game/participant data or config; null = fallback "Car N".

CREATE TABLE IF NOT EXISTS telemetry.session_drivers (
  session_uid   BIGINT      NOT NULL,
  car_index     SMALLINT    NOT NULL,
  driver_label  VARCHAR(16) NULL,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (session_uid, car_index),
  CONSTRAINT fk_session_drivers_session FOREIGN KEY (session_uid) REFERENCES telemetry.sessions(session_uid) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_session_drivers_session_uid ON telemetry.session_drivers(session_uid);
