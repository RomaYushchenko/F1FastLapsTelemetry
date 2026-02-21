interface LiveProgressBarProps {
  value: number | null | undefined
  color: string
}

/**
 * Progress bar for live telemetry (throttle, brake). 80ms transition for smooth updates.
 */
export function LiveProgressBar({ value, color }: LiveProgressBarProps) {
  const percentage = value != null ? Math.max(0, Math.min(1, value)) * 100 : 0

  return (
    <div className="live-widget__progress">
      <div
        className="live-widget__progress-fill"
        style={{ width: `${percentage}%`, backgroundColor: color }}
      />
    </div>
  )
}
