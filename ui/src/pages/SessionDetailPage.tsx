import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { toast } from 'sonner'
import { getLapErs, getLapSpeedTrace, getLapTrace, getSession, getSessionLaps, getSessionPace, getSessionSummary, getSessionTyreWear } from '../api/client'
import { isValidSessionId } from '../api/sessionId'
import type { Lap, Session, SessionSummary } from '../api/types'
import { HttpError } from '../api/types'
import { getTrackName } from '../constants/tracks'
import type { ErsPoint, PacePoint, PedalTracePoint, SpeedTracePoint, TyreWearPoint } from '../charts/types'
import { ErsChart } from '../charts/ers-chart'
import { PaceChart } from '../charts/pace-chart'
import { SpeedChart } from '../charts/speed-chart'
import { ThrottleBrakeChart } from '../charts/throttle-brake-chart'
import { TyreWearChart } from '../charts/tyre-wear-chart'

type LoadStatus = 'idle' | 'loading' | 'loaded' | 'error'

export function SessionDetailPage() {
  const { sessionUid } = useParams<{ sessionUid: string }>()
  const [session, setSession] = useState<Session | null>(null)
  const [laps, setLaps] = useState<Lap[]>([])
  const [summary, setSummary] = useState<SessionSummary | null>(null)
  const [status, setStatus] = useState<LoadStatus>('idle')
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const isCancelledRef = useRef(false)
  /** Track laps length so we can auto-switch pedal trace to the newly completed lap. */
  const prevLapsLengthRef = useRef(0)
  const [pacePoints, setPacePoints] = useState<PacePoint[]>([])
  const [tyreWearPoints, setTyreWearPoints] = useState<TyreWearPoint[]>([])
  const [selectedLapForTrace, setSelectedLapForTrace] = useState<number | null>(null)
  const [tracePoints, setTracePoints] = useState<PedalTracePoint[] | null>(null)
  const [traceStatus, setTraceStatus] = useState<'idle' | 'loading' | 'loaded' | 'error'>('idle')
  const [traceError, setTraceError] = useState<string | null>(null)
  const [ersPoints, setErsPoints] = useState<ErsPoint[]>([])
  const [ersStatus, setErsStatus] = useState<'idle' | 'loading' | 'loaded' | 'error'>('idle')
  const [speedTracePoints, setSpeedTracePoints] = useState<SpeedTracePoint[]>([])
  const [speedTraceStatus, setSpeedTraceStatus] = useState<'idle' | 'loading' | 'loaded' | 'error'>('idle')

  const loadTrace = useCallback(
    async (currentSessionUid: string, lapNumber: number, carIndex = 0) => {
      setTraceStatus('loading')
      setTraceError(null)
      try {
        const points = await getLapTrace(currentSessionUid, lapNumber, carIndex)
        if (isCancelledRef.current) return
        setTracePoints(points)
        setTraceStatus('loaded')
      } catch (error) {
        if (isCancelledRef.current) return
        const msg = error instanceof Error ? error.message : 'Failed to load pedal trace'
        setTraceStatus('error')
        setTraceError(msg)
        toast.error(msg)
      }
    },
    [],
  )

  const loadErs = useCallback(
    async (currentSessionUid: string, lapNumber: number, carIndex = 0) => {
      setErsStatus('loading')
      try {
        const points = await getLapErs(currentSessionUid, lapNumber, carIndex)
        if (isCancelledRef.current) return
        setErsPoints(points)
        setErsStatus('loaded')
      } catch (error) {
        if (isCancelledRef.current) return
        const msg = error instanceof Error ? error.message : 'Failed to load ERS'
        toast.error(msg)
        setErsStatus('error')
        setErsPoints([])
      }
    },
    [],
  )

  const loadSpeedTrace = useCallback(
    async (currentSessionUid: string, lapNumber: number, carIndex = 0) => {
      setSpeedTraceStatus('loading')
      try {
        const points = await getLapSpeedTrace(currentSessionUid, lapNumber, carIndex)
        if (isCancelledRef.current) return
        setSpeedTracePoints(points)
        setSpeedTraceStatus('loaded')
      } catch (error) {
        if (isCancelledRef.current) return
        const msg = error instanceof Error ? error.message : 'Failed to load speed trace'
        toast.error(msg)
        setSpeedTraceStatus('error')
        setSpeedTracePoints([])
      }
    },
    [],
  )

  /** Full load: shows loading state, used on mount and retry. */
  const load = useCallback(async () => {
    if (sessionUid == null) {
      setStatus('error')
      setErrorMessage('Session ID is missing in URL')
      return
    }
    if (!isValidSessionId(sessionUid)) {
      setStatus('error')
      setErrorMessage(
        'Invalid session ID. Use a session link from the Sessions list.',
      )
      return
    }

    setStatus('loading')
    setErrorMessage(null)

    try {
      const id = sessionUid
      const sessionRes = await getSession(id)
      if (isCancelledRef.current) return
      const carIndex = sessionRes.playerCarIndex ?? 0

      const [lapsRes, summaryRes] = await Promise.all([
        getSessionLaps(id, carIndex),
        getSessionSummary(id, carIndex),
      ])

      if (isCancelledRef.current) return

      setSession(sessionRes)
      setLaps(lapsRes)
      setSummary(summaryRes)

      const initialSummary = (summaryRes.totalLaps != null && summaryRes.totalLaps > 0) || summaryRes.bestLapTimeMs != null
        ? summaryRes
        : deriveSummaryFromLaps(lapsRes)
      const initialLapForTrace = initialSummary.bestLapNumber ?? (lapsRes[0]?.lapNumber ?? null)
      if (initialLapForTrace != null) {
        setSelectedLapForTrace(initialLapForTrace)
        void loadTrace(id, initialLapForTrace, carIndex)
        void loadErs(id, initialLapForTrace, carIndex)
        void loadSpeedTrace(id, initialLapForTrace, carIndex)
      }
      prevLapsLengthRef.current = lapsRes.length

      // Pace and tyre wear APIs are optional: load separately so failures do not block core session details.
      void (async () => {
        try {
          const paceRes = await getSessionPace(id, carIndex)
          if (isCancelledRef.current) return
          setPacePoints(paceRes)
        } catch {
          // ignore
        }
      })()
      void (async () => {
        try {
          const tyreWearRes = await getSessionTyreWear(id, carIndex)
          if (isCancelledRef.current) return
          setTyreWearPoints(tyreWearRes)
        } catch {
          // ignore
        }
      })()

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
  }, [sessionUid, loadTrace, loadSpeedTrace])

  /** Background refresh: updates data without loading state or re-mount feel. */
  const refreshInBackground = useCallback(async () => {
    if (sessionUid == null || !isValidSessionId(sessionUid)) return

    const id = sessionUid
    try {
      const sessionRes = await getSession(id)
      if (isCancelledRef.current) return
      const carIndex = sessionRes.playerCarIndex ?? 0

      const [lapsRes, summaryRes] = await Promise.all([
        getSessionLaps(id, carIndex),
        getSessionSummary(id, carIndex),
      ])

      if (isCancelledRef.current) return

      setSession(sessionRes)
      setLaps(lapsRes)
      setSummary(summaryRes)

      // When a new lap appears, auto-switch pedal trace to that lap (avoid setState in effect).
      // Do not auto-switch on first data load: ref starts at 0, so existing laps would be
      // treated as "new lap" and override the initial best-lap selection.
      const currentLength = lapsRes.length
      if (prevLapsLengthRef.current === 0) {
        prevLapsLengthRef.current = currentLength
      } else if (currentLength > prevLapsLengthRef.current && currentLength > 0) {
        const lastLap = lapsRes[currentLength - 1]
        const newLapNumber = lastLap?.lapNumber
        if (newLapNumber != null) {
          prevLapsLengthRef.current = currentLength
          setSelectedLapForTrace(newLapNumber)
          void loadTrace(id, newLapNumber, carIndex)
          void loadErs(id, newLapNumber, carIndex)
          void loadSpeedTrace(id, newLapNumber, carIndex)
        }
      } else {
        prevLapsLengthRef.current = currentLength
      }

      try {
        const paceRes = await getSessionPace(id, carIndex)
        if (!isCancelledRef.current) setPacePoints(paceRes)
      } catch {
        // ignore
      }
      try {
        const tyreWearRes = await getSessionTyreWear(id, carIndex)
        if (!isCancelledRef.current) setTyreWearPoints(tyreWearRes)
      } catch {
        // ignore
      }
    } catch {
      // Keep current data on refresh failure; do not show error or loading.
    }
  }, [sessionUid, loadTrace])

  /** Quiet refresh of pedal trace, ERS and speed trace for current lap (no loading state). */
  const refreshTraceInBackground = useCallback(
    async (currentSessionUid: string, lapNumber: number, carIndex = 0) => {
      const [traceResult, ersResult, speedResult] = await Promise.allSettled([
        getLapTrace(currentSessionUid, lapNumber, carIndex),
        getLapErs(currentSessionUid, lapNumber, carIndex),
        getLapSpeedTrace(currentSessionUid, lapNumber, carIndex),
      ])
      if (isCancelledRef.current) return
      if (traceResult.status === 'fulfilled') {
        setTracePoints(traceResult.value)
      }
      if (ersResult.status === 'fulfilled') {
        setErsPoints(ersResult.value)
      }
      if (speedResult.status === 'fulfilled') {
        setSpeedTracePoints(speedResult.value)
      }
    },
    [],
  )

  useEffect(() => {
    isCancelledRef.current = false
    // eslint-disable-next-line react-hooks/set-state-in-effect
    void load()

    return () => {
      isCancelledRef.current = true
    }
  }, [load])

  // Background refresh while session is active: update data without loading state or full reload
  const isActiveSession = session?.state === 'ACTIVE'
  const playerCarIndex = session?.playerCarIndex ?? 0
  useEffect(() => {
    if (!isActiveSession || status !== 'loaded') return
    const intervalMs = 5000
    const t = setInterval(() => {
      void refreshInBackground()
      if (sessionUid != null && selectedLapForTrace != null) {
        void refreshTraceInBackground(sessionUid, selectedLapForTrace, playerCarIndex)
      }
    }, intervalMs)
    return () => clearInterval(t)
  }, [isActiveSession, status, refreshInBackground, refreshTraceInBackground, sessionUid, selectedLapForTrace, playerCarIndex])

  // Always derive best lap and best sectors from current laps so table and summary show real best per column
  const effectiveSummary = useMemo((): SessionSummary | null => {
    if (!summary) return null
    if (laps.length > 0) {
      const derived = deriveSummaryFromLaps(laps)
      return { ...summary, ...derived }
    }
    return summary
  }, [summary, laps])

  const bestLapNumber = effectiveSummary?.bestLapNumber ?? null

  const bestSectors = useMemo(() => {
    if (!effectiveSummary) {
      return { s1: null as number | null, s2: null as number | null, s3: null as number | null }
    }
    return {
      s1: effectiveSummary.bestSector1Ms,
      s2: effectiveSummary.bestSector2Ms,
      s3: effectiveSummary.bestSector3Ms,
    }
  }, [effectiveSummary])

  const bestSectorLapNumbers = useMemo(() => {
    if (!effectiveSummary || laps.length === 0) {
      return { s1: null as number | null, s2: null as number | null, s3: null as number | null }
    }

    function findLapNumberForSector(targetMs: number | null, pick: (lap: Lap) => number | null): number | null {
      if (targetMs == null) return null
      const lap = laps.find(currentLap => !currentLap.isInvalid && pick(currentLap) === targetMs)
      if (!lap) return null
      return lap.lapNumber
    }

    return {
      s1: findLapNumberForSector(effectiveSummary.bestSector1Ms, lap => lap.sector1Ms),
      s2: findLapNumberForSector(effectiveSummary.bestSector2Ms, lap => lap.sector2Ms),
      s3: findLapNumberForSector(effectiveSummary.bestSector3Ms, lap => lap.sector3Ms),
    }
  }, [laps, effectiveSummary])

  /** Median lap time (ms) of valid laps for pace chart reference line. */
  const medianLapTimeMs = useMemo((): number | null => {
    const validTimes = laps
      .filter(l => !l.isInvalid && l.lapTimeMs != null && l.lapTimeMs > 0)
      .map(l => l.lapTimeMs as number)
    if (validTimes.length === 0) return null
    const sorted = [...validTimes].sort((a, b) => a - b)
    const mid = Math.floor(sorted.length / 2)
    return sorted.length % 2 === 1 ? sorted[mid]! : (sorted[mid - 1]! + sorted[mid]!) / 2
  }, [laps])

  const titleIdPart =
    sessionUid != null && sessionUid.length > 0 ? `${sessionUid.slice(0, 12)}…` : '—'
  const pageTitle = session?.sessionDisplayName?.trim() || titleIdPart

  return (
    <div>
      <h1 className="heading-page">Session {pageTitle}</h1>

      {session && (
        <p className="text-muted" style={{ marginBottom: 'var(--space-4)' }}>
          {session.sessionType ?? '—'} · {session.trackDisplayName ?? getTrackName(session.trackId)}{' '}
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

      {status === 'loaded' && session && effectiveSummary && (
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
                  effectiveSummary.bestLapTimeMs != null
                    ? formatLapTime(effectiveSummary.bestLapTimeMs)
                    : undefined
                }
                extra={
                  effectiveSummary.bestLapNumber != null
                    ? `Lap ${effectiveSummary.bestLapNumber}`
                    : undefined
                }
              />
              <SummaryItem
                label="Total laps"
                value={effectiveSummary.totalLaps != null ? String(effectiveSummary.totalLaps) : undefined}
              />
              {effectiveSummary.leaderPosition != null && (
                <SummaryItem
                  label="Leader"
                  value={
                    effectiveSummary.leaderIsPlayer
                      ? 'You'
                      : effectiveSummary.leaderDriverName ?? effectiveSummary.leaderTeamName
                        ? [effectiveSummary.leaderDriverName, effectiveSummary.leaderTeamName].filter(Boolean).join(' · ')
                        : effectiveSummary.leaderCarIndex != null
                          ? `Car #${effectiveSummary.leaderCarIndex}`
                          : 'P1'
                  }
                  extra={effectiveSummary.leaderIsPlayer ? undefined : 'P1'}
                  highlight={effectiveSummary.leaderIsPlayer ?? undefined}
                />
              )}
              <SummaryItem
                label="Best S1"
                value={
                  effectiveSummary.bestSector1Ms != null
                    ? formatSectorTime(effectiveSummary.bestSector1Ms)
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
                  effectiveSummary.bestSector2Ms != null
                    ? formatSectorTime(effectiveSummary.bestSector2Ms)
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
                  effectiveSummary.bestSector3Ms != null
                    ? formatSectorTime(effectiveSummary.bestSector3Ms)
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
            <PaceChart points={pacePoints} medianTimeMs={medianLapTimeMs} />
          </div>

          <div className="card" style={{ marginBottom: 'var(--space-4)', padding: 'var(--space-4)' }}>
            <h2 id="tyre-wear" style={{ fontSize: 'var(--text-lg)', marginBottom: 'var(--space-1)' }}>
              Tyre wear
            </h2>
            <p className="text-muted" style={{ fontSize: 'var(--text-sm)', marginBottom: 'var(--space-3)' }}>
              Якість гуми по колах (FL, FR, RL, RR). Увімкніть car damage у налаштуваннях F1 25 для запису.
            </p>
            <TyreWearChart points={tyreWearPoints} />
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
                        const car = session?.playerCarIndex ?? 0
                        void loadTrace(sessionUid, value, car)
                        void loadErs(sessionUid, value, car)
                        void loadSpeedTrace(sessionUid, value, car)
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
                    onClick={() => {
                      const car = session?.playerCarIndex ?? 0
                      void loadTrace(sessionUid, selectedLapForTrace, car)
                      void loadErs(sessionUid, selectedLapForTrace, car)
                      void loadSpeedTrace(sessionUid, selectedLapForTrace, car)
                    }}
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

          {/* Speed vs distance */}
          <div className="card" style={{ marginBottom: 'var(--space-4)', padding: 'var(--space-4)' }}>
            <h2 style={{ fontSize: 'var(--text-lg)', marginBottom: 'var(--space-1)' }}>Speed vs distance</h2>
            <p className="text-muted" style={{ fontSize: 'var(--text-sm)', marginBottom: 'var(--space-3)' }}>
              Speed (km/h) along the lap. Same lap as pedal trace above.
            </p>
            {speedTraceStatus === 'loading' && <p className="text-muted">Loading speed trace…</p>}
            {speedTraceStatus === 'error' && (
              <p className="text-error">Failed to load speed trace for this lap.</p>
            )}
            {(speedTraceStatus === 'loaded' || speedTraceStatus === 'idle') && (
              <SpeedChart points={speedTracePoints} />
            )}
          </div>

          {/* ERS (Energy Recovery System) */}
          <div className="card" style={{ marginBottom: 'var(--space-4)', padding: 'var(--space-4)' }}>
            <h2 style={{ fontSize: 'var(--text-lg)', marginBottom: 'var(--space-1)' }}>ERS</h2>
            <p className="text-muted" style={{ fontSize: 'var(--text-sm)', marginBottom: 'var(--space-3)' }}>
              Stored energy along the lap (merged from telemetry and car status). Same lap as pedal trace above.
            </p>
            {ersStatus === 'loading' && <p className="text-muted">Loading ERS…</p>}
            {ersStatus === 'error' && (
              <p className="text-error">Failed to load ERS for this lap.</p>
            )}
            {(ersStatus === 'loaded' || ersStatus === 'idle') && ersPoints.length > 0 && (
              <ErsChart points={ersPoints} />
            )}
            {(ersStatus === 'loaded' || ersStatus === 'idle') && ersPoints.length === 0 && selectedLapForTrace != null && (
              <p className="text-muted">No ERS data for lap {selectedLapForTrace}.</p>
            )}
          </div>

          {/* Laps table */}
          <div className="card card-table">
            <table className="table">
              <thead>
                <tr>
                  <th style={{ textAlign: 'left' }}>Lap</th>
                  <th>Time</th>
                  <th>Delta</th>
                  <th>Position</th>
                  <th>S1</th>
                  <th>S2</th>
                  <th>S3</th>
                  <th>Valid</th>
                </tr>
              </thead>
              <tbody>
                {laps.map((lap, index) => {
                  const isBestLap = bestLapNumber != null && lap.lapNumber === bestLapNumber
                  const isBestS1 =
                    bestSectors.s1 != null && lap.sector1Ms === bestSectors.s1 && !lap.isInvalid
                  const isBestS2 =
                    bestSectors.s2 != null && lap.sector2Ms === bestSectors.s2 && !lap.isInvalid
                  const isBestS3 =
                    bestSectors.s3 != null && lap.sector3Ms === bestSectors.s3 && !lap.isInvalid
                  const bestLapTimeMs = effectiveSummary?.bestLapTimeMs ?? null
                  const deltaMs =
                    lap.lapTimeMs != null && bestLapTimeMs != null && !lap.isInvalid
                      ? lap.lapTimeMs - bestLapTimeMs
                      : null
                  const prevLap = index > 0 ? laps[index - 1] : null
                  const position = lap.positionAtLapStart ?? null
                  const prevPosition = prevLap?.positionAtLapStart ?? null
                  const positionChange =
                    position != null && prevPosition != null
                      ? position - prevPosition
                      : null

                  return (
                    <tr
                      key={lap.lapNumber}
                      style={{
                        backgroundColor: isBestLap ? 'rgba(34, 197, 94, 0.12)' : 'transparent',
                      }}
                    >
                      <td
                        style={{
                          textAlign: 'left',
                          fontWeight: isBestLap ? 'var(--font-weight-medium)' : undefined,
                          color: isBestLap ? 'var(--success)' : undefined,
                        }}
                      >
                        {lap.lapNumber}
                      </td>
                      <td>
                        <span
                          style={{
                            fontFamily: 'var(--font-mono)',
                            color: isBestLap ? 'var(--success)' : undefined,
                            fontWeight: isBestLap ? 'var(--font-weight-medium)' : undefined,
                          }}
                        >
                          {lap.lapTimeMs != null ? formatLapTime(lap.lapTimeMs) : '—'}
                        </span>
                        {isBestLap && (
                          <span
                            style={{
                              marginLeft: 'var(--space-1)',
                              fontSize: 'var(--text-xs)',
                              color: 'var(--success)',
                              fontWeight: 'var(--font-weight-medium)',
                            }}
                          >
                            BEST
                          </span>
                        )}
                      </td>
                      <td style={{ fontFamily: 'var(--font-mono)', fontSize: 'var(--text-sm)' }}>
                        {deltaMs != null ? (
                          <span style={{ color: deltaMs <= 0 ? 'var(--success)' : 'var(--text-secondary)' }}>
                            {deltaMs >= 0 ? '+' : ''}{(deltaMs / 1000).toFixed(3)}
                          </span>
                        ) : (
                          '—'
                        )}
                      </td>
                      <td>
                        {position != null ? (
                          <span style={{ display: 'inline-flex', alignItems: 'center', gap: 4 }}>
                            {positionChange !== null && positionChange !== 0 && (
                              <span
                                style={{
                                  color: positionChange < 0 ? 'var(--success)' : 'var(--error)',
                                  fontSize: 'var(--text-sm)',
                                }}
                                title={positionChange < 0 ? 'Gained position' : 'Lost position'}
                              >
                                {positionChange < 0 ? '↑' : '↓'}
                              </span>
                            )}
                            {position}
                          </span>
                        ) : (
                          '—'
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
  highlight?: boolean
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
          color: props.highlight ? 'var(--success)' : 'var(--text-primary)',
          fontFamily: 'var(--font-mono)',
          fontWeight: props.highlight ? 'var(--font-weight-medium)' : undefined,
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
        color: props.isBest ? 'var(--success)' : 'inherit',
        fontWeight: props.isBest ? 'var(--font-weight-medium)' : undefined,
      }}
    >
      {formatSectorTime(props.value)}
      {props.isBest && (
        <span
          style={{
            marginLeft: 'var(--space-1)',
            fontSize: 'var(--text-xs)',
            color: 'var(--success)',
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

/** Derive session summary from laps when API summary is missing or empty. */
function deriveSummaryFromLaps(laps: Lap[]): SessionSummary {
  const validLaps = laps.filter(lap => !lap.isInvalid)
  let bestLapTimeMs: number | null = null
  let bestLapNumber: number | null = null
  let bestSector1Ms: number | null = null
  let bestSector2Ms: number | null = null
  let bestSector3Ms: number | null = null

  for (const lap of validLaps) {
    if (lap.lapTimeMs != null && lap.lapTimeMs > 0) {
      if (bestLapTimeMs == null || lap.lapTimeMs < bestLapTimeMs) {
        bestLapTimeMs = lap.lapTimeMs
        bestLapNumber = lap.lapNumber
      }
    }
    if (lap.sector1Ms != null && (bestSector1Ms == null || lap.sector1Ms < bestSector1Ms)) {
      bestSector1Ms = lap.sector1Ms
    }
    if (lap.sector2Ms != null && (bestSector2Ms == null || lap.sector2Ms < bestSector2Ms)) {
      bestSector2Ms = lap.sector2Ms
    }
    if (lap.sector3Ms != null && (bestSector3Ms == null || lap.sector3Ms < bestSector3Ms)) {
      bestSector3Ms = lap.sector3Ms
    }
  }

  return {
    totalLaps: laps.length,
    bestLapTimeMs,
    bestLapNumber,
    bestSector1Ms,
    bestSector2Ms,
    bestSector3Ms,
  }
}

