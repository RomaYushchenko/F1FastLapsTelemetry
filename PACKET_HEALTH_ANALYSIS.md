## Packet health (approx.) — analysis and implementation options

### 1. Why it does nothing today

- **UI side**:
  - The Dashboard `Connection Status` card renders a static label and bar:
    - Label: `Packet health (approx.)`
    - Value: hardcoded `—`
    - Bar width: `w-0` (always empty), no state or props behind it.
  - The component does not read any packet-loss or quality metric from:
    - `useLiveTelemetry()` state
    - REST endpoints
    - Settings / preferences
- **Backend side**:
  - REST/WebSocket contracts (`rest_web_socket_api_contracts_f_1_telemetry.md`) describe telemetry, laps, summaries, live snapshot, leaderboard, etc., but:
    - There is **no explicit field** for packet loss or packet health in any REST DTO or WS message.
    - MVP requirements (section 12) treat UDP packet loss as “normal” and do not expose it to clients.
  - Observability section (MVP §14.1) lists `packet_loss_ratio` as an internal metric for monitoring, not as part of the public API.
- **Settings page hints**:
  - `Settings` has **Alert Settings → Packet Loss Alerts** and a configurable threshold, but this is purely UI for now:
    - No backend endpoint to store/read the threshold.
    - No live signal that would trigger “packet loss exceeded” alerts.

**Conclusion:**  
The `Packet health (approx.)` indicator is currently **pure UI placeholder** with no data source on either frontend or backend. All behaviour (value and bar) is intentionally disabled until we implement a packet-quality metric.

### 2. What “packet health” should roughly represent

From docs and overall design, the intended semantics are:

- **Goal:** Show a simple, user-friendly indication of how “good” the UDP telemetry stream is over the last short window.
- **Not a precise network metric**, but an approximate health score, e.g.:
  - 100% — effectively no loss, stable stream;
  - 80–99% — minor loss, acceptable;
  - 50–79% — noticeable gaps; user should check network;
  - <50% — poor; live UI may be unreliable.
- **Inputs we realistically have or can derive:**
  - Expected send rate from the game: e.g. 20 Hz (Settings recommendations already mention it).
  - Actual arrival rate of relevant packets (LapData, CarTelemetry, Motion).
  - Ingest-side logging of dropped/out-of-order frames (we already track some idempotency and gaps).
  - Optional: application metrics (`packet_loss_ratio`) from ingest/processing services.

### 3. Implementation options

#### Option A — Backend-computed loss ratio, exposed via REST

**Idea:**  
Compute a rolling packet-loss ratio per active session on the backend and expose it via a small REST endpoint; UI polls it while the session is live.

- **Backend changes (high level):**
  - In `udp-ingest-service` or `telemetry-processing-api-service`:
    - For a chosen packet type (e.g. Car Telemetry or LapData), keep a **per-session sliding window** (e.g. last 10–30 seconds) of:
      - expected frames: based on configured send rate × window duration;
      - actual received frames: counted from successfully ingested messages.
    - Compute `packetLossRatio = 1 - (received / expected)`, clamped to `[0, 1]`.
    - Persist this metric in a lightweight in-memory structure keyed by `sessionUID` (and optionally expose via Micrometer/Actuator for observability).
  - Add REST endpoint in `telemetry-processing-api-service`, for example:
    - `GET /api/sessions/active/packet-health` → returns `{ "healthPercent": 92, "windowSeconds": 30 }`.
    - Or generalised per-session: `GET /api/sessions/{id}/packet-health`.
- **Frontend changes:**
  - Extend `api/client.ts` with `getPacketHealth()` for active session (or for a given id).
  - In `LiveTelemetryProvider` or directly in `Dashboard`:
    - Poll this endpoint every N seconds (e.g. 5–10s) **while status is `live` or `waiting`**.
    - Map `healthPercent` into:
      - text value (e.g. `92%`),
      - bar width (CSS `%`),
      - optional colour thresholds (green/yellow/red).
  - Use Settings “Packet Loss Threshold”:
    - If `healthPercent < (100 - threshold)`, trigger `notify.warning("High packet loss detected…")` once per session.

**Pros:**
- Clear separation of concerns; backend owns the definition of “packet health”.
- Easy to evolve without touching UI (e.g. switch to more sophisticated calculation).

**Cons:**
- Requires new endpoint and some runtime state/metrics logic.
- Polling adds a small amount of extra traffic (though negligible).

#### Option B — UI-estimated health from live snapshots

**Idea:**  
Approximate health purely on the client by observing **how often live snapshots arrive** via WebSocket.

- **Mechanism:**
  - `LiveTelemetryProvider` already receives `SNAPSHOT` messages at a fixed target rate (e.g. 10 Hz).
  - Track the timestamps of the **last N snapshots** in the provider.
  - Compute:
    - expected count = snapshotRate × windowSeconds,
    - actual count = number of snapshots in that sliding window,
    - `healthPercent ≈ (actual / expected) * 100`.
- **Usage:**
  - Expose `packetHealthPercent` from `useLiveTelemetry()`.
  - Dashboard `Packet health (approx.)` reads that value:
    - show `—` when disconnected/no active session;
    - otherwise show `healthPercent` rounded.

**Pros:**
- No backend changes; can be implemented entirely in the frontend.
- Reflects **effective UI update rate**, which is what the user perceives.

**Cons:**
- Mixes network issues with **UI throttling** or browser slowdowns.
- Does not see packet loss earlier in the pipeline (e.g. between game and ingest service).
- Requires a clear, documented assumption about snapshot target rate.

#### Option C — Observability integration (future, internal metric)

**Idea:**  
Reuse the existing internal metric `packet_loss_ratio` (mentioned in MVP Observability) and surface a coarse health band into the UI.

- **Implementation sketch:**
  - Ensure ingest/processing publishes `packet_loss_ratio` per session or globally into metrics backend (Prometheus/Actuator).
  - Add a small API layer (`/api/diagnostics` or `/api/sessions/{id}/diagnostics`) that:
    - pulls the current numeric ratio;
    - maps it into health bands (good/ok/poor).
  - Use this aggregated diagnostic both in:
    - **Settings → View Diagnostics** page,
    - and Dashboard packet-health card.

**Pros:**
- Aligns UI with the same metrics used for operational monitoring.
- Can present richer diagnostics (e.g. “ingest CPU high + packet loss”).

**Cons:**
- Requires observability stack (metrics registry) to be wired into business services.
- Slightly heavier than A/B for MVP.

### 4. Recommended path

For a first working version of `Packet health (approx.)` that matches the current scope and effort:

1. **Short term (fastest): implement Option B on the frontend.**
   - Use only WebSocket snapshot arrival frequency to compute an approximate health score.
   - Update `LiveTelemetryProvider` to maintain a small time window of snapshots and expose `packetHealthPercent`.
   - Wire Dashboard’s `Packet health (approx.)` label and bar to this value, with simple green/yellow/red thresholds.
2. **Medium term (more accurate): move to Option A on the backend.**
   - Once ingest/processing has a per-session packet-loss metric, expose it via REST and switch Dashboard to that value.
   - Keep the same UI mapping logic (thresholds, bar), just change the data source.
3. **Later (advanced diagnostics): explore Option C.**
   - Add a diagnostics endpoint that includes packet loss plus other health signals (Kafka lag, DB issues, etc.).
   - Show detailed info on `/app/settings/diagnostics`, while keeping Dashboard to a single “health” number.

In all options, the UI should:

- **Fallback gracefully** to `—` when no data is available (no active session, disconnected, or endpoint not implemented).
- **Avoid over-precision**: one decimal or whole percentages are enough; this is an approximate UX signal, not a network monitor.

