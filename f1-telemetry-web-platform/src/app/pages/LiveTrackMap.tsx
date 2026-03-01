import { DataCard } from "../components/DataCard";
import { StatusBadge } from "../components/StatusBadge";
import { TelemetryStat } from "../components/TelemetryStat";

export default function LiveTrackMap() {
  const drivers = [
    { id: 1, name: "VER", pos: 1, color: "#00E5FF" },
    { id: 2, name: "HAM", pos: 2, color: "#00FF85" },
    { id: 3, name: "LEC", pos: 3, color: "#E10600" },
    { id: 4, name: "NOR", pos: 4, color: "#FACC15" },
    { id: 5, name: "PIA", pos: 5, color: "#A855F7" },
  ];

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold mb-2">Live Track Map</h1>
          <p className="text-text-secondary">Real-time track positioning and telemetry</p>
        </div>
        <div className="flex items-center gap-4">
          <StatusBadge variant="active">Live</StatusBadge>
          <div className="text-sm text-text-secondary">
            Lap <span className="font-bold text-foreground">24/52</span>
          </div>
        </div>
      </div>

      <div className="grid lg:grid-cols-3 gap-6">
        {/* Track Map */}
        <div className="lg:col-span-2">
          <DataCard title="Silverstone Circuit" variant="live" noPadding>
            <div className="aspect-[4/3] bg-secondary/30 relative p-8">
              {/* Simplified track outline */}
              <svg viewBox="0 0 800 600" className="w-full h-full">
                {/* Track outline */}
                <path
                  d="M 100 300 Q 100 150, 250 100 L 550 100 Q 700 100, 700 250 L 700 400 Q 700 500, 600 500 L 200 500 Q 100 500, 100 400 Z"
                  fill="none"
                  stroke="rgba(249,250,251,0.1)"
                  strokeWidth="40"
                />
                <path
                  d="M 100 300 Q 100 150, 250 100 L 550 100 Q 700 100, 700 250 L 700 400 Q 700 500, 600 500 L 200 500 Q 100 500, 100 400 Z"
                  fill="none"
                  stroke="rgba(249,250,251,0.3)"
                  strokeWidth="4"
                />

                {/* Sector markers */}
                <circle cx="100" cy="300" r="8" fill="#A855F7" opacity="0.5" />
                <circle cx="550" cy="100" r="8" fill="#00FF85" opacity="0.5" />
                <circle cx="700" cy="400" r="8" fill="#FACC15" opacity="0.5" />

                {/* Start/Finish line */}
                <rect x="95" y="290" width="10" height="30" fill="#F9FAFB" opacity="0.8" />
                <text x="110" y="310" fill="#F9FAFB" fontSize="12" fontWeight="bold">START</text>

                {/* Driver positions (animated dots) */}
                {drivers.map((driver, idx) => {
                  const positions = [
                    { x: 120, y: 300 },
                    { x: 180, y: 220 },
                    { x: 350, y: 105 },
                    { x: 620, y: 120 },
                    { x: 700, y: 280 },
                  ];
                  const pos = positions[idx];
                  
                  return (
                    <g key={driver.id}>
                      <circle 
                        cx={pos.x} 
                        cy={pos.y} 
                        r="12" 
                        fill={driver.color}
                        opacity="0.3"
                        className="animate-pulse"
                      />
                      <circle 
                        cx={pos.x} 
                        cy={pos.y} 
                        r="8" 
                        fill={driver.color}
                      />
                      {idx === 0 && (
                        <circle 
                          cx={pos.x} 
                          cy={pos.y} 
                          r="14" 
                          fill="none"
                          stroke={driver.color}
                          strokeWidth="2"
                        />
                      )}
                      <text 
                        x={pos.x} 
                        y={pos.y + 4} 
                        fill="#0B0F14" 
                        fontSize="10" 
                        fontWeight="bold"
                        textAnchor="middle"
                      >
                        {driver.pos}
                      </text>
                    </g>
                  );
                })}
              </svg>

              {/* Mini Leaderboard Overlay */}
              <div className="absolute top-4 left-4 bg-card/90 backdrop-blur-sm border border-border rounded-lg p-3 min-w-[150px]">
                <div className="text-xs text-text-secondary uppercase mb-2">Positions</div>
                <div className="space-y-2">
                  {drivers.map((driver) => (
                    <div key={driver.id} className="flex items-center gap-2">
                      <div className="w-4 text-xs font-bold text-text-secondary">{driver.pos}</div>
                      <div className="w-2 h-2 rounded-full" style={{ backgroundColor: driver.color }} />
                      <div className="text-sm font-bold">{driver.name}</div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </DataCard>
        </div>

        {/* Right Panel */}
        <div className="space-y-6">
          {/* Car Selector */}
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
                  value="287"
                  unit="KM/H"
                  variant="performance"
                  size="medium"
                />
                
                <div className="grid grid-cols-2 gap-3">
                  <TelemetryStat
                    label="Gear"
                    value="6"
                    variant="neutral"
                    size="small"
                  />
                  <TelemetryStat
                    label="RPM"
                    value="11.2k"
                    variant="neutral"
                    size="small"
                  />
                  <TelemetryStat
                    label="Throttle"
                    value="98%"
                    variant="performance"
                    size="small"
                  />
                  <TelemetryStat
                    label="Brake"
                    value="0%"
                    variant="neutral"
                    size="small"
                  />
                </div>
              </div>
            </div>
          </DataCard>

          {/* Sector Times */}
          <DataCard title="Sector Times">
            <div className="space-y-3">
              <div className="flex items-center justify-between p-2 bg-secondary/30 rounded">
                <div className="text-sm text-text-secondary">Sector 1</div>
                <div className="text-sm font-mono font-bold text-[#A855F7]">24.532</div>
              </div>
              <div className="flex items-center justify-between p-2 bg-secondary/30 rounded">
                <div className="text-sm text-text-secondary">Sector 2</div>
                <div className="text-sm font-mono font-bold text-[#00FF85]">38.421</div>
              </div>
              <div className="flex items-center justify-between p-2 bg-secondary/30 rounded">
                <div className="text-sm text-text-secondary">Sector 3</div>
                <div className="text-sm font-mono font-bold text-[#FACC15]">21.599</div>
              </div>
              <div className="pt-3 border-t border-border/50 flex items-center justify-between">
                <div className="text-xs text-text-secondary uppercase">Last Lap</div>
                <div className="text-lg font-mono font-bold text-[#00E5FF]">1:24.532</div>
              </div>
            </div>
          </DataCard>

          {/* Connection Warning */}
          <DataCard variant="default">
            <div className="text-sm text-text-secondary">
              <div className="flex items-center gap-2 text-[#00FF85] mb-2">
                <div className="w-2 h-2 bg-[#00FF85] rounded-full animate-pulse" />
                <span className="font-medium">Connected</span>
              </div>
              <p>Receiving live telemetry data from F1 25</p>
              <p className="mt-2 text-xs">Packet loss: <span className="font-mono text-foreground">0.2%</span></p>
            </div>
          </DataCard>
        </div>
      </div>
    </div>
  );
}
