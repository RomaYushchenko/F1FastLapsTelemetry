import type { TrackBounds, TrackPoint3D, SectorBoundary } from '@/api/types'

// ─── Shared ────────────────────────────────────────────────────────────────

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
  const zs = points.map(p => p.z)
  const ys = points.map(p => p.y)
  return {
    minX: Math.min(...xs), maxX: Math.max(...xs),
    minZ: Math.min(...zs), maxZ: Math.max(...zs),
    minElev: Math.min(...ys), maxElev: Math.max(...ys),
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

export function pointsToSvgPath(
  points: TrackPoint3D[],
  bounds: TrackBounds,
  canvas: CanvasConfig,
): string {
  if (!points.length) return ''
  return (
    points
      .map((p, i) => {
        const { nx, ny } = normalize2D(p.x, p.z, bounds, canvas)
        return `${i === 0 ? 'M' : 'L'}${nx.toFixed(1)},${ny.toFixed(1)}`
      })
      .join(' ') + ' Z'
  )
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
    const d = (p.x - targetX) ** 2 + (p.z - targetZ) ** 2
    if (d < bestDist) {
      bestDist = d
      bestIdx = i
    }
  })
  return bestIdx
}

export function splitIntoSectors(
  points: TrackPoint3D[],
  boundaries: SectorBoundary[] | null | undefined,
): [TrackPoint3D[], TrackPoint3D[], TrackPoint3D[]] {
  if (!points.length || !boundaries?.length) return [points, [], []]

  const s2 = boundaries.find(b => b.sector === 2)
  const s3 = boundaries.find(b => b.sector === 3)

  if (!s2 || !s3 || points.length < 3) {
    return [points, [], []]
  }

  const s2Idx = findNearestPointIndex(points, s2.x, s2.z)
  const s3Idx = findNearestPointIndex(points, s3.x, s3.z)

  const [startS2, startS3] = s2Idx < s3Idx ? [s2Idx, s3Idx] : [s3Idx, s2Idx]

  return [
    points.slice(0, startS2 + 1),
    points.slice(startS2, startS3 + 1),
    points.slice(startS3),
  ]
}

// ─── 3D (Three.js normalization) ─────────────────────────────────────────────

export interface ThreeTransform {
  centerX: number
  centerY: number
  centerZ: number
  scale: number
}

export function computeThreeTransform(
  bounds: TrackBounds,
  sceneSize = 100,
): ThreeTransform {
  const rangeX = bounds.maxX - bounds.minX
  const rangeZ = bounds.maxZ - bounds.minZ
  const maxHRange = Math.max(rangeX, rangeZ, 1)

  return {
    centerX: (bounds.minX + bounds.maxX) / 2,
    centerY: (bounds.minElev + bounds.maxElev) / 2,
    centerZ: (bounds.minZ + bounds.maxZ) / 2,
    scale: sceneSize / maxHRange,
  }
}

export function worldToThree(
  worldX: number,
  worldY: number,
  worldZ: number,
  t: ThreeTransform,
): [number, number, number] {
  return [
    (worldX - t.centerX) * t.scale,
    (worldY - t.centerY) * t.scale,
    -(worldZ - t.centerZ) * t.scale,
  ]
}

export function pointsToThreeBuffer(
  points: TrackPoint3D[],
  transform: ThreeTransform,
): Float32Array {
  const arr = new Float32Array(points.length * 3)
  points.forEach((p, i) => {
    const [tx, ty, tz] = worldToThree(p.x, p.y, p.z, transform)
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

