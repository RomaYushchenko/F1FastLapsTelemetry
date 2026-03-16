-- Migrate all STATIC track layouts from legacy {x,y} to new {x,y,z} format.
-- Legacy format: x = worldPositionX, y = worldPositionZ, no z field.
-- New format:    x = worldPositionX, y = elevation (worldPositionY, 0.0 for static), z = worldPositionZ.
-- Applies only to rows where points are still in the old shape (no 'z' key in first element).

UPDATE telemetry.track_layout
SET points = (
        SELECT jsonb_agg(
                       CASE
                           -- If point already has z, keep as is
                           WHEN (el->>'z') IS NOT NULL THEN el
                           -- Otherwise convert legacy {x,y} → {x,y,z}
                           ELSE jsonb_build_object(
                                   'x', (el->>'x')::float,
                                   'y', 0.0,
                                   'z', (el->>'y')::float
                               )
                           END
                   )
        FROM jsonb_array_elements(points) AS el
    ),
    min_elev = COALESCE(min_elev, 0.0),
    max_elev = COALESCE(max_elev, 0.0)
WHERE source = 'STATIC'
  AND points IS NOT NULL
  AND (points->0->>'z') IS NULL;

