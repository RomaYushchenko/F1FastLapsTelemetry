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

-- Silverstone (track_id 8): simplified centreline approximating the circuit shape.
INSERT INTO telemetry.track_layout (track_id, points, version, min_x, min_y, max_x, max_y)
VALUES (
  8,
  '[
    {"x":100,"y":300},{"x":100,"y":200},{"x":150,"y":150},{"x":250,"y":100},{"x":400,"y":100},{"x":550,"y":100},
    {"x":700,"y":100},{"x":700,"y":250},{"x":700,"y":400},{"x":700,"y":500},{"x":600,"y":500},{"x":400,"y":500},
    {"x":200,"y":500},{"x":100,"y":500},{"x":100,"y":400},{"x":100,"y":300}
  ]'::jsonb,
  1,
  100,
  100,
  700,
  500
)
ON CONFLICT (track_id) DO NOTHING;
