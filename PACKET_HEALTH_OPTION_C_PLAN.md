### Packet health via observability (Option C) — implementation plan

This plan describes how to fully implement **Option C — Observability integration** for `Packet health (approx.)`, using the internal `packet_loss_ratio` metric and exposing it to the UI via a diagnostics API.

---

### 1. Target behaviour (end state)

- **Backend**
  - Ingest / processing services continuously publish **`packet_loss_ratio`** into the metrics system (Micrometer → Prometheus / Actuator).
  - `telemetry-processing-api-service` exposes a small **diagnostics REST API**:
    - Session-scoped: `GET /api/sessions/{publicId}/diagnostics`
    - Optional global/system: `GET /api/diagnostics`
  - Diagnostics payload includes:
    - A numeric **packet loss ratio** (\(0.0\)–\(1.0\))
    - Derived **packet health band** (`GOOD`, `OK`, `POOR`)
    - Optional extra fields for future use (CPU, Kafka lag, DB health).
- **Frontend**
  - **Dashboard** packet health card:
    - Reads `packetHealthBand` (and optional percentage) from diagnostics.
    - Shows a simple **green / yellow / red** state and optional percentage.
    - Falls back to `—` when diagnostics are missing.
  - **Settings → View Diagnostics** page:
    - Calls the same diagnostics API.
    - Shows packet health band and numeric details plus any extra signals.

---

### 2. Metric definition: `packet_loss_ratio`

**Goal:** standardise how `packet_loss_ratio` is produced, where it lives, and how it is keyed.

#### 2.1. Location and responsibility

- **Service:** `udp-ingest-service` (preferred source of truth, closest to raw packets).
- **Pattern:**
  - Introduce a small **metrics component**, e.g. `PacketLossMetricsRecorder`.
  - This component receives events like “frame expected” / “frame received” from the ingest handlers or processors.
  - It maintains **rolling counters** per session:
    - `expectedFrames{sessionUid}`
    - `receivedFrames{sessionUid}`

#### 2.2. Micrometer metric

- Define Micrometer gauge or function counter:
  - Name: `f1.telemetry.packet_loss_ratio`
  - Tags:
    - `session_uid` (internal UID from the game)
    - Optional: `packet_type` (`car_telemetry`, `lap_data`, `motion`).
- Calculation:
  - \(packetLossRatio = 1 - (receivedFrames / max(expectedFrames, 1))\)
  - Clamp to \([0, 1]\).
- Implementation sketch (conceptual, not full code):

```java
// Inside udp-ingest-service
public class PacketLossMetricsRecorder {
    // Map<sessionUid, RollingWindowStats>
    // Expose a Micrometer gauge that reads loss ratio from stats
}
```

#### 2.3. Rolling window

- Use a **time-based sliding window** (e.g. last 30 seconds) per session:
  - Store timestamps + counters in `RollingWindowStats`.
  - Periodically evict old entries (background task or on write).
- This makes `packet_loss_ratio` reflect **recent** health, not entire-session average.

---

### 3. Diagnostics model in processing API

**Goal:** unify how diagnostics are represented in the public REST API.

#### 3.1. New DTOs (in `telemetry-api-contracts`)

- Add a diagnostics DTO, for example in `telemetry-api-contracts`:
  - `SessionDiagnosticsDto`:
    - `String sessionPublicId`
    - `Double packetLossRatio` (0.0–1.0, nullable when unknown)
    - `String packetHealthBand` (`GOOD`, `OK`, `POOR`, or `UNKNOWN`)
    - `Integer packetHealthPercent` (0–100, optional)
    - Reserved fields for future metrics (CPU, Kafka lag, etc.).

#### 3.2. Health band mapping

- Implement mapping **inside processing service**, not in controller:
  - Source: numeric `packetLossRatio`.
  - Derive:
    - `packetHealthPercent = (1 - packetLossRatio) * 100`
    - `packetHealthBand` by thresholds (configurable):
      - `GOOD` if `packetHealthPercent >= 90`
      - `OK` if `70 <= packetHealthPercent < 90`
      - `POOR` if `packetHealthPercent < 70`
      - `UNKNOWN` if ratio is null.
- Thresholds:
  - Keep in Spring config properties (e.g. `PacketHealthProperties`).
  - Allow future tuning without code changes.

---

### 4. Diagnostics service and mapper (telemetry-processing-api-service)

Follow layering rules (service + mapper, thin controller).

#### 4.1. Metrics access abstraction

- Add an abstraction to **read observability metrics** from Micrometer / Prometheus:
  - E.g. `PacketLossMetricsReader` interface:
    - `Optional<Double> getPacketLossRatioBySessionUid(long sessionUid)`
    - Optionally `Optional<Double> getGlobalPacketLossRatio()`
  - Implementation options (choose simplest first):
    - **Micrometer direct access**:
      - Inject `MeterRegistry`.
      - Read `Gauge`/`FunctionCounter` for `f1.telemetry.packet_loss_ratio` with tag `session_uid`.
    - **Prometheus scraping** (later if needed):
      - Call Prometheus HTTP API from processing service (more flexible but heavier).
- Keep this class in a small **diagnostics/metrics** package to avoid polluting services with Micrometer details.

#### 4.2. New service: `DiagnosticsService`

- Responsibilities:
  - Resolve session (by public id → internal `sessionUid`).
  - Query `PacketLossMetricsReader`.
  - Map numeric ratio → percent + band.
  - Build and return `SessionDiagnosticsDto`.
- Example public methods:
  - `SessionDiagnosticsDto getSessionDiagnostics(String sessionPublicId)`
  - Optional: `GlobalDiagnosticsDto getGlobalDiagnostics()`

#### 4.3. Mapper

- Implement `DiagnosticsMapper` (or similar) to construct DTOs:
  - Input: `sessionPublicId`, `Optional<Double> packetLossRatio`, computed percent + band.
  - Output: `SessionDiagnosticsDto`.
- Mapper must be **pure** (no repositories, no business logic), only transformation.

---

### 5. REST API: diagnostics controller

#### 5.1. Endpoints

- Add a new controller, e.g. `DiagnosticsController` in `telemetry-processing-api-service`:
  - `GET /api/sessions/{publicId}/diagnostics`
    - Path variable: `publicId` (same type as existing session endpoints).
    - Returns `SessionDiagnosticsDto`.
  - Optionally later:
    - `GET /api/diagnostics` → global/system-level diagnostics.

#### 5.2. Behaviour

- **When session exists and metric is available:**
  - Return full DTO with `packetLossRatio`, `packetHealthPercent` and `packetHealthBand`.
- **When session exists but metric is not available (no data yet):**
  - `packetLossRatio = null`, `packetHealthBand = "UNKNOWN"`, `packetHealthPercent = null`.
- **When session does not exist:**
  - Reuse existing session-not-found exception pattern and error handling.

#### 5.3. Logging

- Follow logging policy:
  - `log.debug("getSessionDiagnostics: sessionPublicId={}", publicId)` at method entry.
  - `log.debug("getSessionDiagnostics: packetHealthBand={}, packetLossRatio={}", band, ratio)` before returning, when useful.

---

### 6. Frontend changes (Dashboard + Settings diagnostics)

Module: `f1-telemetry-web-platform`.

#### 6.1. API client

- Extend the REST client layer to call diagnostics:
  - Add function, e.g. `getSessionDiagnostics(sessionPublicId: string)`:
    - `GET /api/sessions/{publicId}/diagnostics`
    - Returns a typed object:
      - `packetLossRatio?: number`
      - `packetHealthBand: 'GOOD' | 'OK' | 'POOR' | 'UNKNOWN'`
      - `packetHealthPercent?: number`
  - Handle errors via existing error handling (toast and typed errors).

#### 6.2. State integration

- Decide where diagnostics live:
  - Option A (simple): fetch diagnostics **directly in `Dashboard`** using React Query (or the existing data-fetch pattern) for the active session.
  - Option B (more structured): extend `LiveTelemetryProvider` or a dedicated `DiagnosticsProvider` to expose diagnostics through a hook such as `useDiagnostics()`.
- For MVP of Option C, Option A is enough:
  - Given `activeSession` (already known on Dashboard), call `getSessionDiagnostics(activeSession.publicId)`.
  - Poll every 5–10 seconds while the session is live.

#### 6.3. Dashboard packet health card

- Update `Dashboard.tsx`:
  - Replace placeholder `—` and empty bar with real values from diagnostics:
    - When `packetHealthBand === 'UNKNOWN'` or diagnostics missing → show `—`.
    - Otherwise:
      - Label: either `packetHealthPercent.toFixed(0) + '%'` or coarse labels (`Good`, `Ok`, `Poor`).
      - Bar width: use `packetHealthPercent` or map band to fixed widths (e.g. 30 / 60 / 100%).
      - Bar colour:
        - `GOOD` → green,
        - `OK` → yellow,
        - `POOR` → red.
  - Ensure the card gracefully falls back to the previous “approx.” placeholder when no active session.

#### 6.4. Settings → View Diagnostics page

- Use the **same diagnostics API**:
  - If page already exists:
    - Add a “Packet health” section showing:
      - Band (GOOD/OK/POOR/UNKNOWN).
      - Numeric percentage and loss ratio (for advanced users).
      - Short description / hint text (English only) explaining what the band means.
  - If page is not yet implemented:
    - Create a simple implementation:
      - Table or list of diagnostic items for the active session:
        - `Packet health` (band + percent)
        - (Placeholder rows for future metrics, e.g. CPU, Kafka lag).

---

### 7. Configuration and thresholds

- Add configuration properties in `telemetry-processing-api-service`, e.g.:
  - `packetHealth.threshold.goodMinPercent = 90`
  - `packetHealth.threshold.okMinPercent = 70`
- Expose them through a `@ConfigurationProperties` class used in `DiagnosticsService`.
- Document defaults in a short comment in the config file (English).

---

### 8. Testing strategy

#### 8.1. Backend unit tests

- `PacketLossMetricsRecorder`:
  - Verify loss ratio calculation for:
    - No expected frames (edge case).
    - Partial loss.
    - Full loss.
  - Verify rolling-window trimming logic.
- `DiagnosticsService`:
  - Session exists + metric present → correct band and percent.
  - Session exists + metric missing → `UNKNOWN` band.
  - Session does not exist → proper exception and error mapping.
- `DiagnosticsMapper`:
  - Pure mapping tests, including `null` ratios.
- `DiagnosticsController`:
  - Web-layer test (`@WebMvcTest` or existing pattern) checking status codes and payload structure.

#### 8.2. Frontend tests

- Unit or component tests for the Dashboard card:
  - `GOOD`, `OK`, `POOR`, `UNKNOWN` inputs produce correct labels, colours, and bar widths.
  - No diagnostics → `—`.
- If there is an existing test harness for API clients:
  - Add tests for `getSessionDiagnostics` response shape and error handling.

---

### 9. Rollout and migration

1. **Backend first**
   - Implement `PacketLossMetricsRecorder` and metric publishing.
   - Implement diagnostics reader, service, mapper, and controller.
   - Deploy and verify:
     - Metrics appear in Prometheus / Actuator.
     - Diagnostics endpoint returns expected values for a live session.
2. **Frontend**
   - Integrate diagnostics client and wire Dashboard card to the new endpoint.
   - Implement Settings diagnostics UI reusing the same API.
3. **Tuning**
   - Adjust health thresholds based on real-world packet patterns.
   - Optionally add more diagnostics signals (CPU, lag) to the payload and UI.

---

### 10. Scope checklist (done when all are implemented)

- [ ] `packet_loss_ratio` Micrometer metric emitted in `udp-ingest-service` per session.
- [ ] Diagnostics metric reader available in `telemetry-processing-api-service`.
- [ ] `SessionDiagnosticsDto` and related contracts added to `telemetry-api-contracts`.
- [ ] `DiagnosticsService` and `DiagnosticsMapper` implemented.
- [ ] `DiagnosticsController` exposes `GET /api/sessions/{publicId}/diagnostics`.
- [ ] Frontend diagnostics client implemented.
- [ ] Dashboard packet health card wired to diagnostics.
- [ ] Settings diagnostics page shows packet health using the same API.
- [ ] Unit tests added for metrics, diagnostics service, controller, and Dashboard card.

