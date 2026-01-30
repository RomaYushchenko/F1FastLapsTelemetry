-- Етап 2.8: car_status_raw + hypertable
-- Джерело: f_1_telemetry_project_architecture.md § 9.5.2

CREATE TABLE IF NOT EXISTS telemetry.car_status_raw (
  ts               TIMESTAMPTZ NOT NULL,

  session_uid      BIGINT      NOT NULL,
  frame_identifier INTEGER     NOT NULL,
  car_index        SMALLINT    NOT NULL,

  traction_control SMALLINT    NULL,
  abs              SMALLINT    NULL,
  fuel_in_tank     REAL        NULL,
  fuel_mix         SMALLINT    NULL,
  drs_allowed      BOOLEAN     NULL,
  tyres_compound    SMALLINT   NULL,
  ers_store_energy REAL        NULL,

  session_time_s   REAL        NULL,

  PRIMARY KEY (ts, session_uid, frame_identifier, car_index)
);

SELECT create_hypertable('telemetry.car_status_raw', 'ts', if_not_exists => TRUE);

CREATE INDEX IF NOT EXISTS idx_car_status_raw_session_time ON telemetry.car_status_raw(session_uid, ts DESC);
