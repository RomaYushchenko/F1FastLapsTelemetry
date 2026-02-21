interface LiveDrsWidgetProps {
  drs: boolean | null | undefined
}

export function LiveDrsWidget({ drs }: LiveDrsWidgetProps) {
  const badgeClass =
    drs == null
      ? 'live-widget__badge live-widget__badge--unknown'
      : drs
        ? 'live-widget__badge live-widget__badge--on'
        : 'live-widget__badge live-widget__badge--off'

  return (
    <div className="card live-widget live-widget--compact live-widget--center">
      <div className="live-widget__label live-widget__label--tight">DRS</div>
      <span className={badgeClass}>
        {drs == null ? '—' : drs ? 'ON' : 'OFF'}
      </span>
    </div>
  )
}
