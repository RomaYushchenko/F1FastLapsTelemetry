import { useState, useMemo, useEffect, useRef } from "react";
import { Link } from "react-router";
import { DataCard } from "../components/DataCard";
import { StatusBadge } from "../components/StatusBadge";
import { useLiveTelemetry, TYRE_LABELS } from "@/ws";
import { Button } from "../components/ui/button";
import {
  Tooltip as UITooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "../components/ui/tooltip";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "../components/ui/select";
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend } from "recharts";
import { Pause, Play } from "lucide-react";
import { formatLapTime, formatSessionTime } from "@/api/format";

/** Expected snapshot rate from backend (20 Hz). Used for chart buffer length. */
const SNAPSHOT_HZ = 20;

interface ChartPoint {
  time: number;
  speed?: number;
  throttle?: number;
  brake?: number;
  rpm?: number;
  gear?: number;
  ers?: number;
  fuel?: number;
}

export default function LiveTelemetry() {
  const { session, snapshot, status, sessionEnded } = useLiveTelemetry();
  const [isPaused, setIsPaused] = useState(false);
  const [selectedDriver, setSelectedDriver] = useState("player");
  const [timeRange, setTimeRange] = useState("30");
  const [buffer, setBuffer] = useState<ChartPoint[]>([]);
  const startTimeRef = useRef<number | null>(null);

  const noActiveSession = status === "no-data" || session == null;
  const maxBufferLength = parseInt(timeRange, 10) * SNAPSHOT_HZ;

  useEffect(() => {
    if (noActiveSession || !snapshot || isPaused) return;
    const t = startTimeRef.current ?? (snapshot.timestamp ? new Date(snapshot.timestamp).getTime() : Date.now());
    if (startTimeRef.current == null) startTimeRef.current = t;
    const timeSec = (Date.now() - t) / 1000;
    setBuffer((prev) => {
      const next = [
        ...prev,
        {
          time: Math.round(timeSec * 10) / 10,
          speed: snapshot.speedKph ?? undefined,
          throttle: snapshot.throttle != null ? snapshot.throttle * 100 : undefined,
          brake: snapshot.brake != null ? snapshot.brake * 100 : undefined,
          rpm: snapshot.engineRpm ?? undefined,
          gear: snapshot.gear ?? undefined,
          ers: snapshot.ersEnergyPercent ?? undefined,
          fuel: snapshot.fuelRemainingPercent ?? undefined,
        },
      ];
      return next.slice(-maxBufferLength);
    });
  }, [snapshot, isPaused, noActiveSession, maxBufferLength]);

  useEffect(() => {
    if (noActiveSession) {
      setBuffer([]);
      startTimeRef.current = null;
    }
  }, [noActiveSession]);

  const tyreTempData = useMemo(() => {
    const t = snapshot?.tyresSurfaceTempC;
    if (t && t.length >= 4) {
      return TYRE_LABELS.map((tyre, i) => ({ tyre, temp: t[i] as number }));
    }
    return TYRE_LABELS.map((tyre) => ({ tyre, temp: null as number | null }));
  }, [snapshot?.tyresSurfaceTempC]);

  const fuelPercent = snapshot?.fuelRemainingPercent;

  const chartData = buffer.length > 0 ? buffer : [];
  const emptyChartData = [{ time: 0, speed: 0, throttle: 0, brake: 0, rpm: 0, gear: 0, ers: 0, fuel: 0 }];
  const speedData = chartData.length ? chartData : emptyChartData;
  const throttleBrakeData = chartData.length ? chartData : emptyChartData;
  const rpmGearData = chartData.length ? chartData : emptyChartData;
  const ersData = chartData.length ? chartData : emptyChartData;

  return (
    <div className="space-y-6 relative">
      {sessionEnded && (
        <div className="rounded-lg border border-[#FACC15]/50 bg-[#FACC15]/10 px-4 py-2 text-sm text-text-secondary">
          Session ended. Last snapshot still visible below.
        </div>
      )}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold mb-2">Live Telemetry</h1>
          <p className="text-text-secondary">Real-time telemetry data visualization</p>
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
      {/* Controls */}
      <DataCard>
        <div className="flex flex-wrap items-center gap-4">
          <div className="flex-1 min-w-[200px]">
            <label className="text-xs text-text-secondary uppercase mb-2 block">Driver</label>
            <Select value={selectedDriver} onValueChange={setSelectedDriver}>
              <SelectTrigger className="bg-input-background border-border">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="player">You (VER)</SelectItem>
                <SelectItem value="ham">HAM</SelectItem>
                <SelectItem value="lec">LEC</SelectItem>
                <SelectItem value="nor">NOR</SelectItem>
              </SelectContent>
            </Select>
          </div>

          <div className="flex-1 min-w-[200px]">
            <label className="text-xs text-text-secondary uppercase mb-2 block">Time Range</label>
            <Select value={timeRange} onValueChange={setTimeRange}>
              <SelectTrigger className="bg-input-background border-border">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="10">Last 10 seconds</SelectItem>
                <SelectItem value="30">Last 30 seconds</SelectItem>
                <SelectItem value="60">Last 60 seconds</SelectItem>
              </SelectContent>
            </Select>
          </div>

          <div className="flex items-end">
            <Button
              variant="outline"
              size="icon"
              onClick={() => setIsPaused(!isPaused)}
              className="h-10 w-10"
            >
              {isPaused ? <Play className="w-4 h-4" /> : <Pause className="w-4 h-4" />}
            </Button>
          </div>
        </div>
      </DataCard>

      {/* Session time, current lap, delta */}
      <DataCard title="Time & Delta" variant="live">
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <div>
            <div className="text-xs text-text-secondary uppercase mb-1">Session time</div>
            <div className="text-xl font-bold font-mono text-[#00E5FF]">
              {formatSessionTime(snapshot?.sessionTimeSeconds ?? null)}
            </div>
          </div>
          <div>
            <div className="text-xs text-text-secondary uppercase mb-1">Current lap</div>
            <div className="text-xl font-bold font-mono">
              {formatLapTime(snapshot?.currentLapTimeMs ?? null)}
            </div>
          </div>
          <div>
            <div className="text-xs text-text-secondary uppercase mb-1">Delta to best</div>
            <div className={`text-xl font-bold font-mono ${
              snapshot?.deltaMs != null
                ? snapshot.deltaMs <= 0
                  ? "text-[#00FF85]"
                  : "text-[#E10600]"
                : ""
            }`}>
              {snapshot?.deltaMs != null
                ? (snapshot.deltaMs <= 0 ? "-" : "+") + formatLapTime(Math.abs(snapshot.deltaMs))
                : "—"}
            </div>
          </div>
          <div>
            <div className="text-xs text-text-secondary uppercase mb-1">Best lap</div>
            <div className="text-xl font-bold font-mono text-[#A855F7]">
              {formatLapTime(snapshot?.bestLapTimeMs ?? null)}
            </div>
          </div>
        </div>
      </DataCard>

      {/* Speed Graph */}
      <DataCard title="Speed" variant="live">
        <ResponsiveContainer width="100%" height={300}>
          <LineChart data={speedData}>
            <CartesianGrid strokeDasharray="3 3" stroke="rgba(249,250,251,0.06)" />
            <XAxis 
              dataKey="time" 
              stroke="#9CA3AF"
              label={{ value: 'Time (s)', position: 'insideBottom', offset: -5, fill: '#9CA3AF' }}
            />
            <YAxis 
              stroke="#9CA3AF"
              label={{ value: 'Speed (km/h)', angle: -90, position: 'insideLeft', fill: '#9CA3AF' }}
            />
            <Tooltip 
              contentStyle={{ 
                backgroundColor: '#1F2937', 
                border: '1px solid rgba(249,250,251,0.08)',
                borderRadius: '8px'
              }}
            />
            <Line 
              type="monotone" 
              dataKey="speed" 
              stroke="#00E5FF" 
              strokeWidth={2}
              dot={false}
              isAnimationActive={false}
            />
          </LineChart>
        </ResponsiveContainer>
      </DataCard>

      {/* Throttle & Brake */}
      <DataCard title="Throttle & Brake">
        <ResponsiveContainer width="100%" height={300}>
          <LineChart data={throttleBrakeData}>
            <CartesianGrid strokeDasharray="3 3" stroke="rgba(249,250,251,0.06)" />
            <XAxis 
              dataKey="time" 
              stroke="#9CA3AF"
              label={{ value: 'Time (s)', position: 'insideBottom', offset: -5, fill: '#9CA3AF' }}
            />
            <YAxis 
              stroke="#9CA3AF"
              label={{ value: 'Input (%)', angle: -90, position: 'insideLeft', fill: '#9CA3AF' }}
            />
            <Tooltip 
              contentStyle={{ 
                backgroundColor: '#1F2937', 
                border: '1px solid rgba(249,250,251,0.08)',
                borderRadius: '8px'
              }}
            />
            <Legend />
            <Line 
              type="monotone" 
              dataKey="throttle" 
              stroke="#00FF85" 
              strokeWidth={2}
              dot={false}
              name="Throttle"
              isAnimationActive={false}
            />
            <Line 
              type="monotone" 
              dataKey="brake" 
              stroke="#E10600" 
              strokeWidth={2}
              dot={false}
              name="Brake"
              isAnimationActive={false}
            />
          </LineChart>
        </ResponsiveContainer>
      </DataCard>

      {/* RPM & Gear */}
      <div className="grid lg:grid-cols-2 gap-6">
        <DataCard title="RPM">
          <ResponsiveContainer width="100%" height={250}>
            <LineChart data={rpmGearData}>
              <CartesianGrid strokeDasharray="3 3" stroke="rgba(249,250,251,0.06)" />
              <XAxis dataKey="time" stroke="#9CA3AF" />
              <YAxis stroke="#9CA3AF" />
              <Tooltip 
                contentStyle={{ 
                  backgroundColor: '#1F2937', 
                  border: '1px solid rgba(249,250,251,0.08)',
                  borderRadius: '8px'
                }}
              />
              <Line 
                type="monotone" 
                dataKey="rpm" 
                stroke="#FACC15" 
                strokeWidth={2}
                dot={false}
                isAnimationActive={false}
              />
            </LineChart>
          </ResponsiveContainer>
        </DataCard>

        <DataCard title="Gear">
          <ResponsiveContainer width="100%" height={250}>
            <LineChart data={rpmGearData}>
              <CartesianGrid strokeDasharray="3 3" stroke="rgba(249,250,251,0.06)" />
              <XAxis dataKey="time" stroke="#9CA3AF" />
              <YAxis stroke="#9CA3AF" domain={[0, 8]} />
              <Tooltip 
                contentStyle={{ 
                  backgroundColor: '#1F2937', 
                  border: '1px solid rgba(249,250,251,0.08)',
                  borderRadius: '8px'
                }}
              />
              <Line 
                type="stepAfter" 
                dataKey="gear" 
                stroke="#A855F7" 
                strokeWidth={2}
                dot={false}
                isAnimationActive={false}
              />
            </LineChart>
          </ResponsiveContainer>
        </DataCard>
      </div>

      {/* Tyre Temps & ERS/Fuel */}
      <div className="grid lg:grid-cols-2 gap-6">
        <DataCard title="Tyre Temperatures">
          <div className="grid grid-cols-2 gap-4">
            {tyreTempData.map((data) => (
              <div key={data.tyre} className="p-4 bg-secondary/30 rounded-lg border border-border">
                <div className="text-xs text-text-secondary uppercase mb-2">{data.tyre}</div>
                <div className="text-3xl font-bold font-mono text-[#E10600]">
                  {data.temp != null ? `${data.temp}°C` : "—"}
                </div>
                <div className="mt-2 h-2 bg-background rounded-full overflow-hidden">
                  {data.temp != null && (
                    <div 
                      className={`h-full ${
                        data.temp > 100 ? 'bg-[#E10600]' : 
                        data.temp > 90 ? 'bg-[#FACC15]' : 
                        'bg-[#00FF85]'
                      }`}
                      style={{ width: `${Math.min(100, data.temp)}%` }}
                    />
                  )}
                </div>
              </div>
            ))}
          </div>
          {tyreTempData.every((d) => d.temp == null) && (
            <TooltipProvider>
              <UITooltip>
                <TooltipTrigger asChild>
                  <p className="text-xs text-text-secondary mt-2 cursor-help">Live tyre/fuel coming soon (—)</p>
                </TooltipTrigger>
                <TooltipContent>
                  <p>Live tyre temperatures and fuel will be available in a follow-up update.</p>
                </TooltipContent>
              </UITooltip>
            </TooltipProvider>
          )}
        </DataCard>

        <DataCard title="ERS & Fuel">
          {(snapshot?.ersEnergyPercent != null || fuelPercent != null) && (
            <div className="flex gap-6 mb-4 pb-4 border-b border-border/50">
              {snapshot?.ersEnergyPercent != null && (
                <div>
                  <div className="text-xs text-text-secondary uppercase">ERS</div>
                  <div className="text-2xl font-bold font-mono text-[#A855F7]">{snapshot.ersEnergyPercent}%</div>
                </div>
              )}
              {fuelPercent != null && (
                <div>
                  <div className="text-xs text-text-secondary uppercase">Fuel</div>
                  <div className="text-2xl font-bold font-mono text-[#00E5FF]">{fuelPercent}%</div>
                </div>
              )}
            </div>
          )}
          <ResponsiveContainer width="100%" height={250}>
            <LineChart data={ersData}>
              <CartesianGrid strokeDasharray="3 3" stroke="rgba(249,250,251,0.06)" />
              <XAxis dataKey="time" stroke="#9CA3AF" />
              <YAxis stroke="#9CA3AF" />
              <Tooltip 
                contentStyle={{ 
                  backgroundColor: '#1F2937', 
                  border: '1px solid rgba(249,250,251,0.08)',
                  borderRadius: '8px'
                }}
              />
              <Legend />
              <Line 
                type="monotone" 
                dataKey="ers" 
                stroke="#A855F7" 
                strokeWidth={2}
                dot={false}
                name="ERS %"
                isAnimationActive={false}
              />
              <Line 
                type="monotone" 
                dataKey="fuel" 
                stroke="#00E5FF" 
                strokeWidth={2}
                dot={false}
                name="Fuel %"
                isAnimationActive={false}
              />
            </LineChart>
          </ResponsiveContainer>
        </DataCard>
      </div>
        </>
      )}
    </div>
  );
}
