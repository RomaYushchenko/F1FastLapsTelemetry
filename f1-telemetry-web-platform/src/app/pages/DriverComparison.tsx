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

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold mb-2">Driver Comparison</h1>
        <p className="text-text-secondary">
          Compare telemetry and performance between two cars in a session
        </p>
      </div>

      <DataCard>
        <div className="grid md:grid-cols-3 gap-4">
          <div>
            <label className="text-xs text-text-secondary uppercase mb-2 block">
              Session
            </label>
            {sessionsLoading ? (
              <Skeleton className="h-10 w-full" />
            ) : (
              <Select
                value={sessionUid || undefined}
                onValueChange={(v) => setSessionUid(v)}
              >
                <SelectTrigger className="bg-input-background border-border">
                  <SelectValue placeholder="Select session" />
                </SelectTrigger>
                <SelectContent>
                  {sessionOptions.map((opt) => (
                    <SelectItem key={opt.value} value={opt.value}>
                      {opt.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            )}
          </div>
          <div>
            <label className="text-xs text-text-secondary uppercase mb-2 block">
              Driver A
            </label>
            {sessionDetailLoading ? (
              <Skeleton className="h-10 w-full" />
            ) : (
              <Select
                value={carIndexA === "" ? undefined : String(carIndexA)}
                onValueChange={(v) => setCarIndexA(v === "" ? "" : Number(v))}
              >
                <SelectTrigger className="bg-input-background border-border">
                  <SelectValue placeholder="Select driver" />
                </SelectTrigger>
                <SelectContent>
                  {participants.map((p) => (
                    <SelectItem key={p.carIndex} value={String(p.carIndex)}>
                      {p.displayLabel ?? `Car ${p.carIndex}`}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            )}
          </div>
          <div>
            <label className="text-xs text-text-secondary uppercase mb-2 block">
              Driver B
            </label>
            {sessionDetailLoading ? (
              <Skeleton className="h-10 w-full" />
            ) : (
              <Select
                value={carIndexB === "" ? undefined : String(carIndexB)}
                onValueChange={(v) => setCarIndexB(v === "" ? "" : Number(v))}
              >
                <SelectTrigger className="bg-input-background border-border">
                  <SelectValue placeholder="Select driver" />
                </SelectTrigger>
                <SelectContent>
                  {participants.map((p) => (
                    <SelectItem key={p.carIndex} value={String(p.carIndex)}>
                      {p.displayLabel ?? `Car ${p.carIndex}`}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            )}
          </div>
        </div>
        {canCompare && (
          <div className="grid md:grid-cols-2 gap-4 mt-4">
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
      </DataCard>

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
            <DataCard title={`Driver A — ${labelA}`}>
              <div className="grid grid-cols-3 gap-4">
                <div>
                  <div className="text-xs text-text-secondary uppercase mb-1">
                    Best Lap
                  </div>
                  <div
                    className="text-xl font-bold font-mono"
                    style={{ color: DRIVER_A_COLOR }}
                  >
                    {formatLapTime(comparison.summaryA.bestLapTimeMs)}
                  </div>
                </div>
                <div>
                  <div className="text-xs text-text-secondary uppercase mb-1">
                    Total Laps
                  </div>
                  <div className="text-xl font-bold">
                    {comparison.summaryA.totalLaps ?? "—"}
                  </div>
                </div>
                <div>
                  <div className="text-xs text-text-secondary uppercase mb-1">
                    Best lap #
                  </div>
                  <div className="text-xl font-bold">
                    {comparison.summaryA.bestLapNumber ?? "—"}
                  </div>
                </div>
              </div>
            </DataCard>
            <DataCard title={`Driver B — ${labelB}`}>
              <div className="grid grid-cols-3 gap-4">
                <div>
                  <div className="text-xs text-text-secondary uppercase mb-1">
                    Best Lap
                  </div>
                  <div
                    className="text-xl font-bold font-mono"
                    style={{ color: DRIVER_B_COLOR }}
                  >
                    {formatLapTime(comparison.summaryB.bestLapTimeMs)}
                  </div>
                </div>
                <div>
                  <div className="text-xs text-text-secondary uppercase mb-1">
                    Total Laps
                  </div>
                  <div className="text-xl font-bold">
                    {comparison.summaryB.totalLaps ?? "—"}
                  </div>
                </div>
                <div>
                  <div className="text-xs text-text-secondary uppercase mb-1">
                    Best lap #
                  </div>
                  <div className="text-xl font-bold">
                    {comparison.summaryB.bestLapNumber ?? "—"}
                  </div>
                </div>
              </div>
            </DataCard>
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
            <DataCard title="Speed Overlay">
              <ResponsiveContainer width="100%" height={350}>
                <LineChart data={speedOverlayData}>
                  <CartesianGrid
                    strokeDasharray="3 3"
                    stroke="rgba(249,250,251,0.06)"
                  />
                  <XAxis
                    dataKey="distance"
                    stroke="#9CA3AF"
                    label={{
                      value: "Distance (m)",
                      position: "insideBottom",
                      offset: -5,
                      fill: "#9CA3AF",
                    }}
                  />
                  <YAxis
                    stroke="#9CA3AF"
                    label={{
                      value: "Speed (km/h)",
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
                      value != null ? `${Math.round(Number(value))} km/h` : "—"
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
                    isAnimationActive={false}
                  />
                  <Line
                    type="monotone"
                    dataKey="driverB"
                    stroke={DRIVER_B_COLOR}
                    strokeWidth={2}
                    dot={false}
                    name={labelB}
                    isAnimationActive={false}
                  />
                </LineChart>
              </ResponsiveContainer>
            </DataCard>
          )}

          {throttleOverlayData.length > 0 && (
            <DataCard title="Throttle Overlay">
              <ResponsiveContainer width="100%" height={350}>
                <LineChart data={throttleOverlayData}>
                  <CartesianGrid
                    strokeDasharray="3 3"
                    stroke="rgba(249,250,251,0.06)"
                  />
                  <XAxis
                    dataKey="distance"
                    stroke="#9CA3AF"
                    label={{
                      value: "Distance (m)",
                      position: "insideBottom",
                      offset: -5,
                      fill: "#9CA3AF",
                    }}
                  />
                  <YAxis
                    stroke="#9CA3AF"
                    domain={[0, 100]}
                    label={{
                      value: "Throttle (%)",
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
                      value != null ? `${Math.round(Number(value))}%` : "—"
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
                    isAnimationActive={false}
                  />
                  <Line
                    type="monotone"
                    dataKey="driverB"
                    stroke={DRIVER_B_COLOR}
                    strokeWidth={2}
                    dot={false}
                    name={labelB}
                    isAnimationActive={false}
                  />
                </LineChart>
              </ResponsiveContainer>
            </DataCard>
          )}

          {timeDeltaData.length > 0 && (
            <DataCard title={`Time Delta (${labelA} − ${labelB})`}>
              <ResponsiveContainer width="100%" height={300}>
                <LineChart data={timeDeltaData}>
                  <CartesianGrid
                    strokeDasharray="3 3"
                    stroke="rgba(249,250,251,0.06)"
                  />
                  <XAxis
                    dataKey="distance"
                    stroke="#9CA3AF"
                    label={{
                      value: "Distance (m)",
                      position: "insideBottom",
                      offset: -5,
                      fill: "#9CA3AF",
                    }}
                  />
                  <YAxis
                    stroke="#9CA3AF"
                    label={{
                      value: "Delta (s)",
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
                      value != null
                        ? `${Number(value) > 0 ? "+" : ""}${Number(value).toFixed(3)}s`
                        : "—"
                    }
                  />
                  <Line
                    type="monotone"
                    dataKey="delta"
                    stroke={DELTA_COLOR}
                    strokeWidth={2}
                    dot={false}
                    isAnimationActive={false}
                  />
                </LineChart>
              </ResponsiveContainer>
              <div className="mt-4 text-sm text-text-secondary text-center">
                Positive = {labelA} ahead · Negative = {labelB} ahead
              </div>
            </DataCard>
          )}
        </>
      )}
    </div>
  )
}
