-- Add tyres_age_laps to car_status_raw for optional fallback (tyre age in laps when car damage is disabled).

ALTER TABLE telemetry.car_status_raw
  ADD COLUMN IF NOT EXISTS tyres_age_laps SMALLINT NULL;
