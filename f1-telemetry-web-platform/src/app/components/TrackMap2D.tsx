import type { CarPositionDto, TrackLayoutResponseDto } from '@/api/types'
import {
  CanvasConfig,
  SECTOR_COLORS,
  computeBounds,
  normalize2D,
  pointsToSmoothSvgPath,
} from '@/utils/trackNormalization'

const SVG_W = 600
const SVG_H = 400
const CANVAS: CanvasConfig = { width: SVG_W, height: SVG_H, padding: 24 }

/** Track stroke colour to match 3D view (grey). */
const TRACK_STROKE = '#6B7280'
const TRACK_OUTLINE = '#1F2937'

interface Props {
  layout: TrackLayoutResponseDto
  cars: CarPositionDto[]
}

export function TrackMap2D({ layout, cars }: Props) {
  const bounds = layout.bounds ?? computeBounds(layout.points)
  const sectors = layout.sectorBoundaries ?? []
  const trackPath = pointsToSmoothSvgPath(layout.points, bounds, CANVAS)

  return (
    <svg
      width={SVG_W}
      height={SVG_H}
      viewBox={`0 0 ${SVG_W} ${SVG_H}`}
      className="w-full h-auto"
    >
      {/* Single grey track line (same style as 3D) */}
      <path
        d={trackPath}
        fill="none"
        stroke={TRACK_OUTLINE}
        strokeWidth={10}
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <path
        d={trackPath}
        fill="none"
        stroke={TRACK_STROKE}
        strokeWidth={4}
        strokeLinecap="round"
        strokeLinejoin="round"
      />

      {/* S2 / S3: circles with label on dark background (match 3D) */}
      {sectors.map(b => {
        if (b.sector === 1) return null
        const z = b.z ?? b.y
        const { nx, ny } = normalize2D(b.x, z, bounds, CANVAS)
        const color = SECTOR_COLORS[b.sector as 2 | 3]
        const label = `S${b.sector}`
        return (
          <g key={`sector-${b.sector}`}>
            <circle
              cx={nx}
              cy={ny}
              r={8}
              fill={color}
              stroke="#111827"
              strokeWidth={1.5}
            />
            <rect
              x={nx + 10}
              y={ny - 8}
              width={label.length * 7 + 8}
              height={16}
              rx={3}
              fill="rgba(0,0,0,0.6)"
            />
            <text
              x={nx + 14}
              y={ny + 4}
              fill={color}
              fontSize={12}
              fontWeight="bold"
            >
              {label}
            </text>
          </g>
        )
      })}

      {/* S/F at points[0]: white bar + label on dark background (match 3D) */}
      {layout.points.length > 0 && (() => {
        const start = layout.points[0]
        const z = start.z ?? start.y ?? 0
        const { nx, ny } = normalize2D(start.x, z, bounds, CANVAS)
        return (
          <g>
            <rect
              x={nx - 4}
              y={ny - 10}
              width={8}
              height={20}
              fill="#FFFFFF"
              stroke="#111827"
              strokeWidth={1.5}
              rx={1}
            />
            <rect
              x={nx + 8}
              y={ny - 8}
              width={28}
              height={16}
              rx={3}
              fill="rgba(0,0,0,0.6)"
            />
            <text
              x={nx + 12}
              y={ny + 4}
              fill="#FFFFFF"
              fontSize={11}
              fontWeight="bold"
            >
              S/F
            </text>
          </g>
        )
      })()}

      {cars.map(car => {
        const { nx, ny } = normalize2D(car.worldPosX, car.worldPosZ, bounds, CANVAS)
        const label = car.driverLabel ?? `Car ${car.carIndex}`
        const title = `#${car.racingNumber ?? car.carIndex} ${label}`
        return (
          <g key={car.carIndex}>
            <title>{title}</title>
            <circle cx={nx} cy={ny} r={7} fill={car.color} />
            <text
              x={nx + 9}
              y={ny + 4}
              fill={car.color}
              fontSize={11}
              fontWeight="bold"
            >
              {car.racingNumber ?? car.carIndex}
            </text>
          </g>
        )
      })}
    </svg>
  )
}

