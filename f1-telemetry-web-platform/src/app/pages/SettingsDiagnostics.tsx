import { useEffect, useState } from "react";
import { useNavigate } from "react-router";
import { DataCard } from "../components/DataCard";
import { Button } from "../components/ui/button";
import { useLiveTelemetry } from "@/ws";
import { getSessionDiagnostics } from "@/api/client";
import type { PacketHealthBand, SessionDiagnosticsDto } from "@/api/types";

export default function SettingsDiagnostics() {
  const navigate = useNavigate();
  const { status, session: liveSession } = useLiveTelemetry();
  const [diagnostics, setDiagnostics] = useState<SessionDiagnosticsDto | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!liveSession?.id) {
      setDiagnostics(null);
      return;
    }
    let cancelled = false;
    setLoading(true);
    setError(null);

    getSessionDiagnostics(liveSession.id)
      .then((dto) => {
        if (!cancelled) setDiagnostics(dto);
      })
      .catch((e) => {
        if (!cancelled) setError(e instanceof Error ? e.message : "Failed to load diagnostics");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [liveSession?.id]);

  const packetHealthBand: PacketHealthBand | "MISSING" =
    diagnostics?.packetHealthBand ?? (diagnostics ? "UNKNOWN" : "MISSING");

  return (
    <div className="space-y-6 max-w-4xl">
      <div>
        <h1 className="text-3xl font-bold mb-2">Diagnostics</h1>
        <p className="text-text-secondary">
          View packet health and low-level telemetry diagnostics for the current session.
        </p>
      </div>

      <DataCard>
        <div className="flex items-center justify-between mb-4">
          <div>
            <h2 className="text-xl font-bold">Session Diagnostics</h2>
            <p className="text-sm text-text-secondary">
              Packet health is based on recent UDP packet loss ratio from ingest.
            </p>
          </div>
          <Button variant="outline" onClick={() => navigate("/app/settings")}>
            Back to Settings
          </Button>
        </div>

        {!liveSession && (
          <div className="text-sm text-text-secondary">
            No active session detected. Start a session in F1 25 to see diagnostics here.
          </div>
        )}

        {liveSession && (
          <div className="space-y-4">
            <div className="text-sm">
              <span className="text-text-secondary mr-2">Current session:</span>
              <span className="font-medium">
                {liveSession.trackDisplayName ?? "Unknown track"} — {liveSession.sessionType ?? "Session"}
              </span>
            </div>

            {loading && (
              <div className="text-sm text-text-secondary">Loading diagnostics…</div>
            )}

            {error && (
              <div className="text-sm text-[#EF4444]">
                {error}
              </div>
            )}

            {diagnostics && !loading && !error && (
              <div className="space-y-3">
                <div className="flex items-center justify-between">
                  <div>
                    <div className="font-medium">Packet health</div>
                    <div className="text-sm text-text-secondary">
                      GOOD means low packet loss; POOR indicates significant gaps in UDP telemetry.
                    </div>
                  </div>
                  <div className="text-right">
                    <div className="text-lg font-mono">
                      {diagnostics.packetHealthPercent != null
                        ? `${diagnostics.packetHealthPercent.toFixed(0)}%`
                        : "—"}
                    </div>
                    <div className="text-sm text-text-secondary">
                      Band: {diagnostics.packetHealthBand}
                    </div>
                  </div>
                </div>

                <div className="flex items-center justify-between">
                  <div>
                    <div className="font-medium">Packet loss ratio</div>
                    <div className="text-sm text-text-secondary">
                      0.0 means no loss observed in the recent window; 1.0 means all expected packets were missing.
                    </div>
                  </div>
                  <div className="text-right font-mono">
                    {diagnostics.packetLossRatio != null
                      ? diagnostics.packetLossRatio.toFixed(3)
                      : "—"}
                  </div>
                </div>
              </div>
            )}
          </div>
        )}
      </DataCard>
    </div>
  );
}

