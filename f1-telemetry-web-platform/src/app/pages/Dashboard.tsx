import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router";
import { DataCard } from "../components/DataCard";
import { StatusBadge } from "../components/StatusBadge";
import { Button } from "../components/ui/button";
import { Input } from "../components/ui/input";
import { getSessions, getSessionDiagnostics } from "@/api/client";
import { useLiveTelemetry } from "@/ws";
import { getTrackName } from "@/constants/tracks";
import { formatLapTime } from "@/api/format";
import type { Session, PacketHealthBand } from "@/api/types";
import { notify } from "@/notify";
import { Wifi, AlertCircle, Search, Eye } from "lucide-react";

export default function Dashboard() {
  const { status, session: liveSession, errorMessage: liveErrorMessage } = useLiveTelemetry();
  const [sessions, setSessions] = useState<Session[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchTerm, setSearchTerm] = useState("");
  const [packetHealthBand, setPacketHealthBand] = useState<PacketHealthBand>("UNKNOWN");
  const [packetHealthPercent, setPacketHealthPercent] = useState<number | null>(null);
  const [packetHealthAlertShown, setPacketHealthAlertShown] = useState(false);

  const hasSearch = useMemo(() => searchTerm.trim().length > 0, [searchTerm]);

  useEffect(() => {
    if (!liveSession?.id || status !== "live") {
      setPacketHealthBand("UNKNOWN");
      setPacketHealthPercent(null);
      return;
    }

    let cancelled = false;

    async function fetchDiagnosticsOnce() {
      try {
        const diagnostics = await getSessionDiagnostics(liveSession.id);
        if (cancelled) return;
        setPacketHealthBand(diagnostics.packetHealthBand);
        setPacketHealthPercent(
          diagnostics.packetHealthPercent != null ? diagnostics.packetHealthPercent : null
        );

        const healthPercent = diagnostics.packetHealthPercent;
        if (
          healthPercent != null &&
          healthPercent < 70 &&
          !packetHealthAlertShown
        ) {
          notify.warning("High packet loss detected. Live telemetry may be incomplete.");
          setPacketHealthAlertShown(true);
        }
      } catch {
        if (cancelled) return;
        setPacketHealthBand("UNKNOWN");
        setPacketHealthPercent(null);
      }
    }

    fetchDiagnosticsOnce();
    const intervalId = window.setInterval(fetchDiagnosticsOnce, 5000);

    return () => {
      cancelled = true;
      window.clearInterval(intervalId);
    };
  }, [liveSession?.id, status, packetHealthAlertShown]);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    const timeoutId = window.setTimeout(() => {
      getSessions({
        limit: 5,
        offset: 0,
        search: hasSearch ? searchTerm.trim() : undefined,
      })
      .then((res) => {
        if (!cancelled) {
          setSessions(res.sessions);
          setTotal(res.total);
        }
      })
      .catch((err) => {
        if (!cancelled) setError(err instanceof Error ? err.message : "Failed to load sessions");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    }, 350);

    return () => {
      cancelled = true;
      window.clearTimeout(timeoutId);
    };
  }, [hasSearch, searchTerm]);

  const lastSession = sessions[0];
  const lastSessionStartedAtLabel = useMemo(() => {
    if (!lastSession?.startedAt) return "—";
    const date = new Date(lastSession.startedAt);
    if (Number.isNaN(date.getTime())) return "—";
    return date.toLocaleDateString("en-US", {
      weekday: "long",
      month: "long",
      day: "numeric",
    });
  }, [lastSession]);

  const connectionSubtitle = useMemo(() => {
    if (status === "live" && liveSession) {
      const trackName = liveSession.trackDisplayName ?? getTrackName(liveSession.trackId ?? null) ?? "Unknown track";
      const type = liveSession.sessionType ?? "Session";
      return `Live: ${trackName} — ${type}`;
    }
    if (status === "no-data") {
      return "No active session — start a session in F1 25";
    }
    if (status === "error" && liveErrorMessage) {
      return liveErrorMessage.length > 80 ? `${liveErrorMessage.slice(0, 77)}...` : liveErrorMessage;
    }
    if (status === "disconnected") {
      return "Disconnected from live feed — will retry shortly";
    }
    return "Waiting for active session…";
  }, [status, liveSession, liveErrorMessage]);

  const packetHealthLabel = useMemo(() => {
    if (!liveSession || status !== "live" || packetHealthBand === "UNKNOWN" || packetHealthPercent == null) {
      return "—";
    }
    return `${packetHealthPercent.toFixed(0)}%`;
  }, [liveSession, status, packetHealthBand, packetHealthPercent]);

  const packetHealthBarClass = useMemo(() => {
    if (!liveSession || status !== "live" || packetHealthBand === "UNKNOWN" || packetHealthPercent == null) {
      return "bg-[#00FF85]";
    }
    let color = "bg-[#00FF85]";
    if (packetHealthBand === "OK") color = "bg-[#FACC15]";
    if (packetHealthBand === "POOR") color = "bg-[#EF4444]";
    return color;
  }, [liveSession, status, packetHealthBand, packetHealthPercent]);

  const packetHealthBarWidth = useMemo(() => {
    if (!liveSession || status !== "live" || packetHealthBand === "UNKNOWN" || packetHealthPercent == null) {
      return "0%";
    }
    const clamped = Math.max(0, Math.min(100, packetHealthPercent));
    return `${clamped}%`;
  }, [liveSession, status, packetHealthBand, packetHealthPercent]);

  const packetHealthDescription = useMemo(() => {
    if (packetHealthBand === "GOOD") {
      return "Low packet loss (<5%), live telemetry is reliable.";
    }
    if (packetHealthBand === "OK") {
      return "Moderate packet loss (5–20%); small gaps in live data are possible.";
    }
    if (packetHealthBand === "POOR") {
      return "High packet loss (>20%); live telemetry may be incomplete or delayed.";
    }
    return "Packet health is not yet available for this session.";
  }, [packetHealthBand]);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold mb-2">Dashboard</h1>
        <p className="text-text-secondary">Welcome back! Here's your telemetry overview.</p>
      </div>

      {/* Top Cards */}
      <div className="grid lg:grid-cols-3 gap-6">
        {/* Connection Status */}
        <DataCard title="Connection Status" variant="live">
          <div className="space-y-4">
            <div className="flex items-center gap-3">
              <div className="p-2 bg-[#00E5FF]/10 rounded-lg">
                <Wifi className="w-6 h-6 text-[#00E5FF]" />
              </div>
              <div>
                <StatusBadge variant={status === "live" ? "active" : status === "error" ? "error" : status === "no-data" ? "finished" : "warning"}>
                  {status === "live" ? "Live" : status === "waiting" ? "Waiting" : status === "no-data" ? "No Data" : status === "disconnected" ? "Disconnected" : "Error"}
                </StatusBadge>
                <p className="text-sm text-text-secondary mt-1">
                  {connectionSubtitle}
                </p>
              </div>
            </div>
            <div className="pt-4 border-t border-border/50">
              <div className="flex justify-between text-sm mb-1">
                <div className="flex items-center gap-2">
                  <span className="text-text-secondary">Packet health (approx.)</span>
                  <span
                    className="w-2 h-2 rounded-full"
                    aria-hidden="true"
                    style={{ backgroundColor: packetHealthBand === "GOOD" ? "#22C55E" : packetHealthBand === "OK" ? "#FACC15" : packetHealthBand === "POOR" ? "#EF4444" : "#6B7280" }}
                  />
                </div>
                <span className="font-mono text-[#00FF85]" title={packetHealthDescription}>
                  {packetHealthLabel}
                </span>
              </div>
              <div className="h-2 bg-secondary rounded-full overflow-hidden">
                <div
                  className={`h-full ${packetHealthBarClass}`}
                  style={{ width: packetHealthBarWidth }}
                />
              </div>
            </div>
            <Link to="/app/settings">
              <Button variant="link" className="text-[#00E5FF] p-0 h-auto">
                Troubleshoot UDP →
              </Button>
            </Link>
          </div>
        </DataCard>

        {/* Last Session */}
        <DataCard title="Last Session">
          {sessions.length > 0 ? (
            <div className="space-y-3">
              <div>
                <div className="text-2xl font-bold">
                  {lastSession.trackDisplayName ?? getTrackName(lastSession.trackId ?? null) ?? "—"}
                </div>
                <div className="text-text-secondary">{lastSession.sessionType ?? "—"}</div>
              </div>
              <div className="grid grid-cols-2 gap-4 pt-3 border-t border-border/50">
                <div>
                  <div className="text-xs text-text-secondary uppercase">Best Lap</div>
                  <div className="text-lg font-bold font-mono text-[#00E5FF]">
                    {formatLapTime(lastSession.bestLapTimeMs ?? null)}
                  </div>
                </div>
                <div>
                  <div className="text-xs text-text-secondary uppercase">Result</div>
                  <div className="text-lg font-bold text-[#00FF85]">
                    {lastSession.finishingPosition != null ? `P${lastSession.finishingPosition}` : "—"}
                  </div>
                </div>
              </div>
              <div className="text-sm text-text-secondary">
                {lastSessionStartedAtLabel}
              </div>
              <Link to={`/app/sessions/${lastSession.id}`}>
                <Button variant="link" className="text-[#00E5FF] p-0 h-auto">View session →</Button>
              </Link>
            </div>
          ) : (
            <div className="space-y-2 text-sm text-text-secondary">
              <p>No sessions recorded yet — start your first session to see recent history here.</p>
              <Link to="/app/settings">
                <Button variant="link" className="text-[#00E5FF] p-0 h-auto">
                  Learn how to connect →
                </Button>
              </Link>
            </div>
          )}
        </DataCard>

        {/* Quick Compare */}
        <DataCard title="Quick Compare">
          <div className="space-y-4">
            <p className="text-sm text-text-secondary">
              {status === "live" && liveSession
                ? "Compare your performance with other drivers in the current live session."
                : sessions.length > 0
                  ? "Compare your performance with other drivers from your recorded sessions."
                  : "Once you record some sessions, you can compare your performance with other drivers."}
            </p>
            <div className="flex gap-2">
              <Link to="/app/comparison" className="flex-1">
                <Button className="w-full bg-[#00E5FF] hover:bg-[#00E5FF]/90 text-background">
                  Open Comparison
                </Button>
              </Link>
            </div>
            <div className="pt-3 border-t border-border/50 text-xs text-text-secondary">
              <div className="flex items-center gap-2">
                <AlertCircle className="w-4 h-4" />
                <span>
                  {status === "live" && liveSession
                    ? "Uses the current live session for comparison."
                    : sessions.length > 0
                      ? "Uses your recorded sessions when available."
                      : "Start a session to unlock driver comparison."}
                </span>
              </div>
            </div>
          </div>
        </DataCard>
      </div>

      {/* Session History Table */}
      <DataCard title="Previous Sessions" noPadding>
        <div className="p-4 border-b border-border/50">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-text-secondary" />
            <Input
              placeholder="Search sessions..."
              className="pl-10 bg-input-background border-border"
              value={searchTerm}
              onChange={(event) => setSearchTerm(event.target.value)}
            />
          </div>
        </div>

        {loading && (
          <div className="p-8 text-center text-text-secondary">Loading sessions…</div>
        )}
        {error && (
          <div className="p-8 text-center">
            <p className="text-[#EF4444] mb-2">{error}</p>
            <Button variant="outline" size="sm" onClick={() => window.location.reload()}>
              Retry
            </Button>
          </div>
        )}
        {!loading && !error && (
          <>
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-secondary/50 border-b border-border/50">
              <tr>
                <th className="px-4 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                  Track
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                  Date
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                  Session Type
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                  Best Lap
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                  Result
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border/30">
              {sessions.length === 0 ? (
                <tr>
                  <td colSpan={6} className="px-4 py-8 text-center text-text-secondary">
                    {hasSearch ? "No sessions match your search" : "No sessions yet"}
                  </td>
                </tr>
              ) : (
                sessions.map((session) => (
                  <tr key={session.id} className="hover:bg-secondary/30 transition-colors">
                    <td className="px-4 py-4 font-medium">
                      {session.trackDisplayName ?? getTrackName(session.trackId ?? null) ?? "—"}
                    </td>
                    <td className="px-4 py-4 text-text-secondary">
                      {new Date(session.startedAt).toLocaleDateString("en-US", {
                        month: "short",
                        day: "numeric",
                        year: "numeric",
                      })}
                    </td>
                    <td className="px-4 py-4">
                      <span className="inline-flex px-2 py-1 rounded text-xs bg-secondary/50 border border-border">
                        {session.sessionType ?? "—"}
                      </span>
                    </td>
                    <td className="px-4 py-4 font-mono text-[#00E5FF]">
                      {formatLapTime(session.bestLapTimeMs ?? null)}
                    </td>
                    <td className="px-4 py-4 font-bold text-[#00FF85]">
                      {session.finishingPosition != null ? `P${session.finishingPosition}` : "—"}
                    </td>
                    <td className="px-4 py-4">
                      <Link to={`/app/sessions/${session.id}`}>
                        <Button variant="ghost" size="sm" className="h-8 gap-2">
                          <Eye className="w-4 h-4" />
                          View
                        </Button>
                      </Link>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        <div className="p-4 border-t border-border/50 flex items-center justify-between">
          <div className="text-sm text-text-secondary">
            Showing {sessions.length} of {total} sessions
          </div>
          <Link to="/app/sessions">
            <Button variant="link" className="text-[#00E5FF]">
              View all sessions →
            </Button>
          </Link>
        </div>
          </>
        )}
      </DataCard>
    </div>
  );
}
