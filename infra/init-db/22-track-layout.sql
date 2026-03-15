-- Track layout (2D map): centreline/outline points for Live Track Map. Block F — B8.
-- See: .github/draft/implementation-plans/new-ui-backend/block-f-live-track-map.md

CREATE TABLE IF NOT EXISTS telemetry.track_layout (
  track_id   SMALLINT    NOT NULL PRIMARY KEY,
  points     JSONB      NOT NULL,
  version    SMALLINT    NOT NULL DEFAULT 1,
  min_x      DOUBLE PRECISION NULL,
  min_y      DOUBLE PRECISION NULL,
  max_x      DOUBLE PRECISION NULL,
  max_y      DOUBLE PRECISION NULL
);

COMMENT ON TABLE telemetry.track_layout IS '2D layout points per F1 track for Live Track Map; bounds optional for client viewBox.';