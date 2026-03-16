-- Extend track_layout for auto-recording and 3D elevation (plan 14, Phase 1 — Step 1.1)

ALTER TABLE telemetry.track_layout
    ADD COLUMN IF NOT EXISTS min_elev    DOUBLE PRECISION NULL,
    ADD COLUMN IF NOT EXISTS max_elev    DOUBLE PRECISION NULL,
    ADD COLUMN IF NOT EXISTS source      VARCHAR(10) NOT NULL DEFAULT 'STATIC',
    ADD COLUMN IF NOT EXISTS recorded_at TIMESTAMPTZ NULL,
    ADD COLUMN IF NOT EXISTS session_uid BIGINT NULL,
    ADD COLUMN IF NOT EXISTS sector_boundaries JSONB NULL;

ALTER TABLE telemetry.track_layout
    ADD CONSTRAINT IF NOT EXISTS chk_track_layout_source
    CHECK (source IN ('STATIC', 'RECORDED'));

COMMENT ON COLUMN telemetry.track_layout.min_elev IS
    'Min worldPositionY (elevation) of recorded track in metres';
COMMENT ON COLUMN telemetry.track_layout.max_elev IS
    'Max worldPositionY (elevation) of recorded track in metres';
COMMENT ON COLUMN telemetry.track_layout.source IS
    'STATIC = manually created; RECORDED = auto-recorded from UDP telemetry';
COMMENT ON COLUMN telemetry.track_layout.sector_boundaries IS
    '[{sector:1|2|3, x, y, z}] — XYZ world position at start of each sector';

-- Update Silverstone mock: convert {"x","y"} → {"x","y","z"}
-- (old "y" was worldPositionZ; elevation y = 0 because flat track)
UPDATE telemetry.track_layout
SET
    points = (
        SELECT jsonb_agg(
            jsonb_build_object(
                'x', (el->>'x')::float,
                'y', 0.0,
                'z', (el->>'y')::float
            )
        )
        FROM jsonb_array_elements(points) AS el
    ),
    min_elev = 0.0,
    max_elev = 0.0,
    sector_boundaries = NULL
WHERE track_id = 8;

