import type { CarPositionDto, TrackLayoutResponseDto, TrackPoint3D } from '@/api/types'
import {
  CanvasConfig,
  SECTOR_COLORS,
  computeBounds,
  normalize2D,
  pointsToSvgPath,
  splitIntoSectors,
} from '@/utils/trackNormalization'

const SVG_W = 600
const SVG_H = 400
const CANVAS: CanvasConfig = { width: SVG_W, height: SVG_H, padding: 24 }

interface Props {
  layout: TrackLayoutResponseDto
  cars: CarPositionDto[]
}

export function TrackMap2D({ layout, cars }: Props) {
  const bounds = layout.bounds ?? computeBounds(layout.points)
  const sectors = layout.sectorBoundaries ?? []

  const [s1pts, s2pts, s3pts] = splitIntoSectors(layout.points, sectors)
  const trackPath = pointsToSvgPath(layout.points, bounds, CANVAS)

  const renderSegment = (pts: TrackPoint3D[], color: string, closeZ = false) => {
    if (pts.length < 2) return null
    const d =
      pts
        .map((p, i) => {
          const { nx, ny } = normalize2D(p.x, p.z, bounds, CANVAS)
          return `${i === 0 ? 'M' : 'L'}${nx.toFixed(1)},${ny.toFixed(1)}`
        })
        .join(' ') + (closeZ ? ' Z' : '')
    return (
      <>
        <path
          d={d}
          fill="none"
          stroke="#1F2937"
          strokeWidth={10}
          strokeLinecap="round"
          strokeLinejoin="round"
        />
        <path
          d={d}
          fill="none"
          stroke={color}
          strokeWidth={4}
          strokeLinecap="round"
          strokeLinejoin="round"
          strokeOpacity={0.85}
        />
      </>
    )
  }

  return (
    <svg
      width={SVG_W}
      height={SVG_H}
      viewBox={`0 0 ${SVG_W} ${SVG_H}`}
      className="w-full h-auto"
    >
      {!sectors.length && (
        <>
          <path
            d={trackPath}
            fill="none"
            stroke="#1F2937"
            strokeWidth={10}
            strokeLinecap="round"
            strokeLinejoin="round"
          />
          <path
            d={trackPath}
            fill="none"
            stroke="#6B7280"
            strokeWidth={4}
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </>
      )}

      {sectors.length > 0 && (
        <>
          {renderSegment(s1pts, SECTOR_COLORS[1])}
          {renderSegment(s2pts, SECTOR_COLORS[2])}
          {renderSegment(s3pts, SECTOR_COLORS[3], true)}
        </>
      )}

      {sectors.map(b => {
        if (b.sector === 1) return null
        const { nx, ny } = normalize2D(b.x, b.z, bounds, CANVAS)
        const color = SECTOR_COLORS[b.sector as 2 | 3]
        return (
          <g key={`sector-${b.sector}`}>
            <polygon
              points={`${nx},${ny - 10} ${nx + 7},${ny} ${nx},${ny + 10} ${nx - 7},${ny}`}
              fill={color}
              stroke="#111827"
              strokeWidth={1.5}
            />
            <text
              x={nx + 11}
              y={ny + 4}
              fill={color}
              fontSize={11}
              fontWeight="bold"
            >
              S{b.sector}
            </text>
          </g>
        )
      })}

      {sectors.find(b => b.sector === 1) && (() => {
        const s1 = sectors.find(b => b.sector === 1)!
        const { nx, ny } = normalize2D(s1.x, s1.z, bounds, CANVAS)
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
            <text
              x={nx + 8}
              y={ny + 4}
              fill="#FFFFFF"
              fontSize={10}
              fontWeight="bold"
            >
              S/F
            </text>
          </g>
        )
      })()}

      <g transform="translate(12, 12)">
        {[1, 2, 3].map((s, i) => (
          <g key={s} transform={`translate(0, ${i * 18})`}>
            <rect
              width={14}
              height={6}
              y={1}
              rx={3}
              fill={SECTOR_COLORS[s as 1 | 2 | 3]}
            />
            <text
              x={18}
              y={9}
              fill="#9CA3AF"
              fontSize={10}
            >
              Sector {s}
            </text>
          </g>
        ))}
      </g>

      {cars.map(car => {
        const { nx, ny } = normalize2D(car.worldPosX, car.worldPosZ, bounds, CANVAS)
        return (
          <g key={car.carIndex}>
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

