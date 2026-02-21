import { LiveProgressBar } from './LiveProgressBar'

interface LiveBrakeWidgetProps {
  brake: number | null | undefined
}

export function LiveBrakeWidget({ brake }: LiveBrakeWidgetProps) {
  return (
    <div className="card live-widget live-widget--compact">
      <div className="live-widget__label">Brake</div>
      <LiveProgressBar value={brake} color="var(--accent)" />
    </div>
  )
}
