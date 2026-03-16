import { describe, expect, it } from 'vitest'
import type { TrackBounds, TrackPoint3D } from '@/api/types'
import {
  computeBounds,
  computeThreeTransform,
  elevationToColor,
  findNearestPointIndex,
  getTrackElevationAt,
  normalize2D,
  pointsToSvgPath,
  pointsToThreeBuffer,
  trackPointToWorld3D,
  type CanvasConfig,
} from './trackNormalization'

describe('trackNormalization', () => {
  const samplePoints: TrackPoint3D[] = [
    { x: 0, y: 0, z: 0 },
    { x: 10, y: 1, z: 0 },
    { x: 10, y: 2, z: 10 },
    { x: 0, y: 3, z: 10 },
  ]

  const canvas: CanvasConfig = { width: 200, height: 100, padding: 10 }

  it('trackPointToWorld3D maps legacy 2D points to XZ plane with zero elevation', () => {
    const legacyPoint: TrackPoint3D = { x: 5, y: 7 }
    const result = trackPointToWorld3D(legacyPoint)
    expect(result).toEqual({ x: 5, y: 0, z: 7 })
  })

  it('trackPointToWorld3D keeps 3D points with elevation', () => {
    const p: TrackPoint3D = { x: 1, y: 2, z: 3 }
    const result = trackPointToWorld3D(p)
    expect(result).toEqual({ x: 1, y: 2, z: 3 })
  })

  it('computeBounds returns zero bounds for empty points', () => {
    const bounds = computeBounds([])
    expect(bounds).toEqual<TrackBounds>({
      minX: 0,
      maxX: 0,
      minZ: 0,
      maxZ: 0,
      minElev: 0,
      maxElev: 0,
    })
  })

  it('computeBounds computes ranges for 3D points', () => {
    const bounds = computeBounds(samplePoints)
    expect(bounds.minX).toBe(0)
    expect(bounds.maxX).toBe(10)
    expect(bounds.minZ).toBe(0)
    expect(bounds.maxZ).toBe(10)
    expect(bounds.minElev).toBe(0)
    expect(bounds.maxElev).toBe(3)
  })

  it('normalize2D maps world coordinates into canvas space', () => {
    const bounds = computeBounds(samplePoints)
    const { nx, ny } = normalize2D(0, 0, bounds, canvas)
    expect(nx).toBeGreaterThanOrEqual(0)
    expect(ny).toBeGreaterThanOrEqual(0)
  })

  it('pointsToSvgPath returns empty string for no points', () => {
    const bounds = computeBounds([])
    const path = pointsToSvgPath([], bounds, canvas)
    expect(path).toBe('')
  })

  it('pointsToSvgPath builds closed path string', () => {
    const bounds = computeBounds(samplePoints)
    const path = pointsToSvgPath(samplePoints, bounds, canvas)
    expect(path.startsWith('M')).toBe(true)
    expect(path.endsWith(' Z')).toBe(true)
  })

  it('getTrackElevationAt falls back when no points', () => {
    const elev = getTrackElevationAt(0, 0, [], 42)
    expect(elev).toBe(42)
  })

  it('getTrackElevationAt interpolates elevation along nearest segment', () => {
    const elev = getTrackElevationAt(5, 0, samplePoints, 0)
    expect(elev).toBeGreaterThanOrEqual(0)
    expect(elev).toBeLessThanOrEqual(1)
  })

  it('findNearestPointIndex returns 0 for empty points', () => {
    const idx = findNearestPointIndex([], 10, 10)
    expect(idx).toBe(0)
  })

  it('findNearestPointIndex finds closest point by distance', () => {
    const idx = findNearestPointIndex(samplePoints, 11, 0)
    expect(idx).toBe(1)
  })

  it('computeThreeTransform produces non-zero scale and centers', () => {
    const bounds = computeBounds(samplePoints)
    const t = computeThreeTransform(bounds, 100)
    expect(t.scale).toBeGreaterThan(0)
    expect(t.centerX).toBeGreaterThanOrEqual(bounds.minX)
    expect(t.centerX).toBeLessThanOrEqual(bounds.maxX)
  })

  it('pointsToThreeBuffer maps all points into Float32Array', () => {
    const bounds = computeBounds(samplePoints)
    const t = computeThreeTransform(bounds, 100)
    const buffer = pointsToThreeBuffer(samplePoints, t)
    expect(buffer.length).toBe(samplePoints.length * 3)
  })

  it('elevationToColor returns neutral color when range is zero', () => {
    const c = elevationToColor(1, 1, 1)
    expect(c).toBe('#6B7280')
  })

  it('elevationToColor returns gradient color within range', () => {
    const cLow = elevationToColor(0, 0, 10)
    const cHigh = elevationToColor(10, 0, 10)
    expect(cLow).not.toBe('#6B7280')
    expect(cHigh).not.toBe('#6B7280')
    expect(cLow).not.toBe(cHigh)
  })
})

