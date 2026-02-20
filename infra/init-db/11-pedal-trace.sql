-- Pedal trace: lap_number and lap_distance_m for GET .../laps/{lapNum}/trace.
-- Джерело: PEDAL_TRACE_FEATURE_ANALYSIS_AND_PLAN.md

ALTER TABLE telemetry.car_telemetry_raw
  ADD COLUMN IF NOT EXISTS lap_number SMALLINT NULL,
  ADD COLUMN IF NOT EXISTS lap_distance_m REAL NULL;

CREATE INDEX IF NOT EXISTS idx_car_tel_raw_lap_trace
  ON telemetry.car_telemetry_raw(session_uid, car_index, lap_number, frame_identifier)
  WHERE lap_number IS NOT NULL;
