import { useParams, Link } from "react-router";
import { DataCard } from "../components/DataCard";
import { Button } from "../components/ui/button";
import { ArrowLeft, Download } from "lucide-react";
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, BarChart, Bar, Cell } from "recharts";

export default function SessionDetails() {
  const { id } = useParams();

  // Mock lap data
  const lapTimeData = Array.from({ length: 30 }, (_, i) => ({
    lap: i + 1,
    time: 87.4 + Math.random() * 2 - 1,
  }));

  const positionData = Array.from({ length: 30 }, (_, i) => ({
    lap: i + 1,
    position: Math.max(1, Math.min(5, 3 + Math.floor(Math.sin(i / 5) * 2) + (Math.random() > 0.8 ? 1 : 0))),
  }));

  const tyreStints = [
    { lap: 1, laps: 12, compound: "Soft", color: "#E10600" },
    { lap: 13, laps: 15, compound: "Medium", color: "#FACC15" },
    { lap: 28, laps: 3, compound: "Soft", color: "#E10600" },
  ];

  const sectorHeatmap = Array.from({ length: 10 }, (_, lapIdx) => ({
    lap: lapIdx + 1,
    s1: 24.5 + Math.random() * 0.5,
    s2: 38.4 + Math.random() * 0.6,
    s3: 21.6 + Math.random() * 0.4,
  }));

  const events = [
    { lap: 12, event: "Pit Stop", detail: "Soft → Medium (2.8s)", type: "pit" },
    { lap: 18, event: "Fastest Lap", detail: "1:27.451", type: "fastest" },
    { lap: 27, event: "Pit Stop", detail: "Medium → Soft (2.6s)", type: "pit" },
  ];

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <Link to="/app/sessions">
          <Button variant="ghost" size="sm" className="gap-2">
            <ArrowLeft className="w-4 h-4" />
            Back
          </Button>
        </Link>
        <div className="flex-1">
          <h1 className="text-3xl font-bold mb-2">Session Details</h1>
          <p className="text-text-secondary">Silverstone • Race • February 24, 2026</p>
        </div>
        <Button variant="outline" className="gap-2">
          <Download className="w-4 h-4" />
          Export Data
        </Button>
      </div>

      {/* Session Summary */}
      <DataCard title="Session Summary">
        <div className="grid md:grid-cols-2 lg:grid-cols-5 gap-6">
          <div>
            <div className="text-xs text-text-secondary uppercase mb-1">Result</div>
            <div className="text-2xl font-bold text-[#00E5FF]">P3</div>
          </div>
          <div>
            <div className="text-xs text-text-secondary uppercase mb-1">Best Lap</div>
            <div className="text-2xl font-bold font-mono text-[#00FF85]">1:27.451</div>
          </div>
          <div>
            <div className="text-xs text-text-secondary uppercase mb-1">Total Time</div>
            <div className="text-2xl font-bold font-mono">1:42:34.123</div>
          </div>
          <div>
            <div className="text-xs text-text-secondary uppercase mb-1">Total Laps</div>
            <div className="text-2xl font-bold">30</div>
          </div>
          <div>
            <div className="text-xs text-text-secondary uppercase mb-1">Pit Stops</div>
            <div className="text-2xl font-bold">2</div>
          </div>
        </div>

        <div className="mt-6 pt-6 border-t border-border/50 grid md:grid-cols-3 gap-4 text-sm">
          <div>
            <span className="text-text-secondary">Weather:</span>{' '}
            <span className="font-medium">Clear (42°C track)</span>
          </div>
          <div>
            <span className="text-text-secondary">Assists:</span>{' '}
            <span className="font-medium">ABS: Off, TC: Medium</span>
          </div>
          <div>
            <span className="text-text-secondary">Fuel Load:</span>{' '}
            <span className="font-medium">100% Start</span>
          </div>
        </div>
      </DataCard>

      {/* Charts Row 1 */}
      <div className="grid lg:grid-cols-2 gap-6">
        <DataCard title="Lap Time Evolution">
          <ResponsiveContainer width="100%" height={300}>
            <LineChart data={lapTimeData}>
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
                formatter={(value: any) => [`${value.toFixed(3)}s`, 'Lap Time']}
              />
              <Line 
                type="monotone" 
                dataKey="time" 
                stroke="#00E5FF" 
                strokeWidth={2}
                dot={{ fill: '#00E5FF', r: 3 }}
              />
            </LineChart>
          </ResponsiveContainer>
        </DataCard>

        <DataCard title="Position Evolution">
          <ResponsiveContainer width="100%" height={300}>
            <LineChart data={positionData}>
              <CartesianGrid strokeDasharray="3 3" stroke="rgba(249,250,251,0.06)" />
              <XAxis 
                dataKey="lap" 
                stroke="#9CA3AF"
                label={{ value: 'Lap', position: 'insideBottom', offset: -5, fill: '#9CA3AF' }}
              />
              <YAxis 
                stroke="#9CA3AF"
                reversed
                domain={[1, 5]}
                label={{ value: 'Position', angle: -90, position: 'insideLeft', fill: '#9CA3AF' }}
              />
              <Tooltip 
                contentStyle={{ 
                  backgroundColor: '#1F2937', 
                  border: '1px solid rgba(249,250,251,0.08)',
                  borderRadius: '8px'
                }}
                formatter={(value: any) => [`P${value}`, 'Position']}
              />
              <Line 
                type="stepAfter" 
                dataKey="position" 
                stroke="#00FF85" 
                strokeWidth={2}
                dot={{ fill: '#00FF85', r: 3 }}
              />
            </LineChart>
          </ResponsiveContainer>
        </DataCard>
      </div>

      {/* Tyre Strategy */}
      <DataCard title="Tyre Strategy">
        <div className="space-y-4">
          <div className="relative h-16 bg-secondary/30 rounded-lg overflow-hidden">
            {tyreStints.map((stint, idx) => (
              <div
                key={idx}
                className="absolute top-0 h-full flex items-center justify-center text-sm font-bold"
                style={{
                  left: `${((stint.lap - 1) / 30) * 100}%`,
                  width: `${(stint.laps / 30) * 100}%`,
                  backgroundColor: stint.color,
                }}
              >
                {stint.compound} ({stint.laps} laps)
              </div>
            ))}
          </div>
          
          <div className="grid grid-cols-3 gap-4">
            {tyreStints.map((stint, idx) => (
              <div key={idx} className="p-3 bg-secondary/30 rounded-lg">
                <div className="flex items-center gap-2 mb-2">
                  <div className="w-3 h-3 rounded-full" style={{ backgroundColor: stint.color }} />
                  <div className="font-semibold">{stint.compound}</div>
                </div>
                <div className="text-sm text-text-secondary">
                  Laps {stint.lap}-{stint.lap + stint.laps - 1} • {stint.laps} laps
                </div>
              </div>
            ))}
          </div>
        </div>
      </DataCard>

      {/* Sector Comparison */}
      <DataCard title="Sector Times Heatmap">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-secondary/50 border-b border-border/50">
              <tr>
                <th className="px-4 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                  Lap
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                  Sector 1
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                  Sector 2
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                  Sector 3
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                  Total
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border/30">
              {sectorHeatmap.map((lap) => {
                const total = lap.s1 + lap.s2 + lap.s3;
                const bestS1 = Math.min(...sectorHeatmap.map(l => l.s1));
                const bestS2 = Math.min(...sectorHeatmap.map(l => l.s2));
                const bestS3 = Math.min(...sectorHeatmap.map(l => l.s3));
                
                return (
                  <tr key={lap.lap} className="hover:bg-secondary/30 transition-colors">
                    <td className="px-4 py-3 font-bold">{lap.lap}</td>
                    <td className={`px-4 py-3 font-mono ${lap.s1 === bestS1 ? 'text-[#A855F7] font-bold' : ''}`}>
                      {lap.s1.toFixed(3)}
                    </td>
                    <td className={`px-4 py-3 font-mono ${lap.s2 === bestS2 ? 'text-[#A855F7] font-bold' : ''}`}>
                      {lap.s2.toFixed(3)}
                    </td>
                    <td className={`px-4 py-3 font-mono ${lap.s3 === bestS3 ? 'text-[#A855F7] font-bold' : ''}`}>
                      {lap.s3.toFixed(3)}
                    </td>
                    <td className="px-4 py-3 font-mono font-bold text-[#00E5FF]">
                      {total.toFixed(3)}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </DataCard>

      {/* Event Timeline */}
      <DataCard title="Event Timeline">
        <div className="space-y-3">
          {events.map((event, idx) => (
            <div 
              key={idx}
              className="flex items-start gap-4 p-3 rounded-lg border border-border/30 bg-secondary/20"
            >
              <div className="flex-shrink-0 w-16 text-sm text-text-secondary font-bold">
                Lap {event.lap}
              </div>
              <div className="flex-1">
                <div className="font-semibold">{event.event}</div>
                <div className="text-sm text-text-secondary">{event.detail}</div>
              </div>
            </div>
          ))}
        </div>
      </DataCard>
    </div>
  );
}
