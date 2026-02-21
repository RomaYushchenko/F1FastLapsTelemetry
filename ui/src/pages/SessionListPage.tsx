import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { getSessions, updateSessionDisplayName } from '../api/client'
import { isValidSessionId, toSessionIdString } from '../api/sessionId'
import type { Session } from '../api/types'
import { getTrackName } from '../constants/tracks'

const DISPLAY_NAME_MAX_LENGTH = 64

type LoadStatus = 'idle' | 'loading' | 'loaded' | 'error'

export function SessionListPage() {
  const navigate = useNavigate()
  const [status, setStatus] = useState<LoadStatus>('idle')
  const [sessions, setSessions] = useState<Session[]>([])
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const [editingSession, setEditingSession] = useState<Session | null>(null)
  const [editValue, setEditValue] = useState('')
  const [editError, setEditError] = useState<string | null>(null)
  const [editSaving, setEditSaving] = useState(false)
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

  const openEditModal = useCallback((session: Session) => {
    setEditingSession(session)
    setEditValue(session.sessionDisplayName?.trim() ?? session.id)
    setEditError(null)
  }, [])

  const closeEditModal = useCallback(() => {
    setEditingSession(null)
    setEditValue('')
    setEditError(null)
  }, [])

  const saveDisplayName = useCallback(async () => {
    if (!editingSession) return
    const trimmed = editValue.trim()
    if (!trimmed) {
      setEditError('Display name cannot be empty')
      return
    }
    if (trimmed.length > DISPLAY_NAME_MAX_LENGTH) {
      setEditError(`Maximum ${DISPLAY_NAME_MAX_LENGTH} characters`)
      return
    }
    setEditSaving(true)
    setEditError(null)
    try {
      const updated = await updateSessionDisplayName(editingSession.id, trimmed)
      if (isCancelledRef.current) return
      setSessions(prev =>
        prev.map(s => (s.id === updated.id ? { ...s, sessionDisplayName: updated.sessionDisplayName } : s)),
      )
      closeEditModal()
    } catch (error) {
      if (isCancelledRef.current) return
      setEditError(error instanceof Error ? error.message : 'Failed to update name')
    } finally {
      if (!isCancelledRef.current) setEditSaving(false)
    }
  }, [editingSession, editValue, closeEditModal])

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
                const sessionId = toSessionIdString(session.id)
                const displayName = session.sessionDisplayName?.trim() || session.id
                const validId = isValidSessionId(sessionId)
                const startedAt = new Date(session.startedAt)
                const endedAt = session.endedAt ? new Date(session.endedAt) : null

                const startedLabel = isNaN(startedAt.getTime())
                  ? '—'
                  : startedAt.toLocaleString()
                const endedLabel =
                  endedAt && !isNaN(endedAt.getTime()) ? endedAt.toLocaleTimeString() : '—'

                return (
                  <tr
                    key={sessionId}
                    onClick={() => validId && navigate(`/sessions/${sessionId}`)}
                    style={{ cursor: validId ? 'pointer' : undefined }}
                  >
                    <td>
                      <span style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-2)' }}>
                        {validId ? (
                          <Link
                            to={`/sessions/${sessionId}`}
                            className="text-mono"
                            onClick={event => event.stopPropagation()}
                            title={session.id}
                          >
                            {displayName}
                          </Link>
                        ) : (
                          <span className="text-mono" title="Invalid session ID (use Sessions list)">
                            {displayName} (unavailable)
                          </span>
                        )}
                        <button
                          type="button"
                          aria-label="Edit display name"
                          onClick={event => {
                            event.stopPropagation()
                            openEditModal(session)
                          }}
                          style={{
                            padding: '4px 8px',
                            borderRadius: 'var(--radius-sm)',
                            border: '1px solid var(--border)',
                            background: 'var(--bg-surface)',
                            color: 'var(--text-secondary)',
                            cursor: 'pointer',
                            fontSize: 'var(--text-sm)',
                          }}
                        >
                          Edit
                        </button>
                      </span>
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

      {editingSession && (
        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby="edit-display-name-title"
          style={{
            position: 'fixed',
            inset: 0,
            backgroundColor: 'rgba(0,0,0,0.5)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 1000,
          }}
          onClick={closeEditModal}
        >
          <div
            className="card"
            style={{
              padding: 'var(--space-4)',
              minWidth: '320px',
              maxWidth: '90vw',
            }}
            onClick={e => e.stopPropagation()}
          >
            <h2 id="edit-display-name-title" style={{ fontSize: 'var(--text-lg)', marginBottom: 'var(--space-3)' }}>
              Edit session name
            </h2>
            <p className="text-muted" style={{ fontSize: 'var(--text-sm)', marginBottom: 'var(--space-3)' }}>
              Session ID: {editingSession.id}
            </p>
            <input
              type="text"
              value={editValue}
              onChange={e => setEditValue(e.target.value)}
              maxLength={DISPLAY_NAME_MAX_LENGTH + 1}
              placeholder="Display name"
              autoFocus
              style={{
                width: '100%',
                padding: 'var(--space-2) var(--space-3)',
                borderRadius: 'var(--radius-md)',
                border: '1px solid var(--border)',
                backgroundColor: 'var(--bg-surface)',
                color: 'var(--text-primary)',
                fontSize: 'var(--text-base)',
                marginBottom: 'var(--space-2)',
              }}
            />
            <p style={{ fontSize: 'var(--text-xs)', color: 'var(--text-muted)', marginBottom: 'var(--space-3)' }}>
              {editValue.length} / {DISPLAY_NAME_MAX_LENGTH}
            </p>
            {editError && (
              <p className="text-error" style={{ marginBottom: 'var(--space-3)' }}>
                {editError}
              </p>
            )}
            <div style={{ display: 'flex', gap: 'var(--space-2)', justifyContent: 'flex-end' }}>
              <button
                type="button"
                onClick={closeEditModal}
                disabled={editSaving}
                style={{
                  padding: '6px 12px',
                  borderRadius: 'var(--radius-md)',
                  border: '1px solid var(--border)',
                  background: 'var(--bg-surface)',
                  color: 'var(--text-primary)',
                  cursor: editSaving ? 'not-allowed' : 'pointer',
                }}
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={saveDisplayName}
                disabled={editSaving}
                style={{
                  padding: '6px 12px',
                  borderRadius: 'var(--radius-md)',
                  border: 'none',
                  background: 'var(--accent)',
                  color: 'white',
                  cursor: editSaving ? 'not-allowed' : 'pointer',
                }}
              >
                {editSaving ? 'Saving…' : 'Save'}
              </button>
            </div>
          </div>
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

