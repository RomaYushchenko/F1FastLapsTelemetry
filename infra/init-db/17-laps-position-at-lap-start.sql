-- Add race position at lap start (from LapData carPosition when lap was entered).
-- Plan: 04-session-summary-page.md Etap 4.

ALTER TABLE telemetry.laps
  ADD COLUMN IF NOT EXISTS position_at_lap_start INTEGER NULL;

COMMENT ON COLUMN telemetry.laps.position_at_lap_start IS 'Race position at the start of this lap (from LapData m_carPosition).';
