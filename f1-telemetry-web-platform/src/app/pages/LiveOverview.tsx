import { Link } from "react-router";
import { useCallback, useEffect, useState } from "react";
import { DataCard } from "../components/DataCard";
import { StatusBadge } from "../components/StatusBadge";
import { TelemetryStat } from "../components/TelemetryStat";
import { useLiveTelemetry, TYRE_LABELS } from "@/ws";
import { getTrackName } from "@/constants/tracks";
import { formatLapTime } from "@/api/format";
import { getSessionEvents } from "@/api/client";
import type { SessionEventDto } from "@/api/types";
import { Flag, Clock, CloudRain, AlertTriangle } from "lucide-react";

function eventCodeToLabel(code: string): string {
  const map: Record<string, string> = {
    FTLP: "Fastest Lap",
    PENA: "Penalty",
    SPTP: "Speed Trap",
    SSTA: "Session Started",
    SEND: "Session Ended",
    RTMT: "Retirement",
    DRSD: "DRS Disabled",
    SCAR: "Safety Car",
    DTS: "Drive Through Served",
    SGS: "Stop Go Served",
  };
  return map[code] ?? code;
}

function formatEventDetail(event: SessionEventDto): string {
  const d = event.detail;
  if (!d || typeof d !== "object") return "";
  if (event.eventCode === "FTLP" && typeof d.lapTime === "number") {
    const sec = Math.floor(d.lapTime);
    const ms = Math.round((d.lapTime - sec) * 1000);
    return `${Math.floor(sec / 60)}:${String(sec % 60).padStart(2, "0")}.${String(ms).padStart(3, "0")}`;
  }
  if (event.eventCode === "PENA" && d.penaltyTime != null) return `${d.penaltyTime}s Time Penalty`;
  if (event.eventCode === "SCAR") return "Safety car";
  return Object.entries(d).map(([k, v]) => `${k}: ${v}`).join(", ") || "";
}

function EventTimelineRow({ event }: { event: SessionEventDto }) {
  const isPenalty = event.eventCode === "PENA";
  const isSc = event.eventCode === "SCAR";
  const timeStr = event.lap != null ? `Lap ${event.lap}` : "—";
  const detailStr = formatEventDetail(event);
  const driverStr = event.carIndex != null ? `Car ${event.carIndex}` : "";

  return (
    <div
      className={`flex items-start gap-4 p-3 rounded-lg border transition-all ${
        isPenalty ? "border-[#E10600]/50 bg-[#E10600]/10" : isSc ? "border-[#FACC15]/50 bg-[#FACC15]/10" : "border-border/30 bg-secondary/20"
      }`}
    >
      <div className="flex-shrink-0 w-16 text-xs text-text-secondary font-mono">{timeStr}</div>
      <div className="flex-shrink-0">
        {isPenalty ? (
          <div className="w-8 h-8 bg-[#E10600]/20 rounded-lg flex items-center justify-center">
            <AlertTriangle className="w-4 h-4 text-[#E10600]" />
          </div>
        ) : isSc ? (
          <div className="w-8 h-8 bg-[#FACC15]/20 rounded-lg flex items-center justify-center">
            <Flag className="w-4 h-4 text-[#FACC15]" />
          </div>
        ) : (
          <div className="w-8 h-8 bg-[#00E5FF]/20 rounded-lg flex items-center justify-center text-xs font-bold text-[#00E5FF]">🏁</div>
        )}
      </div>
      <div className="flex-1">
        <div className="font-semibold">{eventCodeToLabel(event.eventCode)}</div>
        <div className="text-sm text-text-secondary">
          {driverStr && <span className="font-bold">{driverStr}</span>}
          {driverStr && detailStr && " • "}
          {detailStr}
        </div>
      </div>
    </div>
  );
}

export default function LiveOverview() {
  const { session, snapshot, status, leaderboard } = useLiveTelemetry();
  const noActiveSession = status === "no-data" || session == null;
  const tyres = snapshot?.tyresSurfaceTempC;
  const fuelPercent = snapshot?.fuelRemainingPercent;

  const [events, setEvents] = useState<SessionEventDto[]>([]);
  const [eventsLoading, setEventsLoading] = useState(false);
  const [eventsError, setEventsError] = useState<string | null>(null);

  const fetchEvents = useCallback(() => {
    if (!session?.id) return;
    setEventsLoading(true);
    setEventsError(null);
    getSessionEvents(session.id)
      .then(setEvents)
      .catch((err) => setEventsError(err instanceof Error ? err.message : "Failed to load events"))
      .finally(() => setEventsLoading(false));
  }, [session?.id]);

  useEffect(() => {
    if (session?.id) fetchEvents();
    else setEvents([]);
  }, [session?.id, fetchEvents]);

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold mb-2">Live Overview</h1>
          <p className="text-text-secondary">Real-time session monitoring and analytics</p>
        </div>
        <StatusBadge variant={status === "live" ? "active" : status === "error" ? "error" : "warning"}>
          {status === "live" ? "Live" : status === "waiting" ? "Waiting" : status === "no-data" ? "No Data" : status === "disconnected" ? "Disconnected" : "Error"}
        </StatusBadge>
      </div>

      {noActiveSession && (
        <DataCard>
          <div className="py-8 text-center space-y-4">
            <p className="text-text-secondary">No active session. Start a session in F1 25 to see live telemetry.</p>
            <Link to="/app/sessions" className="text-[#00E5FF] hover:underline font-medium">
              View session history →
            </Link>
          </div>
        </DataCard>
      )}

      {!noActiveSession && (
        <>
      {/* Session Info */}
      <div className="grid lg:grid-cols-4 gap-6">
        <DataCard variant="live">
          <div className="space-y-2">
            <div className="text-xs text-text-secondary uppercase">Track</div>
            <div className="text-xl font-bold">{session?.trackDisplayName ?? getTrackName(session?.trackId ?? null) ?? "—"}</div>
            <StatusBadge variant="active">{session?.sessionType ?? "—"}</StatusBadge>
          </div>
        </DataCard>

        <DataCard>
          <div className="space-y-2">
            <div className="flex items-center gap-2 text-xs text-text-secondary uppercase">
              <Flag className="w-4 h-4" />
              Lap
            </div>
            <div className="text-3xl font-bold font-mono text-[#00E5FF]">
              {snapshot?.currentLap != null ? `${snapshot.currentLap}` : "—"}
              {session?.totalLaps != null ? `/${session.totalLaps}` : ""}
            </div>
          </div>
        </DataCard>

        <DataCard>
          <div className="space-y-2">
            <div className="flex items-center gap-2 text-xs text-text-secondary uppercase">
              <Clock className="w-4 h-4" />
              Session Time
            </div>
            <div className="text-3xl font-bold font-mono">—</div>
          </div>
        </DataCard>

        <DataCard>
          <div className="space-y-2">
            <div className="flex items-center gap-2 text-xs text-text-secondary uppercase">
              <CloudRain className="w-4 h-4" />
              Weather
            </div>
            <div className="text-xl font-bold">—</div>
          </div>
        </DataCard>
      </div>

      {/* Main Content */}
      <div className="grid lg:grid-cols-3 gap-6">
        {/* Leaderboard */}
        <div className="lg:col-span-2">
          <DataCard title="Leaderboard" noPadding>
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead className="bg-secondary/50 border-b border-border/50">
                  <tr>
                    <th className="px-4 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider w-16">
                      Pos
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                      Driver
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                      Tyre
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                      Gap
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                      Last Lap
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                      S1
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                      S2
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                      S3
                    </th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-border/30">
                  {leaderboard.length === 0 && (
                    <tr>
                      <td colSpan={8} className="px-4 py-8 text-center text-text-secondary">
                        No leaderboard data yet. Positions appear when LapData is received.
                      </td>
                    </tr>
                  )}
                  {leaderboard.map((item) => (
                    <tr key={`${item.position}-${item.carIndex}`} className="hover:bg-secondary/30 transition-colors">
                      <td className="px-4 py-4 font-bold text-[#00E5FF]">{item.position}</td>
                      <td className="px-4 py-4 font-bold">{item.driverLabel ?? `Car ${item.carIndex}`}</td>
                      <td className="px-4 py-4">
                        <span className={`inline-flex px-2 py-1 rounded text-xs font-bold ${
                          item.compound === 'S' ? 'bg-[#E10600]/20 text-[#E10600]' :
                          item.compound === 'M' ? 'bg-[#FACC15]/20 text-[#FACC15]' :
                          'bg-[#F9FAFB]/20 text-[#F9FAFB]'
                        }`}>
                          {item.compound ?? "—"}
                        </span>
                      </td>
                      <td className="px-4 py-4 font-mono text-text-secondary">{item.gap ?? "—"}</td>
                      <td className="px-4 py-4 font-mono">{item.lastLapTimeMs != null ? formatLapTime(item.lastLapTimeMs) : "—"}</td>
                      <td className="px-4 py-4">
                        <span className="inline-flex w-6 h-6 items-center justify-center rounded text-xs font-bold bg-secondary/50 text-text-secondary">
                          {item.sector1Ms != null ? formatLapTime(item.sector1Ms) : "—"}
                        </span>
                      </td>
                      <td className="px-4 py-4">
                        <span className="inline-flex w-6 h-6 items-center justify-center rounded text-xs font-bold bg-secondary/50 text-text-secondary">
                          {item.sector2Ms != null ? formatLapTime(item.sector2Ms) : "—"}
                        </span>
                      </td>
                      <td className="px-4 py-4">
                        <span className="inline-flex w-6 h-6 items-center justify-center rounded text-xs font-bold bg-secondary/50 text-text-secondary">
                          {item.sector3Ms != null ? formatLapTime(item.sector3Ms) : "—"}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </DataCard>
        </div>

        {/* Player Telemetry */}
        <div>
          <DataCard title="Your Telemetry" variant="live">
            <div className="space-y-4">
              <TelemetryStat
                label="Speed"
                value={snapshot?.speedKph != null ? String(Math.round(snapshot.speedKph)) : "—"}
                unit="KM/H"
                variant="performance"
                size="large"
              />
              
              <div className="grid grid-cols-2 gap-4">
                <TelemetryStat
                  label="Gear"
                  value={snapshot?.gear != null ? String(snapshot.gear) : "—"}
                  variant="neutral"
                  size="medium"
                />
                <TelemetryStat
                  label="RPM"
                  value={snapshot?.engineRpm != null ? `${(snapshot.engineRpm / 1000).toFixed(1)}k` : "—"}
                  variant="neutral"
                  size="medium"
                />
              </div>

              <div className="pt-4 border-t border-border/50 space-y-3">
                <TelemetryStat
                  label="Throttle"
                  value={snapshot?.throttle != null ? `${Math.round(snapshot.throttle * 100)}%` : "—"}
                  variant="performance"
                  size="small"
                />
                <div className="h-2 bg-secondary rounded-full overflow-hidden">
                  <div className="h-full bg-[#00FF85]" style={{ width: snapshot?.throttle != null ? `${snapshot.throttle * 100}%` : "0%" }} />
                </div>
              </div>

              <div className="space-y-3">
                <TelemetryStat
                  label="Brake"
                  value={snapshot?.brake != null ? `${Math.round(snapshot.brake * 100)}%` : "—"}
                  variant="neutral"
                  size="small"
                />
                <div className="h-2 bg-secondary rounded-full overflow-hidden">
                  <div className="h-full bg-[#E10600]" style={{ width: snapshot?.brake != null ? `${snapshot.brake * 100}%` : "0%" }} />
                </div>
              </div>

              <div className="pt-4 border-t border-border/50 grid grid-cols-2 gap-4">
                <TelemetryStat
                  label="ERS"
                  value={snapshot?.ersEnergyPercent != null ? `${snapshot.ersEnergyPercent}%` : "—"}
                  variant="neutral"
                  size="small"
                />
                <TelemetryStat
                  label="Delta"
                  value={snapshot?.deltaMs != null ? formatLapTime(snapshot.deltaMs) : "—"}
                  variant="neutral"
                  size="small"
                />
                <TelemetryStat
                  label="Current lap"
                  value={formatLapTime(snapshot?.currentLapTimeMs ?? null)}
                  variant="neutral"
                  size="small"
                />
                <TelemetryStat
                  label="Best lap"
                  value={formatLapTime(snapshot?.bestLapTimeMs ?? null)}
                  variant="neutral"
                  size="small"
                />
                <TelemetryStat
                  label="Sector"
                  value={snapshot?.currentSector != null ? `Sector ${snapshot.currentSector}` : "—"}
                  variant="neutral"
                  size="small"
                />
                <TelemetryStat
                  label="Fuel"
                  value={fuelPercent != null ? `${fuelPercent}%` : "—"}
                  variant={fuelPercent != null && fuelPercent < 25 ? "warning" : "neutral"}
                  size="small"
                />
              </div>
              {tyres && tyres.length >= 4 && (
                <div className="pt-4 border-t border-border/50 grid grid-cols-4 gap-2">
                  {TYRE_LABELS.map((label, i) => (
                    <TelemetryStat
                      key={label}
                      label={label}
                      value={`${tyres[i]}°C`}
                      variant="neutral"
                      size="small"
                    />
                  ))}
                </div>
              )}
              {(!tyres || tyres.length < 4) && (
                <div className="pt-4 border-t border-border/50 grid grid-cols-4 gap-2">
                  {TYRE_LABELS.map((label) => (
                    <TelemetryStat key={label} label={label} value="—" variant="neutral" size="small" />
                  ))}
                  <p className="col-span-4 text-xs text-text-secondary mt-1">Live tyre temps coming in a follow-up</p>
                </div>
              )}
            </div>
          </DataCard>
        </div>
      </div>

      {/* Event Timeline (API) */}
      <DataCard title="Event Timeline">
        {eventsLoading && (
          <div className="py-6 text-center text-text-secondary">Loading events…</div>
        )}
        {eventsError && (
          <div className="py-4 text-center text-destructive">
            {eventsError}
            <button type="button" onClick={fetchEvents} className="ml-2 text-[#00E5FF] hover:underline">Retry</button>
          </div>
        )}
        {!eventsLoading && !eventsError && (
          <div className="space-y-3">
            {events.length === 0 && (
              <div className="py-6 text-center text-text-secondary">No session events yet.</div>
            )}
            {events.map((event, idx) => (
              <EventTimelineRow key={event.createdAt + String(idx)} event={event} />
            ))}
          </div>
        )}
      </DataCard>
        </>
      )}
    </div>
  );
}
