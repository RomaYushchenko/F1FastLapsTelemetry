import { useMemo } from 'react'
import { ResponsiveContainer, LineChart, Line, XAxis, YAxis, Tooltip, Legend } from 'recharts'
import type { PedalTracePoint } from './types'

interface ThrottleBrakeChartProps {
  points: PedalTracePoint[]
}

interface PedalTooltipProps {
  active?: boolean
  payload?: Array<{ payload: PedalTracePoint }>
}

/** Clamp and ensure number for 0–1 range. */
function clamp01(value: number | null | undefined): number {
  if (value == null || !Number.isFinite(value)) return 0
  return Math.max(0, Math.min(1, value))
}

export function ThrottleBrakeChart(props: ThrottleBrakeChartProps) {
  const data = useMemo(() => {
    const raw = props.points
    if (!raw.length) return []
    return [...raw]
      .map(p => ({
        distance: Number.isFinite(p.distance) ? p.distance : 0,
        throttle: clamp01(p.throttle),
        brake: clamp01(p.brake),
      }))
      .sort((a, b) => a.distance - b.distance)
  }, [props.points])

  if (!data.length) {
    return (
      <p className="text-muted">
        No pedal trace data available for this lap.
      </p>
    )
  }

  return (
    <ResponsiveContainer width="100%" height={300}>
      <LineChart data={data} margin={{ top: 8, right: 8, left: 8, bottom: 8 }}>
        <XAxis dataKey="distance" tickFormatter={formatDistance} />
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

function formatDistance(m: number): string {
  if (!Number.isFinite(m)) return '—'
  if (m >= 1000) return `${(m / 1000).toFixed(1)} km`
  return `${Math.round(m)} m`
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
        <span style={{ fontFamily: 'var(--font-mono)' }}>
          {Number.isFinite(datum.distance)
            ? datum.distance >= 1000
              ? `${(datum.distance / 1000).toFixed(2)} km`
              : `${datum.distance.toFixed(1)} m`
            : '—'}
        </span>
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

