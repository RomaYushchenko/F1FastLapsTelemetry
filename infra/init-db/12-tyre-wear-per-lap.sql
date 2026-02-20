-- Tyre wear per lap: one row per (session, car, lap) with wear % per wheel.
-- Populated when a lap is finalized from last Car Damage (packet 10) snapshot.

CREATE TABLE IF NOT EXISTS telemetry.tyre_wear_per_lap (
  session_uid   BIGINT    NOT NULL,
  car_index    SMALLINT  NOT NULL,
  lap_number   SMALLINT  NOT NULL,
  wear_fl      REAL      NULL,
  wear_fr      REAL      NULL,
  wear_rl      REAL      NULL,
  wear_rr      REAL      NULL,
  PRIMARY KEY (session_uid, car_index, lap_number)
);

CREATE INDEX IF NOT EXISTS idx_tyre_wear_per_lap_session_car
  ON telemetry.tyre_wear_per_lap(session_uid, car_index, lap_number ASC);
