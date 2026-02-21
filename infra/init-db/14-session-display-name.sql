-- Session display name: user-facing label (default = public_id). Plan: 03-session-page.md Etap 1.

ALTER TABLE telemetry.sessions ADD COLUMN IF NOT EXISTS session_display_name VARCHAR(64);

UPDATE telemetry.sessions SET session_display_name = public_id::text WHERE session_display_name IS NULL;

ALTER TABLE telemetry.sessions ALTER COLUMN session_display_name SET NOT NULL;
