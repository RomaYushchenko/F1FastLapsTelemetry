-- Migration: add public_id (UUID) to telemetry.sessions for API/UI.
-- Run once on existing databases that were created before this column existed.
-- New installs get public_id from 03-sessions.sql.

ALTER TABLE telemetry.sessions ADD COLUMN IF NOT EXISTS public_id UUID;
UPDATE telemetry.sessions SET public_id = gen_random_uuid() WHERE public_id IS NULL;
ALTER TABLE telemetry.sessions ALTER COLUMN public_id SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_sessions_public_id ON telemetry.sessions(public_id);
