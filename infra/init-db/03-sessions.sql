-- Етап 2.4–2.5: sessions та session_cars
-- Джерело: f_1_telemetry_project_architecture.md § 9.3

-- 2.4 telemetry.sessions
CREATE TABLE IF NOT EXISTS telemetry.sessions (
  session_uid        BIGINT PRIMARY KEY,
  public_id          UUID          NOT NULL DEFAULT gen_random_uuid() UNIQUE,

  packet_format      SMALLINT      NOT NULL,
  game_major_version SMALLINT      NOT NULL,
  game_minor_version SMALLINT      NOT NULL,

  session_type       SMALLINT      NULL,
  track_id           SMALLINT      NULL,
  track_length_m     INTEGER       NULL,
  total_laps         SMALLINT      NULL,
  player_car_index   SMALLINT      NULL,
  ai_difficulty      SMALLINT      NULL,

  started_at         TIMESTAMPTZ   NULL,
  ended_at           TIMESTAMPTZ   NULL,
  end_reason         VARCHAR(32)   NULL,

  created_at         TIMESTAMPTZ   NOT NULL DEFAULT now(),
  updated_at         TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_sessions_started_at ON telemetry.sessions(started_at DESC);

-- 2.5 telemetry.session_cars
CREATE TABLE IF NOT EXISTS telemetry.session_cars (
  session_uid   BIGINT      NOT NULL REFERENCES telemetry.sessions(session_uid) ON DELETE CASCADE,
  car_index     SMALLINT    NOT NULL,
  is_player     BOOLEAN     NOT NULL DEFAULT false,

  created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),

  PRIMARY KEY (session_uid, car_index)
);

CREATE INDEX IF NOT EXISTS idx_session_cars_player ON telemetry.session_cars(session_uid) WHERE is_player = true;
