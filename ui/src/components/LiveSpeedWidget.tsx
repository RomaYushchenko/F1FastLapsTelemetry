interface LiveSpeedWidgetProps {
  speedKph: number | null | undefined
}

export function LiveSpeedWidget({ speedKph }: LiveSpeedWidgetProps) {
  return (
    <div className="card live-widget">
      <div className="live-widget__label">Speed</div>
      <div className="live-widget__value live-widget__value--speed">
        {speedKph != null ? speedKph : '—'}
        <span className="live-widget__value--unit">km/h</span>
      </div>
    </div>
  )
}
