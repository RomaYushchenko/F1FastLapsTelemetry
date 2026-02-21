import { LiveProgressBar } from './LiveProgressBar'

interface LiveThrottleWidgetProps {
  throttle: number | null | undefined
}

export function LiveThrottleWidget({ throttle }: LiveThrottleWidgetProps) {
  return (
    <div className="card live-widget live-widget--compact">
      <div className="live-widget__label">Throttle</div>
      <LiveProgressBar value={throttle} color="var(--success)" />
    </div>
  )
}
