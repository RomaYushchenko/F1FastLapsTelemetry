import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { getSessions } from '../api/client'
import type { Session } from '../api/types'
import { getTrackName } from '../constants/tracks'

type LoadStatus = 'idle' | 'loading' | 'loaded' | 'error'

export function SessionListPage() {
  const navigate = useNavigate()
  const [status, setStatus] = useState<LoadStatus>('idle')
  const [sessions, setSessions] = useState<Session[]>([])
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const isCancelledRef = useRef(false)

  const load = useCallback(async () => {
    setStatus('loading')
    setErrorMessage(null)
    try {
      const data = await getSessions()
      if (isCancelledRef.current) return
      setSessions(data)
      setStatus('loaded')
    } catch (error) {
      if (isCancelledRef.current) return
      setStatus('error')
      setErrorMessage(error instanceof Error ? error.message : 'Failed to load sessions')
    }
  }, [])

  useEffect(() => {
    isCancelledRef.current = false
    // eslint-disable-next-line react-hooks/set-state-in-effect
    void load()

    return () => {
      isCancelledRef.current = true
    }
  }, [load])

  const hasSessions = sessions.length > 0

  return (
    <div>
      <h1 className="heading-page">Sessions</h1>

      {status === 'loading' && (
        <div className="card card-table" style={{ marginTop: 'var(--space-3)' }}>
          <p className="text-muted" style={{ padding: 'var(--space-3)' }}>
            Loading sessions…
          </p>
        </div>
      )}

      {status === 'error' && (
        <div style={{ marginTop: 'var(--space-3)' }}>
          <p className="text-error">
            {errorMessage ?? 'Something went wrong while loading sessions.'}
          </p>
          <button
            type="button"
            onClick={load}
            style={{
              marginTop: 'var(--space-2)',
              padding: '6px 12px',
              borderRadius: 'var(--radius-md)',
              border: '1px solid var(--border)',
              backgroundColor: 'var(--bg-surface)',
              color: 'var(--text-primary)',
              cursor: 'pointer',
            }}
          >
            Retry
          </button>
        </div>
      )}

      {status === 'loaded' && !hasSessions && (
        <p className="text-muted">No sessions yet. Start driving to record one.</p>
      )}

      {hasSessions && (
        <div className="card card-table" style={{ marginTop: 'var(--space-3)' }}>
          <table className="table">
            <thead>
              <tr>
                <th>Session</th>
                <th>Type</th>
                <th>Track</th>
                <th>Started</th>
                <th>Ended</th>
                <th>State</th>
              </tr>
            </thead>
            <tbody>
              {sessions.map(session => {
                const shortId = String(session.sessionUID)
                const startedAt = new Date(session.startedAt)
                const endedAt = session.endedAt ? new Date(session.endedAt) : null

                const startedLabel = isNaN(startedAt.getTime())
                  ? '—'
                  : startedAt.toLocaleString()
                const endedLabel =
                  endedAt && !isNaN(endedAt.getTime()) ? endedAt.toLocaleTimeString() : '—'

                return (
                  <tr
                    key={session.sessionUID}
                    onClick={() => navigate(`/sessions/${session.sessionUID}`)}
                  >
                    <td>
                      <Link
                        to={`/sessions/${session.sessionUID}`}
                        className="text-mono"
                        onClick={event => event.stopPropagation()}
                      >
                        {shortId.slice(0, 10)}…
                      </Link>
                    </td>
                    <td className="text-muted">{session.sessionType ?? '—'}</td>
                    <td className="text-muted">{getTrackName(session.trackId)}</td>
                    <td className="text-muted">{startedLabel}</td>
                    <td className="text-muted">{endedLabel}</td>
                    <td>
                      <StateBadge state={session.state} />
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}

interface StateBadgeProps {
  state?: Session['state']
}

function StateBadge(props: StateBadgeProps) {
  if (!props.state) {
    return (
      <span className="badge badge-unknown">UNKNOWN</span>
    )
  }

  const isActive = props.state === 'ACTIVE'

  return (
    <span className={`badge ${isActive ? 'badge-live' : 'badge-muted'}`}>
      {isActive ? 'ACTIVE' : 'FINISHED'}
    </span>
  )
}

