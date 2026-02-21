-- Add tyre compound (F1 25 m_actualTyreCompound) to tyre_wear_per_lap.
-- Plan: 04-session-summary-page.md Etap 3.

ALTER TABLE telemetry.tyre_wear_per_lap
  ADD COLUMN IF NOT EXISTS compound SMALLINT NULL;

COMMENT ON COLUMN telemetry.tyre_wear_per_lap.compound IS 'F1 25 actual tyre compound at end of lap (e.g. 16=C5, 18=C3, 7=inter, 8=wet).';
