import { DataCard } from "../components/DataCard";
import { StatusBadge } from "../components/StatusBadge";
import { TelemetryStat } from "../components/TelemetryStat";
import { Flag, Clock, CloudRain, AlertTriangle } from "lucide-react";

export default function LiveOverview() {
  const leaderboard = [
    { pos: 1, driver: "VER", tyre: "S", gap: "LEAD", lastLap: "1:24.532", sectors: [1, 1, 1] },
    { pos: 2, driver: "HAM", tyre: "M", gap: "+2.134", lastLap: "1:25.012", sectors: [2, 2, 1] },
    { pos: 3, driver: "LEC", tyre: "S", gap: "+5.421", lastLap: "1:24.998", sectors: [1, 3, 2] },
    { pos: 4, driver: "NOR", tyre: "M", gap: "+8.765", lastLap: "1:25.234", sectors: [3, 2, 2] },
    { pos: 5, driver: "PIA", tyre: "M", gap: "+12.123", lastLap: "1:25.456", sectors: [2, 3, 3] },
  ];

  const events = [
    { time: "Lap 24", event: "Fastest Lap", driver: "VER", detail: "1:24.532", type: "fastest" },
    { time: "Lap 23", event: "Pit Stop", driver: "HAM", detail: "2.8s", type: "pit" },
    { time: "Lap 22", event: "Penalty", driver: "SAI", detail: "5s Time Penalty", type: "penalty" },
    { time: "Lap 19", event: "Safety Car", driver: "", detail: "Debris on track", type: "sc" },
  ];

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold mb-2">Live Overview</h1>
          <p className="text-text-secondary">Real-time session monitoring and analytics</p>
        </div>
        <StatusBadge variant="active">Live</StatusBadge>
      </div>

      {/* Session Info */}
      <div className="grid lg:grid-cols-4 gap-6">
        <DataCard variant="live">
          <div className="space-y-2">
            <div className="text-xs text-text-secondary uppercase">Track</div>
            <div className="text-xl font-bold">Silverstone</div>
            <StatusBadge variant="active">Race</StatusBadge>
          </div>
        </DataCard>

        <DataCard>
          <div className="space-y-2">
            <div className="flex items-center gap-2 text-xs text-text-secondary uppercase">
              <Flag className="w-4 h-4" />
              Lap
            </div>
            <div className="text-3xl font-bold font-mono text-[#00E5FF]">24/52</div>
          </div>
        </DataCard>

        <DataCard>
          <div className="space-y-2">
            <div className="flex items-center gap-2 text-xs text-text-secondary uppercase">
              <Clock className="w-4 h-4" />
              Session Time
            </div>
            <div className="text-3xl font-bold font-mono">32:14</div>
          </div>
        </DataCard>

        <DataCard>
          <div className="space-y-2">
            <div className="flex items-center gap-2 text-xs text-text-secondary uppercase">
              <CloudRain className="w-4 h-4" />
              Weather
            </div>
            <div className="text-xl font-bold">Clear</div>
            <div className="text-sm text-text-secondary">Track Temp: 42°C</div>
          </div>
        </DataCard>
      </div>

      {/* Main Content */}
      <div className="grid lg:grid-cols-3 gap-6">
        {/* Leaderboard */}
        <div className="lg:col-span-2">
          <DataCard title="Leaderboard" noPadding>
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead className="bg-secondary/50 border-b border-border/50">
                  <tr>
                    <th className="px-4 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider w-16">
                      Pos
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                      Driver
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                      Tyre
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                      Gap
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                      Last Lap
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                      S1
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                      S2
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                      S3
                    </th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-border/30">
                  {leaderboard.map((item) => (
                    <tr key={item.pos} className="hover:bg-secondary/30 transition-colors">
                      <td className="px-4 py-4 font-bold text-[#00E5FF]">{item.pos}</td>
                      <td className="px-4 py-4 font-bold">{item.driver}</td>
                      <td className="px-4 py-4">
                        <span className={`inline-flex px-2 py-1 rounded text-xs font-bold ${
                          item.tyre === 'S' ? 'bg-[#E10600]/20 text-[#E10600]' :
                          item.tyre === 'M' ? 'bg-[#FACC15]/20 text-[#FACC15]' :
                          'bg-[#F9FAFB]/20 text-[#F9FAFB]'
                        }`}>
                          {item.tyre}
                        </span>
                      </td>
                      <td className="px-4 py-4 font-mono text-text-secondary">{item.gap}</td>
                      <td className="px-4 py-4 font-mono">{item.lastLap}</td>
                      <td className="px-4 py-4">
                        <span className={`inline-flex w-6 h-6 items-center justify-center rounded text-xs font-bold ${
                          item.sectors[0] === 1 ? 'bg-[#A855F7]/20 text-[#A855F7]' :
                          item.sectors[0] === 2 ? 'bg-[#00FF85]/20 text-[#00FF85]' :
                          'bg-[#FACC15]/20 text-[#FACC15]'
                        }`}>
                          {item.sectors[0]}
                        </span>
                      </td>
                      <td className="px-4 py-4">
                        <span className={`inline-flex w-6 h-6 items-center justify-center rounded text-xs font-bold ${
                          item.sectors[1] === 1 ? 'bg-[#A855F7]/20 text-[#A855F7]' :
                          item.sectors[1] === 2 ? 'bg-[#00FF85]/20 text-[#00FF85]' :
                          'bg-[#FACC15]/20 text-[#FACC15]'
                        }`}>
                          {item.sectors[1]}
                        </span>
                      </td>
                      <td className="px-4 py-4">
                        <span className={`inline-flex w-6 h-6 items-center justify-center rounded text-xs font-bold ${
                          item.sectors[2] === 1 ? 'bg-[#A855F7]/20 text-[#A855F7]' :
                          item.sectors[2] === 2 ? 'bg-[#00FF85]/20 text-[#00FF85]' :
                          'bg-[#FACC15]/20 text-[#FACC15]'
                        }`}>
                          {item.sectors[2]}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </DataCard>
        </div>

        {/* Player Telemetry */}
        <div>
          <DataCard title="Your Telemetry" variant="live">
            <div className="space-y-4">
              <TelemetryStat
                label="Speed"
                value="287"
                unit="KM/H"
                variant="performance"
                size="large"
              />
              
              <div className="grid grid-cols-2 gap-4">
                <TelemetryStat
                  label="Gear"
                  value="6"
                  variant="neutral"
                  size="medium"
                />
                <TelemetryStat
                  label="RPM"
                  value="11.2k"
                  variant="neutral"
                  size="medium"
                />
              </div>

              <div className="pt-4 border-t border-border/50 space-y-3">
                <TelemetryStat
                  label="Throttle"
                  value="98%"
                  variant="performance"
                  size="small"
                />
                <div className="h-2 bg-secondary rounded-full overflow-hidden">
                  <div className="h-full w-[98%] bg-[#00FF85]" />
                </div>
              </div>

              <div className="space-y-3">
                <TelemetryStat
                  label="Brake"
                  value="0%"
                  variant="neutral"
                  size="small"
                />
                <div className="h-2 bg-secondary rounded-full overflow-hidden">
                  <div className="h-full w-0 bg-[#E10600]" />
                </div>
              </div>

              <div className="pt-4 border-t border-border/50 grid grid-cols-2 gap-4">
                <TelemetryStat
                  label="ERS"
                  value="82%"
                  variant="neutral"
                  size="small"
                />
                <TelemetryStat
                  label="Fuel"
                  value="34%"
                  variant="warning"
                  size="small"
                />
              </div>
            </div>
          </DataCard>
        </div>
      </div>

      {/* Event Timeline */}
      <DataCard title="Event Timeline">
        <div className="space-y-3">
          {events.map((event, idx) => (
            <div 
              key={idx}
              className={`flex items-start gap-4 p-3 rounded-lg border transition-all ${
                event.type === 'penalty' 
                  ? 'border-[#E10600]/50 bg-[#E10600]/10 animate-pulse' 
                  : event.type === 'sc'
                  ? 'border-[#FACC15]/50 bg-[#FACC15]/10'
                  : 'border-border/30 bg-secondary/20'
              }`}
            >
              <div className="flex-shrink-0 w-16 text-xs text-text-secondary font-mono">
                {event.time}
              </div>
              <div className="flex-shrink-0">
                {event.type === 'penalty' ? (
                  <div className="w-8 h-8 bg-[#E10600]/20 rounded-lg flex items-center justify-center">
                    <AlertTriangle className="w-4 h-4 text-[#E10600]" />
                  </div>
                ) : event.type === 'sc' ? (
                  <div className="w-8 h-8 bg-[#FACC15]/20 rounded-lg flex items-center justify-center">
                    <Flag className="w-4 h-4 text-[#FACC15]" />
                  </div>
                ) : (
                  <div className="w-8 h-8 bg-[#00E5FF]/20 rounded-lg flex items-center justify-center text-xs font-bold text-[#00E5FF]">
                    {event.type === 'fastest' ? '🏁' : '🔧'}
                  </div>
                )}
              </div>
              <div className="flex-1">
                <div className="font-semibold">{event.event}</div>
                <div className="text-sm text-text-secondary">
                  {event.driver && <span className="font-bold">{event.driver}</span>}
                  {event.driver && event.detail && ' • '}
                  {event.detail}
                </div>
              </div>
            </div>
          ))}
        </div>
      </DataCard>
    </div>
  );
}
