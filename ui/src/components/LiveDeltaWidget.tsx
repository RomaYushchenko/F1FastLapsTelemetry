interface LiveDeltaWidgetProps {
  /** Delta to best lap in ms. Negative = faster than best, positive = slower. */
  deltaMs: number | null | undefined
}

/**
 * Displays delta to best lap: ±seconds. Green when faster than best, red when slower.
 */
export function LiveDeltaWidget({ deltaMs }: LiveDeltaWidgetProps) {
  if (deltaMs == null) {
    return (
      <div className="card live-widget live-widget--compact live-widget--center">
        <div className="live-widget__label live-widget__label--tight">Delta to best</div>
        <div className="live-widget__value" style={{ fontSize: 'var(--text-base)' }}>
          —
        </div>
      </div>
    )
  }

  const seconds = deltaMs / 1000
  const sign = seconds >= 0 ? '+' : ''
  const text = `${sign}${seconds.toFixed(3)}`
  const isFaster = deltaMs < 0
  const color = isFaster ? 'var(--success)' : deltaMs > 0 ? 'var(--error)' : 'var(--text-primary)'

  return (
    <div className="card live-widget live-widget--compact live-widget--center">
      <div className="live-widget__label live-widget__label--tight">Delta to best</div>
      <div
        className="live-widget__value"
        style={{ fontSize: 'var(--text-lg)', fontWeight: 'var(--font-weight-semibold)', color }}
      >
        {text}s
      </div>
    </div>
  )
}
