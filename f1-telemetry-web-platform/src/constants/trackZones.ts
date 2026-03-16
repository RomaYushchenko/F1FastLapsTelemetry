/**
 * Optional DRS and brake zone point indices per track (trackId).
 * Used when layout.points[].drs/brake are not set (e.g. from API).
 * Add indices for known F1 tracks so zones render correctly in 2D and 3D.
 * Format: array of point indices along the track centerline where the zone is active.
 */
const DRS_ZONE_INDICES: Record<number, number[]> = {
  // Example (Austria): main straight DRS – replace with real indices for your layout point count
  // 17: [0, 1, 2, ...], // Austria trackId 17
}

const BRAKE_ZONE_INDICES: Record<number, number[]> = {
  // Example: heavy braking points – replace with real indices per track
}

/** Returns point indices where DRS zone should be drawn. Uses layout point.drs when set, else static config. */
export function getDrsPointIndices(
  trackId: number,
  points: { drs?: boolean | null }[],
): number[] {
  const fromPoints = points
    .map((p, i) => (p.drs ? i : -1))
    .filter(i => i >= 0)
  if (fromPoints.length > 0) return fromPoints
  return DRS_ZONE_INDICES[trackId] ?? []
}

/** Returns point indices where brake zone should be drawn. Uses layout point.brake when set, else static config. */
export function getBrakePointIndices(
  trackId: number,
  points: { brake?: boolean | null }[],
): number[] {
  const fromPoints = points
    .map((p, i) => (p.brake ? i : -1))
    .filter(i => i >= 0)
  if (fromPoints.length > 0) return fromPoints
  return BRAKE_ZONE_INDICES[trackId] ?? []
}
