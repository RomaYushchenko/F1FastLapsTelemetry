import { useEffect, useState } from "react";
import { Link } from "react-router";
import { DataCard } from "../components/DataCard";
import { StatusBadge } from "../components/StatusBadge";
import { Button } from "../components/ui/button";
import { Input } from "../components/ui/input";
import { getSessions } from "@/api/client";
import { useLiveTelemetry } from "@/ws";
import { getTrackName } from "@/constants/tracks";
import { formatLapTime } from "@/api/format";
import type { Session } from "@/api/types";
import { Wifi, AlertCircle, Search, Eye } from "lucide-react";

export default function Dashboard() {
  const { status } = useLiveTelemetry();
  const [sessions, setSessions] = useState<Session[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    getSessions({ limit: 5, offset: 0 })
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
    return () => { cancelled = true; };
  }, []);

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
                <StatusBadge variant={status === "live" ? "active" : status === "error" ? "error" : "warning"}>
                  {status === "live" ? "Live" : status === "waiting" ? "Waiting" : status === "no-data" ? "No Data" : status === "disconnected" ? "Disconnected" : "Error"}
                </StatusBadge>
                <p className="text-sm text-text-secondary mt-1">
                  {status === "live" ? "Connected to F1 25" : status === "no-data" ? "No active session" : "Connecting…"}
                </p>
              </div>
            </div>
            <div className="pt-4 border-t border-border/50">
              <div className="flex justify-between text-sm mb-1">
                <span className="text-text-secondary">Packet Loss</span>
                <span className="font-mono text-[#00FF85]">0.2%</span>
              </div>
              <div className="h-2 bg-secondary rounded-full overflow-hidden">
                <div className="h-full w-[0.2%] bg-[#00FF85]" />
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
                  {sessions[0].trackDisplayName ?? getTrackName(sessions[0].trackId ?? null) ?? "—"}
                </div>
                <div className="text-text-secondary">{sessions[0].sessionType ?? "—"}</div>
              </div>
              <div className="grid grid-cols-2 gap-4 pt-3 border-t border-border/50">
                <div>
                  <div className="text-xs text-text-secondary uppercase">Best Lap</div>
                  <div className="text-lg font-bold font-mono text-[#00E5FF]">
                    {formatLapTime(sessions[0].bestLapTimeMs ?? null)}
                  </div>
                </div>
                <div>
                  <div className="text-xs text-text-secondary uppercase">Result</div>
                  <div className="text-lg font-bold text-[#00FF85]">
                    {sessions[0].finishingPosition != null ? `P${sessions[0].finishingPosition}` : "—"}
                  </div>
                </div>
              </div>
              <div className="text-sm text-text-secondary">
                {new Date(sessions[0].startedAt).toLocaleDateString("en-US", {
                  weekday: "long",
                  month: "long",
                  day: "numeric",
                })}
              </div>
              <Link to={`/app/sessions/${sessions[0].id}`}>
                <Button variant="link" className="text-[#00E5FF] p-0 h-auto">View session →</Button>
              </Link>
            </div>
          ) : (
            <p className="text-sm text-text-secondary">No sessions yet</p>
          )}
        </DataCard>

        {/* Quick Compare */}
        <DataCard title="Quick Compare">
          <div className="space-y-4">
            <p className="text-sm text-text-secondary">
              Compare your performance with other drivers in the session
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
                <span>Requires an active session</span>
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
                    No sessions yet
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
