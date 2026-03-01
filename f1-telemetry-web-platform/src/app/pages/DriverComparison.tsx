import { DataCard } from "../components/DataCard";
import { 
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "../components/ui/select";
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend, BarChart, Bar } from "recharts";

export default function DriverComparison() {
  // Mock comparison data
  const lapTimeComparison = Array.from({ length: 20 }, (_, i) => ({
    lap: i + 1,
    driverA: 87.4 + Math.random() * 1.5,
    driverB: 87.8 + Math.random() * 1.5,
  }));

  const sectorComparison = [
    { sector: "Sector 1", driverA: 24.532, driverB: 24.687 },
    { sector: "Sector 2", driverA: 38.421, driverB: 38.234 },
    { sector: "Sector 3", driverA: 21.599, driverB: 21.812 },
  ];

  const speedOverlay = Array.from({ length: 100 }, (_, i) => ({
    distance: i * 100,
    driverA: 200 + Math.sin(i / 10) * 80 + Math.random() * 10,
    driverB: 195 + Math.sin(i / 10) * 75 + Math.random() * 10,
  }));

  const throttleOverlay = Array.from({ length: 100 }, (_, i) => ({
    distance: i * 100,
    driverA: Math.max(0, Math.min(100, 70 + Math.sin(i / 8) * 40 + Math.random() * 10)),
    driverB: Math.max(0, Math.min(100, 68 + Math.sin(i / 8) * 38 + Math.random() * 10)),
  }));

  const deltaData = Array.from({ length: 100 }, (_, i) => ({
    distance: i * 100,
    delta: (speedOverlay[i].driverA - speedOverlay[i].driverB) / 10,
  }));

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold mb-2">Driver Comparison</h1>
        <p className="text-text-secondary">Compare telemetry and performance between drivers</p>
      </div>

      {/* Driver Selectors */}
      <DataCard>
        <div className="grid md:grid-cols-3 gap-4">
          <div>
            <label className="text-xs text-text-secondary uppercase mb-2 block">Driver A</label>
            <Select defaultValue="ver">
              <SelectTrigger className="bg-input-background border-border">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ver">VER (You)</SelectItem>
                <SelectItem value="ham">HAM</SelectItem>
                <SelectItem value="lec">LEC</SelectItem>
                <SelectItem value="nor">NOR</SelectItem>
              </SelectContent>
            </Select>
          </div>

          <div>
            <label className="text-xs text-text-secondary uppercase mb-2 block">Driver B</label>
            <Select defaultValue="ham">
              <SelectTrigger className="bg-input-background border-border">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ver">VER</SelectItem>
                <SelectItem value="ham">HAM</SelectItem>
                <SelectItem value="lec">LEC</SelectItem>
                <SelectItem value="nor">NOR</SelectItem>
              </SelectContent>
            </Select>
          </div>

          <div>
            <label className="text-xs text-text-secondary uppercase mb-2 block">Session</label>
            <Select defaultValue="latest">
              <SelectTrigger className="bg-input-background border-border">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="latest">Latest Session</SelectItem>
                <SelectItem value="silverstone">Silverstone Race</SelectItem>
                <SelectItem value="monaco">Monaco Quali</SelectItem>
                <SelectItem value="spa">Spa Race</SelectItem>
              </SelectContent>
            </Select>
          </div>
        </div>
      </DataCard>

      {/* Summary Cards */}
      <div className="grid md:grid-cols-2 gap-6">
        <DataCard title="Driver A - VER">
          <div className="grid grid-cols-3 gap-4">
            <div>
              <div className="text-xs text-text-secondary uppercase mb-1">Best Lap</div>
              <div className="text-xl font-bold font-mono text-[#00E5FF]">1:27.451</div>
            </div>
            <div>
              <div className="text-xs text-text-secondary uppercase mb-1">Avg Lap</div>
              <div className="text-xl font-bold font-mono">1:28.234</div>
            </div>
            <div>
              <div className="text-xs text-text-secondary uppercase mb-1">Position</div>
              <div className="text-xl font-bold text-[#00FF85]">P3</div>
            </div>
          </div>
        </DataCard>

        <DataCard title="Driver B - HAM">
          <div className="grid grid-cols-3 gap-4">
            <div>
              <div className="text-xs text-text-secondary uppercase mb-1">Best Lap</div>
              <div className="text-xl font-bold font-mono text-[#E10600]">1:27.789</div>
            </div>
            <div>
              <div className="text-xs text-text-secondary uppercase mb-1">Avg Lap</div>
              <div className="text-xl font-bold font-mono">1:28.567</div>
            </div>
            <div>
              <div className="text-xs text-text-secondary uppercase mb-1">Position</div>
              <div className="text-xl font-bold">P5</div>
            </div>
          </div>
        </DataCard>
      </div>

      {/* Lap Time Comparison */}
      <DataCard title="Lap Time Comparison">
        <ResponsiveContainer width="100%" height={350}>
          <LineChart data={lapTimeComparison}>
            <CartesianGrid strokeDasharray="3 3" stroke="rgba(249,250,251,0.06)" />
            <XAxis 
              dataKey="lap" 
              stroke="#9CA3AF"
              label={{ value: 'Lap', position: 'insideBottom', offset: -5, fill: '#9CA3AF' }}
            />
            <YAxis 
              stroke="#9CA3AF"
              domain={[85, 90]}
              label={{ value: 'Time (s)', angle: -90, position: 'insideLeft', fill: '#9CA3AF' }}
            />
            <Tooltip 
              contentStyle={{ 
                backgroundColor: '#1F2937', 
                border: '1px solid rgba(249,250,251,0.08)',
                borderRadius: '8px'
              }}
              formatter={(value: any) => `${value.toFixed(3)}s`}
            />
            <Legend />
            <Line 
              type="monotone" 
              dataKey="driverA" 
              stroke="#00E5FF" 
              strokeWidth={2}
              dot={false}
              name="VER (You)"
            />
            <Line 
              type="monotone" 
              dataKey="driverB" 
              stroke="#E10600" 
              strokeWidth={2}
              dot={false}
              name="HAM"
            />
          </LineChart>
        </ResponsiveContainer>
      </DataCard>

      {/* Sector Delta Comparison */}
      <DataCard title="Sector Delta Comparison">
        <ResponsiveContainer width="100%" height={300}>
          <BarChart data={sectorComparison}>
            <CartesianGrid strokeDasharray="3 3" stroke="rgba(249,250,251,0.06)" />
            <XAxis dataKey="sector" stroke="#9CA3AF" />
            <YAxis stroke="#9CA3AF" domain={[20, 40]} />
            <Tooltip 
              contentStyle={{ 
                backgroundColor: '#1F2937', 
                border: '1px solid rgba(249,250,251,0.08)',
                borderRadius: '8px'
              }}
              formatter={(value: any) => `${value.toFixed(3)}s`}
            />
            <Legend />
            <Bar dataKey="driverA" fill="#00E5FF" name="VER (You)" />
            <Bar dataKey="driverB" fill="#E10600" name="HAM" />
          </BarChart>
        </ResponsiveContainer>

        <div className="mt-6 grid grid-cols-3 gap-4">
          {sectorComparison.map((sector) => {
            const delta = sector.driverA - sector.driverB;
            return (
              <div key={sector.sector} className="p-3 bg-secondary/30 rounded-lg">
                <div className="text-sm text-text-secondary mb-1">{sector.sector}</div>
                <div className={`text-lg font-bold ${delta < 0 ? 'text-[#00FF85]' : 'text-[#E10600]'}`}>
                  {delta > 0 ? '+' : ''}{delta.toFixed(3)}s
                </div>
                <div className="text-xs text-text-secondary">
                  {delta < 0 ? 'Faster' : 'Slower'}
                </div>
              </div>
            );
          })}
        </div>
      </DataCard>

      {/* Speed Overlay */}
      <DataCard title="Speed Overlay">
        <ResponsiveContainer width="100%" height={350}>
          <LineChart data={speedOverlay}>
            <CartesianGrid strokeDasharray="3 3" stroke="rgba(249,250,251,0.06)" />
            <XAxis 
              dataKey="distance" 
              stroke="#9CA3AF"
              label={{ value: 'Distance (m)', position: 'insideBottom', offset: -5, fill: '#9CA3AF' }}
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
              formatter={(value: any) => `${Math.round(value)} km/h`}
            />
            <Legend />
            <Line 
              type="monotone" 
              dataKey="driverA" 
              stroke="#00E5FF" 
              strokeWidth={2}
              dot={false}
              name="VER (You)"
              isAnimationActive={false}
            />
            <Line 
              type="monotone" 
              dataKey="driverB" 
              stroke="#E10600" 
              strokeWidth={2}
              dot={false}
              name="HAM"
              isAnimationActive={false}
            />
          </LineChart>
        </ResponsiveContainer>
      </DataCard>

      {/* Throttle Overlay */}
      <DataCard title="Throttle Overlay">
        <ResponsiveContainer width="100%" height={350}>
          <LineChart data={throttleOverlay}>
            <CartesianGrid strokeDasharray="3 3" stroke="rgba(249,250,251,0.06)" />
            <XAxis 
              dataKey="distance" 
              stroke="#9CA3AF"
              label={{ value: 'Distance (m)', position: 'insideBottom', offset: -5, fill: '#9CA3AF' }}
            />
            <YAxis 
              stroke="#9CA3AF"
              domain={[0, 100]}
              label={{ value: 'Throttle (%)', angle: -90, position: 'insideLeft', fill: '#9CA3AF' }}
            />
            <Tooltip 
              contentStyle={{ 
                backgroundColor: '#1F2937', 
                border: '1px solid rgba(249,250,251,0.08)',
                borderRadius: '8px'
              }}
              formatter={(value: any) => `${Math.round(value)}%`}
            />
            <Legend />
            <Line 
              type="monotone" 
              dataKey="driverA" 
              stroke="#00E5FF" 
              strokeWidth={2}
              dot={false}
              name="VER (You)"
              isAnimationActive={false}
            />
            <Line 
              type="monotone" 
              dataKey="driverB" 
              stroke="#E10600" 
              strokeWidth={2}
              dot={false}
              name="HAM"
              isAnimationActive={false}
            />
          </LineChart>
        </ResponsiveContainer>
      </DataCard>

      {/* Delta Graph */}
      <DataCard title="Time Delta (VER - HAM)">
        <ResponsiveContainer width="100%" height={300}>
          <LineChart data={deltaData}>
            <CartesianGrid strokeDasharray="3 3" stroke="rgba(249,250,251,0.06)" />
            <XAxis 
              dataKey="distance" 
              stroke="#9CA3AF"
              label={{ value: 'Distance (m)', position: 'insideBottom', offset: -5, fill: '#9CA3AF' }}
            />
            <YAxis 
              stroke="#9CA3AF"
              label={{ value: 'Delta (s)', angle: -90, position: 'insideLeft', fill: '#9CA3AF' }}
            />
            <Tooltip 
              contentStyle={{ 
                backgroundColor: '#1F2937', 
                border: '1px solid rgba(249,250,251,0.08)',
                borderRadius: '8px'
              }}
              formatter={(value: any) => `${value > 0 ? '+' : ''}${value.toFixed(3)}s`}
            />
            <Line 
              type="monotone" 
              dataKey="delta" 
              stroke="#00FF85" 
              strokeWidth={2}
              dot={false}
              isAnimationActive={false}
            />
          </LineChart>
        </ResponsiveContainer>
        <div className="mt-4 text-sm text-text-secondary text-center">
          Positive values indicate VER is faster • Negative values indicate HAM is faster
        </div>
      </DataCard>
    </div>
  );
}
