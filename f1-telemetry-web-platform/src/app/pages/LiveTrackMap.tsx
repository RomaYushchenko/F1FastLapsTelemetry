import { useCallback, useEffect, useMemo, useState } from "react";
import { Link } from "react-router";
import { DataCard } from "../components/DataCard";
import { StatusBadge } from "../components/StatusBadge";
import { TelemetryStat } from "../components/TelemetryStat";
import { useLiveTelemetry } from "@/ws";
import { getTrackName } from "@/constants/tracks";
import { getTrackLayout } from "@/api/client";
import type { TrackLayoutResponseDto } from "@/api/types";
import { toast } from "sonner";

/** Build SVG path d from points (polyline). */
function pointsToPath(points: { x: number; y: number }[]): string {
  if (points.length === 0) return "";
  const [first, ...rest] = points;
  let d = `M ${first.x} ${first.y}`;
  for (const p of rest) {
    d += ` L ${p.x} ${p.y}`;
  }
  return d;
}

/** Compute bounding box from points. */
function boundsFromPoints(points: { x: number; y: number }[]): { minX: number; minY: number; maxX: number; maxY: number } | null {
  if (points.length === 0) return null;
  let minX = points[0].x;
  let minY = points[0].y;
  let maxX = points[0].x;
  let maxY = points[0].y;
  for (const p of points) {
    minX = Math.min(minX, p.x);
    minY = Math.min(minY, p.y);
    maxX = Math.max(maxX, p.x);
    maxY = Math.max(maxY, p.y);
  }
  return { minX, minY, maxX, maxY };
}

export default function LiveTrackMap() {
  const { session, status, snapshot } = useLiveTelemetry();
  const noActiveSession = status === "no-data" || session == null;
  const trackId = session?.trackId ?? null;

  const [layout, setLayout] = useState<TrackLayoutResponseDto | null | undefined>(undefined);
  const [layoutError, setLayoutError] = useState<string | null>(null);

  const fetchLayout = useCallback(async (id: number) => {
    setLayoutError(null);
    try {
      const data = await getTrackLayout(id);
      setLayout(data ?? null);
      if (data == null) {
        setLayoutError("Track layout not available for this track");
      }
    } catch (e) {
      setLayout(null);
      setLayoutError(e instanceof Error ? e.message : "Failed to load track layout");
      toast.error("Failed to load track layout");
    }
  }, []);

  useEffect(() => {
    if (trackId != null && typeof trackId === "number") {
      setLayout(undefined);
      fetchLayout(trackId);
    } else {
      setLayout(null);
      setLayoutError(null);
    }
  }, [trackId, fetchLayout]);

  const layoutLoading = trackId != null && layout === undefined;
  const points = layout?.points ?? [];
  const bounds = useMemo(() => {
    if (layout?.bounds) return layout.bounds;
    return boundsFromPoints(points);
  }, [layout?.bounds, points]);
  const viewBox = bounds
    ? `${bounds.minX} ${bounds.minY} ${bounds.maxX - bounds.minX} ${bounds.maxY - bounds.minY}`
    : "0 0 800 600";
  const pathD = pointsToPath(points);

  const drivers = [
    { id: 1, name: "VER", pos: 1, color: "#00E5FF" },
    { id: 2, name: "HAM", pos: 2, color: "#00FF85" },
    { id: 3, name: "LEC", pos: 3, color: "#E10600" },
    { id: 4, name: "NOR", pos: 4, color: "#FACC15" },
    { id: 5, name: "PIA", pos: 5, color: "#A855F7" },
  ];

  const trackTitle =
    session?.trackDisplayName ?? (trackId != null ? getTrackName(trackId) : null) ?? "Track Map";

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold mb-2">Live Track Map</h1>
          <p className="text-text-secondary">Real-time track positioning and telemetry</p>
        </div>
        <div className="flex items-center gap-4">
          <StatusBadge variant={status === "live" ? "active" : status === "error" ? "error" : "warning"}>
            {status === "live" ? "Live" : status === "waiting" ? "Waiting" : status === "no-data" ? "No Data" : status === "disconnected" ? "Disconnected" : "Error"}
          </StatusBadge>
          <div className="text-sm text-text-secondary">
            Lap <span className="font-bold text-foreground">{snapshot?.currentLap ?? "—"}/{session?.totalLaps ?? "—"}</span>
          </div>
        </div>
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
        <div className="grid lg:grid-cols-3 gap-6">
          <div className="lg:col-span-2">
            <DataCard title={trackTitle} variant="live" noPadding>
              <div className="aspect-[4/3] bg-secondary/30 relative p-8">
                {layoutLoading && (
                  <div className="absolute inset-0 flex items-center justify-center bg-secondary/50 rounded-lg">
                    <div className="animate-pulse text-text-secondary">Loading track layout…</div>
                  </div>
                )}
                {!layoutLoading && layoutError && (
                  <div className="absolute inset-0 flex items-center justify-center p-4">
                    <p className="text-text-secondary text-center">{layoutError}</p>
                  </div>
                )}
                {!layoutLoading && points.length > 0 && (
                  <svg viewBox={viewBox} className="w-full h-full min-h-[280px]" preserveAspectRatio="xMidYMid meet">
                    <path
                      d={pathD}
                      fill="none"
                      stroke="rgba(249,250,251,0.1)"
                      strokeWidth="40"
                    />
                    <path
                      d={pathD}
                      fill="none"
                      stroke="rgba(249,250,251,0.3)"
                      strokeWidth="4"
                    />
                    {/* Static sector markers (Option B) */}
                    {points.length >= 3 && (
                      <>
                        <circle cx={points[0]?.x} cy={points[0]?.y} r="8" fill="#A855F7" opacity="0.5" />
                        <circle cx={points[Math.floor(points.length / 3)]?.x} cy={points[Math.floor(points.length / 3)]?.y} r="8" fill="#00FF85" opacity="0.5" />
                        <circle cx={points[Math.floor((2 * points.length) / 3)]?.x} cy={points[Math.floor((2 * points.length) / 3)]?.y} r="8" fill="#FACC15" opacity="0.5" />
                      </>
                    )}
                    {points.length > 0 && (
                      <>
                        <rect x={(points[0]?.x ?? 0) - 5} y={(points[0]?.y ?? 0) - 15} width="10" height="30" fill="#F9FAFB" opacity="0.8" />
                        <text x={(points[0]?.x ?? 0) + 10} y={(points[0]?.y ?? 0) + 5} fill="#F9FAFB" fontSize="12" fontWeight="bold">START</text>
                      </>
                    )}
                    {/* Driver positions (mock until B9) */}
                    {drivers.map((driver, idx) => {
                      const pos = points.length >= 5
                        ? points[Math.floor((idx / 5) * points.length) % points.length]
                        : [{ x: 120, y: 300 }, { x: 180, y: 220 }, { x: 350, y: 105 }, { x: 620, y: 120 }, { x: 700, y: 280 }][idx];
                      if (!pos) return null;
                      return (
                        <g key={driver.id}>
                          <circle
                            cx={pos.x}
                            cy={pos.y}
                            r="12"
                            fill={driver.color}
                            opacity="0.3"
                            className="animate-pulse"
                          />
                          <circle cx={pos.x} cy={pos.y} r="8" fill={driver.color} />
                          {idx === 0 && (
                            <circle
                              cx={pos.x}
                              cy={pos.y}
                              r="14"
                              fill="none"
                              stroke={driver.color}
                              strokeWidth="2"
                            />
                          )}
                          <text
                            x={pos.x}
                            y={pos.y + 4}
                            fill="#0B0F14"
                            fontSize="10"
                            fontWeight="bold"
                            textAnchor="middle"
                          >
                            {driver.pos}
                          </text>
                        </g>
                      );
                    })}
                  </svg>
                )}
                {!layoutLoading && !layoutError && points.length === 0 && trackId != null && (
                  <div className="absolute inset-0 flex items-center justify-center p-4">
                    <p className="text-text-secondary text-center">Track layout not available for this track</p>
                  </div>
                )}

                <div className="absolute top-4 left-4 bg-card/90 backdrop-blur-sm border border-border rounded-lg p-3 min-w-[150px]">
                  <div className="text-xs text-text-secondary uppercase mb-2">Positions</div>
                  <div className="space-y-2">
                    {drivers.map((driver) => (
                      <div key={driver.id} className="flex items-center gap-2">
                        <div className="w-4 text-xs font-bold text-text-secondary">{driver.pos}</div>
                        <div className="w-2 h-2 rounded-full" style={{ backgroundColor: driver.color }} />
                        <div className="text-sm font-bold">{driver.name}</div>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            </DataCard>
          </div>

          <div className="space-y-6">
            <DataCard title="Selected Driver">
              <div className="space-y-4">
                <div className="flex items-center gap-3">
                  <div className="w-12 h-12 rounded-lg bg-[#00E5FF]/20 flex items-center justify-center">
                    <span className="text-2xl font-bold text-[#00E5FF]">1</span>
                  </div>
                  <div>
                    <div className="text-xl font-bold">VER</div>
                    <div className="text-sm text-text-secondary">You</div>
                  </div>
                </div>
                <div className="pt-4 border-t border-border/50 space-y-3">
                  <TelemetryStat
                    label="Current Speed"
                    value={snapshot?.speedKph != null ? String(snapshot.speedKph) : "—"}
                    unit="KM/H"
                    variant="performance"
                    size="medium"
                  />
                  <div className="grid grid-cols-2 gap-3">
                    <TelemetryStat
                      label="Gear"
                      value={snapshot?.gear != null ? String(snapshot.gear) : "—"}
                      variant="neutral"
                      size="small"
                    />
                    <TelemetryStat
                      label="RPM"
                      value={snapshot?.engineRpm != null ? `${(snapshot.engineRpm / 1000).toFixed(1)}k` : "—"}
                      variant="neutral"
                      size="small"
                    />
                    <TelemetryStat
                      label="Throttle"
                      value={snapshot?.throttle != null ? `${Math.round(snapshot.throttle * 100)}%` : "—"}
                      variant="performance"
                      size="small"
                    />
                    <TelemetryStat
                      label="Brake"
                      value={snapshot?.brake != null ? `${Math.round(snapshot.brake * 100)}%` : "—"}
                      variant="neutral"
                      size="small"
                    />
                  </div>
                </div>
              </div>
            </DataCard>

            <DataCard title="Sector Times">
              <div className="space-y-3">
                <div className="flex items-center justify-between p-2 bg-secondary/30 rounded">
                  <div className="text-sm text-text-secondary">Sector 1</div>
                  <div className="text-sm font-mono font-bold text-[#A855F7]">—</div>
                </div>
                <div className="flex items-center justify-between p-2 bg-secondary/30 rounded">
                  <div className="text-sm text-text-secondary">Sector 2</div>
                  <div className="text-sm font-mono font-bold text-[#00FF85]">—</div>
                </div>
                <div className="flex items-center justify-between p-2 bg-secondary/30 rounded">
                  <div className="text-sm text-text-secondary">Sector 3</div>
                  <div className="text-sm font-mono font-bold text-[#FACC15]">—</div>
                </div>
                <div className="pt-3 border-t border-border/50 flex items-center justify-between">
                  <div className="text-xs text-text-secondary uppercase">Last Lap</div>
                  <div className="text-lg font-mono font-bold text-[#00E5FF]">
                    {snapshot?.bestLapTimeMs != null ? `${(snapshot.bestLapTimeMs / 1000).toFixed(3)}` : "—"}
                  </div>
                </div>
              </div>
            </DataCard>

            <DataCard variant="default">
              <div className="text-sm text-text-secondary">
                <div className={`flex items-center gap-2 mb-2 ${status === "live" ? "text-[#00FF85]" : "text-text-secondary"}`}>
                  <div className={`w-2 h-2 rounded-full ${status === "live" ? "bg-[#00FF85] animate-pulse" : "bg-text-secondary"}`} />
                  <span className="font-medium">{status === "live" ? "Connected" : "Disconnected"}</span>
                </div>
                <p>Receiving live telemetry data from F1 25</p>
              </div>
            </DataCard>
          </div>
        </div>
      )}
    </div>
  );
}
