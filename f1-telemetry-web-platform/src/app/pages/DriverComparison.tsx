import { useCallback, useEffect, useMemo, useState } from "react"
import { ChevronDown, ChevronUp, Settings } from "lucide-react"
import { DataCard } from "../components/DataCard"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "../components/ui/select"
import {
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
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
  PedalTracePoint,
  SpeedTracePoint,
} from "@/api/types"
import { notify } from "@/notify"
import { Skeleton } from "../components/ui/skeleton"

const DRIVER_A_COLOR = "#00E5FF"
const DRIVER_B_COLOR = "#FACC15"
const DELTA_COLOR = "#00FF85"

/** Distance bucket (m) when merging two laps: coarser than raw samples so both drivers share the same X grid. */
const COMPARISON_DISTANCE_BUCKET_M = 1

function splitDriverLabel(label: string): { first: string; last: string } {
  const t = label.trim()
  if (!t) return { first: "Driver", last: "—" }
  const parts = t.split(/\s+/)
  if (parts.length === 1) return { first: "Driver", last: parts[0] }
  return { first: parts[0]!, last: parts.slice(1).join(" ") }
}

function formatSessionDate(iso: string | undefined | null): string {
  if (!iso) return ""
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso.slice(0, 10)
  return d.toLocaleDateString(undefined, {
    year: "numeric",
    month: "short",
    day: "numeric",
  })
}

interface ComparisonChartTooltipProps {
  active?: boolean
  payload?: ReadonlyArray<{
    name?: string
    value?: number
    color?: string
  }>
  label?: string | number
  /** Decimal places for numeric values (default 1, matching reference UI). */
  decimals?: number
  suffix?: string
  /** When true, prefix positive values with "+". */
  signed?: boolean
}

function ComparisonChartTooltip({
  active,
  payload,
  label,
  decimals = 1,
  suffix = "",
  signed = false,
}: ComparisonChartTooltipProps) {
  if (!active || !payload?.length) return null
  return (
    <div className="bg-card/95 backdrop-blur-sm border border-border rounded-lg p-3 shadow-xl">
      <p className="text-xs text-text-secondary mb-2">
        Distance: {label}m
      </p>
      {payload.map((entry, index) => {
        const v = entry.value
        const sign =
          signed && typeof v === "number" && v > 0 ? "+" : ""
        return (
          <p
            key={index}
            style={{ color: entry.color }}
            className="text-sm font-bold"
          >
            {entry.name}:{" "}
            {typeof v === "number" && !Number.isNaN(v)
              ? `${sign}${v.toFixed(decimals)}${suffix}`
              : "—"}
          </p>
        )
      })}
    </div>
  )
}

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

/** One row for dual-driver charts: distance on X, optional per-driver Y (null = no series). */
interface MergedTwinSeriesRow {
  distance: number
  driverA: number | null
  driverB: number | null
}

function bucketDistanceM(d: number): number {
  const s = COMPARISON_DISTANCE_BUCKET_M
  return Math.round(d / s) * s
}

/**
 * Fills null driver columns for chart display only: forward pass (carry last value), then backward
 * pass (carry next value) so Recharts does not break lines on every row with a single-driver sample.
 */
function fillTwinSeriesGaps(rows: MergedTwinSeriesRow[]): MergedTwinSeriesRow[] {
  if (rows.length === 0) return rows
  const sorted = [...rows].sort((a, b) => a.distance - b.distance)
  const out = sorted.map((r) => ({ ...r }))

  let lastA: number | null = null
  let lastB: number | null = null
  for (let i = 0; i < out.length; i++) {
    const row = out[i]!
    if (row.driverA != null) lastA = row.driverA
    else if (lastA != null) row.driverA = lastA
    if (row.driverB != null) lastB = row.driverB
    else if (lastB != null) row.driverB = lastB
  }

  let nextA: number | null = null
  let nextB: number | null = null
  for (let i = out.length - 1; i >= 0; i--) {
    const row = out[i]!
    if (row.driverA != null) nextA = row.driverA
    else if (nextA != null) row.driverA = nextA
    if (row.driverB != null) nextB = row.driverB
    else if (nextB != null) row.driverB = nextB
  }

  return out
}

/**
 * Merges two speed traces onto one distance axis. Buckets distance (see COMPARISON_DISTANCE_BUCKET_M)
 * so samples from both laps align, then fills gaps for continuous lines (display only).
 */
function mergeSpeedTracesForChart(
  traceA: SpeedTracePoint[],
  traceB: SpeedTracePoint[]
): MergedTwinSeriesRow[] {
  if (!traceA.length && !traceB.length) return []
  if (!traceA.length) {
    return fillTwinSeriesGaps(
      traceB.map((p) => ({
        distance: bucketDistanceM(p.distanceM ?? 0),
        driverA: null,
        driverB: p.speedKph ?? 0,
      })).sort((a, b) => a.distance - b.distance)
    )
  }
  if (!traceB.length) {
    return fillTwinSeriesGaps(
      traceA.map((p) => ({
        distance: bucketDistanceM(p.distanceM ?? 0),
        driverA: p.speedKph ?? 0,
        driverB: null,
      })).sort((a, b) => a.distance - b.distance)
    )
  }
  const map = new Map<number, MergedTwinSeriesRow>()
  for (const p of traceA) {
    const k = bucketDistanceM(p.distanceM ?? 0)
    const cur = map.get(k) ?? { distance: k, driverA: null, driverB: null }
    cur.driverA = p.speedKph ?? 0
    map.set(k, cur)
  }
  for (const p of traceB) {
    const k = bucketDistanceM(p.distanceM ?? 0)
    const cur = map.get(k) ?? { distance: k, driverA: null, driverB: null }
    cur.driverB = p.speedKph ?? 0
    map.set(k, cur)
  }
  return fillTwinSeriesGaps(
    Array.from(map.values()).sort((a, b) => a.distance - b.distance)
  )
}

function mergePedalFieldForChart(
  traceA: PedalTracePoint[],
  traceB: PedalTracePoint[],
  field: "throttle" | "brake"
): MergedTwinSeriesRow[] {
  const scaled = (p: PedalTracePoint) => (p[field] ?? 0) * 100
  if (!traceA.length && !traceB.length) return []
  if (!traceA.length) {
    return fillTwinSeriesGaps(
      traceB.map((p) => ({
        distance: bucketDistanceM(p.distance ?? 0),
        driverA: null,
        driverB: scaled(p),
      })).sort((a, b) => a.distance - b.distance)
    )
  }
  if (!traceB.length) {
    return fillTwinSeriesGaps(
      traceA.map((p) => ({
        distance: bucketDistanceM(p.distance ?? 0),
        driverA: scaled(p),
        driverB: null,
      })).sort((a, b) => a.distance - b.distance)
    )
  }
  const map = new Map<number, MergedTwinSeriesRow>()
  for (const p of traceA) {
    const k = bucketDistanceM(p.distance ?? 0)
    const cur = map.get(k) ?? { distance: k, driverA: null, driverB: null }
    cur.driverA = scaled(p)
    map.set(k, cur)
  }
  for (const p of traceB) {
    const k = bucketDistanceM(p.distance ?? 0)
    const cur = map.get(k) ?? { distance: k, driverA: null, driverB: null }
    cur.driverB = scaled(p)
    map.set(k, cur)
  }
  return fillTwinSeriesGaps(
    Array.from(map.values()).sort((a, b) => a.distance - b.distance)
  )
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

  const nameA = splitDriverLabel(labelA)
  const nameB = splitDriverLabel(labelB)
  const chartNameA = nameA.last
  const chartNameB = nameB.last

  const {
    speedOverlayData,
    showSpeedTraceA,
    showSpeedTraceB,
  } = useMemo(() => {
    const a = comparison?.speedTraceA ?? []
    const b = comparison?.speedTraceB ?? []
    return {
      speedOverlayData: mergeSpeedTracesForChart(a, b),
      showSpeedTraceA: a.length > 0,
      showSpeedTraceB: b.length > 0,
    }
  }, [comparison])

  const {
    throttleOverlayData,
    showThrA,
    showThrB,
  } = useMemo(() => {
    const a = comparison?.traceA ?? []
    const b = comparison?.traceB ?? []
    return {
      throttleOverlayData: mergePedalFieldForChart(a, b, "throttle"),
      showThrA: a.length > 0,
      showThrB: b.length > 0,
    }
  }, [comparison])

  const {
    brakeOverlayData,
    showBrkA,
    showBrkB,
  } = useMemo(() => {
    const a = comparison?.traceA ?? []
    const b = comparison?.traceB ?? []
    return {
      brakeOverlayData: mergePedalFieldForChart(a, b, "brake"),
      showBrkA: a.length > 0,
      showBrkB: b.length > 0,
    }
  }, [comparison])

  const hasAnyTelemetryChart =
    speedOverlayData.length > 0 ||
    throttleOverlayData.length > 0 ||
    brakeOverlayData.length > 0

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

  const refLapA = comparison?.lapsA.find(
    (l) => l.lapNumber === comparison.referenceLapNumA
  )
  const refLapB = comparison?.lapsB.find(
    (l) => l.lapNumber === comparison.referenceLapNumB
  )
  const refLapTimeMsA = refLapA?.lapTimeMs ?? null
  const refLapTimeMsB = refLapB?.lapTimeMs ?? null
  /** Seconds: positive means driver A slower than B on the selected laps. */
  const refLapDeltaSeconds =
    refLapTimeMsA != null &&
    refLapTimeMsB != null &&
    !refLapA?.isInvalid &&
    !refLapB?.isInvalid
      ? (refLapTimeMsA - refLapTimeMsB) / 1000
      : null

  const positionA = refLapA?.positionAtLapStart
  const positionB = refLapB?.positionAtLapStart

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
            <div className="flex items-center gap-6 min-w-0">
              <div className="flex items-center gap-2 shrink-0">
                <Settings className="w-4 h-4 text-[#00E5FF]" aria-hidden />
                <span className="text-sm font-bold text-text-secondary uppercase">
                  Comparison Settings
                </span>
              </div>
              <div className="flex items-center gap-4 flex-wrap min-w-0">
                <div className="flex items-center gap-2">
                  <span className="text-xs text-text-secondary">Session:</span>
                  <span className="text-sm font-bold truncate">
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
                        className="w-2 h-2 rounded-full shrink-0"
                        style={{ backgroundColor: DRIVER_A_COLOR }}
                      />
                      <span className="text-sm font-bold font-mono truncate max-w-[10rem]">
                        {labelA}
                      </span>
                    </div>
                    <span className="text-xs text-text-secondary">vs</span>
                    <div className="flex items-center gap-2">
                      <span
                        className="w-2 h-2 rounded-full shrink-0"
                        style={{ backgroundColor: DRIVER_B_COLOR }}
                      />
                      <span className="text-sm font-bold font-mono truncate max-w-[10rem]">
                        {labelB}
                      </span>
                    </div>
                  </>
                )}
              </div>
            </div>
            <ChevronDown
              className="w-5 h-5 text-text-secondary shrink-0"
              aria-hidden
            />
          </button>
        )}

        {showSelectionPanel && (
          <div className="p-6">
            <div className="flex items-center justify-between mb-6">
              <div className="flex items-center gap-2">
                <Settings className="w-5 h-5 text-[#00E5FF]" aria-hidden />
                <span className="text-sm font-bold text-text-secondary uppercase">
                  Comparison Settings
                </span>
              </div>
              <button
                type="button"
                onClick={() => setShowSelectionPanel(false)}
                className="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-secondary/50 hover:bg-secondary/70 transition-colors text-sm"
              >
                <span>Hide</span>
                <ChevronUp className="w-4 h-4" aria-hidden />
              </button>
            </div>

            <div className="grid md:grid-cols-3 gap-6">
              <div>
                <label className="text-xs text-text-secondary uppercase mb-3 block font-bold">
                  Select Session
                </label>
                {sessionsLoading ? (
                  <Skeleton className="h-24 w-full" />
                ) : (
                  <div className="space-y-2">
                    {sessionOptions.map((session) => {
                      const meta = sessions.find((s) => s.id === session.value)
                      const dateLine = formatSessionDate(meta?.startedAt)
                      return (
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
                          <div className="text-sm font-bold">{session.label}</div>
                          {dateLine ? (
                            <div className="text-xs text-text-secondary mt-1">
                              {dateLine}
                            </div>
                          ) : null}
                        </button>
                      )
                    })}
                  </div>
                )}
              </div>

              <div>
                <label className="text-xs text-text-secondary uppercase mb-3 block font-bold">
                  Driver A
                  {carIndexA !== "" ? (
                    <span className="ml-2 text-[#00E5FF] font-mono">
                      {labelA}
                    </span>
                  ) : null}
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
                      const selected = p.carIndex === carIndexA
                      const blocked =
                        carIndexB !== "" && p.carIndex === carIndexB
                      const plabel = p.displayLabel ?? `Car ${p.carIndex}`
                      return (
                        <button
                          key={p.carIndex}
                          type="button"
                          disabled={blocked}
                          onClick={() => {
                            if (blocked) return
                            setCarIndexA(p.carIndex)
                          }}
                          className={`w-full text-left p-3 rounded-lg border-2 transition-all ${
                            selected
                              ? "border-[#00E5FF] bg-[#00E5FF]/10"
                              : blocked
                                ? "opacity-50 cursor-not-allowed border-border/30 bg-secondary/20"
                                : "border-border/50 bg-secondary/30 hover:border-border hover:bg-secondary/50"
                          }`}
                          style={
                            selected
                              ? {
                                  borderColor: DRIVER_A_COLOR,
                                  backgroundColor: `${DRIVER_A_COLOR}15`,
                                }
                              : undefined
                          }
                        >
                          <div className="flex items-center justify-between">
                            <div>
                              <div className="text-sm font-bold">{plabel}</div>
                              <div className="text-xs text-text-secondary">
                                Car {p.carIndex}
                              </div>
                            </div>
                            <span
                              className="w-3 h-3 rounded-full shrink-0"
                              style={{ backgroundColor: DRIVER_A_COLOR }}
                            />
                          </div>
                        </button>
                      )
                    })}
                  </div>
                )}
              </div>

              <div>
                <label className="text-xs text-text-secondary uppercase mb-3 block font-bold">
                  Driver B
                  {carIndexB !== "" ? (
                    <span className="ml-2 text-[#FACC15] font-mono">
                      {labelB}
                    </span>
                  ) : null}
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
                      const selected = p.carIndex === carIndexB
                      const blocked =
                        carIndexA !== "" && p.carIndex === carIndexA
                      const plabel = p.displayLabel ?? `Car ${p.carIndex}`
                      return (
                        <button
                          key={p.carIndex}
                          type="button"
                          disabled={blocked}
                          onClick={() => {
                            if (blocked) return
                            setCarIndexB(p.carIndex)
                          }}
                          className={`w-full text-left p-3 rounded-lg border-2 transition-all ${
                            selected
                              ? "border-[#FACC15] bg-[#FACC15]/10"
                              : blocked
                                ? "opacity-50 cursor-not-allowed border-border/30 bg-secondary/20"
                                : "border-border/50 bg-secondary/30 hover:border-border hover:bg-secondary/50"
                          }`}
                          style={
                            selected
                              ? {
                                  borderColor: DRIVER_B_COLOR,
                                  backgroundColor: `${DRIVER_B_COLOR}15`,
                                }
                              : undefined
                          }
                        >
                          <div className="flex items-center justify-between">
                            <div>
                              <div className="text-sm font-bold">{plabel}</div>
                              <div className="text-xs text-text-secondary">
                                Car {p.carIndex}
                              </div>
                            </div>
                            <span
                              className="w-3 h-3 rounded-full shrink-0"
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
              <div className="mt-6 pt-6 border-t border-border/50 grid md:grid-cols-2 gap-4">
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
              <div className="mt-6 pt-6 border-t border-border/50 flex items-center justify-between">
                <div className="text-sm text-text-secondary">
                  Analyzing:{" "}
                  <span className="font-bold text-foreground">
                    {selectedSessionData.sessionDisplayName ??
                      selectedSessionData.sessionType ??
                      selectedSessionData.id}
                  </span>
                </div>
                <div className="text-xs text-text-secondary">
                  {formatSessionDate(selectedSessionData.startedAt)}
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
                      {positionA != null ? positionA : "—"}
                    </div>
                    <div>
                      <div className="text-sm text-text-secondary uppercase tracking-wide">
                        {nameA.first}
                      </div>
                      <div
                        className="text-2xl font-bold"
                        style={{ color: DRIVER_A_COLOR }}
                      >
                        {nameA.last}
                      </div>
                      <div className="text-xs text-text-secondary mt-0.5">
                        Car {carIndexA}
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
                        {formatLapTime(
                          refLapTimeMsA ?? comparison.summaryA.bestLapTimeMs
                        )}
                      </div>
                    </div>
                    <div className="text-right">
                      <div className="text-xs text-text-secondary uppercase mb-1">
                        Delta
                      </div>
                      <div
                        className={`text-xl font-bold font-mono ${
                          refLapDeltaSeconds == null
                            ? "text-text-secondary"
                            : refLapDeltaSeconds <= 0
                              ? "text-[#00FF85]"
                              : "text-[#E10600]"
                        }`}
                      >
                        {refLapDeltaSeconds != null
                          ? `${refLapDeltaSeconds > 0 ? "+" : ""}${refLapDeltaSeconds.toFixed(3)}s`
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
                        Max Gear
                      </div>
                      <div className="text-lg font-bold font-mono">—</div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

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
                      {positionB != null ? positionB : "—"}
                    </div>
                    <div>
                      <div className="text-sm text-text-secondary uppercase tracking-wide">
                        {nameB.first}
                      </div>
                      <div
                        className="text-2xl font-bold"
                        style={{ color: DRIVER_B_COLOR }}
                      >
                        {nameB.last}
                      </div>
                      <div className="text-xs text-text-secondary mt-0.5">
                        Car {carIndexB}
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
                        {formatLapTime(
                          refLapTimeMsB ?? comparison.summaryB.bestLapTimeMs
                        )}
                      </div>
                    </div>
                    <div className="text-right">
                      <div className="text-xs text-text-secondary uppercase mb-1">
                        Delta
                      </div>
                      <div
                        className={`text-xl font-bold font-mono ${
                          refLapDeltaSeconds == null
                            ? "text-text-secondary"
                            : -refLapDeltaSeconds <= 0
                              ? "text-[#00FF85]"
                              : "text-[#E10600]"
                        }`}
                      >
                        {refLapDeltaSeconds != null
                          ? `${-refLapDeltaSeconds > 0 ? "+" : ""}${(-refLapDeltaSeconds).toFixed(3)}s`
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
                        Max Gear
                      </div>
                      <div className="text-lg font-bold font-mono">—</div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          {!hasAnyTelemetryChart && (
            <DataCard>
              <p className="text-text-secondary text-sm text-center py-6 px-4">
                No speed or pedal samples for the selected reference laps, so charts are hidden. If lap
                times look correct but this persists, confirm raw car telemetry is stored for both drivers on
                those laps.
              </p>
            </DataCard>
          )}

          {speedOverlayData.length > 0 && (
            <DataCard title="SPEED" noPadding>
              <div className="p-6 pb-2 min-w-0 w-full min-h-[220px]">
                <ResponsiveContainer width="100%" height={220}>
                  <AreaChart
                    data={speedOverlayData}
                    margin={{ top: 10, right: 10, left: 0, bottom: 0 }}
                  >
                    <defs>
                      <linearGradient
                        id="dcCmpSpeedA"
                        x1="0"
                        y1="0"
                        x2="0"
                        y2="1"
                      >
                        <stop
                          offset="5%"
                          stopColor={DRIVER_A_COLOR}
                          stopOpacity={0.3}
                        />
                        <stop
                          offset="95%"
                          stopColor={DRIVER_A_COLOR}
                          stopOpacity={0}
                        />
                      </linearGradient>
                      <linearGradient
                        id="dcCmpSpeedB"
                        x1="0"
                        y1="0"
                        x2="0"
                        y2="1"
                      >
                        <stop
                          offset="5%"
                          stopColor={DRIVER_B_COLOR}
                          stopOpacity={0.3}
                        />
                        <stop
                          offset="95%"
                          stopColor={DRIVER_B_COLOR}
                          stopOpacity={0}
                        />
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
                      content={(props) => (
                        <ComparisonChartTooltip
                          {...props}
                          decimals={0}
                          suffix=" km/h"
                        />
                      )}
                    />
                    {showSpeedTraceA ? (
                      <Area
                        type="monotone"
                        dataKey="driverA"
                        stroke={DRIVER_A_COLOR}
                        strokeWidth={2.5}
                        fill="url(#dcCmpSpeedA)"
                        name={chartNameA}
                        connectNulls={false}
                        isAnimationActive={false}
                      />
                    ) : null}
                    {showSpeedTraceB ? (
                      <Area
                        type="monotone"
                        dataKey="driverB"
                        stroke={DRIVER_B_COLOR}
                        strokeWidth={2.5}
                        fill="url(#dcCmpSpeedB)"
                        name={chartNameB}
                        connectNulls={false}
                        isAnimationActive={false}
                      />
                    ) : null}
                  </AreaChart>
                </ResponsiveContainer>
              </div>
            </DataCard>
          )}

          {throttleOverlayData.length > 0 && (
            <DataCard title="THROTTLE" noPadding>
              <div className="p-6 pb-2 min-w-0 w-full min-h-[140px]">
                <ResponsiveContainer width="100%" height={140}>
                  <AreaChart
                    data={throttleOverlayData}
                    margin={{ top: 5, right: 10, left: 0, bottom: 0 }}
                  >
                    <defs>
                      <linearGradient
                        id="dcCmpThrA"
                        x1="0"
                        y1="0"
                        x2="0"
                        y2="1"
                      >
                        <stop
                          offset="5%"
                          stopColor={DRIVER_A_COLOR}
                          stopOpacity={0.4}
                        />
                        <stop
                          offset="95%"
                          stopColor={DRIVER_A_COLOR}
                          stopOpacity={0.05}
                        />
                      </linearGradient>
                      <linearGradient
                        id="dcCmpThrB"
                        x1="0"
                        y1="0"
                        x2="0"
                        y2="1"
                      >
                        <stop
                          offset="5%"
                          stopColor={DRIVER_B_COLOR}
                          stopOpacity={0.4}
                        />
                        <stop
                          offset="95%"
                          stopColor={DRIVER_B_COLOR}
                          stopOpacity={0.05}
                        />
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
                      content={(props) => (
                        <ComparisonChartTooltip
                          {...props}
                          decimals={0}
                          suffix="%"
                        />
                      )}
                    />
                    {showThrA ? (
                      <Area
                        type="monotone"
                        dataKey="driverA"
                        stroke={DRIVER_A_COLOR}
                        strokeWidth={2}
                        fill="url(#dcCmpThrA)"
                        name={chartNameA}
                        connectNulls={false}
                        isAnimationActive={false}
                      />
                    ) : null}
                    {showThrB ? (
                      <Area
                        type="monotone"
                        dataKey="driverB"
                        stroke={DRIVER_B_COLOR}
                        strokeWidth={2}
                        fill="url(#dcCmpThrB)"
                        name={chartNameB}
                        connectNulls={false}
                        isAnimationActive={false}
                      />
                    ) : null}
                  </AreaChart>
                </ResponsiveContainer>
              </div>
            </DataCard>
          )}

          {brakeOverlayData.length > 0 && (
            <DataCard title="BRAKE" noPadding>
              <div className="p-6 pb-2 min-w-0 w-full min-h-[140px]">
                <ResponsiveContainer width="100%" height={140}>
                  <AreaChart
                    data={brakeOverlayData}
                    margin={{ top: 5, right: 10, left: 0, bottom: 0 }}
                  >
                    <defs>
                      <linearGradient
                        id="dcCmpBrkA"
                        x1="0"
                        y1="0"
                        x2="0"
                        y2="1"
                      >
                        <stop offset="5%" stopColor="#E10600" stopOpacity={0.5} />
                        <stop offset="95%" stopColor="#E10600" stopOpacity={0.05} />
                      </linearGradient>
                      <linearGradient
                        id="dcCmpBrkB"
                        x1="0"
                        y1="0"
                        x2="0"
                        y2="1"
                      >
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
                      content={(props) => (
                        <ComparisonChartTooltip
                          {...props}
                          decimals={0}
                          suffix="%"
                        />
                      )}
                    />
                    {showBrkA ? (
                      <Area
                        type="monotone"
                        dataKey="driverA"
                        stroke="#E10600"
                        strokeWidth={2}
                        fill="url(#dcCmpBrkA)"
                        name={chartNameA}
                        connectNulls={false}
                        isAnimationActive={false}
                      />
                    ) : null}
                    {showBrkB ? (
                      <Area
                        type="monotone"
                        dataKey="driverB"
                        stroke="#FF6B6B"
                        strokeWidth={2}
                        fill="url(#dcCmpBrkB)"
                        name={chartNameB}
                        connectNulls={false}
                        isAnimationActive={false}
                      />
                    ) : null}
                  </AreaChart>
                </ResponsiveContainer>
              </div>
            </DataCard>
          )}

          {timeDeltaData.length > 0 && (
            <DataCard title="DELTA" noPadding>
              <div className="p-6 pb-2 min-w-0 w-full min-h-[140px]">
                <ResponsiveContainer width="100%" height={140}>
                  <AreaChart
                    data={timeDeltaData}
                    margin={{ top: 5, right: 10, left: 0, bottom: 0 }}
                  >
                    <defs>
                      <linearGradient
                        id="dcCmpDelta"
                        x1="0"
                        y1="0"
                        x2="0"
                        y2="1"
                      >
                        <stop
                          offset="5%"
                          stopColor={DELTA_COLOR}
                          stopOpacity={0.3}
                        />
                        <stop
                          offset="95%"
                          stopColor={DELTA_COLOR}
                          stopOpacity={0}
                        />
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
                      content={(props) => (
                        <ComparisonChartTooltip
                          {...props}
                          decimals={3}
                          suffix="s"
                          signed
                        />
                      )}
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
                      fill="url(#dcCmpDelta)"
                      name="Time Delta"
                      isAnimationActive={false}
                    />
                  </AreaChart>
                </ResponsiveContainer>
                <div className="px-6 pb-4 pt-2">
                  <div className="text-xs text-text-secondary text-center">
                    Negative = {chartNameA} faster · Positive = {chartNameB}{" "}
                    faster
                  </div>
                </div>
              </div>
            </DataCard>
          )}
        </>
      )}
    </div>
  )
}
