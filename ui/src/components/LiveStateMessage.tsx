import { Link } from 'react-router-dom'

export type LiveStateType = 'idle' | 'loading-active-session' | 'error' | 'no-active-session'

interface LiveStateMessageProps {
  state: LiveStateType
  errorMessage?: string | null
  onRetry?: () => void
}

/**
 * Reusable state block for Live page: loading, error, no active session.
 * Keeps layout and styling consistent per ui_ux_specification.
 */
export function LiveStateMessage({ state, errorMessage, onRetry }: LiveStateMessageProps) {
  if (state === 'idle') {
    return (
      <div className="live-state-card card" data-state="idle">
        <p className="text-muted">Loading…</p>
      </div>
    )
  }

  if (state === 'loading-active-session') {
    return (
      <div className="live-state-card card" data-state="loading">
        <p className="text-muted">Checking for active session…</p>
      </div>
    )
  }

  if (state === 'error') {
    return (
      <div className="live-state-card card live-state-card--error" data-state="error">
        <p className="live-state-card__title">Cannot connect to the telemetry service</p>
        <p className="text-muted live-state-card__message">
          {errorMessage ?? 'The server may be stopped or unreachable.'}
        </p>
        <p className="text-muted live-state-card__hint">
          Make sure the backend is running and, if you use telemetry, that the game or UDP ingest is connected.
        </p>
        <div className="live-state-card__actions">
          {onRetry && (
            <button type="button" className="live-state-card__btn live-state-card__btn--primary" onClick={onRetry}>
              Retry
            </button>
          )}
          <Link to="/sessions" className="live-state-card__btn live-state-card__btn--secondary">
            View sessions
          </Link>
        </div>
      </div>
    )
  }

  if (state === 'no-active-session') {
    return (
      <div className="live-state-card card live-state-card--welcome" data-state="no-session">
        <p className="live-state-card__title">Welcome to F1 FastLaps Telemetry</p>
        <p className="text-muted live-state-card__message">
          This app shows live telemetry from F1 25 and lets you browse recorded sessions, laps and charts.
        </p>
        <p className="live-state-card__subtitle">
          Right now there is no active session and the app is not receiving live data.
        </p>
        <ul className="live-state-card__list text-muted">
          <li>Start a session in F1 25 and ensure the telemetry backend (UDP ingest) is running and connected.</li>
          <li>Or open <Link to="/sessions">Sessions</Link> to view and analyse past sessions.</li>
        </ul>
        <Link to="/sessions" className="live-state-card__btn live-state-card__btn--primary">
          View sessions
        </Link>
      </div>
    )
  }

  return null
}
