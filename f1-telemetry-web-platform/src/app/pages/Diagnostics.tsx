import { useEffect, useState } from "react";
import { Link } from "react-router";
import { DataCard } from "../components/DataCard";
import { Button } from "../components/ui/button";
import { API_BASE_URL } from "@/api/config";

/**
 * Diagnostics page: connection/ingest status when backend exposes GET /api/diagnostics;
 * otherwise placeholder and link to UDP instructions. Block I — Step 34.
 */
export default function Diagnostics() {
  const [diagnostics, setDiagnostics] = useState<Record<string, unknown> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    fetch(`${API_BASE_URL}/api/diagnostics`, { headers: { Accept: "application/json" } })
      .then((res) => {
        if (cancelled) return;
        if (!res.ok) {
          setDiagnostics(null);
          setError(res.status === 404 ? null : `Request failed: ${res.status}`);
          return;
        }
        return res.json();
      })
      .then((data) => {
        if (!cancelled && data != null) setDiagnostics(data as Record<string, unknown>);
      })
      .catch((e) => {
        if (!cancelled) {
          setDiagnostics(null);
          setError(e instanceof Error ? e.message : "Failed to load diagnostics");
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <div className="space-y-6 max-w-4xl">
      <div>
        <h1 className="text-3xl font-bold mb-2">Diagnostics</h1>
        <p className="text-text-secondary">Connection and ingest status</p>
      </div>

      {loading && (
        <DataCard>
          <p className="text-text-secondary">Loading diagnostics…</p>
        </DataCard>
      )}

      {error && (
        <DataCard variant="error">
          <p className="text-destructive">{error}</p>
        </DataCard>
      )}

      {diagnostics != null && !loading && (
        <DataCard title="Backend diagnostics">
          <pre className="text-sm overflow-x-auto p-4 bg-secondary/30 rounded-lg">
            {JSON.stringify(diagnostics, null, 2)}
          </pre>
        </DataCard>
      )}

      {!loading && diagnostics == null && !error && (
        <DataCard>
          <p className="text-text-secondary mb-4">
            Diagnostics data is available when the backend supports the diagnostics endpoint.
          </p>
          <h3 className="font-semibold mb-2">UDP connection instructions</h3>
          <ol className="text-sm text-text-secondary space-y-2 list-decimal list-inside mb-4">
            <li>Open F1 25 and go to Settings → Telemetry Settings</li>
            <li>Set UDP Telemetry to &quot;On&quot;</li>
            <li>Set UDP Broadcast Mode to &quot;On&quot;</li>
            <li>Set UDP IP Address to your computer&apos;s local IP</li>
            <li>Set UDP Port to: <span className="font-mono font-bold text-foreground">20777</span></li>
            <li>Set UDP Send Rate to &quot;20Hz&quot; or higher</li>
          </ol>
          <Link to="/app/settings">
            <Button variant="outline">Back to Settings</Button>
          </Link>
        </DataCard>
      )}
    </div>
  );
}
