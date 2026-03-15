import type { TrackBounds, TrackPoint3D, SectorBoundary } from '@/api/types'

// ─── Shared ────────────────────────────────────────────────────────────────

/**
 * Normalize a track point to world 3D (x, y=elevation, z).
 * Legacy points have no z: then p.y is worldPositionZ; elevation is 0.
 */
export function trackPointToWorld3D(p: TrackPoint3D): { x: number; y: number; z: number } {
  const hasZ = p.z != null
  return {
    x: p.x,
    y: hasZ ? (p.y ?? 0) : 0,
    z: (p.z ?? p.y ?? 0),
  }
}

export function computeBounds(points: TrackPoint3D[]): TrackBounds {
  if (!points.length) {
    return {
      minX: 0,
      maxX: 0,
      minZ: 0,
      maxZ: 0,
      minElev: 0,
      maxElev: 0,
    }
  }
  const xs = points.map(p => p.x)
  const zs = points.map(p => (p.z ?? p.y ?? 0))
  const ys = points.map(p => (p.z != null ? (p.y ?? 0) : 0))
  return {
    minX: Math.min(...xs), maxX: Math.max(...xs),
    minZ: Math.min(...zs), maxZ: Math.max(...zs),
    minElev: ys.length ? Math.min(...ys) : 0,
    maxElev: ys.length ? Math.max(...ys) : 0,
  }
}

// ─── 2D (top-down, XZ projection) ─────────────────────────────────────────

export interface CanvasConfig {
  width: number
  height: number
  padding?: number
}

/**
 * XZ world coordinates → SVG pixels (top-down view).
 * worldPositionY (elevation) is ignored for 2D.
 */
export function normalize2D(
  x: number,
  z: number,
  bounds: TrackBounds,
  canvas: CanvasConfig,
): { nx: number; ny: number } {
  const pad = canvas.padding ?? 20
  const w = canvas.width - 2 * pad
  const h = canvas.height - 2 * pad
  const nx = ((x - bounds.minX) / (bounds.maxX - bounds.minX || 1)) * w + pad
  const ny = (1 - (z - bounds.minZ) / (bounds.maxZ - bounds.minZ || 1)) * h + pad
  return { nx, ny }
}

const PATH_PRECISION = 2

export function pointsToSvgPath(
  points: TrackPoint3D[],
  bounds: TrackBounds,
  canvas: CanvasConfig,
): string {
  if (!points.length) return ''
  return (
    points
      .map((p, i) => {
        const z = p.z ?? p.y ?? bounds.minZ
        const { nx, ny } = normalize2D(p.x, z, bounds, canvas)
        return `${i === 0 ? 'M' : 'L'}${nx.toFixed(PATH_PRECISION)},${ny.toFixed(PATH_PRECISION)}`
      })
      .join(' ') + ' Z'
  )
}

/**
 * Builds a smooth closed SVG path through the points using Catmull-Rom-style
 * cubic Bezier segments, so the 2D track matches the apparent precision of the 3D line.
 * Uses higher coordinate precision to avoid jagged edges.
 */
export function pointsToSmoothSvgPath(
  points: TrackPoint3D[],
  bounds: TrackBounds,
  canvas: CanvasConfig,
): string {
  if (!points.length) return ''
  if (points.length === 1) {
    const z = points[0].z ?? points[0].y ?? bounds.minZ
    const { nx, ny } = normalize2D(points[0].x, z, bounds, canvas)
    return `M${nx.toFixed(PATH_PRECISION)},${ny.toFixed(PATH_PRECISION)} Z`
  }
  const n = points.length
  const pt = (i: number) => {
    const p = points[((i % n) + n) % n]
    const z = p.z ?? p.y ?? bounds.minZ
    return normalize2D(p.x, z, bounds, canvas)
  }
  const fmt = (nx: number, ny: number) =>
    `${nx.toFixed(PATH_PRECISION)},${ny.toFixed(PATH_PRECISION)}`
  const parts: string[] = []
  const p0 = pt(0)
  parts.push(`M${fmt(p0.nx, p0.ny)}`)
  for (let i = 0; i < n; i++) {
    const pPrev = pt(i - 1)
    const pCur = pt(i)
    const pNext = pt(i + 1)
    const pNext2 = pt(i + 2)
    const c1x = pCur.nx + (pNext.nx - pPrev.nx) / 6
    const c1y = pCur.ny + (pNext.ny - pPrev.ny) / 6
    const c2x = pNext.nx - (pNext2.nx - pCur.nx) / 6
    const c2y = pNext.ny - (pNext2.ny - pCur.ny) / 6
    parts.push(`C${fmt(c1x, c1y)} ${fmt(c2x, c2y)} ${fmt(pNext.nx, pNext.ny)}`)
  }
  return parts.join(' ') + ' Z'
}

/**
 * Builds a smooth open SVG path through the points (for sector segments).
 * Uses Catmull-Rom-style cubic Bezier with end tangents extended from the first/last segment.
 */
export function pointsToSmoothOpenSvgPath(
  points: TrackPoint3D[],
  bounds: TrackBounds,
  canvas: CanvasConfig,
): string {
  if (!points.length) return ''
  if (points.length === 1) {
    const z = points[0].z ?? points[0].y ?? bounds.minZ
    const { nx, ny } = normalize2D(points[0].x, z, bounds, canvas)
    return `M${nx.toFixed(PATH_PRECISION)},${ny.toFixed(PATH_PRECISION)}`
  }
  const n = points.length
  const pt = (i: number) => {
    if (i < 0) return normalize2D(points[0].x, points[0].z ?? points[0].y ?? bounds.minZ, bounds, canvas)
    if (i >= n) return normalize2D(points[n - 1].x, points[n - 1].z ?? points[n - 1].y ?? bounds.minZ, bounds, canvas)
    const p = points[i]
    const z = p.z ?? p.y ?? bounds.minZ
    return normalize2D(p.x, z, bounds, canvas)
  }
  const fmt = (nx: number, ny: number) =>
    `${nx.toFixed(PATH_PRECISION)},${ny.toFixed(PATH_PRECISION)}`
  const parts: string[] = []
  const p0 = pt(0)
  parts.push(`M${fmt(p0.nx, p0.ny)}`)
  for (let i = 0; i < n - 1; i++) {
    const pPrev = pt(i - 1)
    const pCur = pt(i)
    const pNext = pt(i + 1)
    const pNext2 = pt(i + 2)
    const c1x = pCur.nx + (pNext.nx - pPrev.nx) / 6
    const c1y = pCur.ny + (pNext.ny - pPrev.ny) / 6
    const c2x = pNext.nx - (pNext2.nx - pCur.nx) / 6
    const c2y = pNext.ny - (pNext2.ny - pCur.ny) / 6
    parts.push(`C${fmt(c1x, c1y)} ${fmt(c2x, c2y)} ${fmt(pNext.nx, pNext.ny)}`)
  }
  return parts.join(' ')
}

// ─── Sectors (2D + 3D shared helpers) ─────────────────────────────────────

export const SECTOR_COLORS = {
  1: '#00FF85',
  2: '#FACC15',
  3: '#A855F7',
} as const

export function findNearestPointIndex(
  points: TrackPoint3D[],
  targetX: number,
  targetZ: number,
): number {
  if (!points.length) return 0
  let bestIdx = 0
  let bestDist = Number.POSITIVE_INFINITY
  points.forEach((p, i) => {
    const pz = p.z ?? p.y ?? targetZ
    const d = (p.x - targetX) ** 2 + (pz - targetZ) ** 2
    if (d < bestDist) {
      bestDist = d
      bestIdx = i
    }
  })
  return bestIdx
}

/** Minimum share of points per sector (avoid degenerate split when boundaries are wrong). */
const MIN_SECTOR_FRACTION = 0.05

/**
 * @param points - Track centreline points
 * @param boundaries - Sector boundary markers (sector 2 and 3 starts)
 * @param options - pointsInLapOrder: true when points are ordered by lap distance (RECORDED layouts);
 *   then sector 3 is closed back to point 0 (S/F). For STATIC layouts pass false to avoid wrong closing line.
 */
export function splitIntoSectors(
  points: TrackPoint3D[],
  boundaries: SectorBoundary[] | null | undefined,
  options?: { pointsInLapOrder?: boolean },
): [TrackPoint3D[], TrackPoint3D[], TrackPoint3D[]] {
  if (!points.length || !boundaries?.length) return [points, [], []]

  const s2 = boundaries.find(b => b.sector === 2)
  const s3 = boundaries.find(b => b.sector === 3)

  if (!s2 || !s3 || points.length < 3) {
    return [points, [], []]
  }

  const pointsInLapOrder = options?.pointsInLapOrder ?? false

  // Prefer backend-provided indices so 1→2→3→1 order is correct (no wrong match by 2D distance on overlapping tracks).
  // Use firstCut < secondCut so S3 segment (secondCut..0) always closes the loop; some tracks/game builds send S3 index before S2.
  const s2Idx =
    typeof s2.pointIndex === 'number' && s2.pointIndex >= 0
      ? Math.min(s2.pointIndex, points.length - 1)
      : findNearestPointIndex(points, s2.x, s2.z ?? s2.y)
  const s3Idx =
    typeof s3.pointIndex === 'number' && s3.pointIndex >= 0
      ? Math.min(s3.pointIndex, points.length - 1)
      : findNearestPointIndex(points, s3.x, s3.z ?? s3.y)

  const firstCut = Math.min(s2Idx, s3Idx)
  const secondCut = Math.max(s2Idx, s3Idx)

  const s1pts = points.slice(0, firstCut + 1)
  const s2pts = points.slice(firstCut, secondCut + 1)
  // Only close sector 3 to point 0 when points are in lap order (RECORDED); for STATIC, points[0] may not be S/F
  const s3pts =
    pointsInLapOrder && secondCut > 0
      ? points.slice(secondCut).concat([points[0]])
      : points.slice(secondCut)
  const minPoints = Math.max(3, Math.floor(points.length * MIN_SECTOR_FRACTION))

  if (s1pts.length < minPoints || s2pts.length < minPoints || s3pts.length < minPoints) {
    return [points, [], []]
  }

  return [s1pts, s2pts, s3pts]
}

// ─── 3D (Three.js normalization) ─────────────────────────────────────────────

export interface ThreeTransform {
  centerX: number
  centerY: number
  centerZ: number
  scale: number
  /** Multiply Y (elevation) in scene space so elevation changes are visible; 1 = no exaggeration */
  elevationScale?: number
}

export function computeThreeTransform(
  bounds: TrackBounds,
  sceneSize = 100,
  options?: { targetElevationSceneUnits?: number; minElevationScale?: number },
): ThreeTransform {
  const rangeX = bounds.maxX - bounds.minX
  const rangeZ = bounds.maxZ - bounds.minZ
  const maxHRange = Math.max(rangeX, rangeZ, 1)
  const scale = sceneSize / maxHRange

  const minElev = bounds.minElev ?? 0
  const maxElev = bounds.maxElev ?? 0
  const elevRange = maxElev - minElev
  const targetElev = options?.targetElevationSceneUnits ?? 25
  const minScale = options?.minElevationScale ?? 1
  const elevationScale =
    elevRange > 0.5
      ? Math.max(minScale, Math.min(15, targetElev / (elevRange * scale)))
      : 1

  return {
    centerX: (bounds.minX + bounds.maxX) / 2,
    centerY: (minElev + maxElev) / 2,
    centerZ: (bounds.minZ + bounds.maxZ) / 2,
    scale,
    elevationScale: elevRange > 0.5 ? elevationScale : undefined,
  }
}

export function worldToThree(
  worldX: number,
  worldY: number,
  worldZ: number,
  t: ThreeTransform,
): [number, number, number] {
  const elevScale = t.elevationScale ?? 1
  return [
    (worldX - t.centerX) * t.scale,
    (worldY - t.centerY) * t.scale * elevScale,
    -(worldZ - t.centerZ) * t.scale,
  ]
}

export function pointsToThreeBuffer(
  points: TrackPoint3D[],
  transform: ThreeTransform,
): Float32Array {
  const arr = new Float32Array(points.length * 3)
  points.forEach((p, i) => {
    const w = trackPointToWorld3D(p)
    const [tx, ty, tz] = worldToThree(w.x, w.y, w.z, transform)
    arr[i * 3] = tx
    arr[i * 3 + 1] = ty
    arr[i * 3 + 2] = tz
  })
  return arr
}

export function elevationToColor(
  elev: number,
  minElev: number,
  maxElev: number,
): string {
  if (elev == null || Number.isNaN(elev)) return '#6B7280'
  if (maxElev === minElev) return '#6B7280'
  const t = (elev - minElev) / (maxElev - minElev)

  if (t < 0.5) {
    const s = t * 2
    const r = 0
    const g = Math.round(102 + s * 153)
    const b = Math.round(255 - s * 122)
    return `rgb(${r},${g},${b})`
  }

  const s = (t - 0.5) * 2
  const r = Math.round(s * 225)
  const g = Math.round(255 - s * 249)
  const b = Math.round(133 - s * 133)
  return `rgb(${r},${g},${b})`
}

