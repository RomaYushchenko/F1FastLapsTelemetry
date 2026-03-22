import { useMemo } from "react";
import type { LeaderboardEntry } from "@/api/types";
import { formatLapTime } from "@/api/format";

export interface LeaderboardTableProps {
  entries: LeaderboardEntry[];
  /** Shown when entries.length === 0 */
  emptyMessage: string;
}

const FASTEST_PURPLE = "text-[#A855F7]";
const FASTEST_SECTOR_WRAP = "bg-purple-500/15 text-[#A855F7]";

function minPositive(values: (number | null | undefined)[]): number | null {
  let best: number | null = null;
  for (const v of values) {
    if (v != null && v > 0 && (best === null || v < best)) {
      best = v;
    }
  }
  return best;
}

/**
 * Shared leaderboard grid for Live Overview and Session Details (Race Overview).
 * Time column shows each car's cumulative race time (valid completed laps), not interval to leader.
 */
function formatTotalRaceTime(item: LeaderboardEntry): string {
  if (item.totalRaceTimeMs != null && item.totalRaceTimeMs > 0) {
    return formatLapTime(item.totalRaceTimeMs);
  }
  return "—";
}

export function LeaderboardTable({ entries, emptyMessage }: LeaderboardTableProps) {
  const bestBestLapMs = useMemo(
    () => minPositive(entries.map((e) => e.bestLapTimeMs)),
    [entries]
  );
  const bestS1 = useMemo(() => minPositive(entries.map((e) => e.sector1Ms)), [entries]);
  const bestS2 = useMemo(() => minPositive(entries.map((e) => e.sector2Ms)), [entries]);
  const bestS3 = useMemo(() => minPositive(entries.map((e) => e.sector3Ms)), [entries]);

  return (
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
              Best
            </th>
            <th className="px-4 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
              Time
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
          {entries.length === 0 && (
            <tr>
              <td colSpan={9} className="px-4 py-8 text-center text-text-secondary">
                {emptyMessage}
              </td>
            </tr>
          )}
          {entries.map((item) => {
            const isFastestBestLap =
              item.bestLapTimeMs != null &&
              item.bestLapTimeMs > 0 &&
              bestBestLapMs != null &&
              item.bestLapTimeMs === bestBestLapMs;
            const s1Best =
              item.sector1Ms != null && item.sector1Ms > 0 && bestS1 != null && item.sector1Ms === bestS1;
            const s2Best =
              item.sector2Ms != null && item.sector2Ms > 0 && bestS2 != null && item.sector2Ms === bestS2;
            const s3Best =
              item.sector3Ms != null && item.sector3Ms > 0 && bestS3 != null && item.sector3Ms === bestS3;

            return (
              <tr
                key={`${item.position}-${item.carIndex}`}
                className="hover:bg-secondary/30 transition-colors"
              >
                <td className="px-4 py-4 font-bold text-[#00E5FF]">{item.position}</td>
                <td className="px-4 py-4 font-bold">
                  {item.driverLabel ?? `Car ${item.carIndex}`}
                </td>
                <td className="px-4 py-4">
                  <span
                    className={`inline-flex px-2 py-1 rounded text-xs font-bold ${
                      item.compound === "S"
                        ? "bg-[#E10600]/20 text-[#E10600]"
                        : item.compound === "M"
                          ? "bg-[#FACC15]/20 text-[#FACC15]"
                          : item.compound === "H"
                            ? "bg-slate-500/25 text-slate-200"
                            : "bg-[#F9FAFB]/20 text-[#F9FAFB]"
                    }`}
                  >
                    {item.compound ?? "—"}
                  </span>
                </td>
                <td
                  className={`px-4 py-4 font-mono ${
                    isFastestBestLap ? `${FASTEST_PURPLE} font-semibold` : ""
                  }`}
                >
                  {item.bestLapTimeMs != null ? formatLapTime(item.bestLapTimeMs) : "—"}
                </td>
                <td className="px-4 py-4 font-mono text-text-secondary">
                  {formatTotalRaceTime(item)}
                </td>
                <td className="px-4 py-4">
                  <span
                    className={`inline-flex min-w-[4.5rem] px-1.5 py-1 items-center justify-center rounded text-xs font-bold ${
                      s1Best ? FASTEST_SECTOR_WRAP : "bg-secondary/50 text-text-secondary"
                    }`}
                  >
                    {item.sector1Ms != null ? formatLapTime(item.sector1Ms) : "—"}
                  </span>
                </td>
                <td className="px-4 py-4">
                  <span
                    className={`inline-flex min-w-[4.5rem] px-1.5 py-1 items-center justify-center rounded text-xs font-bold ${
                      s2Best ? FASTEST_SECTOR_WRAP : "bg-secondary/50 text-text-secondary"
                    }`}
                  >
                    {item.sector2Ms != null ? formatLapTime(item.sector2Ms) : "—"}
                  </span>
                </td>
                <td className="px-4 py-4">
                  <span
                    className={`inline-flex min-w-[4.5rem] px-1.5 py-1 items-center justify-center rounded text-xs font-bold ${
                      s3Best ? FASTEST_SECTOR_WRAP : "bg-secondary/50 text-text-secondary"
                    }`}
                  >
                    {item.sector3Ms != null ? formatLapTime(item.sector3Ms) : "—"}
                  </span>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
