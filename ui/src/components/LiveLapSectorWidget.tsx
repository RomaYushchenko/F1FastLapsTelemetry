interface LiveLapSectorWidgetProps {
  currentLap: number | null | undefined
  currentSector: number | null | undefined
}

export function LiveLapSectorWidget({ currentLap, currentSector }: LiveLapSectorWidgetProps) {
  const lapText =
    currentLap != null && currentLap > 0 ? `Lap ${currentLap}` : 'Lap —'
  const sectorText =
    currentSector != null && currentSector >= 0 && currentSector <= 2
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
