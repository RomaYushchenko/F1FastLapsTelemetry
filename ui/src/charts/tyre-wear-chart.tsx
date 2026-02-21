import {
  ResponsiveContainer,
  LineChart,
  Line,
  XAxis,
  YAxis,
  Tooltip,
  Legend,
  CartesianGrid,
} from 'recharts'
import type { TyreWearPoint } from './types'

interface TyreWearChartProps {
  points: TyreWearPoint[]
}

interface TyreWearTooltipProps {
  active?: boolean
  payload?: Array<{ name: string; value: number; color: string; payload?: { compound?: number } }>
  label?: string | number
}

/** F1 25 compound code to Pirelli label (16=C5 soft … 21=C0 hard, 22=C6, 7=Inter, 8=Wet). */
function compoundLabel(code: number): string {
  if (code === 7) return 'Inter'
  if (code === 8) return 'Wet'
  if (code === 22) return 'C6'
  if (code >= 16 && code <= 21) return `C${21 - code}` // C5(16) … C0(21)
  return `#${code}`
}

const WEAR_FL_COLOR = '#3b82f6'
const WEAR_FR_COLOR = '#8b5cf6'
const WEAR_RL_COLOR = '#f59e0b'
const WEAR_RR_COLOR = '#ef4444'

export function TyreWearChart(props: TyreWearChartProps) {
  if (!props.points.length) {
    return (
      <p className="text-muted">
        No tyre wear data available for this session. Enable car damage in F1 25 settings to record wear.
      </p>
    )
  }

  // Backend may send 0–1 (fraction) or 0–100 (percentage); normalize to 0–100 for display.
  const toPercent = (v: number | null | undefined): number | null =>
    v == null ? null : v > 1 ? v : v * 100

  const data = props.points.map(p => ({
    lapNumber: p.lapNumber,
    wearFL: toPercent(p.wearFL),
    wearFR: toPercent(p.wearFR),
    wearRL: toPercent(p.wearRL),
    wearRR: toPercent(p.wearRR),
    compound: p.compound ?? undefined,
  }))

  return (
    <ResponsiveContainer width="100%" height={300}>
      <LineChart data={data} margin={{ top: 5, right: 20, left: 0, bottom: 5 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="rgba(148,163,184,0.2)" />
        <XAxis dataKey="lapNumber" />
        <YAxis
          domain={[0, 100]}
          tickFormatter={v => (Number.isFinite(v) ? `${v}%` : '—')}
          label={{ value: 'Wear %', angle: -90, position: 'insideLeft', style: { fontSize: 12 } }}
        />
        <Tooltip content={<TyreWearTooltip />} />
        <Legend />
        <Line
          type="monotone"
          dataKey="wearFL"
          name="FL"
          stroke={WEAR_FL_COLOR}
          dot={{ r: 2 }}
          activeDot={{ r: 4 }}
          connectNulls
        />
        <Line
          type="monotone"
          dataKey="wearFR"
          name="FR"
          stroke={WEAR_FR_COLOR}
          dot={{ r: 2 }}
          activeDot={{ r: 4 }}
          connectNulls
        />
        <Line
          type="monotone"
          dataKey="wearRL"
          name="RL"
          stroke={WEAR_RL_COLOR}
          dot={{ r: 2 }}
          activeDot={{ r: 4 }}
          connectNulls
        />
        <Line
          type="monotone"
          dataKey="wearRR"
          name="RR"
          stroke={WEAR_RR_COLOR}
          dot={{ r: 2 }}
          activeDot={{ r: 4 }}
          connectNulls
        />
      </LineChart>
    </ResponsiveContainer>
  )
}

function TyreWearTooltip(props: TyreWearTooltipProps) {
  const { active, payload, label } = props
  if (!active || !payload || !payload.length) return null

  const datum = payload[0]?.payload as { compound?: number } | undefined
  const compound = datum?.compound

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
      {compound != null && (
        <div style={{ marginBottom: 4, color: 'var(--text-secondary)' }}>
          Compound: {compoundLabel(compound)}
        </div>
      )}
      {payload.map(entry => (
        <div key={entry.name} style={{ color: entry.color }}>
          {entry.name}: {entry.value != null && Number.isFinite(entry.value) ? `${Number(entry.value).toFixed(1)}%` : '—'}
        </div>
      ))}
    </div>
  )
}
