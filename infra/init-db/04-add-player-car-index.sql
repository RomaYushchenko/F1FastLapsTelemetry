-- Add player_car_index to existing sessions table (ingest sends only player car; this identifies which car index).
ALTER TABLE telemetry.sessions ADD COLUMN IF NOT EXISTS player_car_index SMALLINT NULL;
