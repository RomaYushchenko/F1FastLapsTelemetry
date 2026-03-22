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
import { exportAllTrackLayouts, exportTrackLayout, getActivePositions, getSessionDiagnostics, getTrackLayout, getTrackLayoutStatus, importAllTrackLayouts, importTrackLayout } from "@/api/client";
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
  const [isFullscreen, setIsFullscreen] = useState(false);
  const trackContainerRef = useRef<HTMLDivElement | null>(null);

  const [packetHealthPercent, setPacketHealthPercent] = useState<number | null>(null);
  const [updateRateHz, setUpdateRateHz] = useState<number | null>(null);
  const updateTimestampsRef = useRef<number[]>([]);
  const UPDATE_RATE_WINDOW_MS = 5000;

  useEffect(() => {
    if (status !== "live" || !session?.id) {
      setPacketHealthPercent(null);
      return;
    }
    let cancelled = false;
    async function fetchDiagnostics() {
      try {
        const d = await getSessionDiagnostics(session!.id!);
        if (cancelled) return;
        setPacketHealthPercent(d.packetHealthPercent ?? null);
      } catch {
        if (!cancelled) setPacketHealthPercent(null);
      }
    }
    fetchDiagnostics();
    const intervalId = window.setInterval(fetchDiagnostics, 5000);
    return () => {
      cancelled = true;
      window.clearInterval(intervalId);
    };
  }, [session?.id, status]);

  useEffect(() => {
    if (status !== "live") {
      setUpdateRateHz(null);
      updateTimestampsRef.current = [];
      return;
    }
    const now = Date.now();
    updateTimestampsRef.current.push(now);
    const cutoff = now - UPDATE_RATE_WINDOW_MS;
    updateTimestampsRef.current = updateTimestampsRef.current.filter(t => t >= cutoff);
  }, [snapshot, positions, status]);

  useEffect(() => {
    if (status !== "live") return;
    const intervalId = window.setInterval(() => {
      const ts = updateTimestampsRef.current;
      const now = Date.now();
      const cutoff = now - UPDATE_RATE_WINDOW_MS;
      const recent = ts.filter(t => t >= cutoff);
      updateTimestampsRef.current = recent;
      const rate = recent.length / (UPDATE_RATE_WINDOW_MS / 1000);
      setUpdateRateHz(rate);
    }, 1000);
    return () => window.clearInterval(intervalId);
  }, [status]);

  useEffect(() => {
    const onChange = () => {
      const currentlyFullscreen = document.fullscreenElement != null;
      setIsFullscreen(currentlyFullscreen);
    };
    document.addEventListener("fullscreenchange", onChange);
    return () => document.removeEventListener("fullscreenchange", onChange);
  }, []);

  const handleToggleFullscreen = useCallback(() => {
    const el = trackContainerRef.current;
    if (!el) return;
    if (!document.fullscreenElement) {
      void el.requestFullscreen?.();
    } else {
      void document.exitFullscreen?.();
    }
  }, []);

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
  const carsForMap: (CarPositionDto & { color: string })[] = useMemo(
    () =>
      positionsForMap.map((p) => {
        const entry = leaderboard.find((e) => e.carIndex === p.carIndex);
        return {
          ...p,
          color: colorForCarIndex(p.carIndex),
          // Prefer driver name from positions (Participants packet), then leaderboard
          driverLabel: p.driverLabel ?? entry?.driverLabel ?? null,
        };
      }),
    [positionsForMap, leaderboard],
  );

  /** Player car from map data for correct number/label in Selected Driver panel. */
  const playerCarOnMap = useMemo(
    () =>
      session?.playerCarIndex != null
        ? carsForMap.find((c) => c.carIndex === session.playerCarIndex)
        : null,
    [session?.playerCarIndex, carsForMap],
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
        <button
          type="button"
          onClick={handleToggleFullscreen}
          className="flex items-center gap-1.5 px-2 py-1 rounded text-xs text-text-secondary hover:text-text-primary hover:bg-surface-secondary transition-colors cursor-pointer"
        >
          {isFullscreen ? "Exit Fullscreen" : "Fullscreen"}
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
              <div
                ref={trackContainerRef}
                className="aspect-[4/3] bg-secondary/30 relative p-8"
              >
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

                {!isLoadingLayout && layout && (
                  <div className="absolute inset-0 flex flex-col bg-gradient-to-br from-secondary/40 via-secondary/20 to-secondary/40">
                    {viewMode === '2d' ? (
                      <TrackMap2D layout={layout} cars={carsForMap} />
                    ) : (
                      <LiveTrackMap3DErrorBoundary>
                        <TrackMap3D
                          layout={layout}
                          cars={carsForMap}
                          selectedCarIndex={session?.playerCarIndex ?? null}
                          leaderCarIndex={leaderboard[0]?.carIndex ?? null}
                        />
                      </LiveTrackMap3DErrorBoundary>
                    )}
                    {/* Track info overlay (2D and 3D) */}
                    <div className="absolute top-4 right-4 bg-card/60 backdrop-blur-md border border-border/40 rounded-lg p-3 min-w-[180px]">
                      <div className="text-xs text-text-secondary uppercase mb-3 font-bold">Track Info</div>
                      <div className="space-y-2 text-xs">
                        <div className="flex items-center justify-between">
                          <span className="text-text-secondary">Length:</span>
                          <span className="font-mono font-bold">
                            {layout.points.length > 1
                              ? `${(layout.points.reduce((acc, p, i) => {
                                  const next = layout.points[(i + 1) % layout.points.length]
                                  const ax = p.x
                                  const az = p.z ?? p.y ?? 0
                                  const bx = next.x
                                  const bz = next.z ?? next.y ?? 0
                                  return acc + Math.hypot(bx - ax, bz - az)
                                }, 0) / 1000).toFixed(2)} km`
                              : "—"}
                          </span>
                        </div>
                        <div className="flex items-center justify-between">
                          <span className="text-text-secondary">Elevation:</span>
                          <span className="font-mono font-bold">
                            {layout.bounds?.minElev != null && layout.bounds?.maxElev != null
                              ? `${(layout.bounds.maxElev - layout.bounds.minElev).toFixed(0)} m`
                              : "—"}
                          </span>
                        </div>
                        <div className="flex items-center justify-between">
                          <span className="text-text-secondary">Points:</span>
                          <span className="font-mono font-bold">{layout.points.length}</span>
                        </div>
                        <div className="flex items-center gap-2 pt-2 border-t border-border/50">
                          <div className="w-3 h-3 bg-[#00E5FF]/30 border border-[#00E5FF] rounded-sm" />
                          <span className="text-text-secondary">DRS Zone</span>
                        </div>
                        <div className="flex items-center gap-2">
                          <div className="w-3 h-3 bg-[#E10600]/30 border border-[#E10600] rounded-sm" />
                          <span className="text-text-secondary">Brake Zone</span>
                        </div>
                      </div>
                    </div>
                    {/* Positions overlay (2D and 3D) */}
                    <div className="absolute bottom-4 left-4 bg-card/60 backdrop-blur-md border border-border/40 rounded-lg p-3 min-w-[160px]">
                      <div className="text-xs text-text-secondary uppercase mb-3 font-bold flex items-center justify-between">
                        <span>Positions</span>
                        <span className="text-[10px] text-text-secondary/60 normal-case font-normal">
                          Lap {snapshot?.currentLap ?? "—"}/{session?.totalLaps ?? "—"}
                        </span>
                      </div>
                      <div className="space-y-2 max-h-48 overflow-y-auto">
                        {carsForMap.length === 0 && (
                          <div className="text-sm text-text-secondary py-1">No position data yet</div>
                        )}
                        {[...carsForMap]
                          .sort((a, b) => (leaderboard.find(e => e.carIndex === a.carIndex)?.position ?? 99) - (leaderboard.find(e => e.carIndex === b.carIndex)?.position ?? 99))
                          .map(car => {
                            const entry = leaderboard.find(e => e.carIndex === car.carIndex)
                            const pos = entry?.position ?? car.carIndex
                            const isPlayer = car.carIndex === session?.playerCarIndex
                            return (
                              <div
                                key={car.carIndex}
                                className={`flex items-center gap-2 p-1.5 rounded transition-all ${
                                  isPlayer ? "bg-secondary/60 border border-border/50" : ""
                                }`}
                              >
                                <div className="w-5 text-xs font-bold text-text-primary">{pos}</div>
                                <div
                                  className="w-2.5 h-2.5 rounded-full ring-2 ring-offset-2 ring-offset-card shrink-0"
                                  style={{ backgroundColor: car.color, ringColor: `${car.color}40` }}
                                />
                                <div className="text-sm font-bold flex-1 truncate">
                                  {car.driverLabel ?? `#${car.racingNumber ?? car.carIndex}`}
                                </div>
                                {isPlayer && (
                                  <div className="w-1.5 h-1.5 rounded-full bg-[#00E5FF] animate-pulse shrink-0" />
                                )}
                              </div>
                            )
                          })}
                      </div>
                    </div>
                  </div>
                )}
              </div>
            </DataCard>
          </div>

          <div className="space-y-6">
            <DataCard title="Selected Driver">
              <div className="space-y-4">
                <div className="flex items-center gap-3 p-3 bg-secondary/30 rounded-lg border border-border/30">
                  <div
                    className="w-14 h-14 rounded-lg flex items-center justify-center relative overflow-hidden"
                    style={{ backgroundColor: `${playerCarOnMap?.color ?? colorForCarIndex(session?.playerCarIndex ?? 0)}20` }}
                  >
                    <div
                      className="absolute inset-0 opacity-20"
                      style={{
                        background: `radial-gradient(circle at center, ${playerCarOnMap?.color ?? colorForCarIndex(session?.playerCarIndex ?? 0)} 0%, transparent 70%)`,
                      }}
                    />
                    <span
                      className="text-3xl font-bold relative z-10"
                      style={{ color: playerCarOnMap?.color ?? colorForCarIndex(session?.playerCarIndex ?? 0) }}
                    >
                      {playerCarOnMap?.racingNumber ?? session?.playerCarIndex ?? "—"}
                    </span>
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="text-xl font-bold truncate">
                      {playerLeaderboardEntry?.driverLabel ?? playerCarOnMap?.driverLabel ?? "—"}
                    </div>
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
                <div className="flex items-center justify-between p-2.5 bg-gradient-to-r from-[#A855F7]/10 to-transparent rounded border border-[#A855F7]/20">
                  <div className="flex items-center gap-2">
                    <div className="w-1.5 h-1.5 rounded-full bg-[#A855F7]" />
                    <div className="text-sm text-text-secondary">Sector 1</div>
                  </div>
                  <div className="text-sm font-mono font-bold text-[#A855F7]">
                    {formatSector(playerLeaderboardEntry?.sector1Ms)}
                  </div>
                </div>
                <div className="flex items-center justify-between p-2.5 bg-gradient-to-r from-[#00FF85]/10 to-transparent rounded border border-[#00FF85]/20">
                  <div className="flex items-center gap-2">
                    <div className="w-1.5 h-1.5 rounded-full bg-[#00FF85]" />
                    <div className="text-sm text-text-secondary">Sector 2</div>
                  </div>
                  <div className="text-sm font-mono font-bold text-[#00FF85]">
                    {formatSector(playerLeaderboardEntry?.sector2Ms)}
                  </div>
                </div>
                <div className="flex items-center justify-between p-2.5 bg-gradient-to-r from-[#FACC15]/10 to-transparent rounded border border-[#FACC15]/20">
                  <div className="flex items-center gap-2">
                    <div className="w-1.5 h-1.5 rounded-full bg-[#FACC15]" />
                    <div className="text-sm text-text-secondary">Sector 3</div>
                  </div>
                  <div className="text-sm font-mono font-bold text-[#FACC15]">
                    {formatSector(playerLeaderboardEntry?.sector3Ms)}
                  </div>
                </div>
                <div className="pt-3 border-t border-border/50 flex items-center justify-between p-2 bg-[#00E5FF]/5 rounded-lg">
                  <div className="text-xs text-text-secondary uppercase">Last Lap</div>
                  <div className="text-lg font-mono font-bold text-[#00E5FF]">
                    {formatLapTime(
                      playerLeaderboardEntry?.lastLapTimeMs ??
                        snapshot?.currentLapTimeMs ??
                        playerLeaderboardEntry?.bestLapTimeMs ??
                        snapshot?.bestLapTimeMs
                    )}
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
                <div className="mt-3 pt-3 border-t border-border/30 flex items-center justify-between text-xs">
                  <span>Packet loss:</span>
                  <span className="font-mono text-foreground font-bold">
                    {packetHealthPercent != null ? `${(100 - packetHealthPercent).toFixed(0)}%` : "—"}
                  </span>
                </div>
                <div className="flex items-center justify-between text-xs mt-1">
                  <span>Update rate:</span>
                  <span className="font-mono text-foreground font-bold">
                    {updateRateHz != null ? `${updateRateHz.toFixed(1)} Hz` : "—"}
                  </span>
                </div>
              </div>
            </DataCard>
          </div>
        </div>
      )}
    </div>
  );
}
