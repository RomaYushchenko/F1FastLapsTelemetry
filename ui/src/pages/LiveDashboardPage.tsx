import { useCallback } from 'react'
import { Link } from 'react-router-dom'
import { useLiveTelemetry } from '../ws/useLiveTelemetry'
import { LiveStateMessage, type LiveStateType } from '../components/LiveStateMessage'
import { LiveSpeedWidget } from '../components/LiveSpeedWidget'
import { LiveRpmWidget } from '../components/LiveRpmWidget'
import { LiveGearWidget } from '../components/LiveGearWidget'
import { LiveThrottleWidget } from '../components/LiveThrottleWidget'
import { LiveBrakeWidget } from '../components/LiveBrakeWidget'
import { LiveDrsWidget } from '../components/LiveDrsWidget'
import { LiveLapSectorWidget } from '../components/LiveLapSectorWidget'
import { LiveDeltaWidget } from '../components/LiveDeltaWidget'

export function LiveDashboardPage() {
  const { status, session, snapshot, sessionEnded, errorMessage, connectionMessage } =
    useLiveTelemetry()
  const hasActiveSession = status !== 'no-active-session' && session != null

  const handleRetry = useCallback(() => {
    window.location.reload()
  }, [])

  const showStateMessage =
    status === 'idle' ||
    status === 'loading-active-session' ||
    status === 'error' ||
    status === 'no-active-session'

  return (
    <div className="live-dashboard">
      <header className="live-dashboard__header">
        <h1 className="heading-page">Live Telemetry</h1>
      </header>

      {showStateMessage && (
        <LiveStateMessage
          state={status as LiveStateType}
          errorMessage={errorMessage}
          onRetry={status === 'error' ? handleRetry : undefined}
        />
      )}

      {hasActiveSession && (
        <div className="live-dashboard__widgets">
          {connectionMessage && (
            <div
              className="card"
              style={{
                padding: 'var(--space-2) var(--space-4)',
                borderLeft: '4px solid var(--warning)',
              }}
            >
              <p className="text-muted" style={{ margin: 0 }}>{connectionMessage}</p>
            </div>
          )}

          {sessionEnded && (
            <div
              className="card"
              style={{
                padding: 'var(--space-2) var(--space-4)',
                borderLeft: '4px solid var(--warning)',
              }}
            >
              <p className="text-muted" style={{ margin: 0 }}>
                Session ended (reason: <strong>{sessionEnded.endReason}</strong>).{' '}
                <Link to="/sessions">View sessions</Link>
              </p>
            </div>
          )}

          <div className="card live-session-bar">
            <span className="live-session-bar__id">Session: <code className="text-mono">{session.id}</code></span>
            <span className="live-session-bar__status">
              {status === 'connected' ? (
                <span className="live-session-bar__badge">Live</span>
              ) : sessionEnded ? (
                <span className="live-session-bar__badge live-session-bar__badge--ended">Ended</span>
              ) : (
                <span className="live-session-bar__badge live-session-bar__badge--disconnected">{status}</span>
              )}
            </span>
            {errorMessage && (
              <span className="text-error" style={{ fontSize: 'var(--text-sm)' }}>{errorMessage}</span>
            )}
          </div>

          <div className="grid-3">
            <LiveSpeedWidget speedKph={snapshot?.speedKph} />
            <LiveRpmWidget engineRpm={snapshot?.engineRpm} />
            <LiveGearWidget gear={snapshot?.gear} />
          </div>

          <div className="grid-4">
            <LiveThrottleWidget throttle={snapshot?.throttle} />
            <LiveBrakeWidget brake={snapshot?.brake} />
            <LiveDrsWidget drs={snapshot?.drs} />
            <LiveLapSectorWidget
              currentLap={snapshot?.currentLap}
              currentSector={snapshot?.currentSector}
            />
          </div>

          <div className="grid-3">
            <LiveDeltaWidget deltaMs={snapshot?.deltaMs} />
          </div>
        </div>
      )}
    </div>
  )
}
