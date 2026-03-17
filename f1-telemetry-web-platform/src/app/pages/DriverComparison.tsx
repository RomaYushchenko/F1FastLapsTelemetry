import { useCallback, useEffect, useMemo, useState } from "react"
import { DataCard } from "../components/DataCard"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "../components/ui/select"
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Legend,
  BarChart,
  Bar,
  AreaChart,
  Area,
  ReferenceLine,
} from "recharts"
import { getSessions, getSession, getComparison } from "@/api/client"
import { formatLapTime } from "@/api/format"
import type {
  Session,
  SessionParticipantDto,
  ComparisonResponseDto,
  SpeedTracePoint,
} from "@/api/types"
import { notify } from "@/notify"
import { Skeleton } from "../components/ui/skeleton"

const DRIVER_A_COLOR = "#00E5FF"
const DRIVER_B_COLOR = "#E10600"
const DELTA_COLOR = "#00FF85"

/** Build common distance grid from two speed traces and compute time delta (Driver A − Driver B) along distance. */
function computeTimeDeltaFromSpeedTraces(
  speedA: SpeedTracePoint[],
  speedB: SpeedTracePoint[]
): { distance: number; delta: number }[] {
  if (speedA.length === 0 || speedB.length === 0) return []

  const minD = Math.min(
    speedA[0]?.distanceM ?? 0,
    speedB[0]?.distanceM ?? 0
  )
  const maxD = Math.max(
    speedA[speedA.length - 1]?.distanceM ?? 0,
    speedB[speedB.length - 1]?.distanceM ?? 0
  )
  const step = Math.max(5, (maxD - minD) / 200)
  const grid: number[] = []
  for (let d = minD; d <= maxD; d += step) grid.push(d)
  if (grid[grid.length - 1] !== maxD) grid.push(maxD)

  function cumulativeTimeAtDistance(
    points: SpeedTracePoint[],
    distance: number
  ): number {
    let time = 0
    let prevD = points[0]?.distanceM ?? 0
    let prevV = (points[0]?.speedKph ?? 0) / 3.6
    for (let i = 1; i < points.length; i++) {
      const nextD = points[i]?.distanceM ?? prevD
      const nextV = (points[i]?.speedKph ?? 0) / 3.6
      if (nextD >= distance) {
        const v = prevV > 0 ? prevV : 1
        time += ((distance - prevD) / v) * 1000
        return time / 1000
      }
      if (nextV > 0) time += ((nextD - prevD) / nextV) * 1000
      prevD = nextD
      prevV = nextV
    }
    if (prevV > 0) time += ((distance - prevD) / prevV) * 1000
    return time / 1000
  }

  return grid.map((distance) => {
    const tA = cumulativeTimeAtDistance(speedA, distance)
    const tB = cumulativeTimeAtDistance(speedB, distance)
    return { distance, delta: tA - tB }
  })
}

export default function DriverComparison() {
  const [sessions, setSessions] = useState<Session[]>([])
  const [sessionsLoading, setSessionsLoading] = useState(true)
  const [sessionsError, setSessionsError] = useState<string | null>(null)

  const [sessionUid, setSessionUid] = useState<string>("")
  const [sessionDetail, setSessionDetail] = useState<Session | null>(null)
  const [sessionDetailLoading, setSessionDetailLoading] = useState(false)

  const [carIndexA, setCarIndexA] = useState<number | "">("")
  const [carIndexB, setCarIndexB] = useState<number | "">("")

  const [referenceLapNumA, setReferenceLapNumA] = useState<number | "">("")
  const [referenceLapNumB, setReferenceLapNumB] = useState<number | "">("")

  const [comparison, setComparison] = useState<ComparisonResponseDto | null>(null)
  const [comparisonLoading, setComparisonLoading] = useState(false)
  const [comparisonError, setComparisonError] = useState<string | null>(null)

  const [showSelectionPanel, setShowSelectionPanel] = useState(false)

  useEffect(() => {
    let cancelled = false
    setSessionsLoading(true)
    setSessionsError(null)
    getSessions({ limit: 100, offset: 0, sort: "startedAt_desc" })
      .then((res) => {
        if (!cancelled) {
          setSessions(res.sessions)
          if (res.sessions.length > 0 && !sessionUid) {
            setSessionUid(res.sessions[0].id ?? "")
          }
        }
      })
      .catch((err) => {
        if (!cancelled) {
          setSessionsError(err?.message ?? "Failed to load sessions")
          notify.error(err?.message ?? "Failed to load sessions")
        }
      })
      .finally(() => {
        if (!cancelled) setSessionsLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [])

  useEffect(() => {
    if (!sessionUid) {
      setSessionDetail(null)
      setCarIndexA("")
      setCarIndexB("")
      setReferenceLapNumA("")
      setReferenceLapNumB("")
      setComparison(null)
      return
    }
    setCarIndexA("")
    setCarIndexB("")
    setReferenceLapNumA("")
    setReferenceLapNumB("")
    setComparison(null)
    let cancelled = false
    setSessionDetailLoading(true)
    getSession(sessionUid)
      .then((s) => {
        if (!cancelled) {
          setSessionDetail(s)
          const participants = s.participants ?? []
          if (participants.length >= 2) {
            const a = participants[0].carIndex
            const b = participants[1].carIndex
            setCarIndexA(a)
            setCarIndexB(b)
          } else if (participants.length === 1) {
            setCarIndexA(participants[0].carIndex)
            setCarIndexB("")
          } else {
            setCarIndexA("")
            setCarIndexB("")
          }
          setReferenceLapNumA("")
          setReferenceLapNumB("")
          setComparison(null)
        }
      })
      .catch((err) => {
        if (!cancelled) {
          setSessionDetail(null)
          setCarIndexA("")
          setCarIndexB("")
          notify.error(err?.message ?? "Failed to load session")
        }
      })
      .finally(() => {
        if (!cancelled) setSessionDetailLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [sessionUid])

  const fetchComparison = useCallback(() => {
    if (
      !sessionUid ||
      carIndexA === "" ||
      carIndexB === "" ||
      carIndexA === carIndexB
    ) {
      setComparison(null)
      return
    }
    setComparisonError(null)
    setComparisonLoading(true)
    getComparison(
      sessionUid,
      Number(carIndexA),
      Number(carIndexB),
      referenceLapNumA === "" ? undefined : Number(referenceLapNumA),
      referenceLapNumB === "" ? undefined : Number(referenceLapNumB)
    )
      .then((data) => setComparison(data))
      .catch((err) => {
        setComparison(null)
        const msg = err?.message ?? "Failed to load comparison"
        setComparisonError(msg)
        notify.error(msg)
      })
      .finally(() => {
        setComparisonLoading(false)
      })
  }, [
    sessionUid,
    carIndexA,
    carIndexB,
    referenceLapNumA,
    referenceLapNumB,
  ])

  useEffect(() => {
    fetchComparison()
  }, [fetchComparison])

  const participants: SessionParticipantDto[] =
    sessionDetail?.participants ?? []
  const labelA =
    participants.find((p) => p.carIndex === carIndexA)?.displayLabel ??
    `Car ${carIndexA}`
  const labelB =
    participants.find((p) => p.carIndex === carIndexB)?.displayLabel ??
    `Car ${carIndexB}`

  const lapTimeChartData = useMemo(() => {
    if (!comparison) return []
    const byLap = new Map<number, { lap: number; driverA: number | null; driverB: number | null }>()
    for (const lap of comparison.lapsA) {
      if (lap.lapTimeMs != null && !lap.isInvalid) {
        byLap.set(lap.lapNumber, {
          lap: lap.lapNumber,
          driverA: lap.lapTimeMs / 1000,
          driverB: null,
        })
      }
    }
    for (const lap of comparison.lapsB) {
      const row = byLap.get(lap.lapNumber) ?? {
        lap: lap.lapNumber,
        driverA: null,
        driverB: null,
      }
      if (lap.lapTimeMs != null && !lap.isInvalid) row.driverB = lap.lapTimeMs / 1000
      byLap.set(lap.lapNumber, row)
    }
    return Array.from(byLap.values()).sort((a, b) => a.lap - b.lap)
  }, [comparison])

  const sectorData = useMemo(() => {
    if (!comparison) return []
    const s1A = (comparison.summaryA.bestSector1Ms ?? 0) / 1000
    const s2A = (comparison.summaryA.bestSector2Ms ?? 0) / 1000
    const s3A = (comparison.summaryA.bestSector3Ms ?? 0) / 1000
    const s1B = (comparison.summaryB.bestSector1Ms ?? 0) / 1000
    const s2B = (comparison.summaryB.bestSector2Ms ?? 0) / 1000
    const s3B = (comparison.summaryB.bestSector3Ms ?? 0) / 1000
    return [
      { sector: "Sector 1", driverA: s1A, driverB: s1B },
      { sector: "Sector 2", driverA: s2A, driverB: s2B },
      { sector: "Sector 3", driverA: s3A, driverB: s3B },
    ]
  }, [comparison])

  const speedOverlayData = useMemo(() => {
    if (!comparison?.speedTraceA?.length || !comparison?.speedTraceB?.length)
      return []
    const byDist = new Map<number, { distance: number; driverA: number; driverB: number }>()
    for (const p of comparison.speedTraceA) {
      const d = p.distanceM ?? 0
      byDist.set(d, { distance: d, driverA: p.speedKph ?? 0, driverB: 0 })
    }
    for (const p of comparison.speedTraceB) {
      const d = p.distanceM ?? 0
      const row = byDist.get(d) ?? { distance: d, driverA: 0, driverB: 0 }
      row.driverB = p.speedKph ?? 0
      byDist.set(d, row)
    }
    return Array.from(byDist.values())
      .filter((r) => r.driverA > 0 || r.driverB > 0)
      .sort((a, b) => a.distance - b.distance)
  }, [comparison])

  const throttleOverlayData = useMemo(() => {
    if (!comparison?.traceA?.length || !comparison?.traceB?.length) return []
    const byDist = new Map<number, { distance: number; driverA: number; driverB: number }>()
    for (const p of comparison.traceA) {
      const d = p.distance ?? 0
      byDist.set(d, {
        distance: d,
        driverA: (p.throttle ?? 0) * 100,
        driverB: 0,
      })
    }
    for (const p of comparison.traceB) {
      const d = p.distance ?? 0
      const row = byDist.get(d) ?? { distance: d, driverA: 0, driverB: 0 }
      row.driverB = (p.throttle ?? 0) * 100
      byDist.set(d, row)
    }
    return Array.from(byDist.values()).sort((a, b) => a.distance - b.distance)
  }, [comparison])

  const brakeOverlayData = useMemo(() => {
    if (!comparison?.traceA?.length || !comparison?.traceB?.length) return []
    const byDist = new Map<number, { distance: number; driverA: number; driverB: number }>()
    for (const p of comparison.traceA) {
      const d = p.distance ?? 0
      byDist.set(d, {
        distance: d,
        driverA: (p.brake ?? 0) * 100,
        driverB: 0,
      })
    }
    for (const p of comparison.traceB) {
      const d = p.distance ?? 0
      const row = byDist.get(d) ?? { distance: d, driverA: 0, driverB: 0 }
      row.driverB = (p.brake ?? 0) * 100
      byDist.set(d, row)
    }
    return Array.from(byDist.values()).sort((a, b) => a.distance - b.distance)
  }, [comparison])

  const timeDeltaData = useMemo(() => {
    if (!comparison?.speedTraceA?.length || !comparison?.speedTraceB?.length)
      return []
    return computeTimeDeltaFromSpeedTraces(
      comparison.speedTraceA,
      comparison.speedTraceB
    )
  }, [comparison])

  const lapOptionsA = useMemo(() => {
    if (!comparison?.lapsA) return []
    return comparison.lapsA
      .filter((l) => l.lapTimeMs != null && !l.isInvalid)
      .map((l) => ({ value: l.lapNumber, label: `Lap ${l.lapNumber}` }))
  }, [comparison])
  const lapOptionsB = useMemo(() => {
    if (!comparison?.lapsB) return []
    return comparison.lapsB
      .filter((l) => l.lapTimeMs != null && !l.isInvalid)
      .map((l) => ({ value: l.lapNumber, label: `Lap ${l.lapNumber}` }))
  }, [comparison])

  const sessionOptions = useMemo(
    () =>
      sessions.map((s) => ({
        value: s.id ?? "",
        label:
          s.sessionDisplayName ||
          s.sessionType ||
          s.id ||
          "Session",
      })),
    [sessions]
  )

  const canCompare =
    sessionUid &&
    carIndexA !== "" &&
    carIndexB !== "" &&
    carIndexA !== carIndexB

  const selectedSessionData = useMemo(
    () => sessions.find((s) => s.id === sessionUid) ?? null,
    [sessions, sessionUid]
  )

  const overallDeltaSeconds =
    comparison && comparison.summaryA.bestLapTimeMs != null && comparison.summaryB.bestLapTimeMs != null
      ? (comparison.summaryA.bestLapTimeMs - comparison.summaryB.bestLapTimeMs) / 1000
      : null

  const topSpeedA =
    comparison?.speedTraceA && comparison.speedTraceA.length > 0
      ? Math.max(...comparison.speedTraceA.map((p) => p.speedKph ?? 0))
      : null
  const topSpeedB =
    comparison?.speedTraceB && comparison.speedTraceB.length > 0
      ? Math.max(...comparison.speedTraceB.map((p) => p.speedKph ?? 0))
      : null

  const avgSpeedA =
    comparison?.speedTraceA && comparison.speedTraceA.length > 0
      ? comparison.speedTraceA.reduce((sum, p) => sum + (p.speedKph ?? 0), 0) /
        comparison.speedTraceA.length
      : null
  const avgSpeedB =
    comparison?.speedTraceB && comparison.speedTraceB.length > 0
      ? comparison.speedTraceB.reduce((sum, p) => sum + (p.speedKph ?? 0), 0) /
        comparison.speedTraceB.length
      : null

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold mb-2">LAP TIME ANALYSIS</h1>
        <p className="text-text-secondary">
          Detailed telemetry comparison • Turn by turn analysis
        </p>
      </div>

      {/* Comparison Settings Panel */}
      <div className="bg-gradient-to-br from-card via-card to-secondary/20 border border-border rounded-xl overflow-hidden">
        {!showSelectionPanel && (
          <button
            type="button"
            onClick={() => setShowSelectionPanel(true)}
            className="w-full p-4 flex items-center justify-between cursor-pointer hover:bg-secondary/20 transition-colors text-left"
          >
            <div className="flex items-center gap-6">
              <div className="flex items-center gap-2">
                <span className="text-sm font-bold text-text-secondary uppercase">
                  Comparison Settings
                </span>
              </div>
              <div className="flex items-center gap-4">
                <div className="flex items-center gap-2">
                  <span className="text-xs text-text-secondary">
                    Session:
                  </span>
                  <span className="text-sm font-bold">
                    {selectedSessionData
                      ? selectedSessionData.sessionDisplayName ??
                        selectedSessionData.sessionType ??
                        selectedSessionData.id
                      : "Select session"}
                  </span>
                </div>
                {canCompare && (
                  <>
                    <div className="flex items-center gap-2">
                      <span
                        className="w-2 h-2 rounded-full"
                        style={{ backgroundColor: DRIVER_A_COLOR }}
                      />
                      <span className="text-sm font-bold font-mono">
                        {labelA}
                      </span>
                    </div>
                    <span className="text-xs text-text-secondary">vs</span>
                    <div className="flex items-center gap-2">
                      <span
                        className="w-2 h-2 rounded-full"
                        style={{ backgroundColor: DRIVER_B_COLOR }}
                      />
                      <span className="text-sm font-bold font-mono">
                        {labelB}
                      </span>
                    </div>
                  </>
                )}
              </div>
            </div>
          </button>
        )}

        {showSelectionPanel && (
          <div className="p-6 space-y-6">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <span className="text-sm font-bold text-text-secondary uppercase">
                  Comparison Settings
                </span>
              </div>
              <button
                type="button"
                onClick={() => setShowSelectionPanel(false)}
                className="px-3 py-1.5 rounded-lg bg-secondary/50 hover:bg-secondary/70 transition-colors text-sm"
              >
                Hide
              </button>
            </div>

            <div className="grid md:grid-cols-3 gap-6">
              {/* Session selector as cards */}
              <div>
                <label className="text-xs text-text-secondary uppercase mb-3 block font-bold">
                  Select Session
                </label>
                {sessionsLoading ? (
                  <Skeleton className="h-24 w-full" />
                ) : (
                  <div className="space-y-2">
                    {sessionOptions.map((session) => (
                      <button
                        key={session.value}
                        type="button"
                        onClick={() => setSessionUid(session.value)}
                        className={`w-full text-left p-3 rounded-lg border-2 cursor-pointer transition-all ${
                          sessionUid === session.value
                            ? "border-[#00E5FF] bg-[#00E5FF]/10"
                            : "border-border/50 bg-secondary/30 hover:border-border hover:bg-secondary/50"
                        }`}
                      >
                        <div className="text-sm font-bold">
                          {session.label}
                        </div>
                      </button>
                    ))}
                  </div>
                )}
              </div>

              {/* Driver A selector as cards */}
              <div>
                <label className="text-xs text-text-secondary uppercase mb-3 block font-bold">
                  Driver A
                </label>
                {sessionDetailLoading ? (
                  <Skeleton className="h-24 w-full" />
                ) : !sessionUid ? (
                  <div className="text-xs text-text-secondary">
                    Select a session to load drivers.
                  </div>
                ) : (
                  <div className="space-y-2">
                    {participants.map((p) => {
                      const disabled = p.carIndex === carIndexB
                      const selected = p.carIndex === carIndexA
                      return (
                        <button
                          key={p.carIndex}
                          type="button"
                          onClick={() =>
                            !disabled && setCarIndexA(p.carIndex)
                          }
                          className={`w-full text-left p-3 rounded-lg border-2 transition-all ${
                            selected
                              ? "border-[#00E5FF] bg-[#00E5FF]/10"
                              : disabled
                                ? "opacity-50 cursor-not-allowed border-border/30 bg-secondary/20"
                                : "border-border/50 bg-secondary/30 hover:border-border hover:bg-secondary/50"
                          }`}
                        >
                          <div className="flex items-center justify-between">
                            <div>
                              <div className="text-sm font-bold">
                                {p.displayLabel ?? `Car ${p.carIndex}`}
                              </div>
                            </div>
                            <span
                              className="w-3 h-3 rounded-full"
                              style={{ backgroundColor: DRIVER_A_COLOR }}
                            />
                          </div>
                        </button>
                      )
                    })}
                  </div>
                )}
              </div>

              {/* Driver B selector as cards */}
              <div>
                <label className="text-xs text-text-secondary uppercase mb-3 block font-bold">
                  Driver B
                </label>
                {sessionDetailLoading ? (
                  <Skeleton className="h-24 w-full" />
                ) : !sessionUid ? (
                  <div className="text-xs text-text-secondary">
                    Select a session to load drivers.
                  </div>
                ) : (
                  <div className="space-y-2">
                    {participants.map((p) => {
                      const disabled = p.carIndex === carIndexA
                      const selected = p.carIndex === carIndexB
                      return (
                        <button
                          key={p.carIndex}
                          type="button"
                          onClick={() =>
                            !disabled && setCarIndexB(p.carIndex)
                          }
                          className={`w-full text-left p-3 rounded-lg border-2 transition-all ${
                            selected
                              ? "border-[#E10600] bg-[#E10600]/10"
                              : disabled
                                ? "opacity-50 cursor-not-allowed border-border/30 bg-secondary/20"
                                : "border-border/50 bg-secondary/30 hover:border-border hover:bg-secondary/50"
                          }`}
                        >
                          <div className="flex items-center justify-between">
                            <div>
                              <div className="text-sm font-bold">
                                {p.displayLabel ?? `Car ${p.carIndex}`}
                              </div>
                            </div>
                            <span
                              className="w-3 h-3 rounded-full"
                              style={{ backgroundColor: DRIVER_B_COLOR }}
                            />
                          </div>
                        </button>
                      )
                    })}
                  </div>
                )}
              </div>
            </div>

            {canCompare && (
              <div className="grid md:grid-cols-2 gap-4">
                <div>
                  <label className="text-xs text-text-secondary uppercase mb-2 block">
                    Lap for Driver A
                  </label>
                  <Select
                    value={
                      referenceLapNumA !== ""
                        ? String(referenceLapNumA)
                        : comparison
                          ? String(comparison.referenceLapNumA)
                          : undefined
                    }
                    onValueChange={(v) =>
                      setReferenceLapNumA(v === "" ? "" : Number(v))
                    }
                  >
                    <SelectTrigger className="bg-input-background border-border">
                      <SelectValue placeholder="Best lap" />
                    </SelectTrigger>
                    <SelectContent>
                      {lapOptionsA.map((opt) => (
                        <SelectItem key={opt.value} value={String(opt.value)}>
                          {opt.label}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
                <div>
                  <label className="text-xs text-text-secondary uppercase mb-2 block">
                    Lap for Driver B
                  </label>
                  <Select
                    value={
                      referenceLapNumB !== ""
                        ? String(referenceLapNumB)
                        : comparison
                          ? String(comparison.referenceLapNumB)
                          : undefined
                    }
                    onValueChange={(v) =>
                      setReferenceLapNumB(v === "" ? "" : Number(v))
                    }
                  >
                    <SelectTrigger className="bg-input-background border-border">
                      <SelectValue placeholder="Best lap" />
                    </SelectTrigger>
                    <SelectContent>
                      {lapOptionsB.map((opt) => (
                        <SelectItem key={opt.value} value={String(opt.value)}>
                          {opt.label}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
              </div>
            )}

            {selectedSessionData && (
              <div className="pt-4 border-t border-border/50 flex items-center justify-between text-sm text-text-secondary">
                <div>
                  Analyzing:{" "}
                  <span className="font-bold text-foreground">
                    {selectedSessionData.sessionDisplayName ??
                      selectedSessionData.sessionType ??
                      selectedSessionData.id}
                  </span>
                </div>
                <div>
                  {selectedSessionData.startedAt}
                </div>
              </div>
            )}
          </div>
        )}
      </div>

      {sessionsError && (
        <div className="text-destructive text-sm">{sessionsError}</div>
      )}

      {!sessionUid && !sessionsLoading && (
        <DataCard>
          <p className="text-text-secondary text-center py-8">
            No session selected. Select a session to load participants.
          </p>
        </DataCard>
      )}

      {sessionUid && participants.length < 2 && !sessionDetailLoading && (
        <DataCard>
          <p className="text-text-secondary text-center py-8">
            This session has fewer than two participants with data. Choose
            another session.
          </p>
        </DataCard>
      )}

      {comparisonError && (
        <div className="text-destructive text-sm">{comparisonError}</div>
      )}

      {comparisonLoading && canCompare && (
        <DataCard>
          <div className="flex items-center justify-center py-12">
            <Skeleton className="h-8 w-48" />
          </div>
        </DataCard>
      )}

      {comparison && !comparisonLoading && (
        <>
          <div className="grid md:grid-cols-2 gap-6">
            {/* Driver A card */}
            <div className="relative overflow-hidden rounded-xl border border-border bg-gradient-to-br from-card via-card to-secondary/20">
              <div
                className="absolute top-0 left-0 w-1 h-full"
                style={{ backgroundColor: DRIVER_A_COLOR }}
              />
              <div className="p-6">
                <div className="flex items-start justify-between mb-4">
                  <div className="flex items-center gap-4">
                    <div
                      className="w-16 h-16 rounded-lg flex items-center justify-center text-3xl font-bold border-2"
                      style={{
                        backgroundColor: `${DRIVER_A_COLOR}20`,
                        borderColor: DRIVER_A_COLOR,
                        color: DRIVER_A_COLOR,
                      }}
                    >
                      A
                    </div>
                    <div>
                      <div className="text-sm text-text-secondary uppercase tracking-wide">
                        Driver A
                      </div>
                      <div
                        className="text-2xl font-bold"
                        style={{ color: DRIVER_A_COLOR }}
                      >
                        {labelA}
                      </div>
                    </div>
                  </div>
                </div>

                <div className="space-y-3 pt-4 border-t border-border/50">
                  <div className="flex items-end justify-between">
                    <div>
                      <div className="text-xs text-text-secondary uppercase mb-1">
                        Lap Time
                      </div>
                      <div
                        className="text-3xl font-bold font-mono"
                        style={{ color: DRIVER_A_COLOR }}
                      >
                        {formatLapTime(comparison.summaryA.bestLapTimeMs)}
                      </div>
                    </div>
                    <div className="text-right">
                      <div className="text-xs text-text-secondary uppercase mb-1">
                        Delta
                      </div>
                      <div className="text-xl font-bold font-mono text-[#00FF85]">
                        {overallDeltaSeconds != null
                          ? `${overallDeltaSeconds > 0 ? "+" : ""}${overallDeltaSeconds.toFixed(
                              3
                            )}s`
                          : "—"}
                      </div>
                    </div>
                  </div>

                  <div className="grid grid-cols-3 gap-3 pt-3">
                    <div className="bg-secondary/30 rounded-lg p-2.5">
                      <div className="text-[10px] text-text-secondary uppercase mb-1">
                        Top Speed
                      </div>
                      <div className="text-lg font-bold font-mono">
                        {topSpeedA != null ? Math.round(topSpeedA) : "—"}
                      </div>
                      <div className="text-[10px] text-text-secondary">km/h</div>
                    </div>
                    <div className="bg-secondary/30 rounded-lg p-2.5">
                      <div className="text-[10px] text-text-secondary uppercase mb-1">
                        Avg Speed
                      </div>
                      <div className="text-lg font-bold font-mono">
                        {avgSpeedA != null ? Math.round(avgSpeedA) : "—"}
                      </div>
                      <div className="text-[10px] text-text-secondary">km/h</div>
                    </div>
                    <div className="bg-secondary/30 rounded-lg p-2.5">
                      <div className="text-[10px] text-text-secondary uppercase mb-1">
                        Total Laps
                      </div>
                      <div className="text-lg font-bold font-mono">
                        {comparison.summaryA.totalLaps ?? "—"}
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            {/* Driver B card */}
            <div className="relative overflow-hidden rounded-xl border border-border bg-gradient-to-br from-card via-card to-secondary/20">
              <div
                className="absolute top-0 left-0 w-1 h-full"
                style={{ backgroundColor: DRIVER_B_COLOR }}
              />
              <div className="p-6">
                <div className="flex items-start justify-between mb-4">
                  <div className="flex items-center gap-4">
                    <div
                      className="w-16 h-16 rounded-lg flex items-center justify-center text-3xl font-bold border-2"
                      style={{
                        backgroundColor: `${DRIVER_B_COLOR}20`,
                        borderColor: DRIVER_B_COLOR,
                        color: DRIVER_B_COLOR,
                      }}
                    >
                      B
                    </div>
                    <div>
                      <div className="text-sm text-text-secondary uppercase tracking-wide">
                        Driver B
                      </div>
                      <div
                        className="text-2xl font-bold"
                        style={{ color: DRIVER_B_COLOR }}
                      >
                        {labelB}
                      </div>
                    </div>
                  </div>
                </div>

                <div className="space-y-3 pt-4 border-t border-border/50">
                  <div className="flex items-end justify-between">
                    <div>
                      <div className="text-xs text-text-secondary uppercase mb-1">
                        Lap Time
                      </div>
                      <div
                        className="text-3xl font-bold font-mono"
                        style={{ color: DRIVER_B_COLOR }}
                      >
                        {formatLapTime(comparison.summaryB.bestLapTimeMs)}
                      </div>
                    </div>
                    <div className="text-right">
                      <div className="text-xs text-text-secondary uppercase mb-1">
                        Delta
                      </div>
                      <div className="text-xl font-bold font-mono text-[#E10600]">
                        {overallDeltaSeconds != null
                          ? `${overallDeltaSeconds < 0 ? "+" : ""}${(-overallDeltaSeconds).toFixed(
                              3
                            )}s`
                          : "—"}
                      </div>
                    </div>
                  </div>

                  <div className="grid grid-cols-3 gap-3 pt-3">
                    <div className="bg-secondary/30 rounded-lg p-2.5">
                      <div className="text-[10px] text-text-secondary uppercase mb-1">
                        Top Speed
                      </div>
                      <div className="text-lg font-bold font-mono">
                        {topSpeedB != null ? Math.round(topSpeedB) : "—"}
                      </div>
                      <div className="text-[10px] text-text-secondary">km/h</div>
                    </div>
                    <div className="bg-secondary/30 rounded-lg p-2.5">
                      <div className="text-[10px] text-text-secondary uppercase mb-1">
                        Avg Speed
                      </div>
                      <div className="text-lg font-bold font-mono">
                        {avgSpeedB != null ? Math.round(avgSpeedB) : "—"}
                      </div>
                      <div className="text-[10px] text-text-secondary">km/h</div>
                    </div>
                    <div className="bg-secondary/30 rounded-lg p-2.5">
                      <div className="text-[10px] text-text-secondary uppercase mb-1">
                        Total Laps
                      </div>
                      <div className="text-lg font-bold font-mono">
                        {comparison.summaryB.totalLaps ?? "—"}
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          {lapTimeChartData.length > 0 && (
            <DataCard title="Lap Time Comparison">
              <ResponsiveContainer width="100%" height={350}>
                <LineChart data={lapTimeChartData}>
                  <CartesianGrid
                    strokeDasharray="3 3"
                    stroke="rgba(249,250,251,0.06)"
                  />
                  <XAxis
                    dataKey="lap"
                    stroke="#9CA3AF"
                    label={{
                      value: "Lap",
                      position: "insideBottom",
                      offset: -5,
                      fill: "#9CA3AF",
                    }}
                  />
                  <YAxis
                    stroke="#9CA3AF"
                    label={{
                      value: "Time (s)",
                      angle: -90,
                      position: "insideLeft",
                      fill: "#9CA3AF",
                    }}
                  />
                  <Tooltip
                    contentStyle={{
                      backgroundColor: "#1F2937",
                      border: "1px solid rgba(249,250,251,0.08)",
                      borderRadius: "8px",
                    }}
                    formatter={(value: unknown) =>
                      value != null ? `${Number(value).toFixed(3)}s` : "—"
                    }
                  />
                  <Legend />
                  <Line
                    type="monotone"
                    dataKey="driverA"
                    stroke={DRIVER_A_COLOR}
                    strokeWidth={2}
                    dot={false}
                    name={labelA}
                  />
                  <Line
                    type="monotone"
                    dataKey="driverB"
                    stroke={DRIVER_B_COLOR}
                    strokeWidth={2}
                    dot={false}
                    name={labelB}
                  />
                </LineChart>
              </ResponsiveContainer>
            </DataCard>
          )}

          {sectorData.length > 0 && (
            <DataCard title="Sector Delta Comparison">
              <ResponsiveContainer width="100%" height={300}>
                <BarChart data={sectorData}>
                  <CartesianGrid
                    strokeDasharray="3 3"
                    stroke="rgba(249,250,251,0.06)"
                  />
                  <XAxis dataKey="sector" stroke="#9CA3AF" />
                  <YAxis stroke="#9CA3AF" />
                  <Tooltip
                    contentStyle={{
                      backgroundColor: "#1F2937",
                      border: "1px solid rgba(249,250,251,0.08)",
                      borderRadius: "8px",
                    }}
                    formatter={(value: unknown) =>
                      value != null ? `${Number(value).toFixed(3)}s` : "—"
                    }
                  />
                  <Legend />
                  <Bar dataKey="driverA" fill={DRIVER_A_COLOR} name={labelA} />
                  <Bar dataKey="driverB" fill={DRIVER_B_COLOR} name={labelB} />
                </BarChart>
              </ResponsiveContainer>
              <div className="mt-6 grid grid-cols-3 gap-4">
                {sectorData.map((row) => {
                  const delta = row.driverA - row.driverB
                  return (
                    <div
                      key={row.sector}
                      className="p-3 bg-secondary/30 rounded-lg"
                    >
                      <div className="text-sm text-text-secondary mb-1">
                        {row.sector}
                      </div>
                      <div
                        className={`text-lg font-bold ${
                          delta < 0 ? "text-[#00FF85]" : "text-[#E10600]"
                        }`}
                      >
                        {delta > 0 ? "+" : ""}
                        {delta.toFixed(3)}s
                      </div>
                      <div className="text-xs text-text-secondary">
                        {delta < 0 ? "A faster" : "B faster"}
                      </div>
                    </div>
                  )
                })}
              </div>
            </DataCard>
          )}

          {speedOverlayData.length > 0 && (
            <DataCard title="SPEED" noPadding>
              <ResponsiveContainer width="100%" height={350}>
                <AreaChart data={speedOverlayData} margin={{ top: 10, right: 10, left: 0, bottom: 0 }}>
                  <defs>
                    <linearGradient id="colorSpeedA" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor={DRIVER_A_COLOR} stopOpacity={0.3} />
                      <stop offset="95%" stopColor={DRIVER_A_COLOR} stopOpacity={0} />
                    </linearGradient>
                    <linearGradient id="colorSpeedB" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor={DRIVER_B_COLOR} stopOpacity={0.3} />
                      <stop offset="95%" stopColor={DRIVER_B_COLOR} stopOpacity={0} />
                    </linearGradient>
                  </defs>
                  <CartesianGrid
                    strokeDasharray="3 3"
                    stroke="rgba(249,250,251,0.05)"
                    vertical={false}
                  />
                  <XAxis
                    dataKey="distance"
                    stroke="rgba(249,250,251,0.3)"
                    tick={{
                      fill: "rgba(249,250,251,0.5)",
                      fontSize: 11,
                    }}
                    axisLine={{ stroke: "rgba(249,250,251,0.1)" }}
                  />
                  <YAxis
                    stroke="rgba(249,250,251,0.3)"
                    tick={{
                      fill: "rgba(249,250,251,0.5)",
                      fontSize: 11,
                    }}
                    axisLine={{ stroke: "rgba(249,250,251,0.1)" }}
                    domain={[0, 350]}
                    label={{
                      value: "km/h",
                      angle: -90,
                      position: "insideLeft",
                      fill: "rgba(249,250,251,0.5)",
                      fontSize: 11,
                    }}
                  />
                  <Tooltip
                    contentStyle={{
                      backgroundColor: "#1F2937",
                      border: "1px solid rgba(249,250,251,0.08)",
                      borderRadius: "8px",
                    }}
                    formatter={(value: unknown) =>
                      value != null ? `${Math.round(Number(value))} km/h` : "—"
                    }
                  />
                  <Area
                    type="monotone"
                    dataKey="driverA"
                    stroke={DRIVER_A_COLOR}
                    strokeWidth={2.5}
                    fill="url(#colorSpeedA)"
                    name={labelA}
                    isAnimationActive={false}
                  />
                  <Area
                    type="monotone"
                    dataKey="driverB"
                    stroke={DRIVER_B_COLOR}
                    strokeWidth={2.5}
                    fill="url(#colorSpeedB)"
                    name={labelB}
                    isAnimationActive={false}
                  />
                </AreaChart>
              </ResponsiveContainer>
            </DataCard>
          )}

          {throttleOverlayData.length > 0 && (
            <DataCard title="THROTTLE" noPadding>
              <ResponsiveContainer width="100%" height={350}>
                <AreaChart data={throttleOverlayData} margin={{ top: 5, right: 10, left: 0, bottom: 0 }}>
                  <defs>
                    <linearGradient id="colorThrottleA" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor={DRIVER_A_COLOR} stopOpacity={0.4} />
                      <stop offset="95%" stopColor={DRIVER_A_COLOR} stopOpacity={0.05} />
                    </linearGradient>
                    <linearGradient id="colorThrottleB" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor={DRIVER_B_COLOR} stopOpacity={0.4} />
                      <stop offset="95%" stopColor={DRIVER_B_COLOR} stopOpacity={0.05} />
                    </linearGradient>
                  </defs>
                  <CartesianGrid
                    strokeDasharray="3 3"
                    stroke="rgba(249,250,251,0.05)"
                    vertical={false}
                  />
                  <XAxis
                    dataKey="distance"
                    stroke="rgba(249,250,251,0.3)"
                    tick={{
                      fill: "rgba(249,250,251,0.5)",
                      fontSize: 11,
                    }}
                    axisLine={{ stroke: "rgba(249,250,251,0.1)" }}
                  />
                  <YAxis
                    stroke="rgba(249,250,251,0.3)"
                    domain={[0, 100]}
                    tick={{
                      fill: "rgba(249,250,251,0.5)",
                      fontSize: 11,
                    }}
                    axisLine={{ stroke: "rgba(249,250,251,0.1)" }}
                    label={{
                      value: "%",
                      angle: -90,
                      position: "insideLeft",
                      fill: "rgba(249,250,251,0.5)",
                      fontSize: 11,
                    }}
                  />
                  <Tooltip
                    contentStyle={{
                      backgroundColor: "#1F2937",
                      border: "1px solid rgba(249,250,251,0.08)",
                      borderRadius: "8px",
                    }}
                    formatter={(value: unknown) =>
                      value != null ? `${Math.round(Number(value))}%` : "—"
                    }
                  />
                  <Area
                    type="monotone"
                    dataKey="driverA"
                    stroke={DRIVER_A_COLOR}
                    strokeWidth={2}
                    fill="url(#colorThrottleA)"
                    name={labelA}
                    isAnimationActive={false}
                  />
                  <Area
                    type="monotone"
                    dataKey="driverB"
                    stroke={DRIVER_B_COLOR}
                    strokeWidth={2}
                    fill="url(#colorThrottleB)"
                    name={labelB}
                    isAnimationActive={false}
                  />
                </AreaChart>
              </ResponsiveContainer>
            </DataCard>
          )}

          {brakeOverlayData.length > 0 && (
            <DataCard title="BRAKE" noPadding>
              <ResponsiveContainer width="100%" height={350}>
                <AreaChart data={brakeOverlayData} margin={{ top: 5, right: 10, left: 0, bottom: 0 }}>
                  <defs>
                    <linearGradient id="colorBrakeA" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#E10600" stopOpacity={0.5} />
                      <stop offset="95%" stopColor="#E10600" stopOpacity={0.05} />
                    </linearGradient>
                    <linearGradient id="colorBrakeB" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#FF6B6B" stopOpacity={0.5} />
                      <stop offset="95%" stopColor="#FF6B6B" stopOpacity={0.05} />
                    </linearGradient>
                  </defs>
                  <CartesianGrid
                    strokeDasharray="3 3"
                    stroke="rgba(249,250,251,0.05)"
                    vertical={false}
                  />
                  <XAxis
                    dataKey="distance"
                    stroke="rgba(249,250,251,0.3)"
                    tick={{
                      fill: "rgba(249,250,251,0.5)",
                      fontSize: 11,
                    }}
                    axisLine={{ stroke: "rgba(249,250,251,0.1)" }}
                  />
                  <YAxis
                    stroke="rgba(249,250,251,0.3)"
                    domain={[0, 100]}
                    tick={{
                      fill: "rgba(249,250,251,0.5)",
                      fontSize: 11,
                    }}
                    axisLine={{ stroke: "rgba(249,250,251,0.1)" }}
                    label={{
                      value: "%",
                      angle: -90,
                      position: "insideLeft",
                      fill: "rgba(249,250,251,0.5)",
                      fontSize: 11,
                    }}
                  />
                  <Tooltip
                    contentStyle={{
                      backgroundColor: "#1F2937",
                      border: "1px solid rgba(249,250,251,0.08)",
                      borderRadius: "8px",
                    }}
                    formatter={(value: unknown) =>
                      value != null ? `${Math.round(Number(value))}%` : "—"
                    }
                  />
                  <Area
                    type="monotone"
                    dataKey="driverA"
                    stroke="#E10600"
                    strokeWidth={2}
                    fill="url(#colorBrakeA)"
                    name={labelA}
                    isAnimationActive={false}
                  />
                  <Area
                    type="monotone"
                    dataKey="driverB"
                    stroke="#FF6B6B"
                    strokeWidth={2}
                    fill="url(#colorBrakeB)"
                    name={labelB}
                    isAnimationActive={false}
                  />
                </AreaChart>
              </ResponsiveContainer>
            </DataCard>
          )}

          {timeDeltaData.length > 0 && (
            <DataCard title="DELTA" noPadding>
              <ResponsiveContainer width="100%" height={300}>
                <AreaChart data={timeDeltaData} margin={{ top: 5, right: 10, left: 0, bottom: 0 }}>
                  <defs>
                    <linearGradient id="colorDelta" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor={DELTA_COLOR} stopOpacity={0.3} />
                      <stop offset="95%" stopColor={DELTA_COLOR} stopOpacity={0} />
                    </linearGradient>
                  </defs>
                  <CartesianGrid
                    strokeDasharray="3 3"
                    stroke="rgba(249,250,251,0.05)"
                    vertical={false}
                  />
                  <XAxis
                    dataKey="distance"
                    stroke="rgba(249,250,251,0.3)"
                    tick={{
                      fill: "rgba(249,250,251,0.5)",
                      fontSize: 11,
                    }}
                    axisLine={{ stroke: "rgba(249,250,251,0.1)" }}
                    label={{
                      value: "DISTANCE (m)",
                      position: "insideBottom",
                      offset: -5,
                      fill: "rgba(249,250,251,0.5)",
                      fontSize: 11,
                    }}
                  />
                  <YAxis
                    stroke="rgba(249,250,251,0.3)"
                    tick={{
                      fill: "rgba(249,250,251,0.5)",
                      fontSize: 11,
                    }}
                    axisLine={{ stroke: "rgba(249,250,251,0.1)" }}
                    label={{
                      value: "seconds",
                      angle: -90,
                      position: "insideLeft",
                      fill: "rgba(249,250,251,0.5)",
                      fontSize: 11,
                    }}
                  />
                  <Tooltip
                    contentStyle={{
                      backgroundColor: "#1F2937",
                      border: "1px solid rgba(249,250,251,0.08)",
                      borderRadius: "8px",
                    }}
                    formatter={(value: unknown) =>
                      value != null
                        ? `${Number(value) > 0 ? "+" : ""}${Number(value).toFixed(3)}s`
                        : "—"
                    }
                  />
                  <ReferenceLine
                    y={0}
                    stroke="rgba(249,250,251,0.3)"
                    strokeDasharray="3 3"
                  />
                  <Area
                    type="monotone"
                    dataKey="delta"
                    stroke={DELTA_COLOR}
                    strokeWidth={2.5}
                    fill="url(#colorDelta)"
                    name="Time Delta"
                    isAnimationActive={false}
                  />
                </AreaChart>
              </ResponsiveContainer>
              <div className="mt-4 text-sm text-text-secondary text-center">
                Positive = {labelA} faster · Negative = {labelB} faster
              </div>
            </DataCard>
          )}
        </>
      )}
    </div>
  )
}
