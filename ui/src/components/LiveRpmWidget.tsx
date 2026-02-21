interface LiveRpmWidgetProps {
  engineRpm: number | null | undefined
}

export function LiveRpmWidget({ engineRpm }: LiveRpmWidgetProps) {
  return (
    <div className="card live-widget">
      <div className="live-widget__label">RPM</div>
      <div className="live-widget__value live-widget__value--rpm">
        {engineRpm != null ? engineRpm : '—'}
      </div>
    </div>
  )
}
