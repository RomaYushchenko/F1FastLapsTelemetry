-- Етап 2.7: car_telemetry_raw + hypertable
-- Джерело: f_1_telemetry_project_architecture.md § 9.5.1

CREATE TABLE IF NOT EXISTS telemetry.car_telemetry_raw (
  ts               TIMESTAMPTZ NOT NULL,

  session_uid      BIGINT      NOT NULL,
  frame_identifier INTEGER     NOT NULL,
  car_index        SMALLINT    NOT NULL,

  speed_kph        SMALLINT    NULL,
  throttle         REAL        NULL,
  steer            REAL        NULL,
  brake            REAL        NULL,
  gear             SMALLINT    NULL,
  engine_rpm       INTEGER     NULL,
  drs              SMALLINT    NULL,

  session_time_s   REAL        NULL,

  PRIMARY KEY (ts, session_uid, frame_identifier, car_index)
);

SELECT create_hypertable('telemetry.car_telemetry_raw', 'ts', if_not_exists => TRUE);

CREATE INDEX IF NOT EXISTS idx_car_tel_raw_session_time ON telemetry.car_telemetry_raw(session_uid, ts DESC);
CREATE INDEX IF NOT EXISTS idx_car_tel_raw_frame ON telemetry.car_telemetry_raw(session_uid, frame_identifier);
