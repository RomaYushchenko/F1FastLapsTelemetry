import { ResponsiveContainer, LineChart, Line, XAxis, YAxis, Tooltip, Legend } from 'recharts'
import type { PedalTracePoint } from './types'

interface ThrottleBrakeChartProps {
  points: PedalTracePoint[]
}

interface PedalTooltipProps {
  active?: boolean
  payload?: Array<{ payload: PedalTracePoint }>
}

export function ThrottleBrakeChart(props: ThrottleBrakeChartProps) {
  if (!props.points.length) {
    return (
      <p className="text-muted">
        No pedal trace data available for this lap.
      </p>
    )
  }

  const data = props.points

  return (
    <ResponsiveContainer width="100%" height={300}>
      <LineChart data={data}>
        <XAxis dataKey="distance" />
        <YAxis domain={[0, 1]} tickFormatter={toPercent} />
        <Tooltip content={<PedalTooltip />} />
        <Legend />
        <Line
          type="monotone"
          dataKey="throttle"
          name="Throttle"
          stroke="#22c55e"
          dot={false}
        />
        <Line
          type="monotone"
          dataKey="brake"
          name="Brake"
          stroke="#ef4444"
          dot={false}
        />
      </LineChart>
    </ResponsiveContainer>
  )
}

function toPercent(value: number): string {
  if (!Number.isFinite(value)) return '—'
  return `${Math.round(value * 100)}%`
}

function PedalTooltip(props: PedalTooltipProps) {
  const { active, payload } = props
  if (!active || !payload || !payload.length) return null

  const item = payload[0]
  const datum = item.payload as PedalTracePoint

  return (
    <div
      style={{
        backgroundColor: '#111827',
        borderRadius: 6,
        padding: '8px 10px',
        border: '1px solid rgba(148,163,184,0.4)',
        fontSize: 'var(--text-sm)',
      }}
    >
      <div style={{ marginBottom: 4 }}>
        Distance:{' '}
        <span style={{ fontFamily: 'var(--font-mono)' }}>{datum.distance.toFixed(1)} m</span>
      </div>
      <div>
        Throttle:{' '}
        <span style={{ fontFamily: 'var(--font-mono)' }}>{toPercent(datum.throttle)}</span>
      </div>
      <div>
        Brake:{' '}
        <span style={{ fontFamily: 'var(--font-mono)' }}>{toPercent(datum.brake)}</span>
      </div>
    </div>
  )
}

