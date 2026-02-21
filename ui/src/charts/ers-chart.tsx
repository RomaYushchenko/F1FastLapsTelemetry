import {
  ResponsiveContainer,
  LineChart,
  Line,
  XAxis,
  YAxis,
  Tooltip,
  CartesianGrid,
} from 'recharts'
import type { ErsPoint } from './types'

interface ErsChartProps {
  points: ErsPoint[]
}

export function ErsChart(props: ErsChartProps) {
  if (!props.points.length) {
    return (
      <p className="text-muted">
        No ERS data for this lap. Data is merged from telemetry and car status for the selected lap.
      </p>
    )
  }

  const data = props.points.map(p => ({
    lapDistanceM: p.lapDistanceM,
    energyPercent: p.energyPercent,
  }))

  return (
    <ResponsiveContainer width="100%" height={260}>
      <LineChart data={data} margin={{ left: 50, right: 20, top: 5, bottom: 5 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="rgba(148,163,184,0.2)" />
        <XAxis
          dataKey="lapDistanceM"
          tickFormatter={v => (Number.isFinite(v) ? `${Number(v).toFixed(0)} m` : '—')}
          label={{ value: 'Lap distance (m)', position: 'insideBottom', offset: -5, style: { fontSize: 12 } }}
        />
        <YAxis
          domain={[0, 100]}
          tickFormatter={v => (Number.isFinite(v) ? `${v}%` : '—')}
          label={{ value: 'ERS %', angle: -90, position: 'insideLeft', style: { fontSize: 12 } }}
        />
        <Tooltip
          formatter={(value: number | undefined) => [value != null && Number.isFinite(value) ? `${value}%` : '—', 'ERS']}
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
          dataKey="energyPercent"
          name="ERS"
          stroke="#eab308"
          dot={{ r: 1 }}
          activeDot={{ r: 4 }}
        />
      </LineChart>
    </ResponsiveContainer>
  )
}
