import type { CarPositionDto, TrackLayoutResponseDto } from '@/api/types'
import { getDrsPointIndices, getBrakePointIndices } from '@/constants/trackZones'
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

/** Track colours and widths to match 3D view: dark base (shadow), light surface, dashed center line. */
const TRACK_BASE = 'rgba(11, 15, 20, 0.8)'
const TRACK_SURFACE = 'rgba(249, 250, 251, 0.2)'
const TRACK_CENTER_LINE = 'rgba(249, 250, 251, 0.4)'
const TRACK_BASE_WIDTH = 24
const TRACK_SURFACE_WIDTH = 20
const TRACK_CENTER_WIDTH = 2
const TRACK_CENTER_DASH = '8, 4'

/** DRS zone circle – same as reference: rgba(0, 229, 255, 0.3), radius 9 in SVG (proportional to 3D 18). */
const DRS_FILL = 'rgba(0, 229, 255, 0.3)'
const DRS_R = 9
/** Brake zone circle – same as reference: rgba(225, 6, 0, 0.4), radius 8 in SVG (proportional to 3D 15). */
const BRAKE_FILL = 'rgba(225, 6, 0, 0.4)'
const BRAKE_R = 8

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
      {/* Track base (shadow) – same as 3D */}
      <path
        d={trackPath}
        fill="none"
        stroke={TRACK_BASE}
        strokeWidth={TRACK_BASE_WIDTH}
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      {/* Track surface – light semi-transparent like 3D */}
      <path
        d={trackPath}
        fill="none"
        stroke={TRACK_SURFACE}
        strokeWidth={TRACK_SURFACE_WIDTH}
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      {/* Center line – dashed, same as 3D */}
      <path
        d={trackPath}
        fill="none"
        stroke={TRACK_CENTER_LINE}
        strokeWidth={TRACK_CENTER_WIDTH}
        strokeDasharray={TRACK_CENTER_DASH}
        strokeLinecap="round"
        strokeLinejoin="round"
      />

      {/* DRS zones – same style as reference, placed using layout.point.drs or static config per trackId */}
      {getDrsPointIndices(layout.trackId, layout.points).map(i => {
        if (i >= layout.points.length) return null
        const pt = layout.points[i]
        const z = pt.z ?? pt.y ?? 0
        const { nx, ny } = normalize2D(pt.x, z, bounds, CANVAS)
        return (
          <circle
            key={`drs-${i}`}
            cx={nx}
            cy={ny}
            r={DRS_R}
            fill={DRS_FILL}
          />
        )
      })}

      {/* Brake zones – same style as reference, placed using layout.point.brake or static config per trackId */}
      {getBrakePointIndices(layout.trackId, layout.points).map(i => {
        if (i >= layout.points.length) return null
        const pt = layout.points[i]
        const z = pt.z ?? pt.y ?? 0
        const { nx, ny } = normalize2D(pt.x, z, bounds, CANVAS)
        return (
          <circle
            key={`brake-${i}`}
            cx={nx}
            cy={ny}
            r={BRAKE_R}
            fill={BRAKE_FILL}
          />
        )
      })}

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

      {/* Cars – 2D styling to visually match 3D (glow-like ring, main body with number, short label) */}
      {cars.map(car => {
        const { nx, ny } = normalize2D(car.worldPosX, car.worldPosZ, bounds, CANVAS)
        const driverLabel = car.driverLabel ?? `Car ${car.carIndex}`
        const title = `#${car.racingNumber ?? car.carIndex} ${driverLabel}`
        const shortLabel =
          driverLabel.length > 6 ? `${driverLabel.slice(0, 5)}…` : driverLabel
        const numberText = String(car.racingNumber ?? car.carIndex)
        return (
          <g key={car.carIndex}>
            <title>{title}</title>
            {/* Glow ring */}
            <circle
              cx={nx}
              cy={ny}
              r={14}
              fill={car.color}
              fillOpacity={0.25}
            />
            {/* Main car body */}
            <circle
              cx={nx}
              cy={ny}
              r={8}
              fill="rgba(11, 15, 20, 0.95)"
              stroke={car.color}
              strokeWidth={1.5}
            />
            {/* Number disk */}
            <circle
              cx={nx}
              cy={ny}
              r={6}
              fill={car.color}
            />
            <text
              x={nx}
              y={ny + 0.5}
              textAnchor="middle"
              dominantBaseline="middle"
              fill="#0B0F14"
              fontSize={9}
              fontWeight="bold"
            >
              {numberText}
            </text>
            {/* Driver short label pill */}
            <g transform={`translate(${nx + 14}, ${ny - 10})`}>
              <rect
                x={-18}
                y={-6}
                width={36}
                height={14}
                rx={4}
                fill="rgba(11, 15, 20, 0.95)"
                stroke={car.color}
                strokeWidth={1}
              />
              <text
                x={0}
                y={3}
                textAnchor="middle"
                fill={car.color}
                fontSize={9}
                fontWeight="bold"
              >
                {shortLabel}
              </text>
            </g>
          </g>
        )
      })}
    </svg>
  )
}

