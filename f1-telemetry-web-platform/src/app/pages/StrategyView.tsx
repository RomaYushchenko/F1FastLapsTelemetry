import { useCallback, useEffect, useState } from "react";
import { useParams, Link } from "react-router";
import { DataCard } from "../components/DataCard";
import { Button } from "../components/ui/button";
import { Skeleton } from "../components/ui/skeleton";
import { getSession, getPitStops, getStints } from "@/api/client";
import { formatLapTime } from "@/api/format";
import { HttpError } from "@/api/types";
import type { Session } from "@/api/types";
import type { PitStopDto, StintDto } from "@/api/types";
import { getCompoundLabel } from "@/constants/compounds";
import { isValidSessionId } from "@/api/sessionId";
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
} from "recharts";

export default function StrategyView() {
  const { id } = useParams<{ id: string }>();
  const [session, setSession] = useState<Session | null>(null);
  const [pitStops, setPitStops] = useState<PitStopDto[]>([]);
  const [stints, setStints] = useState<StintDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [notFound, setNotFound] = useState(false);

  const carIndex = session?.playerCarIndex ?? 0;
  const totalLaps = session?.totalLaps ?? null;

  const fetchData = useCallback(async () => {
    if (!id || !isValidSessionId(id)) return;
    setLoading(true);
    setError(null);
    setNotFound(false);
    try {
      const sessionRes = await getSession(id);
      setSession(sessionRes);
      const carIdx = sessionRes.playerCarIndex ?? 0;
      const [pitStopsRes, stintsRes] = await Promise.all([
        getPitStops(id, carIdx),
        getStints(id, carIdx),
      ]);
      setPitStops(pitStopsRes);
      setStints(stintsRes);
    } catch (e) {
      if (e instanceof HttpError && e.status === 404) {
        setNotFound(true);
        setSession(null);
        setPitStops([]);
        setStints([]);
      } else {
        setError(e instanceof Error ? e.message : "Failed to load strategy");
      }
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    if (!id || !isValidSessionId(id)) return;
    fetchData();
  }, [id, fetchData]);

  // Mock data for charts that stay mock (Block D scope)
  const fuelConsumption = Array.from({ length: 30 }, (_, i) => ({
    lap: i + 1,
    fuel: 100 - i * 3.2,
  }));
  const ersDeployment = Array.from({ length: 30 }, (_, i) => ({
    lap: i + 1,
    deployed: 20 + Math.random() * 40,
    harvested: 15 + Math.random() * 30,
  }));
  const tyreDegradation = Array.from({ length: 15 }, (_, i) => ({
    lap: i + 1,
    performance: 100 - i * 3.5 - Math.random() * 5,
  }));

  const timelineMaxLaps = totalLaps ?? (pitStops.length > 0 ? Math.max(...pitStops.map((p) => p.lapNumber)) : 30);

  if (!id) {
    return (
      <div className="space-y-6">
        <p className="text-text-secondary">Open Strategy from a session (Session Details → Strategy).</p>
        <Link to="/app/sessions">
          <Button variant="outline">Session History</Button>
        </Link>
      </div>
    );
  }

  if (!isValidSessionId(id)) {
    return (
      <div className="space-y-6">
        <p className="text-destructive">Invalid session id</p>
        <Link to="/app/sessions">
          <Button variant="outline">Session History</Button>
        </Link>
      </div>
    );
  }

  if (notFound) {
    return (
      <div className="space-y-6">
        <p className="text-text-secondary">Session not found</p>
        <Link to="/app/sessions">
          <Button variant="outline">Session History</Button>
        </Link>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-12 w-64" />
        <Skeleton className="h-32 w-full" />
        <Skeleton className="h-64 w-full" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="space-y-6">
        <p className="text-destructive">{error}</p>
        <Button onClick={fetchData} variant="outline">
          Retry
        </Button>
        <Link to="/app/sessions">
          <Button variant="ghost">Session History</Button>
        </Link>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <Link to={`/app/sessions/${id}`}>
          <Button variant="ghost" size="sm">
            ← Session Details
          </Button>
        </Link>
        <div>
          <h1 className="text-3xl font-bold mb-2">Strategy View</h1>
          <p className="text-text-secondary">Analyze tyre strategy, fuel management, and ERS deployment</p>
        </div>
      </div>

      {/* Overview Cards */}
      <div className="grid md:grid-cols-4 gap-6">
        <DataCard>
          <div className="space-y-2">
            <div className="text-xs text-text-secondary uppercase">Total Pit Stops</div>
            <div className="text-3xl font-bold text-[#00E5FF]">{pitStops.length}</div>
            <div className="text-sm text-text-secondary">
              {pitStops.length > 0 ? "From session data" : "No pit stops"}
            </div>
          </div>
        </DataCard>
        <DataCard>
          <div className="space-y-2">
            <div className="text-xs text-text-secondary uppercase">Fuel Strategy</div>
            <div className="text-3xl font-bold text-[#00FF85]">Optimal</div>
            <div className="text-sm text-text-secondary">+2.1% margin (mock)</div>
          </div>
        </DataCard>
        <DataCard>
          <div className="space-y-2">
            <div className="text-xs text-text-secondary uppercase">Tyre Management</div>
            <div className="text-3xl font-bold text-[#FACC15]">
              {stints.length} {stints.length === 1 ? "stint" : "stints"}
            </div>
            <div className="text-sm text-text-secondary">From session data</div>
          </div>
        </DataCard>
        <DataCard>
          <div className="space-y-2">
            <div className="text-xs text-text-secondary uppercase">ERS Efficiency</div>
            <div className="text-3xl font-bold">94%</div>
            <div className="text-sm text-text-secondary">Well balanced (mock)</div>
          </div>
        </DataCard>
      </div>

      {/* Tyre Degradation — mock */}
      <DataCard title="Tyre Degradation (Current Stint)">
        <ResponsiveContainer width="100%" height={350}>
          <LineChart data={tyreDegradation}>
            <CartesianGrid strokeDasharray="3 3" stroke="rgba(249,250,251,0.06)" />
            <XAxis dataKey="lap" stroke="#9CA3AF" label={{ value: "Lap on Current Tyres", position: "insideBottom", offset: -5, fill: "#9CA3AF" }} />
            <YAxis stroke="#9CA3AF" domain={[0, 100]} label={{ value: "Performance (%)", angle: -90, position: "insideLeft", fill: "#9CA3AF" }} />
            <Tooltip contentStyle={{ backgroundColor: "#1F2937", border: "1px solid rgba(249,250,251,0.08)", borderRadius: "8px" }} formatter={(value: number) => `${value.toFixed(1)}%`} />
            <Line type="monotone" dataKey="performance" stroke="#E10600" strokeWidth={3} dot={{ fill: "#E10600", r: 4 }} />
          </LineChart>
        </ResponsiveContainer>
        <div className="mt-6 p-4 bg-[#FACC15]/10 border border-[#FACC15]/30 rounded-lg">
          <div className="flex items-center gap-2 text-[#FACC15] font-semibold mb-2">
            <span>⚠️</span>
            <span>Strategy Recommendation</span>
          </div>
          <p className="text-sm text-text-secondary">Tyre degradation chart is mock data. Consider pitting when tyre performance drops.</p>
        </div>
      </DataCard>

      {/* Pit Stop Timeline */}
      <DataCard title="Pit Stop Timeline">
        {pitStops.length === 0 ? (
          <div className="py-8 text-center text-text-secondary">No pit stops for this session</div>
        ) : (
          <>
            <div className="relative h-20 bg-secondary/30 rounded-lg mb-6">
              <div className="absolute inset-0 flex items-center px-4">
                <div className="flex-1 h-1 bg-border" />
              </div>
              {pitStops.map((stop, idx) => (
                <div
                  key={idx}
                  className="absolute top-1/2 -translate-y-1/2 -translate-x-1/2"
                  style={{ left: `${(stop.lapNumber / Math.max(1, timelineMaxLaps)) * 100}%` }}
                >
                  <div className="w-4 h-4 bg-[#00E5FF] rounded-full border-4 border-background" />
                  <div className="absolute top-6 left-1/2 -translate-x-1/2 whitespace-nowrap">
                    <div className="text-xs font-bold">Lap {stop.lapNumber}</div>
                  </div>
                </div>
              ))}
            </div>
            <div className="space-y-3">
              {pitStops.map((stop, idx) => (
                <div key={idx} className="p-4 bg-secondary/30 rounded-lg border border-border">
                  <div className="flex items-center justify-between mb-3">
                    <div className="font-bold text-lg">
                      Pit Stop {idx + 1} — Lap {stop.lapNumber}
                    </div>
                    <div className="text-[#00E5FF] font-mono font-bold">
                      {stop.pitDurationMs != null ? `${(stop.pitDurationMs / 1000).toFixed(1)}s` : "—"}
                    </div>
                  </div>
                  <div className="grid grid-cols-3 gap-4 text-sm">
                    <div>
                      <div className="text-text-secondary">In Lap</div>
                      <div className="font-mono">{formatLapTime(stop.inLapTimeMs)}</div>
                    </div>
                    <div>
                      <div className="text-text-secondary">Out Lap</div>
                      <div className="font-mono">{formatLapTime(stop.outLapTimeMs)}</div>
                    </div>
                    <div>
                      <div className="text-text-secondary">Tyre Change</div>
                      <div className="font-semibold">
                        {getCompoundLabel(stop.compoundIn)} → {getCompoundLabel(stop.compoundOut)}
                      </div>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </>
        )}
      </DataCard>

      {/* Stint Comparison */}
      <DataCard title="Stint Performance Comparison">
        {stints.length === 0 ? (
          <div className="py-8 text-center text-text-secondary">No stint data for this session</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-secondary/50 border-b border-border/50">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">Stint</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">Avg Lap Time</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">Laps</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">Degradation</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border/30">
                {stints.map((stint) => (
                  <tr key={stint.stintIndex} className="hover:bg-secondary/30 transition-colors">
                    <td className="px-4 py-4 font-semibold">
                      Stint {stint.stintIndex} ({getCompoundLabel(stint.compound)})
                    </td>
                    <td className="px-4 py-4 font-mono text-[#00E5FF]">
                      {stint.avgLapTimeMs != null ? `${(stint.avgLapTimeMs / 1000).toFixed(3)}s` : "—"}
                    </td>
                    <td className="px-4 py-4">{stint.lapCount}</td>
                    <td className="px-4 py-4">
                      {stint.degradationIndicator != null ? (
                        <span
                          className={`inline-flex px-2 py-1 rounded text-xs font-medium ${
                            stint.degradationIndicator === "high"
                              ? "bg-[#E10600]/20 text-[#E10600]"
                              : stint.degradationIndicator === "medium"
                                ? "bg-[#FACC15]/20 text-[#FACC15]"
                                : "bg-[#00FF85]/20 text-[#00FF85]"
                          }`}
                        >
                          {stint.degradationIndicator}
                        </span>
                      ) : (
                        "—"
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </DataCard>

      {/* Fuel Consumption — mock */}
      <DataCard title="Fuel Consumption">
        <ResponsiveContainer width="100%" height={300}>
          <LineChart data={fuelConsumption}>
            <CartesianGrid strokeDasharray="3 3" stroke="rgba(249,250,251,0.06)" />
            <XAxis dataKey="lap" stroke="#9CA3AF" label={{ value: "Lap", position: "insideBottom", offset: -5, fill: "#9CA3AF" }} />
            <YAxis stroke="#9CA3AF" domain={[0, 100]} label={{ value: "Fuel Remaining (%)", angle: -90, position: "insideLeft", fill: "#9CA3AF" }} />
            <Tooltip contentStyle={{ backgroundColor: "#1F2937", border: "1px solid rgba(249,250,251,0.08)", borderRadius: "8px" }} formatter={(value: number) => `${value.toFixed(1)}%`} />
            <Line type="monotone" dataKey="fuel" stroke="#00E5FF" strokeWidth={2} dot={false} />
          </LineChart>
        </ResponsiveContainer>
        <div className="mt-4 grid grid-cols-3 gap-4">
          <div className="p-3 bg-secondary/30 rounded-lg">
            <div className="text-xs text-text-secondary uppercase mb-1">Avg Consumption</div>
            <div className="text-lg font-bold font-mono">3.2%<span className="text-sm text-text-secondary">/lap</span></div>
          </div>
          <div className="p-3 bg-secondary/30 rounded-lg">
            <div className="text-xs text-text-secondary uppercase mb-1">Predicted Finish</div>
            <div className="text-lg font-bold font-mono text-[#00FF85]">2.1%</div>
          </div>
          <div className="p-3 bg-secondary/30 rounded-lg">
            <div className="text-xs text-text-secondary uppercase mb-1">Fuel Margin</div>
            <div className="text-lg font-bold text-[#00FF85]">Safe (mock)</div>
          </div>
        </div>
      </DataCard>

      {/* ERS — mock */}
      <DataCard title="ERS Deployment & Harvesting">
        <ResponsiveContainer width="100%" height={300}>
          <BarChart data={ersDeployment}>
            <CartesianGrid strokeDasharray="3 3" stroke="rgba(249,250,251,0.06)" />
            <XAxis dataKey="lap" stroke="#9CA3AF" />
            <YAxis stroke="#9CA3AF" label={{ value: "Energy (%)", angle: -90, position: "insideLeft", fill: "#9CA3AF" }} />
            <Tooltip contentStyle={{ backgroundColor: "#1F2937", border: "1px solid rgba(249,250,251,0.08)", borderRadius: "8px" }} formatter={(value: number) => `${value.toFixed(1)}%`} />
            <Legend />
            <Bar dataKey="deployed" fill="#A855F7" name="Deployed" />
            <Bar dataKey="harvested" fill="#00FF85" name="Harvested" />
          </BarChart>
        </ResponsiveContainer>
      </DataCard>
    </div>
  );
}
