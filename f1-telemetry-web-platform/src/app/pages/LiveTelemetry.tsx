import { useState } from "react";
import { DataCard } from "../components/DataCard";
import { StatusBadge } from "../components/StatusBadge";
import { Button } from "../components/ui/button";
import { 
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "../components/ui/select";
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend } from "recharts";
import { Pause, Play } from "lucide-react";

export default function LiveTelemetry() {
  const [isPaused, setIsPaused] = useState(false);
  const [selectedDriver, setSelectedDriver] = useState("player");
  const [timeRange, setTimeRange] = useState("30");

  // Mock telemetry data
  const speedData = Array.from({ length: 30 }, (_, i) => ({
    time: i,
    speed: 250 + Math.sin(i / 3) * 50 + Math.random() * 20,
  }));

  const throttleBrakeData = Array.from({ length: 30 }, (_, i) => ({
    time: i,
    throttle: Math.max(0, Math.min(100, 80 + Math.sin(i / 2) * 30 + Math.random() * 20)),
    brake: i % 8 === 0 ? 80 + Math.random() * 20 : Math.random() * 5,
  }));

  const rpmGearData = Array.from({ length: 30 }, (_, i) => ({
    time: i,
    rpm: 9000 + Math.sin(i / 2.5) * 2000 + Math.random() * 500,
    gear: Math.min(8, Math.max(1, Math.floor(5 + Math.sin(i / 4) * 2))),
  }));

  const tyreTempData = [
    { tyre: "FL", temp: 98 },
    { tyre: "FR", temp: 102 },
    { tyre: "RL", temp: 95 },
    { tyre: "RR", temp: 99 },
  ];

  const ersData = Array.from({ length: 30 }, (_, i) => ({
    time: i,
    ers: Math.max(0, Math.min(100, 60 - i * 1.5 + Math.random() * 5)),
    fuel: Math.max(0, 100 - i * 2),
  }));

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold mb-2">Live Telemetry</h1>
          <p className="text-text-secondary">Real-time telemetry data visualization</p>
        </div>
        <StatusBadge variant="active">Live</StatusBadge>
      </div>

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
                <div className="text-3xl font-bold font-mono text-[#E10600]">{data.temp}°C</div>
                <div className="mt-2 h-2 bg-background rounded-full overflow-hidden">
                  <div 
                    className={`h-full ${
                      data.temp > 100 ? 'bg-[#E10600]' : 
                      data.temp > 90 ? 'bg-[#FACC15]' : 
                      'bg-[#00FF85]'
                    }`}
                    style={{ width: `${Math.min(100, data.temp)}%` }}
                  />
                </div>
              </div>
            ))}
          </div>
        </DataCard>

        <DataCard title="ERS & Fuel">
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
    </div>
  );
}
