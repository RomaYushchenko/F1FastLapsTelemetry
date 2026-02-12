import { ResponsiveContainer, LineChart, Line, XAxis, YAxis, Tooltip, Legend } from 'recharts'
import type { PacePoint } from './types'

interface PaceChartProps {
  points: PacePoint[]
}

interface PaceChartDatum extends PacePoint {
  lapTimeSeconds: number
}

interface PaceTooltipProps {
  active?: boolean
  payload?: Array<{ payload: PaceChartDatum }>
  label?: string | number
}

export function PaceChart(props: PaceChartProps) {
  if (!props.points.length) {
    return (
      <p className="text-muted">
        No pace data available for this session.
      </p>
    )
  }

  const data: PaceChartDatum[] = props.points.map(point => ({
    ...point,
    lapTimeSeconds: point.lapTimeMs / 1000,
  }))

  return (
    <ResponsiveContainer width="100%" height={300}>
      <LineChart data={data}>
        <XAxis dataKey="lapNumber" />
        <YAxis tickFormatter={formatSecondsLabel} />
        <Tooltip content={<PaceTooltip />} />
        <Legend />
        <Line
          type="monotone"
          dataKey="lapTimeSeconds"
          name="Lap time"
          stroke="#22c55e"
          dot={{ r: 3 }}
          activeDot={{ r: 5 }}
        />
      </LineChart>
    </ResponsiveContainer>
  )
}

function formatSecondsLabel(value: number): string {
  if (!Number.isFinite(value) || value <= 0) return '—'
  const totalMillis = Math.round(value * 1000)
  return formatLapTimeFromMs(totalMillis)
}

function formatLapTimeFromMs(ms: number): string {
  if (!Number.isFinite(ms) || ms <= 0) return '—'
  const totalSeconds = Math.floor(ms / 1000)
  const minutes = Math.floor(totalSeconds / 60)
  const seconds = totalSeconds % 60
  const millis = ms % 1000
  const secondsStr = seconds.toString().padStart(2, '0')
  const millisStr = millis.toString().padStart(3, '0')
  return `${minutes}:${secondsStr}.${millisStr}`
}

function PaceTooltip(props: PaceTooltipProps) {
  const { active, payload, label } = props
  if (!active || !payload || !payload.length) return null

  const item = payload[0]
  const datum = item.payload as PaceChartDatum

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
      <div style={{ marginBottom: 4 }}>Lap {label}</div>
      <div>
        Lap time:{' '}
        <span style={{ fontFamily: 'var(--font-mono)' }}>
          {formatLapTimeFromMs(datum.lapTimeMs)}
        </span>
      </div>
      {datum.tyreCompound && (
        <div>
          Tyre: <span>{datum.tyreCompound}</span>
        </div>
      )}
      {typeof datum.stintIndex === 'number' && (
        <div>
          Stint: <span>{datum.stintIndex}</span>
        </div>
      )}
    </div>
  )
}

