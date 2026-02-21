interface LiveGearWidgetProps {
  gear: number | null | undefined
}

export function LiveGearWidget({ gear }: LiveGearWidgetProps) {
  return (
    <div className="card live-widget live-widget--center">
      <div className="live-widget__label">Gear</div>
      <div className="live-widget__value live-widget__value--gear">
        {gear != null ? gear : '—'}
      </div>
    </div>
  )
}
