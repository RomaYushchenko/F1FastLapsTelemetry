import { useCallback, useEffect, useState } from "react";
import { useParams, Link } from "react-router";
import { DataCard } from "../components/DataCard";
import { Button } from "../components/ui/button";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "../components/ui/select";
import { ArrowLeft, Download, Loader2, ChevronDown } from "lucide-react";
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from "recharts";
import {
  getSession,
  getSessionLaps,
  getSessionSummary,
  getSessionPace,
  getLapTrace,
  getLapErs,
  getLapSpeedTrace,
  getSessionTyreWear,
  exportSession,
} from "@/api/client";
import { HttpError } from "@/api/types";
import type { Session } from "@/api/types";
import type { Lap } from "@/api/types";
import type { SessionSummary as SessionSummaryType } from "@/api/types";
import type {
  PacePoint,
  PedalTracePoint,
  ErsPoint,
  SpeedTracePoint,
  TyreWearPoint,
} from "@/api/types";
import { isValidSessionId } from "@/api/sessionId";
import { getTrackName } from "@/constants/tracks";
import { formatDurationClock, formatLapTime, formatSector } from "@/api/format";
import { Skeleton } from "../components/ui/skeleton";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "../components/ui/dropdown-menu";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "../components/ui/tabs";
import { SessionRaceOverviewPanel } from "../components/SessionRaceOverviewPanel";

const tooltipStyle = {
  backgroundColor: "#1F2937",
  border: "1px solid rgba(249,250,251,0.08)",
  borderRadius: "8px",
};

export default function SessionDetails() {
  const { id } = useParams<{ id: string }>();

  const [session, setSession] = useState<Session | null>(null);
  const [laps, setLaps] = useState<Lap[]>([]);
  const [summary, setSummary] = useState<SessionSummaryType | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [notFound, setNotFound] = useState(false);

  const [selectedLap, setSelectedLap] = useState<number | null>(null);
  const [paceData, setPaceData] = useState<PacePoint[]>([]);
  const [traceData, setTraceData] = useState<PedalTracePoint[]>([]);
  const [ersData, setErsData] = useState<ErsPoint[]>([]);
  const [speedData, setSpeedData] = useState<SpeedTracePoint[]>([]);
  const [tyreWearData, setTyreWearData] = useState<TyreWearPoint[]>([]);
  const [chartsLoading, setChartsLoading] = useState(false);
  const [exporting, setExporting] = useState(false);

  const carIndex = session?.playerCarIndex ?? 0;

  async function handleExport(format: "json" | "csv") {
    if (!id) return;
    setExporting(true);
    try {
      await exportSession(id, format);
    } catch {
      // Error already shown by client
    } finally {
      setExporting(false);
    }
  }

  const fetchSessionData = useCallback(async () => {
    if (!id || !isValidSessionId(id)) return;
    setLoading(true);
    setError(null);
    setNotFound(false);
    try {
      const sessionRes = await getSession(id);
      setSession(sessionRes);
      const carIdx = sessionRes.playerCarIndex ?? 0;
      const lapsRes = await getSessionLaps(id, carIdx);
      setLaps(lapsRes);
      try {
        setSummary(await getSessionSummary(id, carIdx));
      } catch {
        setSummary(null);
      }
      const firstWithTime = lapsRes.find(
        (l) => l.lapTimeMs != null && l.lapTimeMs > 0 && !l.isInvalid
      );
      setSelectedLap(firstWithTime?.lapNumber ?? lapsRes[0]?.lapNumber ?? null);
    } catch (e) {
      if (e instanceof HttpError && e.status === 404) {
        setNotFound(true);
        setSession(null);
        setLaps([]);
        setSummary(null);
      } else {
        setError(e instanceof HttpError ? e.message : "Failed to load session");
      }
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    if (!id || !isValidSessionId(id)) return;
    fetchSessionData();
  }, [id, fetchSessionData]);

  useEffect(() => {
    if (!id || !session || selectedLap == null) return;
    setChartsLoading(true);
    Promise.all([
      getSessionPace(id, carIndex),
      getLapTrace(id, selectedLap, carIndex),
      getLapErs(id, selectedLap, carIndex),
      getLapSpeedTrace(id, selectedLap, carIndex),
      getSessionTyreWear(id, carIndex),
    ])
      .then(([pace, trace, ers, speed, tyreWear]) => {
        setPaceData(pace);
        setTraceData(trace);
        setErsData(ers);
        setSpeedData(speed);
        setTyreWearData(tyreWear);
      })
      .catch(() => {
        setPaceData([]);
        setTraceData([]);
        setErsData([]);
        setSpeedData([]);
        setTyreWearData([]);
      })
      .finally(() => setChartsLoading(false));
  }, [id, session, selectedLap, carIndex]);

  if (!id) {
    return (
      <div className="space-y-6">
        <p className="text-destructive">Missing session id</p>
        <Link to="/app/sessions">
          <Button variant="outline">Back to Session History</Button>
        </Link>
      </div>
    );
  }

  if (!isValidSessionId(id)) {
    return (
      <div className="space-y-6">
        <p className="text-destructive">Invalid session id</p>
        <Link to="/app/sessions">
          <Button variant="outline">Back to Session History</Button>
        </Link>
      </div>
    );
  }

  if (notFound) {
    return (
      <div className="space-y-6">
        <p className="text-text-secondary">Session not found</p>
        <Link to="/app/sessions">
          <Button variant="outline" className="gap-2">
            <ArrowLeft className="w-4 h-4" />
            Back to Session History
          </Button>
        </Link>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-12 w-64" />
        <Skeleton className="h-48 w-full" />
        <Skeleton className="h-64 w-full" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="space-y-6">
        <p className="text-destructive">{error}</p>
        <Button onClick={fetchSessionData} variant="outline">
          Retry
        </Button>
        <Link to="/app/sessions">
          <Button variant="ghost" className="gap-2">
            <ArrowLeft className="w-4 h-4" />
            Back to Session History
          </Button>
        </Link>
      </div>
    );
  }

  const trackName =
    session?.trackDisplayName ?? getTrackName(session?.trackId);
  const subtitle = [
    trackName,
    session?.sessionType,
    session?.startedAt
      ? new Date(session.startedAt).toLocaleDateString("en-US", {
          month: "long",
          day: "numeric",
          year: "numeric",
        })
      : null,
  ]
    .filter(Boolean)
    .join(" • ");

  const positionData = laps.map((l) => ({
    lap: l.lapNumber,
    position: l.positionAtLapStart ?? 0,
  })).filter((d) => d.position > 0);

  const paceChartData = paceData.map((p) => ({
    lap: p.lapNumber,
    time: p.lapTimeMs,
  }));

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <Link to="/app/sessions">
          <Button variant="ghost" size="sm" className="gap-2">
            <ArrowLeft className="w-4 h-4" />
            Back
          </Button>
        </Link>
        <div className="flex-1">
          <h1 className="text-3xl font-bold mb-2">Session Details</h1>
          <p className="text-text-secondary">{subtitle || "—"}</p>
        </div>
        <Link to={`/app/sessions/${id}/strategy`}>
          <Button variant="outline" className="gap-2">
            Strategy
          </Button>
        </Link>
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="outline" className="gap-2" disabled={exporting}>
              <Download className="w-4 h-4" />
              Export Data
              <ChevronDown className="w-4 h-4 opacity-50" />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end">
            <DropdownMenuItem onClick={() => handleExport("json")}>
              Export as JSON
            </DropdownMenuItem>
            <DropdownMenuItem onClick={() => handleExport("csv")}>
              Export as CSV
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>

      <Tabs defaultValue="performance" className="w-full">
        <TabsList className="inline-flex h-auto w-full max-w-lg rounded-xl border border-border/40 bg-secondary/30 p-1.5">
          <TabsTrigger
            value="performance"
            className="flex-1 rounded-lg px-4 py-2 text-sm font-medium data-[state=active]:bg-card data-[state=active]:text-foreground data-[state=active]:shadow-sm"
          >
            My Performance
          </TabsTrigger>
          <TabsTrigger
            value="race"
            className="flex-1 rounded-lg px-4 py-2 text-sm font-medium data-[state=active]:bg-card data-[state=active]:text-foreground data-[state=active]:shadow-sm"
          >
            Race Overview
          </TabsTrigger>
        </TabsList>
        <TabsContent value="performance" className="mt-6 space-y-6 outline-none">
      {/* Session Summary: prefer summary API; fallback to derived from laps when summary missing or empty */}
      {(() => {
        const bestLapFromLaps =
          laps.length > 0
            ? laps
                .filter(
                  (l) =>
                    l.lapTimeMs != null && l.lapTimeMs > 0 && !l.isInvalid
                )
                .map((l) => l.lapTimeMs as number)
                .reduce<number | null>(
                  (best, ms) =>
                    best == null ? ms : Math.min(best, ms),
                  null
                )
            : null;
        const bestLapMs =
          summary?.bestLapTimeMs != null && summary.bestLapTimeMs > 0
            ? summary.bestLapTimeMs
            : bestLapFromLaps;
        const totalLaps = Math.max(
          laps.length,
          summary?.totalLaps != null && summary.totalLaps > 0 ? summary.totalLaps : 0
        );
        const totalTimeFromLaps = laps.reduce((sum, l) => {
          if (l.lapTimeMs != null && l.lapTimeMs > 0 && !l.isInvalid) {
            return sum + l.lapTimeMs
          }
          return sum
        }, 0)
        const totalTimeMs =
          session?.totalTimeMs != null && session.totalTimeMs > 0
            ? session.totalTimeMs
            : totalTimeFromLaps > 0
              ? totalTimeFromLaps
              : null
        return (
          <DataCard title="Session Summary">
            <div className="grid md:grid-cols-2 lg:grid-cols-5 gap-6">
              <div>
                <div className="text-xs text-text-secondary uppercase mb-1">
                  Result
                </div>
                <div className="text-2xl font-bold text-[#00E5FF]">
                  {session?.finishingPosition != null
                    ? `P${session.finishingPosition}`
                    : "—"}
                </div>
              </div>
              <div>
                <div className="text-xs text-text-secondary uppercase mb-1">
                  Best Lap
                </div>
                <div className="text-2xl font-bold font-mono text-[#00FF85]">
                  {formatLapTime(bestLapMs ?? null)}
                </div>
              </div>
              <div>
                <div className="text-xs text-text-secondary uppercase mb-1">
                  Total Time
                </div>
                <div className="text-2xl font-bold font-mono">
                  {totalTimeMs != null ? formatLapTime(totalTimeMs) : "—"}
                </div>
              </div>
              <div>
                <div className="text-xs text-text-secondary uppercase mb-1">
                  Total Laps
                </div>
                <div className="text-2xl font-bold">
                  {totalLaps > 0 ? totalLaps : "—"}
                </div>
              </div>
              <div>
                <div className="text-xs text-text-secondary uppercase mb-1">
                  Pit Stops
                </div>
                <div className="text-2xl font-bold">—</div>
              </div>
            </div>
          </DataCard>
        );
      })()}

      {/* Lap selector */}
      {laps.length > 0 && (
        <DataCard>
          <div className="flex items-center gap-4">
            <label className="text-sm font-medium">Lap for trace / ERS / speed:</label>
            <Select
              value={selectedLap != null ? String(selectedLap) : ""}
              onValueChange={(v) => setSelectedLap(v ? parseInt(v, 10) : null)}
            >
              <SelectTrigger className="w-32 bg-input-background border-border">
                <SelectValue placeholder="Select lap" />
              </SelectTrigger>
              <SelectContent>
                {laps.map((lap) => (
                  <SelectItem key={lap.lapNumber} value={String(lap.lapNumber)}>
                    Lap {lap.lapNumber}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        </DataCard>
      )}

      {/* Charts Row 1: Pace + Position */}
      <div className="grid lg:grid-cols-2 gap-6">
        <DataCard title="Lap Time Evolution (Pace)">
          {chartsLoading ? (
            <div className="h-[300px] flex items-center justify-center">
              <Loader2 className="w-8 h-8 animate-spin text-text-secondary" />
            </div>
          ) : paceChartData.length === 0 ? (
            <div className="h-[300px] flex items-center justify-center text-text-secondary">
              No pace data
            </div>
          ) : (
            <ResponsiveContainer width="100%" height={300}>
              <LineChart data={paceChartData} margin={{ left: 4, right: 8 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="rgba(249,250,251,0.06)" />
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
                  width={84}
                  tickFormatter={(v: number) => formatDurationClock(v)}
                  tick={{ fontSize: 11, fontFamily: "ui-monospace, monospace" }}
                  label={{
                    value: "Time (MM:SS.mmm)",
                    angle: -90,
                    position: "insideLeft",
                    fill: "#9CA3AF",
                  }}
                />
                <Tooltip
                  contentStyle={tooltipStyle}
                  formatter={(value: number) => [
                    formatDurationClock(value),
                    "Lap time",
                  ]}
                />
                <Line
                  type="monotone"
                  dataKey="time"
                  stroke="#00E5FF"
                  strokeWidth={2}
                  dot={{ fill: "#00E5FF", r: 3 }}
                />
              </LineChart>
            </ResponsiveContainer>
          )}
        </DataCard>

        <DataCard title="Position Evolution">
          {positionData.length === 0 ? (
            <div className="h-[300px] flex items-center justify-center text-text-secondary">
              No position data
            </div>
          ) : (
            <ResponsiveContainer width="100%" height={300}>
              <LineChart data={positionData}>
                <CartesianGrid strokeDasharray="3 3" stroke="rgba(249,250,251,0.06)" />
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
                  reversed
                  label={{
                    value: "Position",
                    angle: -90,
                    position: "insideLeft",
                    fill: "#9CA3AF",
                  }}
                />
                <Tooltip
                  contentStyle={tooltipStyle}
                  formatter={(value: number) => [`P${value}`, "Position"]}
                />
                <Line
                  type="stepAfter"
                  dataKey="position"
                  stroke="#00FF85"
                  strokeWidth={2}
                  dot={{ fill: "#00FF85", r: 3 }}
                />
              </LineChart>
            </ResponsiveContainer>
          )}
        </DataCard>
      </div>

      {/* Speed trace */}
      <DataCard title="Speed trace">
        {chartsLoading ? (
          <div className="h-[300px] flex items-center justify-center">
            <Loader2 className="w-8 h-8 animate-spin text-text-secondary" />
          </div>
        ) : speedData.length === 0 ? (
          <div className="h-[300px] flex items-center justify-center text-text-secondary">
            {selectedLap == null ? "Select a lap" : "No speed data for this lap"}
          </div>
        ) : (
          <ResponsiveContainer width="100%" height={300}>
            <LineChart data={speedData}>
              <CartesianGrid strokeDasharray="3 3" stroke="rgba(249,250,251,0.06)" />
              <XAxis dataKey="distanceM" stroke="#9CA3AF" />
              <YAxis stroke="#9CA3AF" />
              <Tooltip contentStyle={tooltipStyle} />
              <Line type="monotone" dataKey="speedKph" stroke="#00E5FF" strokeWidth={2} dot={false} />
            </LineChart>
          </ResponsiveContainer>
        )}
      </DataCard>

      {/* ERS */}
      <DataCard title="ERS (energy %)">
        {chartsLoading ? (
          <div className="h-[300px] flex items-center justify-center">
            <Loader2 className="w-8 h-8 animate-spin text-text-secondary" />
          </div>
        ) : ersData.length === 0 ? (
          <div className="h-[300px] flex items-center justify-center text-text-secondary">
            {selectedLap == null ? "Select a lap" : "No ERS data for this lap"}
          </div>
        ) : (
          <ResponsiveContainer width="100%" height={300}>
            <LineChart data={ersData}>
              <CartesianGrid strokeDasharray="3 3" stroke="rgba(249,250,251,0.06)" />
              <XAxis dataKey="lapDistanceM" stroke="#9CA3AF" />
              <YAxis stroke="#9CA3AF" domain={[0, 100]} />
              <Tooltip contentStyle={tooltipStyle} />
              <Line type="monotone" dataKey="energyPercent" stroke="#00E5FF" strokeWidth={2} dot={false} />
            </LineChart>
          </ResponsiveContainer>
        )}
      </DataCard>

      {/* Pedal trace */}
      <DataCard title="Pedal trace (throttle / brake)">
        {chartsLoading ? (
          <div className="h-[300px] flex items-center justify-center">
            <Loader2 className="w-8 h-8 animate-spin text-text-secondary" />
          </div>
        ) : traceData.length === 0 ? (
          <div className="h-[300px] flex items-center justify-center text-text-secondary">
            {selectedLap == null ? "Select a lap" : "No trace data for this lap"}
          </div>
        ) : (
          <ResponsiveContainer width="100%" height={300}>
            <LineChart
              data={traceData.map((p) => ({
                ...p,
                distance: p.distance,
                throttle: p.throttle * 100,
                brake: p.brake * 100,
              }))}
            >
              <CartesianGrid strokeDasharray="3 3" stroke="rgba(249,250,251,0.06)" />
              <XAxis dataKey="distance" stroke="#9CA3AF" />
              <YAxis stroke="#9CA3AF" domain={[0, 100]} />
              <Tooltip contentStyle={tooltipStyle} />
              <Line type="monotone" dataKey="throttle" stroke="#00FF85" strokeWidth={1.5} dot={false} />
              <Line type="monotone" dataKey="brake" stroke="#E10600" strokeWidth={1.5} dot={false} />
            </LineChart>
          </ResponsiveContainer>
        )}
      </DataCard>

      {/* Tyre wear */}
      <DataCard title="Tyre wear">
        {chartsLoading ? (
          <div className="h-[200px] flex items-center justify-center">
            <Loader2 className="w-8 h-8 animate-spin text-text-secondary" />
          </div>
        ) : tyreWearData.length === 0 ? (
          <div className="h-[200px] flex items-center justify-center text-text-secondary">
            No tyre wear data
          </div>
        ) : (
          <ResponsiveContainer width="100%" height={200}>
            <LineChart data={tyreWearData}>
              <CartesianGrid strokeDasharray="3 3" stroke="rgba(249,250,251,0.06)" />
              <XAxis dataKey="lapNumber" stroke="#9CA3AF" />
              <YAxis stroke="#9CA3AF" domain={[0, 1]} tickFormatter={(v) => `${(v * 100).toFixed(0)}%`} />
              <Tooltip contentStyle={tooltipStyle} formatter={(v: number) => [`${(v * 100).toFixed(1)}%`, ""]} />
              <Line type="monotone" dataKey="wearFL" name="FL" stroke="#E10600" strokeWidth={1.5} dot={false} />
              <Line type="monotone" dataKey="wearFR" name="FR" stroke="#FACC15" strokeWidth={1.5} dot={false} />
              <Line type="monotone" dataKey="wearRL" name="RL" stroke="#00FF85" strokeWidth={1.5} dot={false} />
              <Line type="monotone" dataKey="wearRR" name="RR" stroke="#00E5FF" strokeWidth={1.5} dot={false} />
            </LineChart>
          </ResponsiveContainer>
        )}
      </DataCard>

      {/* Lap list (sectors): highlight best lap time and best sectors */}
      {(() => {
        const validLaps = laps.filter(
          (l) =>
            l.lapTimeMs != null &&
            l.lapTimeMs > 0 &&
            !l.isInvalid
        );
        const bestLapTimeMs =
          summary?.bestLapTimeMs != null && summary.bestLapTimeMs > 0
            ? summary.bestLapTimeMs
            : validLaps.length > 0
              ? Math.min(...validLaps.map((l) => l.lapTimeMs!))
              : null;
        const bestS1 =
          summary?.bestSector1Ms != null && summary.bestSector1Ms > 0
            ? summary.bestSector1Ms
            : (() => {
                const vals = laps
                  .filter((l) => l.sector1Ms != null && l.sector1Ms > 0)
                  .map((l) => l.sector1Ms!);
                return vals.length > 0 ? Math.min(...vals) : null;
              })();
        const bestS2 =
          summary?.bestSector2Ms != null && summary.bestSector2Ms > 0
            ? summary.bestSector2Ms
            : (() => {
                const vals = laps
                  .filter((l) => l.sector2Ms != null && l.sector2Ms > 0)
                  .map((l) => l.sector2Ms!);
                return vals.length > 0 ? Math.min(...vals) : null;
              })();
        const bestS3 =
          summary?.bestSector3Ms != null && summary.bestSector3Ms > 0
            ? summary.bestSector3Ms
            : (() => {
                const vals = laps
                  .filter((l) => l.sector3Ms != null && l.sector3Ms > 0)
                  .map((l) => l.sector3Ms!);
                return vals.length > 0 ? Math.min(...vals) : null;
              })();
        const isBestLap = (lap: Lap) =>
          bestLapTimeMs != null &&
          lap.lapTimeMs != null &&
          lap.lapTimeMs > 0 &&
          !lap.isInvalid &&
          lap.lapTimeMs === bestLapTimeMs;
        const isBestS1 = (lap: Lap) =>
          bestS1 != null &&
          lap.sector1Ms != null &&
          lap.sector1Ms > 0 &&
          lap.sector1Ms === bestS1;
        const isBestS2 = (lap: Lap) =>
          bestS2 != null &&
          lap.sector2Ms != null &&
          lap.sector2Ms > 0 &&
          lap.sector2Ms === bestS2;
        const isBestS3 = (lap: Lap) =>
          bestS3 != null &&
          lap.sector3Ms != null &&
          lap.sector3Ms > 0 &&
          lap.sector3Ms === bestS3;
        const bestCellClass = "font-mono font-semibold text-[#A855F7]";
        return (
          <DataCard title="Lap times">
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead className="bg-secondary/50 border-b border-border/50">
                  <tr>
                    <th className="px-4 py-3 text-left text-xs font-medium text-text-secondary uppercase">Lap</th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-text-secondary uppercase">Time</th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-text-secondary uppercase">S1</th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-text-secondary uppercase">S2</th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-text-secondary uppercase">S3</th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-text-secondary uppercase">Position</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-border/30">
                  {laps.map((lap) => (
                    <tr key={lap.lapNumber} className="hover:bg-secondary/30">
                      <td className="px-4 py-2 font-bold">{lap.lapNumber}</td>
                      <td
                        className={`px-4 py-2 font-mono ${isBestLap(lap) ? bestCellClass : "text-[#00E5FF]"}`}
                      >
                        {formatLapTime(lap.lapTimeMs)}
                        {lap.isInvalid && " *"}
                      </td>
                      <td
                        className={`px-4 py-2 font-mono ${isBestS1(lap) ? bestCellClass : "text-text-secondary"}`}
                      >
                        {formatSector(lap.sector1Ms)}
                      </td>
                      <td
                        className={`px-4 py-2 font-mono ${isBestS2(lap) ? bestCellClass : "text-text-secondary"}`}
                      >
                        {formatSector(lap.sector2Ms)}
                      </td>
                      <td
                        className={`px-4 py-2 font-mono ${isBestS3(lap) ? bestCellClass : "text-text-secondary"}`}
                      >
                        {formatSector(lap.sector3Ms)}
                      </td>
                      <td className="px-4 py-2">
                        {lap.positionAtLapStart != null ? `P${lap.positionAtLapStart}` : "—"}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </DataCard>
        );
      })()}
        </TabsContent>
        <TabsContent value="race" className="mt-6 outline-none">
          {id ? (
            <SessionRaceOverviewPanel
              sessionId={id}
              playerCarIndex={session?.playerCarIndex ?? null}
            />
          ) : null}
        </TabsContent>
      </Tabs>
    </div>
  );
}
