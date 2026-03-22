import { useEffect, useMemo, useState } from "react";
import { getSessionRaceOverview } from "@/api/client";
import type { RaceOverviewChartRow, SessionRaceOverview } from "@/api/types";
import { DataCard } from "./DataCard";
import { LeaderboardTable } from "./LeaderboardTable";
import { Button } from "./ui/button";
import { Skeleton } from "./ui/skeleton";
import {
  CartesianGrid,
  Legend,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";

const tooltipStyle = {
  backgroundColor: "#1F2937",
  border: "1px solid rgba(249,250,251,0.08)",
  borderRadius: "8px",
};

function rowsToRechartsData(
  rows: RaceOverviewChartRow[],
  keyPrefix: string
): Record<string, number | string | null>[] {
  return rows.map((row) => {
    const point: Record<string, number | string | null> = { lap: row.lapNumber };
    row.values.forEach((v, i) => {
      point[`${keyPrefix}${i}`] = v;
    });
    return point;
  });
}

function maxPositionInRows(rows: RaceOverviewChartRow[]): number {
  let m = 1;
  for (const row of rows) {
    for (const v of row.values) {
      if (v != null && v > m) {
        m = Math.ceil(v);
      }
    }
  }
  return m;
}

export interface SessionRaceOverviewPanelProps {
  sessionId: string;
  playerCarIndex?: number | null;
}

/**
 * Post-session race leaderboard and multi-car position / gap-to-leader charts.
 */
export function SessionRaceOverviewPanel({
  sessionId,
  playerCarIndex,
}: SessionRaceOverviewPanelProps) {
  const [data, setData] = useState<SessionRaceOverview | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  /** Y-axis for gap chart: sqrt compresses large spreads (plan: optional sqrt scale). */
  const [gapYScaleSqrt, setGapYScaleSqrt] = useState(true);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    getSessionRaceOverview(sessionId)
      .then((d) => {
        if (!cancelled) {
          setData(d);
        }
      })
      .catch((e) => {
        if (!cancelled) {
          setError(e instanceof Error ? e.message : "Failed to load race overview");
        }
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [sessionId]);

  const positionData = useMemo(
    () => (data?.positionChartRows?.length ? rowsToRechartsData(data.positionChartRows, "p") : []),
    [data?.positionChartRows]
  );
  const gapData = useMemo(
    () => (data?.gapChartRows?.length ? rowsToRechartsData(data.gapChartRows, "g") : []),
    [data?.gapChartRows]
  );

  const posDomainMax = useMemo(
    () => (data?.positionChartRows?.length ? maxPositionInRows(data.positionChartRows) : 20),
    [data?.positionChartRows]
  );

  if (loading) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-72 w-full" />
        <Skeleton className="h-[440px] w-full" />
      </div>
    );
  }

  if (error) {
    return (
      <DataCard>
        <p className="text-destructive">{error}</p>
      </DataCard>
    );
  }

  if (!data || !data.entries?.length) {
    return (
      <DataCard>
        <p className="text-text-secondary py-8 text-center">
          No multi-car lap data for this session yet. Race overview appears when laps are recorded for the grid.
        </p>
      </DataCard>
    );
  }

  return (
    <div className="space-y-6">
      <div>
        <DataCard title="Leaderboard" noPadding>
          <LeaderboardTable
            entries={data.entries}
            emptyMessage="No leaderboard entries."
          />
        </DataCard>
        <p className="text-xs text-text-secondary mt-2 px-1">
          Best is personal best lap; Time is cumulative race time (sum of valid completed laps in DB) per driver.
        </p>
      </div>

      <DataCard title="Race lap chart — position by lap">
        {positionData.length === 0 ? (
          <div className="h-[440px] flex items-center justify-center text-text-secondary">
            No position data
          </div>
        ) : (
          <div className="h-[540px] w-full">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={positionData}>
                <CartesianGrid strokeDasharray="3 3" stroke="rgba(249,250,251,0.06)" />
                <XAxis dataKey="lap" stroke="#9CA3AF" />
                <YAxis
                  stroke="#9CA3AF"
                  reversed
                  domain={[1, Math.max(posDomainMax, 2)]}
                  allowDecimals={false}
                  label={{
                    value: "Position",
                    angle: -90,
                    position: "insideLeft",
                    fill: "#9CA3AF",
                  }}
                />
                <Tooltip
                  contentStyle={tooltipStyle}
                  formatter={(value: number) =>
                    value != null ? [`P${Math.round(value)}`, ""] : ["—", ""]
                  }
                />
                <Legend wrapperStyle={{ paddingTop: 12 }} />
                {data.drivers.map((d, i) => (
                  <Line
                    key={d.carIndex}
                    type="stepAfter"
                    name={d.displayLabel}
                    dataKey={`p${i}`}
                    stroke={d.colorHex}
                    strokeWidth={d.carIndex === playerCarIndex ? 3 : 1.5}
                    dot={false}
                    connectNulls={false}
                  />
                ))}
              </LineChart>
            </ResponsiveContainer>
          </div>
        )}
      </DataCard>

      <DataCard
        title="Gap to leader (cumulative, seconds)"
        actions={
          gapData.length > 0 ? (
            <div className="flex items-center gap-1 shrink-0">
              <span className="text-xs text-text-secondary hidden sm:inline">Y scale</span>
              <Button
                type="button"
                variant={gapYScaleSqrt ? "secondary" : "ghost"}
                size="sm"
                className="h-7 px-2 text-xs"
                onClick={() => setGapYScaleSqrt(true)}
              >
                Sqrt
              </Button>
              <Button
                type="button"
                variant={!gapYScaleSqrt ? "secondary" : "ghost"}
                size="sm"
                className="h-7 px-2 text-xs"
                onClick={() => setGapYScaleSqrt(false)}
              >
                Linear
              </Button>
            </div>
          ) : undefined
        }
      >
        {gapData.length === 0 ? (
          <div className="h-[440px] flex items-center justify-center text-text-secondary">
            No gap data
          </div>
        ) : (
          <div className="h-[540px] w-full">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={gapData}>
                <CartesianGrid strokeDasharray="3 3" stroke="rgba(249,250,251,0.06)" />
                <XAxis dataKey="lap" stroke="#9CA3AF" />
                <YAxis
                  stroke="#9CA3AF"
                  scale={gapYScaleSqrt ? "sqrt" : "linear"}
                  domain={[0, "auto"]}
                  label={{
                    value: gapYScaleSqrt ? "Gap (s), sqrt scale" : "Gap (s), linear",
                    angle: -90,
                    position: "insideLeft",
                    fill: "#9CA3AF",
                  }}
                />
                <Tooltip
                  contentStyle={tooltipStyle}
                  formatter={(value: number) =>
                    value != null ? [`${value.toFixed(2)}s`, ""] : ["—", ""]
                  }
                />
                <Legend wrapperStyle={{ paddingTop: 12 }} />
                {data.drivers.map((d, i) => (
                  <Line
                    key={d.carIndex}
                    type="monotone"
                    name={d.displayLabel}
                    dataKey={`g${i}`}
                    stroke={d.colorHex}
                    strokeWidth={d.carIndex === playerCarIndex ? 3 : 1.5}
                    dot={false}
                    connectNulls={false}
                  />
                ))}
              </LineChart>
            </ResponsiveContainer>
          </div>
        )}
      </DataCard>
    </div>
  );
}
