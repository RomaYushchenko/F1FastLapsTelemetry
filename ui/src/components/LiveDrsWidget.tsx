interface LiveDrsWidgetProps {
  /** DRS wing open (from Car Telemetry). Primary indicator. */
  drs: boolean | null | undefined
  /** DRS zone allowed (from Car Status). Shown as "Available" when in zone but wing closed. */
  drsAllowed?: boolean | null | undefined
}

/**
 * DRS widget: wing state ON/OFF (from telemetry). Optional "Available" when in zone (drsAllowed) but wing closed.
 * Plan 12: drs = wing open, drsAllowed = zone.
 */
export function LiveDrsWidget({ drs, drsAllowed }: LiveDrsWidgetProps) {
  const badgeClass =
    drs == null
      ? 'live-widget__badge live-widget__badge--unknown'
      : drs
        ? 'live-widget__badge live-widget__badge--on'
        : 'live-widget__badge live-widget__badge--off'
  const inZoneNotOpen = drsAllowed === true && drs !== true

  return (
    <div className="card live-widget live-widget--compact live-widget--center">
      <div className="live-widget__label live-widget__label--tight">DRS</div>
      <span className={badgeClass}>
        {drs == null ? '—' : drs ? 'ON' : 'OFF'}
      </span>
      {inZoneNotOpen && (
        <span className="live-widget__value" style={{ fontSize: 'var(--text-xs)', marginTop: 'var(--space-1)' }}>
          Available
        </span>
      )}
    </div>
  )
}
