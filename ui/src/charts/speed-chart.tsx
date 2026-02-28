import {
  ResponsiveContainer,
  LineChart,
  Line,
  ReferenceLine,
  XAxis,
  YAxis,
  Tooltip,
  CartesianGrid,
} from 'recharts'
import type { LapCorner, SpeedTracePoint } from './types'

interface SpeedChartProps {
  points: SpeedTracePoint[]
  /** Optional corners to show turn numbers and vertical markers at apex. */
  corners?: LapCorner[] | null
}

export function SpeedChart(props: SpeedChartProps) {
  if (!props.points.length) {
    return (
      <p className="text-muted">
        No speed data for this lap. Select a lap with telemetry to see speed vs distance.
      </p>
    )
  }

  const data = props.points.map(p => ({
    distanceM: p.distanceM,
    speedKph: p.speedKph,
  }))

  return (
    <ResponsiveContainer width="100%" height={300}>
      <LineChart data={data} margin={{ left: 50, right: 20, top: 5, bottom: 5 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="rgba(148,163,184,0.2)" />
        <XAxis
          dataKey="distanceM"
          tickFormatter={v => (Number.isFinite(v) ? `${Number(v).toFixed(0)} m` : '—')}
          label={{ value: 'Lap distance (m)', position: 'insideBottom', offset: -5, style: { fontSize: 12 } }}
        />
        <YAxis
          tickFormatter={v => (Number.isFinite(v) ? `${v} km/h` : '—')}
          label={{ value: 'Speed (km/h)', angle: -90, position: 'insideLeft', style: { fontSize: 12 } }}
        />
        <Tooltip
          formatter={(value: number | undefined) => [value != null && Number.isFinite(value) ? `${value} km/h` : '—', 'Speed']}
          labelFormatter={label => `Distance: ${Number(label).toFixed(0)} m`}
          contentStyle={{
            backgroundColor: '#111827',
            border: '1px solid rgba(148,163,184,0.4)',
            borderRadius: 6,
            fontSize: 'var(--text-sm)',
          }}
        />
        <Line
          type="monotone"
          dataKey="speedKph"
          name="Speed"
          stroke="#3b82f6"
          dot={{ r: 1 }}
          activeDot={{ r: 4 }}
        />
        {props.corners?.map(c => (
          <ReferenceLine
            key={c.cornerIndex}
            x={c.apexDistanceM}
            stroke="rgba(148,163,184,0.6)"
            strokeDasharray="2 2"
            label={{
              value: c.name ?? `T${c.cornerIndex}`,
              position: 'top',
              fill: 'var(--text-secondary)',
              fontSize: 11,
            }}
          />
        ))}
      </LineChart>
    </ResponsiveContainer>
  )
}
