import { DataCard } from "../components/DataCard";
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend, BarChart, Bar } from "recharts";

export default function StrategyView() {
  // Mock strategy data
  const tyreDegradation = Array.from({ length: 15 }, (_, i) => ({
    lap: i + 1,
    performance: 100 - (i * 3.5) - Math.random() * 5,
  }));

  const fuelConsumption = Array.from({ length: 30 }, (_, i) => ({
    lap: i + 1,
    fuel: 100 - (i * 3.2),
  }));

  const ersDeployment = Array.from({ length: 30 }, (_, i) => ({
    lap: i + 1,
    deployed: 20 + Math.random() * 40,
    harvested: 15 + Math.random() * 30,
  }));

  const pitStops = [
    { lap: 12, inLap: "1:32.456", pitTime: "2.8s", outLap: "1:42.123", tyreChange: "S → M" },
    { lap: 27, inLap: "1:31.234", pitTime: "2.6s", outLap: "1:41.567", tyreChange: "M → S" },
  ];

  const stintComparison = [
    { stint: "Stint 1 (Soft)", avgLap: 87.423, laps: 12, degradation: "High" },
    { stint: "Stint 2 (Medium)", avgLap: 88.234, laps: 15, degradation: "Medium" },
    { stint: "Stint 3 (Soft)", avgLap: 87.789, laps: 3, degradation: "Low" },
  ];

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold mb-2">Strategy View</h1>
        <p className="text-text-secondary">Analyze tyre strategy, fuel management, and ERS deployment</p>
      </div>

      {/* Overview Cards */}
      <div className="grid md:grid-cols-4 gap-6">
        <DataCard>
          <div className="space-y-2">
            <div className="text-xs text-text-secondary uppercase">Total Pit Stops</div>
            <div className="text-3xl font-bold text-[#00E5FF]">2</div>
            <div className="text-sm text-text-secondary">Avg: 2.7s</div>
          </div>
        </DataCard>

        <DataCard>
          <div className="space-y-2">
            <div className="text-xs text-text-secondary uppercase">Fuel Strategy</div>
            <div className="text-3xl font-bold text-[#00FF85]">Optimal</div>
            <div className="text-sm text-text-secondary">+2.1% margin</div>
          </div>
        </DataCard>

        <DataCard>
          <div className="space-y-2">
            <div className="text-xs text-text-secondary uppercase">Tyre Management</div>
            <div className="text-3xl font-bold text-[#FACC15]">Good</div>
            <div className="text-sm text-text-secondary">3 stints</div>
          </div>
        </DataCard>

        <DataCard>
          <div className="space-y-2">
            <div className="text-xs text-text-secondary uppercase">ERS Efficiency</div>
            <div className="text-3xl font-bold">94%</div>
            <div className="text-sm text-text-secondary">Well balanced</div>
          </div>
        </DataCard>
      </div>

      {/* Tyre Degradation */}
      <DataCard title="Tyre Degradation (Current Stint)">
        <ResponsiveContainer width="100%" height={350}>
          <LineChart data={tyreDegradation}>
            <CartesianGrid strokeDasharray="3 3" stroke="rgba(249,250,251,0.06)" />
            <XAxis 
              dataKey="lap" 
              stroke="#9CA3AF"
              label={{ value: 'Lap on Current Tyres', position: 'insideBottom', offset: -5, fill: '#9CA3AF' }}
            />
            <YAxis 
              stroke="#9CA3AF"
              domain={[0, 100]}
              label={{ value: 'Performance (%)', angle: -90, position: 'insideLeft', fill: '#9CA3AF' }}
            />
            <Tooltip 
              contentStyle={{ 
                backgroundColor: '#1F2937', 
                border: '1px solid rgba(249,250,251,0.08)',
                borderRadius: '8px'
              }}
              formatter={(value: any) => `${value.toFixed(1)}%`}
            />
            <Line 
              type="monotone" 
              dataKey="performance" 
              stroke="#E10600" 
              strokeWidth={3}
              dot={{ fill: '#E10600', r: 4 }}
            />
          </LineChart>
        </ResponsiveContainer>

        <div className="mt-6 p-4 bg-[#FACC15]/10 border border-[#FACC15]/30 rounded-lg">
          <div className="flex items-center gap-2 text-[#FACC15] font-semibold mb-2">
            <span>⚠️</span>
            <span>Strategy Recommendation</span>
          </div>
          <p className="text-sm text-text-secondary">
            Consider pitting in the next 3-5 laps. Current tyre performance at 67% and degrading rapidly.
          </p>
        </div>
      </DataCard>

      {/* Pit Stop Timeline */}
      <DataCard title="Pit Stop Timeline">
        <div className="relative h-20 bg-secondary/30 rounded-lg mb-6">
          <div className="absolute inset-0 flex items-center px-4">
            <div className="flex-1 h-1 bg-border" />
          </div>
          {pitStops.map((stop, idx) => (
            <div
              key={idx}
              className="absolute top-1/2 -translate-y-1/2 -translate-x-1/2"
              style={{ left: `${(stop.lap / 30) * 100}%` }}
            >
              <div className="w-4 h-4 bg-[#00E5FF] rounded-full border-4 border-background" />
              <div className="absolute top-6 left-1/2 -translate-x-1/2 whitespace-nowrap">
                <div className="text-xs font-bold">Lap {stop.lap}</div>
              </div>
            </div>
          ))}
        </div>

        <div className="space-y-3">
          {pitStops.map((stop, idx) => (
            <div key={idx} className="p-4 bg-secondary/30 rounded-lg border border-border">
              <div className="flex items-center justify-between mb-3">
                <div className="font-bold text-lg">Pit Stop {idx + 1} - Lap {stop.lap}</div>
                <div className="text-[#00E5FF] font-mono font-bold">{stop.pitTime}</div>
              </div>
              <div className="grid grid-cols-3 gap-4 text-sm">
                <div>
                  <div className="text-text-secondary">In Lap</div>
                  <div className="font-mono">{stop.inLap}</div>
                </div>
                <div>
                  <div className="text-text-secondary">Out Lap</div>
                  <div className="font-mono">{stop.outLap}</div>
                </div>
                <div>
                  <div className="text-text-secondary">Tyre Change</div>
                  <div className="font-semibold">{stop.tyreChange}</div>
                </div>
              </div>
            </div>
          ))}
        </div>
      </DataCard>

      {/* Stint Comparison */}
      <DataCard title="Stint Performance Comparison">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-secondary/50 border-b border-border/50">
              <tr>
                <th className="px-4 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                  Stint
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                  Avg Lap Time
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                  Laps
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                  Degradation
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border/30">
              {stintComparison.map((stint, idx) => (
                <tr key={idx} className="hover:bg-secondary/30 transition-colors">
                  <td className="px-4 py-4 font-semibold">{stint.stint}</td>
                  <td className="px-4 py-4 font-mono text-[#00E5FF]">{stint.avgLap.toFixed(3)}s</td>
                  <td className="px-4 py-4">{stint.laps}</td>
                  <td className="px-4 py-4">
                    <span className={`inline-flex px-2 py-1 rounded text-xs font-medium ${
                      stint.degradation === 'High' ? 'bg-[#E10600]/20 text-[#E10600]' :
                      stint.degradation === 'Medium' ? 'bg-[#FACC15]/20 text-[#FACC15]' :
                      'bg-[#00FF85]/20 text-[#00FF85]'
                    }`}>
                      {stint.degradation}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </DataCard>

      {/* Fuel Consumption */}
      <DataCard title="Fuel Consumption">
        <ResponsiveContainer width="100%" height={300}>
          <LineChart data={fuelConsumption}>
            <CartesianGrid strokeDasharray="3 3" stroke="rgba(249,250,251,0.06)" />
            <XAxis 
              dataKey="lap" 
              stroke="#9CA3AF"
              label={{ value: 'Lap', position: 'insideBottom', offset: -5, fill: '#9CA3AF' }}
            />
            <YAxis 
              stroke="#9CA3AF"
              domain={[0, 100]}
              label={{ value: 'Fuel Remaining (%)', angle: -90, position: 'insideLeft', fill: '#9CA3AF' }}
            />
            <Tooltip 
              contentStyle={{ 
                backgroundColor: '#1F2937', 
                border: '1px solid rgba(249,250,251,0.08)',
                borderRadius: '8px'
              }}
              formatter={(value: any) => `${value.toFixed(1)}%`}
            />
            <Line 
              type="monotone" 
              dataKey="fuel" 
              stroke="#00E5FF" 
              strokeWidth={2}
              dot={false}
            />
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
            <div className="text-lg font-bold text-[#00FF85]">Safe</div>
          </div>
        </div>
      </DataCard>

      {/* ERS Deployment */}
      <DataCard title="ERS Deployment & Harvesting">
        <ResponsiveContainer width="100%" height={300}>
          <BarChart data={ersDeployment}>
            <CartesianGrid strokeDasharray="3 3" stroke="rgba(249,250,251,0.06)" />
            <XAxis dataKey="lap" stroke="#9CA3AF" />
            <YAxis stroke="#9CA3AF" label={{ value: 'Energy (%)', angle: -90, position: 'insideLeft', fill: '#9CA3AF' }} />
            <Tooltip 
              contentStyle={{ 
                backgroundColor: '#1F2937', 
                border: '1px solid rgba(249,250,251,0.08)',
                borderRadius: '8px'
              }}
              formatter={(value: any) => `${value.toFixed(1)}%`}
            />
            <Legend />
            <Bar dataKey="deployed" fill="#A855F7" name="Deployed" />
            <Bar dataKey="harvested" fill="#00FF85" name="Harvested" />
          </BarChart>
        </ResponsiveContainer>
      </DataCard>
    </div>
  );
}
