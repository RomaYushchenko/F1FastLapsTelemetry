import { type ReactNode, Component, Suspense, useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Link } from "react-router";
import { DataCard } from "../components/DataCard";
import { StatusBadge } from "../components/StatusBadge";
import { TelemetryStat } from "../components/TelemetryStat";
import { TrackMap2D } from "../components/TrackMap2D";
import { TrackMap3D } from "../components/TrackMap3D";
import { useLiveTelemetry } from "@/ws";
import { formatLapTime, formatSector } from "@/api/format";
import { getTrackName } from "@/constants/tracks";
import { exportAllTrackLayouts, exportTrackLayout, getActivePositions, getTrackLayout, getTrackLayoutStatus, importAllTrackLayouts, importTrackLayout } from "@/api/client";
import type { TrackLayoutResponseDto, TrackLayoutStatusDto, CarPositionDto, TrackLayoutExportDto, TrackLayoutBulkExportDto } from "@/api/types";
import { toast } from "sonner";

/** Color palette for car indices (B9 positions). */
const CAR_COLORS = ["#00E5FF", "#00FF85", "#E10600", "#FACC15", "#A855F7", "#EC4899", "#14B8A6", "#F97316", "#8B5CF6", "#22C55E", "#EF4444", "#3B82F6", "#EAB308", "#06B6D4", "#84CC16", "#F43F5E", "#6366F1", "#0EA5E9", "#10B981", "#F59E0B", "#8B5CF6", "#EC4899"];
function colorForCarIndex(carIndex: number): string {
  return CAR_COLORS[carIndex % CAR_COLORS.length] ?? "#9CA3AF";
}

type ViewMode = '2d' | '3d';

/** Catches errors in 3D view so the page does not crash; shows fallback and error message. */
class LiveTrackMap3DErrorBoundary extends Component<
  { children: ReactNode },
  { hasError: boolean; error: Error | null }
> {
  state = { hasError: false, error: null as Error | null };

  static getDerivedStateFromError(error: Error) {
    return { hasError: true, error };
  }

  render() {
    if (this.state.hasError && this.state.error) {
      return (
        <div className="absolute inset-0 flex flex-col items-center justify-center gap-3 bg-secondary/50 rounded-lg p-6 text-center">
          <p className="text-text-primary font-medium">3D view failed to load</p>
          <p className="text-sm text-text-secondary max-w-md">{this.state.error.message}</p>
          <button
            type="button"
            className="text-xs text-accent hover:underline"
            onClick={() => this.setState({ hasError: false, error: null })}
          >
            Try again
          </button>
        </div>
      );
    }
    return this.props.children;
  }
}

export default function LiveTrackMap() {
  const { session, status, snapshot, positions, leaderboard } = useLiveTelemetry();
  const noActiveSession = status === "no-data" || session == null;
  const trackId = session?.trackId ?? null;

  const [layout, setLayout] = useState<TrackLayoutResponseDto | null>(null);
  const [layoutStatus, setLayoutStatus] = useState<TrackLayoutStatusDto | null>(null);
  const [isLoadingLayout, setIsLoadingLayout] = useState(false);
  const pollRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const [viewMode, setViewMode] = useState<ViewMode>('2d');
  const [polledPositions, setPolledPositions] = useState<CarPositionDto[]>([]);

  const fetchLayout = useCallback(async (id: number) => {
    setIsLoadingLayout(true);
    try {
      const data = await getTrackLayout(id);
      setLayout(data ?? null);
    } catch (e) {
      setLayout(null);
      const msg = e instanceof Error ? e.message : "Failed to load track layout";
      toast.error(msg);
    }
    setIsLoadingLayout(false);
  }, []);

  const handleExport = useCallback(async () => {
    if (!trackId) return;
    try {
      await exportTrackLayout(trackId);
    } catch {
      // error toast already handled in client
    }
  }, [trackId]);

  const handleExportAll = useCallback(async () => {
    try {
      await exportAllTrackLayouts();
    } catch {
      // error toast already handled in client
    }
  }, []);

  const handleImport = useCallback(
    async (file: File) => {
      try {
        const text = await file.text();
        const data = JSON.parse(text) as TrackLayoutExportDto | TrackLayoutBulkExportDto;

        // Bulk file contains tracks array
        if (Array.isArray((data as TrackLayoutBulkExportDto).tracks)) {
          const bulk = data as TrackLayoutBulkExportDto;
          const result = await importAllTrackLayouts(bulk);
          if (result.errors.length > 0) {
            toast.warning(
              `Imported ${result.imported} tracks. ${result.skipped} failed: ${result.errors[0]}`,
            );
          } else {
            toast.success(`Imported ${result.imported} track layouts`);
          }
          if (trackId && bulk.tracks?.some(t => t.trackId === trackId)) {
            const layoutDto = await getTrackLayout(trackId);
            if (layoutDto) setLayout(layoutDto);
          }
          return;
        }

        const single = data as TrackLayoutExportDto;
        if (!single.trackId || !single.points || single.points.length === 0) {
          toast.error("Invalid track layout file");
          return;
        }

        if (trackId && single.trackId !== trackId) {
          toast.warning(
            `Importing track ${single.trackId} but current session is on track ${trackId}`,
          );
        }

        await importTrackLayout(single);
        toast.success(`Track layout imported for track ${single.trackId}`);

        if (trackId && single.trackId === trackId) {
          const layoutDto = await getTrackLayout(trackId);
          if (layoutDto) setLayout(layoutDto);
        }
      } catch (err) {
        const msg = err instanceof Error ? err.message : "Import failed";
        toast.error(msg);
      }
    },
    [trackId],
  );

  useEffect(() => {
    if (trackId != null && typeof trackId === "number") {
      setLayout(null);
      setLayoutStatus(null);
      setIsLoadingLayout(false);
      if (pollRef.current) {
        clearTimeout(pollRef.current);
        pollRef.current = null;
      }
      fetchLayout(trackId).catch(() => {
        // layout may not exist yet — status polling will handle it
      });
    } else {
      setLayout(null);
      setLayoutStatus(null);
      setIsLoadingLayout(false);
    }
  }, [trackId, fetchLayout]);

  useEffect(() => {
    if (!trackId || layout) {
      return;
    }
    let cancelled = false;
    const poll = async () => {
      if (cancelled || !trackId || layout) return;
      try {
        const st = await getTrackLayoutStatus(trackId);
        if (cancelled) return;
        setLayoutStatus(st);
        if (st.status === "READY") {
          const data = await getTrackLayout(trackId);
          if (!cancelled && data) {
            setLayout(data);
          }
          return;
        }
        const interval = st.status === "RECORDING" ? 2000 : 4000;
        pollRef.current = setTimeout(poll, interval);
      } catch {
        const interval = 4000;
        pollRef.current = setTimeout(poll, interval);
      }
    };
    poll();
    return () => {
      cancelled = true;
      if (pollRef.current) {
        clearTimeout(pollRef.current);
        pollRef.current = null;
      }
    };
  }, [trackId, layout]);

  useEffect(() => {
    if (noActiveSession || !layout) {
      setPolledPositions([]);
      return;
    }
    const poll = async () => {
      try {
        const list = await getActivePositions();
        setPolledPositions(list);
      } catch {
        setPolledPositions([]);
      }
    };
    poll();
    const interval = setInterval(poll, 1000);
    return () => clearInterval(interval);
  }, [noActiveSession, layout]);

  const playerLeaderboardEntry = useMemo(
    () =>
      session?.playerCarIndex != null
        ? leaderboard.find((e) => e.carIndex === session.playerCarIndex) ?? null
        : null,
    [session?.playerCarIndex, leaderboard],
  );

  const positionsForMap = positions.length > 0 ? positions : polledPositions;
  const useRealPositions = positionsForMap.length > 0;
  const carsForMap: (CarPositionDto & { color: string; racingNumber?: number; driverLabel?: string | null })[] = useMemo(
    () =>
      positionsForMap.map((p) => {
        const entry = leaderboard.find((e) => e.carIndex === p.carIndex);
        return {
          ...p,
          color: colorForCarIndex(p.carIndex),
          driverLabel: entry?.driverLabel ?? null,
        };
      }),
    [positionsForMap, leaderboard],
  );

  const trackTitle =
    session?.trackDisplayName ?? (trackId != null ? getTrackName(trackId) : null) ?? "Track Map";

  const headerActions =
    layout != null ? (
      <div className="flex items-center gap-2">
        <button
          type="button"
          onClick={handleExport}
          disabled={!trackId}
          className="flex items-center gap-1.5 px-2 py-1 rounded text-xs text-text-secondary hover:text-text-primary hover:bg-surface-secondary transition-colors cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
        >
          Export
        </button>
        <button
          type="button"
          onClick={handleExportAll}
          className="flex items-center gap-1.5 px-2 py-1 rounded text-xs text-text-secondary hover:text-text-primary hover:bg-surface-secondary transition-colors cursor-pointer"
        >
          Export All
        </button>
        <div className="flex gap-1 rounded-lg bg-surface-secondary p-1">
          {(['2d', '3d'] as ViewMode[]).map(mode => (
            <button
              key={mode}
              className={`px-3 py-1 rounded text-sm font-medium transition-colors ${
                viewMode === mode
                  ? 'bg-accent text-black'
                  : 'text-text-secondary hover:text-text-primary'
              }`}
              onClick={() => setViewMode(mode)}
            >
              {mode.toUpperCase()}
            </button>
          ))}
        </div>
      </div>
    ) : null;

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
            <DataCard title={trackTitle} variant="live" noPadding actions={headerActions}>
              <div className="aspect-[4/3] bg-secondary/30 relative p-8">
                {isLoadingLayout && (
                  <div className="absolute inset-0 flex items-center justify-center bg-secondary/50 rounded-lg">
                    <div className="animate-pulse text-text-secondary">Loading track layout…</div>
                  </div>
                )}

                {!isLoadingLayout && layoutStatus?.status === "RECORDING" && (
                  <div className="absolute inset-0 flex flex-col items-center justify-center gap-4 py-8">
                    <div className="relative flex items-center justify-center">
                      <div className="w-16 h-16 rounded-full bg-red-500/10 animate-ping absolute" />
                      <div className="w-8 h-8 rounded-full bg-red-500/30 animate-ping absolute" />
                      <div className="w-4 h-4 rounded-full bg-red-500" />
                    </div>
                    <div className="text-center">
                      <p className="text-text-primary font-medium">Recording track layout...</p>
                      <p className="text-text-secondary text-sm mt-1">
                        Drive one full lap to generate the track map
                      </p>
                    </div>
                    <div className="w-64">
                      <div className="flex justify-between text-xs text-text-secondary mb-1">
                        <span>Progress</span>
                        <span>{layoutStatus.pointsCollected} pts</span>
                      </div>
                      <div className="h-2 bg-surface-secondary rounded-full overflow-hidden">
                        <div
                          className="h-full bg-yellow-400 rounded-full transition-all duration-500"
                          style={{ width: `${Math.min(100, (layoutStatus.pointsCollected / 10) * 100)}%` }}
                        />
                      </div>
                    </div>
                    <p className="text-xs text-text-secondary">
                      Track map will appear automatically when the lap is complete
                    </p>
                  </div>
                )}

                {!isLoadingLayout && !layout && layoutStatus?.status === "NOT_AVAILABLE" && trackId != null && (
                  <div className="absolute inset-0 flex flex-col items-center justify-center p-4 gap-4">
                    <p className="text-text-secondary text-center">
                      Track layout not yet available. Drive a lap to record it automatically.
                    </p>
                    <label className="cursor-pointer">
                      <input
                        type="file"
                        accept=".json"
                        className="hidden"
                        onChange={e => {
                          const file = e.target.files?.[0];
                          if (file) {
                            handleImport(file);
                          }
                          e.target.value = "";
                        }}
                      />
                      <span className="flex items-center gap-1.5 px-3 py-1.5 rounded border border-surface-border text-xs text-text-secondary hover:text-text-primary hover:border-accent transition-colors">
                        Import track JSON
                      </span>
                    </label>
                  </div>
                )}

                {!isLoadingLayout && layout && viewMode === '2d' && (
                  <TrackMap2D layout={layout} cars={carsForMap} />
                )}
                {!isLoadingLayout && layout && viewMode === '3d' && (
                  <LiveTrackMap3DErrorBoundary>
                    <Suspense fallback={<div className="absolute inset-0 flex items-center justify-center bg-secondary/50 rounded-lg text-text-secondary">Loading 3D…</div>}>
                      <TrackMap3D layout={layout} cars={carsForMap} />
                    </Suspense>
                  </LiveTrackMap3DErrorBoundary>
                )}

                {/* Legend overlaid on the track map */}
                {layout && (
                  <div className="absolute bottom-4 left-4 right-4 flex justify-end pointer-events-none">
                    <div className="pointer-events-auto rounded-lg border border-border/50 bg-background/90 backdrop-blur-sm px-3 py-2 shadow-lg max-h-48 overflow-y-auto">
                      <div className="text-xs font-semibold text-text-secondary uppercase tracking-wide mb-1.5">Cars on track</div>
                      <ul className="flex flex-wrap gap-x-4 gap-y-1">
                        {carsForMap.length === 0 && (
                          <li className="text-sm text-text-secondary py-0.5">No position data yet</li>
                        )}
                        {[...carsForMap]
                          .sort((a, b) => a.carIndex - b.carIndex)
                          .map((car) => (
                            <li
                              key={car.carIndex}
                              className="flex items-center gap-1.5 text-sm"
                            >
                              <span
                                className="shrink-0 w-2.5 h-2.5 rounded-full border border-white/20"
                                style={{ backgroundColor: car.color }}
                                aria-hidden
                              />
                              <span className="font-medium tabular-nums truncate max-w-[6rem]">
                                {car.driverLabel ?? `Car ${car.carIndex}`}
                              </span>
                              <span className="text-text-secondary text-xs">#{car.carIndex}</span>
                            </li>
                          ))}
                      </ul>
                    </div>
                  </div>
                )}
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
                  <div className="text-sm font-mono font-bold text-[#A855F7]">
                    {formatSector(playerLeaderboardEntry?.sector1Ms)}
                  </div>
                </div>
                <div className="flex items-center justify-between p-2 bg-secondary/30 rounded">
                  <div className="text-sm text-text-secondary">Sector 2</div>
                  <div className="text-sm font-mono font-bold text-[#00FF85]">
                    {formatSector(playerLeaderboardEntry?.sector2Ms)}
                  </div>
                </div>
                <div className="flex items-center justify-between p-2 bg-secondary/30 rounded">
                  <div className="text-sm text-text-secondary">Sector 3</div>
                  <div className="text-sm font-mono font-bold text-[#FACC15]">
                    {formatSector(playerLeaderboardEntry?.sector3Ms)}
                  </div>
                </div>
                <div className="pt-3 border-t border-border/50 flex items-center justify-between">
                  <div className="text-xs text-text-secondary uppercase">Last Lap</div>
                  <div className="text-lg font-mono font-bold text-[#00E5FF]">
                    {formatLapTime(playerLeaderboardEntry?.lastLapTimeMs ?? snapshot?.bestLapTimeMs)}
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
