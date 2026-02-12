import { useEffect, useMemo, useRef, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getLapTrace, getSession, getSessionLaps, getSessionPace, getSessionSummary } from '../api/client'
import type { Lap, Session, SessionSummary } from '../api/types'
import { HttpError } from '../api/types'
import { getTrackName } from '../constants/tracks'
import type { PacePoint, PedalTracePoint } from '../charts/types'
import { PaceChart } from '../charts/pace-chart'
import { ThrottleBrakeChart } from '../charts/throttle-brake-chart'

type LoadStatus = 'idle' | 'loading' | 'loaded' | 'error'

export function SessionDetailPage() {
  const { sessionUid } = useParams<{ sessionUid: string }>()
  const [session, setSession] = useState<Session | null>(null)
  const [laps, setLaps] = useState<Lap[]>([])
  const [summary, setSummary] = useState<SessionSummary | null>(null)
  const [status, setStatus] = useState<LoadStatus>('idle')
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const isCancelledRef = useRef(false)
  const [pacePoints, setPacePoints] = useState<PacePoint[]>([])
  const [selectedLapForTrace, setSelectedLapForTrace] = useState<number | null>(null)
  const [tracePoints, setTracePoints] = useState<PedalTracePoint[] | null>(null)
  const [traceStatus, setTraceStatus] = useState<'idle' | 'loading' | 'loaded' | 'error'>('idle')
  const [traceError, setTraceError] = useState<string | null>(null)

  async function load() {
    if (sessionUid == null) {
      setStatus('error')
      setErrorMessage('Session ID is missing in URL')
      return
    }

    setStatus('loading')
    setErrorMessage(null)

    try {
      const id = sessionUid
      const [sessionRes, lapsRes, summaryRes, paceRes] = await Promise.all([
        getSession(id),
        getSessionLaps(id, 0),
        getSessionSummary(id, 0),
        getSessionPace(id),
      ])

      if (isCancelledRef.current) return

      setSession(sessionRes)
      setLaps(lapsRes)
      setSummary(summaryRes)
      setPacePoints(paceRes)

      const initialLapForTrace = summaryRes.bestLapNumber ?? (lapsRes[0]?.lapNumber ?? null)
      if (initialLapForTrace != null) {
        setSelectedLapForTrace(initialLapForTrace)
        void loadTrace(id, initialLapForTrace)
      }
      setStatus('loaded')
    } catch (error) {
      if (isCancelledRef.current) return
      if (error instanceof HttpError && error.status === 404) {
        setStatus('error')
        setErrorMessage('NOT_FOUND')
        return
      }

      setStatus('error')
      setErrorMessage(
        error instanceof Error ? error.message : 'Failed to load session details',
      )
    }
  }

  async function loadTrace(currentSessionUid: string, lapNumber: number) {
    setTraceStatus('loading')
    setTraceError(null)
    try {
      const points = await getLapTrace(currentSessionUid, lapNumber)
      if (isCancelledRef.current) return
      setTracePoints(points)
      setTraceStatus('loaded')
    } catch (error) {
      if (isCancelledRef.current) return
      setTraceStatus('error')
      setTraceError(error instanceof Error ? error.message : 'Failed to load pedal trace')
    }
  }

  useEffect(() => {
    isCancelledRef.current = false
    load()

    return () => {
      isCancelledRef.current = true
    }
  }, [sessionUid])

  const bestLapNumber = summary?.bestLapNumber ?? null

  const bestSectors = useMemo(() => {
    if (!summary) {
      return { s1: null as number | null, s2: null as number | null, s3: null as number | null }
    }
    return {
      s1: summary.bestSector1Ms,
      s2: summary.bestSector2Ms,
      s3: summary.bestSector3Ms,
    }
  }, [summary])

  const bestSectorLapNumbers = useMemo(() => {
    if (!summary || laps.length === 0) {
      return { s1: null as number | null, s2: null as number | null, s3: null as number | null }
    }

    function findLapNumberForSector(targetMs: number | null, pick: (lap: Lap) => number | null): number | null {
      if (targetMs == null) return null
      const lap = laps.find(currentLap => !currentLap.isInvalid && pick(currentLap) === targetMs)
      if (!lap) return null
      return lap.lapNumber
    }

    return {
      s1: findLapNumberForSector(summary.bestSector1Ms, lap => lap.sector1Ms),
      s2: findLapNumberForSector(summary.bestSector2Ms, lap => lap.sector2Ms),
      s3: findLapNumberForSector(summary.bestSector3Ms, lap => lap.sector3Ms),
    }
  }, [laps, summary])

  const titleIdPart =
    sessionUid != null && sessionUid.length > 0 ? `${sessionUid.slice(0, 12)}…` : '—'

  return (
    <div>
      <h1 className="heading-page">Session {titleIdPart}</h1>

      {session && (
        <p className="text-muted" style={{ marginBottom: 'var(--space-4)' }}>
          {session.sessionType} · {getTrackName(session.trackId)}{' '}
          {session.startedAt && (
            <>
              ·{' '}
              {new Date(session.startedAt).toLocaleString(undefined, {
                dateStyle: 'medium',
                timeStyle: 'short',
              })}
            </>
          )}
        </p>
      )}

      {status === 'error' && (
        <>
          {errorMessage === 'NOT_FOUND' ? (
            <div
              className="card"
              style={{
                padding: 'var(--space-4)',
                marginBottom: 'var(--space-4)',
              }}
            >
              <p className="text-muted" style={{ marginBottom: 'var(--space-3)' }}>
                Session not found.
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
                Back to sessions
              </Link>
            </div>
          ) : (
            <div>
              <p className="text-error">
                {errorMessage ?? 'Something went wrong while loading session details.'}
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
        </>
      )}

      {status === 'loading' && (
        <div className="card card-table" style={{ marginTop: 'var(--space-3)' }}>
          <p className="text-muted" style={{ padding: 'var(--space-3)' }}>
            Loading session details…
          </p>
        </div>
      )}

      {status === 'loaded' && session && summary && (
        <>
          {/* Summary block */}
          <div className="card" style={{ marginBottom: 'var(--space-4)', padding: 'var(--space-4)' }}>
            <h2 style={{ fontSize: 'var(--text-lg)', marginBottom: 'var(--space-3)' }}>Summary</h2>
            <div
              style={{
                display: 'flex',
                flexWrap: 'wrap',
                gap: 'var(--space-4)',
                fontSize: 'var(--text-sm)',
                color: 'var(--text-secondary)',
              }}
            >
              <SummaryItem
                label="Best lap"
                value={
                  summary.bestLapTimeMs != null
                    ? formatLapTime(summary.bestLapTimeMs)
                    : undefined
                }
                extra={
                  summary.bestLapNumber != null
                    ? `Lap ${summary.bestLapNumber}`
                    : undefined
                }
              />
              <SummaryItem
                label="Total laps"
                value={summary.totalLaps != null ? String(summary.totalLaps) : undefined}
              />
              <SummaryItem
                label="Best S1"
                value={
                  summary.bestSector1Ms != null
                    ? formatSectorTime(summary.bestSector1Ms)
                    : undefined
                }
                extra={
                  bestSectorLapNumbers.s1 != null
                    ? `Lap ${bestSectorLapNumbers.s1}`
                    : undefined
                }
              />
              <SummaryItem
                label="Best S2"
                value={
                  summary.bestSector2Ms != null
                    ? formatSectorTime(summary.bestSector2Ms)
                    : undefined
                }
                extra={
                  bestSectorLapNumbers.s2 != null
                    ? `Lap ${bestSectorLapNumbers.s2}`
                    : undefined
                }
              />
              <SummaryItem
                label="Best S3"
                value={
                  summary.bestSector3Ms != null
                    ? formatSectorTime(summary.bestSector3Ms)
                    : undefined
                }
                extra={
                  bestSectorLapNumbers.s3 != null
                    ? `Lap ${bestSectorLapNumbers.s3}`
                    : undefined
                }
              />
            </div>
          </div>

          {/* Pace chart */}
          <div className="card" style={{ marginBottom: 'var(--space-4)', padding: 'var(--space-4)' }}>
            <h2 style={{ fontSize: 'var(--text-lg)', marginBottom: 'var(--space-3)' }}>Lap pace</h2>
            <PaceChart points={pacePoints} />
          </div>

          {/* Pedal trace chart */}
          <div className="card" style={{ marginBottom: 'var(--space-4)', padding: 'var(--space-4)' }}>
            <div
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                marginBottom: 'var(--space-3)',
              }}
            >
              <h2 style={{ fontSize: 'var(--text-lg)' }}>Pedal trace</h2>
              {laps.length > 0 && (
                <label
                  style={{
                    fontSize: 'var(--text-sm)',
                    color: 'var(--text-secondary)',
                    display: 'flex',
                    alignItems: 'center',
                    gap: 'var(--space-2)',
                  }}
                >
                  Lap:
                  <select
                    value={selectedLapForTrace ?? ''}
                    onChange={event => {
                      const value = Number(event.target.value)
                      if (!Number.isFinite(value)) return
                      setSelectedLapForTrace(value)
                      if (sessionUid != null) {
                        void loadTrace(sessionUid, value)
                      }
                    }}
                    style={{
                      backgroundColor: 'var(--bg-surface)',
                      color: 'var(--text-primary)',
                      borderRadius: 'var(--radius-sm)',
                      border: '1px solid var(--border)',
                      padding: '4px 8px',
                      fontSize: 'var(--text-sm)',
                    }}
                  >
                    {laps.map(lap => (
                      <option key={lap.lapNumber} value={lap.lapNumber}>
                        Lap {lap.lapNumber}
                      </option>
                    ))}
                  </select>
                </label>
              )}
            </div>

            {traceStatus === 'loading' && (
              <p className="text-muted">Loading pedal trace…</p>
            )}
            {traceStatus === 'error' && (
              <div>
                <p className="text-error">
                  {traceError ?? 'Failed to load pedal trace.'}
                </p>
                {selectedLapForTrace != null && sessionUid != null && (
                  <button
                    type="button"
                    onClick={() => loadTrace(sessionUid, selectedLapForTrace)}
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
                )}
              </div>
            )}
            {(traceStatus === 'loaded' || traceStatus === 'idle') && tracePoints && (
              <ThrottleBrakeChart points={tracePoints} />
            )}
          </div>

          {/* Laps table */}
          <div className="card card-table">
            <table className="table">
              <thead>
                <tr>
                  <th style={{ textAlign: 'left' }}>Lap</th>
                  <th>Time</th>
                  <th>S1</th>
                  <th>S2</th>
                  <th>S3</th>
                  <th>Valid</th>
                </tr>
              </thead>
              <tbody>
                {laps.map(lap => {
                  const isBestLap = bestLapNumber != null && lap.lapNumber === bestLapNumber
                  const isBestS1 =
                    bestSectors.s1 != null && lap.sector1Ms === bestSectors.s1 && !lap.isInvalid
                  const isBestS2 =
                    bestSectors.s2 != null && lap.sector2Ms === bestSectors.s2 && !lap.isInvalid
                  const isBestS3 =
                    bestSectors.s3 != null && lap.sector3Ms === bestSectors.s3 && !lap.isInvalid

                  return (
                    <tr
                      key={lap.lapNumber}
                      style={{
                        backgroundColor: isBestLap ? 'rgba(34,197,94,0.06)' : 'transparent',
                      }}
                    >
                      <td
                        style={{
                          textAlign: 'left',
                          fontWeight: isBestLap ? 'var(--font-weight-medium)' : undefined,
                        }}
                      >
                        {lap.lapNumber}
                      </td>
                      <td>
                        {lap.lapTimeMs != null ? formatLapTime(lap.lapTimeMs) : '—'}
                        {isBestLap && (
                          <span
                            style={{
                              marginLeft: 'var(--space-1)',
                              fontSize: 'var(--text-xs)',
                              color: 'var(--accent-muted)',
                            }}
                          >
                            BEST
                          </span>
                        )}
                      </td>
                      <td>
                        <SectorCell
                          value={lap.sector1Ms}
                          isBest={isBestS1}
                        />
                      </td>
                      <td>
                        <SectorCell
                          value={lap.sector2Ms}
                          isBest={isBestS2}
                        />
                      </td>
                      <td>
                        <SectorCell
                          value={lap.sector3Ms}
                          isBest={isBestS3}
                        />
                      </td>
                      <td>
                        {lap.isInvalid ? (
                          <span
                            style={{
                              fontSize: 'var(--text-xs)',
                              color: 'var(--error)',
                            }}
                          >
                            INVALID
                          </span>
                        ) : (
                          <span
                            style={{
                              fontSize: 'var(--text-xs)',
                              color: 'var(--success)',
                            }}
                          >
                            ✓
                          </span>
                        )}
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        </>
      )}
    </div>
  )
}

interface SummaryItemProps {
  label: string
  value?: string
  extra?: string
}

function SummaryItem(props: SummaryItemProps) {
  return (
    <div
      style={{
        minWidth: '140px',
      }}
    >
      <div
        style={{
          fontSize: 'var(--text-xs)',
          textTransform: 'uppercase',
          letterSpacing: '0.08em',
          marginBottom: '2px',
        }}
      >
        {props.label}
      </div>
      <div
        style={{
          fontSize: 'var(--text-base)',
          color: 'var(--text-primary)',
          fontFamily: 'var(--font-mono)',
        }}
      >
        {props.value ?? '—'}
      </div>
      {props.extra && (
        <div
          style={{
            fontSize: 'var(--text-xs)',
            marginTop: '2px',
          }}
        >
          {props.extra}
        </div>
      )}
    </div>
  )
}

interface SectorCellProps {
  value: number | null
  isBest: boolean
}

function SectorCell(props: SectorCellProps) {
  if (props.value == null) return <span>—</span>

  return (
    <span
      style={{
        fontFamily: 'var(--font-mono)',
        color: props.isBest ? 'var(--accent-muted)' : 'inherit',
        fontWeight: props.isBest ? 'var(--font-weight-medium)' : undefined,
      }}
    >
      {formatSectorTime(props.value)}
      {props.isBest && (
        <span
          style={{
            marginLeft: 'var(--space-1)',
            fontSize: 'var(--text-xs)',
          }}
        >
          *
        </span>
      )}
    </span>
  )
}

function formatLapTime(ms: number): string {
  if (!Number.isFinite(ms) || ms <= 0) return '—'
  const totalSeconds = Math.floor(ms / 1000)
  const minutes = Math.floor(totalSeconds / 60)
  const seconds = totalSeconds % 60
  const millis = ms % 1000
  const secondsStr = seconds.toString().padStart(2, '0')
  const millisStr = millis.toString().padStart(3, '0')
  return `${minutes}:${secondsStr}.${millisStr}`
}

function formatSectorTime(ms: number): string {
  if (!Number.isFinite(ms) || ms <= 0) return '—'
  const totalSeconds = ms / 1000
  return totalSeconds.toFixed(3)
}

