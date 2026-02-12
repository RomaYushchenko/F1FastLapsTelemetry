import { Link } from 'react-router-dom'
import { useLiveTelemetry } from '../ws/useLiveTelemetry'

export function LiveDashboardPage() {
  const { status, session, snapshot, sessionEnded, errorMessage, connectionMessage } =
    useLiveTelemetry()

  const hasActiveSession = status !== 'no-active-session' && session != null

  return (
    <div>
      <h1 className="heading-page">Live Telemetry</h1>

      {status === 'loading-active-session' && (
        <div className="card" style={{ padding: 'var(--space-3)', marginTop: 'var(--space-3)' }}>
          <p className="text-muted">Checking for active session…</p>
        </div>
      )}

      {status === 'no-active-session' && (
        <div>
          <p className="text-muted" style={{ marginBottom: 'var(--space-3)' }}>
            No active session. Start a session in F1 25 or view past sessions.
          </p>
          <Link
            to="/sessions"
            style={{
              display: 'inline-block',
              padding: '8px 16px',
              borderRadius: 'var(--radius-md)',
              backgroundColor: 'var(--accent)',
              color: 'white',
              textDecoration: 'none',
              fontWeight: 'var(--font-weight-medium)',
            }}
          >
            View sessions
          </Link>
        </div>
      )}

      {hasActiveSession && (
        <div style={{ marginTop: 'var(--space-4)', display: 'flex', flexDirection: 'column', gap: 'var(--space-4)' }}>
          {connectionMessage && (
            <div
              className="card"
              style={{
                padding: 'var(--space-2)',
                borderLeft: `4px solid var(--warning)`,
              }}
            >
              <p className="text-muted">{connectionMessage}</p>
            </div>
          )}

          {sessionEnded && (
            <div
              className="card"
              style={{
                padding: 'var(--space-2)',
                borderLeft: `4px solid var(--warning)`,
              }}
            >
              <p className="text-muted">
                Session ended (reason:{' '}
                <strong>{sessionEnded.endReason}</strong>).{' '}
                <Link to="/sessions">View sessions</Link>
              </p>
            </div>
          )}

          <div className="card" style={{ padding: 'var(--space-4)' }}>
            <p className="text-muted" style={{ marginBottom: 'var(--space-2)' }}>
              Session UID: <code className="text-mono">{session.sessionUID}</code>
            </p>
            <p className="text-muted" style={{ marginBottom: 'var(--space-2)' }}>
              Connection status: <strong>{status}</strong>
            </p>
            {sessionEnded && (
              <p className="text-muted" style={{ marginTop: 'var(--space-1)' }}>
                Session ended with reason: <strong>{sessionEnded.endReason}</strong>
              </p>
            )}
            {errorMessage && (
              <p className="text-error" style={{ marginTop: 'var(--space-1)' }}>
                {errorMessage}
              </p>
            )}
          </div>

          <div className="grid-3">
            {/* Speed */}
            <div className="card" style={{ padding: 'var(--space-4)' }}>
              <div
                style={{
                  fontSize: 'var(--text-sm)',
                  color: 'var(--text-secondary)',
                  marginBottom: 'var(--space-2)',
                }}
              >
                Speed
              </div>
              <div
                style={{
                  fontSize: 'var(--text-display-speed)',
                  fontFamily: 'var(--font-mono)',
                  fontWeight: 'var(--font-weight-semibold)',
                }}
              >
                {snapshot?.speedKph != null ? snapshot.speedKph : '—'}
                <span
                  style={{
                    fontSize: 'var(--text-sm)',
                    marginLeft: 'var(--space-2)',
                    color: 'var(--text-secondary)',
                  }}
                >
                  km/h
                </span>
              </div>
            </div>

            {/* RPM */}
            <div className="card" style={{ padding: 'var(--space-4)' }}>
              <div
                style={{
                  fontSize: 'var(--text-sm)',
                  color: 'var(--text-secondary)',
                  marginBottom: 'var(--space-2)',
                }}
              >
                RPM
              </div>
              <div
                style={{
                  fontSize: 'var(--text-display-rpm)',
                  fontFamily: 'var(--font-mono)',
                  fontWeight: 'var(--font-weight-semibold)',
                }}
              >
                {snapshot?.engineRpm != null ? snapshot.engineRpm : '—'}
              </div>
            </div>

            {/* Gear */}
            <div
              className="card"
              style={{
                padding: 'var(--space-4)',
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <div
                style={{
                  fontSize: 'var(--text-sm)',
                  color: 'var(--text-secondary)',
                  marginBottom: 'var(--space-2)',
                }}
              >
                Gear
              </div>
              <div
                style={{
                  fontSize: '2rem',
                  fontFamily: 'var(--font-mono)',
                  fontWeight: 'var(--font-weight-semibold)',
                }}
              >
                {snapshot?.gear != null ? snapshot.gear : '—'}
              </div>
            </div>
          </div>

          <div className="grid-4">
            {/* Throttle */}
            <div className="card" style={{ padding: 'var(--space-3)' }}>
              <div
                style={{
                  fontSize: 'var(--text-sm)',
                  color: 'var(--text-secondary)',
                  marginBottom: 'var(--space-2)',
                }}
              >
                Throttle
              </div>
              <ProgressBar value={snapshot?.throttle} color="var(--success)" />
            </div>

            {/* Brake */}
            <div className="card" style={{ padding: 'var(--space-3)' }}>
              <div
                style={{
                  fontSize: 'var(--text-sm)',
                  color: 'var(--text-secondary)',
                  marginBottom: 'var(--space-2)',
                }}
              >
                Brake
              </div>
              <ProgressBar value={snapshot?.brake} color="var(--accent)" />
            </div>

            {/* DRS */}
            <div
              className="card"
              style={{
                padding: 'var(--space-3)',
                display: 'flex',
                flexDirection: 'column',
                justifyContent: 'center',
                alignItems: 'center',
              }}
            >
              <div
                style={{
                  fontSize: 'var(--text-sm)',
                  color: 'var(--text-secondary)',
                  marginBottom: 'var(--space-1)',
                }}
              >
                DRS
              </div>
              <span
                style={{
                  padding: '4px 10px',
                  borderRadius: '999px',
                  fontSize: 'var(--text-sm)',
                  fontWeight: 'var(--font-weight-medium)',
                  backgroundColor:
                    snapshot?.drs == null
                      ? 'var(--bg-elevated)'
                      : snapshot.drs
                      ? 'var(--success)'
                      : 'var(--border)',
                  color: snapshot?.drs ? '#000' : 'var(--text-secondary)',
                }}
              >
                {snapshot?.drs == null ? '—' : snapshot.drs ? 'ON' : 'OFF'}
              </span>
            </div>

            {/* Lap / Sector */}
            <div
              className="card"
              style={{
                padding: 'var(--space-3)',
                display: 'flex',
                flexDirection: 'column',
                justifyContent: 'center',
                alignItems: 'center',
              }}
            >
              <div
                style={{
                  fontSize: 'var(--text-sm)',
                  color: 'var(--text-secondary)',
                  marginBottom: 'var(--space-1)',
                }}
              >
                Current lap / sector
              </div>
              <div
                style={{
                  fontSize: 'var(--text-base)',
                  fontFamily: 'var(--font-mono)',
                }}
              >
                {snapshot?.currentLap != null ? `Lap ${snapshot.currentLap}` : 'Lap —'} ·{' '}
                {snapshot?.currentSector != null && snapshot.currentSector > 0
                  ? `Sector ${snapshot.currentSector}`
                  : 'Sector —'}
              </div>
            </div>
          </div>

        </div>
      )}
    </div>
  )
}

interface ProgressBarProps {
  value: number | null | undefined
  color: string
}

function ProgressBar(props: ProgressBarProps) {
  const percentage = props.value != null ? Math.max(0, Math.min(1, props.value)) * 100 : 0

  return (
    <div
      style={{
        width: '100%',
        height: '10px',
        borderRadius: '999px',
        backgroundColor: 'var(--bg-elevated)',
        overflow: 'hidden',
      }}
    >
      <div
        style={{
          height: '100%',
          width: `${percentage}%`,
          backgroundColor: props.color,
          transition: 'width 80ms linear',
        }}
      />
    </div>
  )
}

