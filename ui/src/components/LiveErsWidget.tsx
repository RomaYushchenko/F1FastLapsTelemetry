import { LiveProgressBar } from './LiveProgressBar'

interface LiveErsWidgetProps {
  /** ERS energy 0–100%. */
  ersEnergyPercent: number | null | undefined
  /** ERS deploy active (driver using ERS). */
  ersDeployActive: boolean | null | undefined
  /** ERS deploy mode label from backend (e.g. "Hotlap", "Overtake"). Plan 11. */
  ersDeployModeDisplayName?: string | null | undefined
}

/**
 * ERS widget: energy bar (0–100%) and Deploy indicator when active.
 * When deploy active, shows backend label (e.g. "Hotlap") when available, else "Deploy".
 */
export function LiveErsWidget({
  ersEnergyPercent,
  ersDeployActive,
  ersDeployModeDisplayName,
}: LiveErsWidgetProps) {
  const percent = ersEnergyPercent != null ? Math.max(0, Math.min(100, ersEnergyPercent)) / 100 : 0
  const isDeploy = ersDeployActive === true
  const deployLabel =
    isDeploy && ersDeployModeDisplayName && ersDeployModeDisplayName !== 'None' && ersDeployModeDisplayName !== 'Unknown'
      ? ersDeployModeDisplayName
      : 'Deploy'

  return (
    <div className="card live-widget live-widget--compact">
      <div className="live-widget__label live-widget__label--tight">ERS</div>
      <LiveProgressBar value={percent} color="var(--accent)" />
      <div style={{ marginTop: 'var(--space-2)', display: 'flex', alignItems: 'center', gap: 'var(--space-2)' }}>
        {ersEnergyPercent != null && (
          <span className="live-widget__value" style={{ fontSize: 'var(--text-sm)' }}>
            {Math.round(ersEnergyPercent)}%
          </span>
        )}
        {isDeploy && (
          <span
            className="live-widget__badge live-widget__badge--on"
            style={{ fontSize: 'var(--text-xs)' }}
          >
            {deployLabel}
          </span>
        )}
      </div>
    </div>
  )
}
