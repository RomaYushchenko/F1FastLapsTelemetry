import { useEffect, useRef, useState } from 'react'
import type { CarPositionDto, TrackLayoutResponseDto } from '@/api/types'
import { getDrsPointIndices, getBrakePointIndices } from '@/constants/trackZones'
import {
  SECTOR_COLORS,
  computeBounds,
  getTrackElevationAt,
  trackPointToWorld3D,
} from '@/utils/trackNormalization'

const CANVAS_W = 800
const CANVAS_H = 600
const PAD = 40
const SCENE_SIZE = 500
const ELEV_EXAGGERATION = 2

/** Isometric 3D to 2D projection. */
function project3D(x: number, y: number, z: number, angleDeg: number) {
  const rad = (angleDeg * Math.PI) / 180
  const cos = Math.cos(rad)
  const sin = Math.sin(rad)
  return {
    x: x * cos - z * sin,
    y: y - (x * sin + z * cos) * 0.5,
  }
}

interface Props {
  layout: TrackLayoutResponseDto
  cars: (CarPositionDto & { color: string })[]
  /** Car index to show as selected (e.g. player). */
  selectedCarIndex?: number | null
  /** Car index of race leader (pulse ring). */
  leaderCarIndex?: number | null
}

const ZOOM_MIN = 0.4
const ZOOM_MAX = 2.5
const ZOOM_DEFAULT = 1

export function TrackMap3D({ layout, cars, selectedCarIndex, leaderCarIndex }: Props) {
  const canvasRef = useRef<HTMLCanvasElement>(null)
  const [cameraAngle, setCameraAngle] = useState(45)
  const [zoom, setZoom] = useState(ZOOM_DEFAULT)
  const [, setTick] = useState(0)
  useEffect(() => {
    if (leaderCarIndex == null) return
    const id = setInterval(() => setTick(t => t + 1), 100)
    return () => clearInterval(id)
  }, [leaderCarIndex])

  useEffect(() => {
    const canvas = canvasRef.current
    if (!canvas) return
    const onWheel = (e: WheelEvent) => {
      e.preventDefault()
      const delta = e.deltaY > 0 ? -0.1 : 0.1
      setZoom(z => Math.min(ZOOM_MAX, Math.max(ZOOM_MIN, z + delta)))
    }
    canvas.addEventListener('wheel', onWheel, { passive: false })
    return () => canvas.removeEventListener('wheel', onWheel)
  }, [])


  const bounds = layout.bounds ?? computeBounds(layout.points)
  const sectors = layout.sectorBoundaries ?? []
  const rangeX = bounds.maxX - bounds.minX || 1
  const rangeZ = bounds.maxZ - bounds.minZ || 1
  const maxRange = Math.max(rangeX, rangeZ)
  const scaleWorld = SCENE_SIZE / maxRange
  const centerX = (bounds.minX + bounds.maxX) / 2
  const centerZ = (bounds.minZ + bounds.maxZ) / 2
  const minElev = bounds.minElev ?? 0

  const trackPoints = layout.points
    .filter(p => p.x != null && (p.z != null || p.y != null))
    .map(p => {
      const w = trackPointToWorld3D(p)
      return {
        x: (w.x - centerX) * scaleWorld,
        z: (w.z - centerZ) * scaleWorld,
        elevation: (w.y - minElev) * ELEV_EXAGGERATION,
      }
    })

  useEffect(() => {
    const canvas = canvasRef.current
    if (!canvas || trackPoints.length === 0) return

    const ctx = canvas.getContext('2d')
    if (!ctx) return

    const width = canvas.width
    const height = canvas.height

    ctx.clearRect(0, 0, width, height)

    const toScreen = (worldX: number, worldY: number, worldZ: number) => {
      const sx = (worldX - centerX) * scaleWorld
      const sz = (worldZ - centerZ) * scaleWorld
      const sy = (worldY - minElev) * ELEV_EXAGGERATION
      return project3D(sx, -sy, sz, cameraAngle)
    }

    const allProjected: { x: number; y: number }[] = []
    trackPoints.forEach(pt => {
      const p = project3D(pt.x, -pt.elevation * 2, pt.z, cameraAngle)
      allProjected.push(p)
    })
    cars.forEach(car => {
      const elev =
        car.worldPosY ??
        getTrackElevationAt(car.worldPosX, car.worldPosZ, layout.points, minElev)
      const p = toScreen(car.worldPosX, elev, car.worldPosZ)
      allProjected.push({ x: p.x, y: p.y })
    })

    const projMinX = Math.min(...allProjected.map(a => a.x))
    const projMaxX = Math.max(...allProjected.map(a => a.x))
    const projMinY = Math.min(...allProjected.map(a => a.y))
    const projMaxY = Math.max(...allProjected.map(a => a.y))
    const projW = projMaxX - projMinX || 1
    const projH = projMaxY - projMinY || 1
    const baseScale = Math.min((width - 2 * PAD) / projW, (height - 2 * PAD) / projH)
    const scale = baseScale * zoom
    const offsetX = width / 2 - (projMinX + projMaxX) / 2 * scale
    const offsetY = height / 2 - (projMinY + projMaxY) / 2 * scale

    const scr = (p: { x: number; y: number }) => ({
      x: p.x * scale + offsetX,
      y: p.y * scale + offsetY,
    })

    // Grid
    ctx.strokeStyle = 'rgba(249, 250, 251, 0.05)'
    ctx.lineWidth = 1
    const gridStep = 50
    for (let i = -SCENE_SIZE; i <= SCENE_SIZE; i += gridStep) {
      for (let j = -SCENE_SIZE; j <= SCENE_SIZE; j += gridStep) {
        const p1 = scr(project3D(i, 0, j, cameraAngle))
        const p2 = scr(project3D(i + gridStep, 0, j, cameraAngle))
        const p3 = scr(project3D(i, 0, j + gridStep, cameraAngle))
        ctx.beginPath()
        ctx.moveTo(p1.x, p1.y)
        ctx.lineTo(p2.x, p2.y)
        ctx.stroke()
        ctx.beginPath()
        ctx.moveTo(p1.x, p1.y)
        ctx.lineTo(p3.x, p3.y)
        ctx.stroke()
      }
    }

    // Track base (shadow)
    ctx.strokeStyle = 'rgba(11, 15, 20, 0.8)'
    ctx.lineWidth = 30
    ctx.lineCap = 'round'
    ctx.lineJoin = 'round'
    ctx.beginPath()
    trackPoints.forEach((pt, i) => {
      const p = scr(project3D(pt.x, 0, pt.z, cameraAngle))
      if (i === 0) ctx.moveTo(p.x, p.y)
      else ctx.lineTo(p.x, p.y)
    })
    ctx.closePath()
    ctx.stroke()

    // Track with elevation
    ctx.strokeStyle = 'rgba(249, 250, 251, 0.15)'
    ctx.lineWidth = 28
    ctx.beginPath()
    trackPoints.forEach((pt, i) => {
      const p = scr(project3D(pt.x, -pt.elevation * 2, pt.z, cameraAngle))
      if (i === 0) ctx.moveTo(p.x, p.y)
      else ctx.lineTo(p.x, p.y)
    })
    ctx.closePath()
    ctx.stroke()

    // Track sides (3D depth)
    trackPoints.forEach(pt => {
      const pTop = scr(project3D(pt.x, -pt.elevation * 2, pt.z, cameraAngle))
      const pBase = scr(project3D(pt.x, 0, pt.z, cameraAngle))
      ctx.strokeStyle = 'rgba(249, 250, 251, 0.08)'
      ctx.lineWidth = 2
      ctx.beginPath()
      ctx.moveTo(pTop.x, pTop.y)
      ctx.lineTo(pBase.x, pBase.y)
      ctx.stroke()
    })

    // DRS zones (same style as reference: cyan circle rgba(0, 229, 255, 0.3), radius 18)
    const drsIndices = getDrsPointIndices(layout.trackId, layout.points)
    drsIndices.forEach(i => {
      if (i >= trackPoints.length) return
      const tp = trackPoints[i]
      const p = scr(project3D(tp.x, -tp.elevation * 2, tp.z, cameraAngle))
      ctx.fillStyle = 'rgba(0, 229, 255, 0.3)'
      ctx.beginPath()
      ctx.arc(p.x, p.y, 18, 0, Math.PI * 2)
      ctx.fill()
    })

    // Brake zones (same style as reference: red circle rgba(225, 6, 0, 0.4), radius 15)
    const brakeIndices = getBrakePointIndices(layout.trackId, layout.points)
    brakeIndices.forEach(i => {
      if (i >= trackPoints.length) return
      const tp = trackPoints[i]
      const p = scr(project3D(tp.x, -tp.elevation * 2, tp.z, cameraAngle))
      ctx.fillStyle = 'rgba(225, 6, 0, 0.4)'
      ctx.beginPath()
      ctx.arc(p.x, p.y, 15, 0, Math.PI * 2)
      ctx.fill()
    })

    // Center line
    ctx.strokeStyle = 'rgba(249, 250, 251, 0.4)'
    ctx.lineWidth = 2
    ctx.setLineDash([8, 4])
    ctx.beginPath()
    trackPoints.forEach((pt, i) => {
      const p = scr(project3D(pt.x, -pt.elevation * 2, pt.z, cameraAngle))
      if (i === 0) ctx.moveTo(p.x, p.y)
      else ctx.lineTo(p.x, p.y)
    })
    ctx.closePath()
    ctx.stroke()
    ctx.setLineDash([])

    // Sector markers (S2, S3; S1 = start)
    sectors
      .filter(b => b.sector !== 1)
      .forEach(b => {
        const worldY = b.y ?? 0
        const worldZ = b.z ?? b.y
        const sx = (b.x - centerX) * scaleWorld
        const sz = (worldZ - centerZ) * scaleWorld
        const sy = (worldY - minElev) * ELEV_EXAGGERATION
        const p = scr(project3D(sx, -sy * 2 - 20, sz, cameraAngle))
        const color = SECTOR_COLORS[b.sector as 2 | 3]
        ctx.fillStyle = color
        ctx.beginPath()
        ctx.arc(p.x, p.y, 8, 0, Math.PI * 2)
        ctx.fill()
        ctx.fillStyle = color
        ctx.font = 'bold 11px system-ui'
        ctx.textAlign = 'center'
        ctx.fillText(`S${b.sector}`, p.x, p.y - 15)
      })

    // S/F at start
    if (trackPoints.length > 0) {
      const pt = trackPoints[0]
      const p = scr(project3D(pt.x, -pt.elevation * 2, pt.z, cameraAngle))
      ctx.fillStyle = 'rgba(249, 250, 251, 0.9)'
      ctx.fillRect(p.x - 20, p.y - 15, 4, 30)
      ctx.fillStyle = 'rgba(11, 15, 20, 0.9)'
      ctx.fillRect(p.x - 14, p.y - 15, 4, 30)
      ctx.fillStyle = 'rgba(249, 250, 251, 0.9)'
      ctx.fillRect(p.x - 8, p.y - 15, 4, 30)
      ctx.fillStyle = '#F9FAFB'
      ctx.font = 'bold 12px system-ui'
      ctx.textAlign = 'left'
      ctx.fillText('S/F', p.x + 5, p.y + 3)
    }

    // Cars
    cars.forEach(car => {
      const elev =
        car.worldPosY ??
        getTrackElevationAt(car.worldPosX, car.worldPosZ, layout.points, minElev)
      const sx = (car.worldPosX - centerX) * scaleWorld
      const sz = (car.worldPosZ - centerZ) * scaleWorld
      const sy = (elev - minElev) * ELEV_EXAGGERATION
      const projected = scr(project3D(sx, -sy * 2 - 10, sz, cameraAngle))
      const isSelected = car.carIndex === selectedCarIndex
      const isLeader = car.carIndex === leaderCarIndex

      // Glow
      const gradient = ctx.createRadialGradient(
        projected.x, projected.y, 0,
        projected.x, projected.y, 30
      )
      gradient.addColorStop(0, car.color + '60')
      gradient.addColorStop(0.5, car.color + '20')
      gradient.addColorStop(1, car.color + '00')
      ctx.fillStyle = gradient
      ctx.beginPath()
      ctx.arc(projected.x, projected.y, 30, 0, Math.PI * 2)
      ctx.fill()

      // Speed trail
      const trailGradient = ctx.createLinearGradient(
        projected.x - 15, projected.y,
        projected.x, projected.y
      )
      trailGradient.addColorStop(0, car.color + '00')
      trailGradient.addColorStop(1, car.color + '40')
      ctx.fillStyle = trailGradient
      ctx.beginPath()
      ctx.ellipse(projected.x - 10, projected.y, 12, 4, 0, 0, Math.PI * 2)
      ctx.fill()

      // Car body
      ctx.save()
      ctx.translate(projected.x, projected.y)
      ctx.fillStyle = 'rgba(11, 15, 20, 0.5)'
      ctx.beginPath()
      ctx.ellipse(0, 8, 12, 6, 0, 0, Math.PI * 2)
      ctx.fill()
      ctx.fillStyle = 'rgba(11, 15, 20, 0.9)'
      ctx.strokeStyle = car.color
      ctx.lineWidth = 2
      ctx.beginPath()
      ctx.ellipse(0, 0, 14, 8, 0, 0, Math.PI * 2)
      ctx.fill()
      ctx.stroke()
      ctx.fillStyle = car.color + '99'
      ctx.beginPath()
      ctx.ellipse(2, 0, 6, 4, 0, 0, Math.PI * 2)
      ctx.fill()
      ctx.fillStyle = car.color
      ctx.beginPath()
      ctx.arc(0, 0, 9, 0, Math.PI * 2)
      ctx.fill()
      ctx.fillStyle = '#0B0F14'
      ctx.font = 'bold 11px system-ui'
      ctx.textAlign = 'center'
      ctx.textBaseline = 'middle'
      ctx.fillText(String(car.racingNumber ?? car.carIndex), 0, 0)
      ctx.restore()

      // Driver label
      const label = car.driverLabel ?? `Car ${car.carIndex}`
      ctx.fillStyle = 'rgba(11, 15, 20, 0.95)'
      ctx.strokeStyle = car.color
      ctx.lineWidth = 1.5
      ctx.beginPath()
      if (typeof ctx.roundRect === 'function') {
        ctx.roundRect(projected.x - 20, projected.y - 32, 40, 18, 3)
      } else {
        ctx.rect(projected.x - 20, projected.y - 32, 40, 18)
      }
      ctx.fill()
      ctx.stroke()
      ctx.fillStyle = car.color
      ctx.font = 'bold 11px system-ui'
      ctx.textAlign = 'center'
      ctx.textBaseline = 'middle'
      ctx.fillText(label.length > 6 ? label.slice(0, 5) + '…' : label, projected.x, projected.y - 23)

      if (isSelected) {
        ctx.strokeStyle = car.color
        ctx.lineWidth = 2
        ctx.setLineDash([4, 4])
        ctx.beginPath()
        ctx.arc(projected.x, projected.y, 22, 0, Math.PI * 2)
        ctx.stroke()
        ctx.strokeStyle = car.color + '60'
        ctx.lineWidth = 1
        ctx.setLineDash([])
        ctx.beginPath()
        ctx.arc(projected.x, projected.y, 26, 0, Math.PI * 2)
        ctx.stroke()
      }
      if (isLeader) {
        ctx.strokeStyle = car.color + '80'
        ctx.lineWidth = 2
        ctx.beginPath()
        ctx.arc(projected.x, projected.y, 20 + Math.sin(Date.now() / 200) * 3, 0, Math.PI * 2)
        ctx.stroke()
      }
    })
  }, [cameraAngle, zoom, layout, cars, trackPoints, selectedCarIndex, leaderCarIndex, bounds, sectors, centerX, centerZ, minElev])

  return (
    <div className="relative w-full h-full flex flex-col">
      <div className="absolute top-4 left-4 z-10 flex flex-col gap-3 bg-card/60 backdrop-blur-md border border-border/40 rounded-lg p-3">
        <div>
          <div className="text-xs text-text-secondary uppercase mb-2 font-bold">Camera Angle</div>
          <input
            type="range"
            min={0}
            max={360}
            value={cameraAngle}
            onChange={e => setCameraAngle(Number(e.target.value))}
            className="w-32"
          />
          <div className="text-xs text-center mt-1 font-mono font-bold">{cameraAngle}°</div>
        </div>
        <div>
          <div className="text-xs text-text-secondary uppercase mb-2 font-bold">Zoom</div>
          <input
            type="range"
            min={ZOOM_MIN}
            max={ZOOM_MAX}
            step={0.1}
            value={zoom}
            onChange={e => setZoom(Number(e.target.value))}
            className="w-32"
          />
          <div className="text-xs text-center mt-1 font-mono font-bold">{zoom.toFixed(1)}×</div>
        </div>
      </div>

      <canvas
        ref={canvasRef}
        width={CANVAS_W}
        height={CANVAS_H}
        className="w-full h-full object-contain"
        style={{ touchAction: 'none' }}
      />
    </div>
  )
}
