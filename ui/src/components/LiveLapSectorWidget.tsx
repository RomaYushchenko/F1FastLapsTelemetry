interface LiveLapSectorWidgetProps {
  currentLap: number | null | undefined
  currentSector: number | null | undefined
  /** Human-readable sector from backend (e.g. "Sector 1"). Plan 10. Preferred when present. */
  currentSectorDisplayName?: string | null | undefined
}

/**
 * Lap / sector widget. Uses currentSectorDisplayName from backend when present (single source of truth).
 */
export function LiveLapSectorWidget({
  currentLap,
  currentSector,
  currentSectorDisplayName,
}: LiveLapSectorWidgetProps) {
  const lapText =
    currentLap != null && currentLap > 0 ? `Lap ${currentLap}` : 'Lap —'
  const sectorText =
    currentSectorDisplayName != null && currentSectorDisplayName !== ''
      ? currentSectorDisplayName
      : currentSector != null && currentSector >= 0 && currentSector <= 2
        ? `Sector ${currentSector + 1}`
        : 'Sector —'

  return (
    <div className="card live-widget live-widget--compact live-widget--center">
      <div className="live-widget__label live-widget__label--tight">Current lap / sector</div>
      <div className="live-widget__value" style={{ fontSize: 'var(--text-base)' }}>
        {lapText} · {sectorText}
      </div>
    </div>
  )
}
