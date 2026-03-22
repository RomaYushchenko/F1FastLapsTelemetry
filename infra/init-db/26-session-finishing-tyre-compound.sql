-- Tyre compound (S/M/H) at session end per car, from CarStatus visual compound at SEND.

ALTER TABLE telemetry.session_finishing_positions
    ADD COLUMN IF NOT EXISTS tyre_compound VARCHAR(1) NULL;

COMMENT ON COLUMN telemetry.session_finishing_positions.tyre_compound IS 'Tyre compound at session end: S, M, or H; null if unknown.';
