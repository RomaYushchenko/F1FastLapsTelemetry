# All Cars Car Telemetry — Driver Comparison Data

**Goal:** Persist **high-frequency car telemetry** (speed, throttle, brake, lap distance, etc.) for **every car index** in the session, not only the player car, so **GET /comparison** and the **Driver Comparison** UI can show speed / pedal / delta traces for **any two drivers** on the same session.

**Status:** Draft (not implemented).

**References:**
- Current behaviour: `udp-ingest-service` — `CarTelemetryPacketHandler` uses `seekToPlayerCar` and publishes **one** event per UDP packet with `carIndex = playerCarIndex` (see `CarTelemetryEventBuilder`).
- Processing: `CarTelemetryProcessor` + `RawTelemetryWriter` → `telemetry.car_telemetry_raw` keyed by `(session_uid, frame_identifier, car_index, ts)`.
- Comparison API: `ComparisonQueryService`, `LapQueryService.getSpeedTrace` / `getLapTrace` — already parameterized by `carIndex`; empty traces for non-player cars today because ingest never writes those rows.
- UI: `f1-telemetry-web-platform` — `DriverComparison.tsx` consumes `speedTraceA/B`, `traceA/B`.

---

## 1. Context

### 1.1 Problem

- F1 **Packet Car Telemetry (id 6)** contains **22 × CarTelemetryData** slots per frame (one per grid slot).
- Ingest currently reads **only the player’s slot** and emits **one Kafka message** per UDP packet.
- The database therefore has `car_telemetry_raw` rows almost exclusively for **`car_index = player_car_index`**.
- **Lap times** for all cars still exist (`LapData` path → `telemetry.laps` per `car_index`), so the Comparison page can show two drivers’ **lap times** but **not** two full telemetry traces.

### 1.2 Success Criteria

- For a typical session with 20 cars on track, **each** `car_index` that appears in the game receives telemetry samples in `car_telemetry_raw` (subject to configurable limits — see §5).
- `GET /api/sessions/{sessionUid}/comparison?carIndexA=&carIndexB=` returns **non-empty** `speedTraceA` / `speedTraceB` and `traceA` / `traceB` when raw data exists for both cars on the selected reference laps.
- **No regression** for the player car: existing dashboards (Session Summary speed trace, pedal trace, Live) keep working.

---

## 2. Scope

| In scope | Out of scope (follow-ups) |
|----------|---------------------------|
| UDP ingest: parse **all 22** (or N) car slots from packet 6 | **Motion packet** for all cars (separate plan if needed for map / g-force) |
| Kafka: publish **multiple** car telemetry events per frame (or one batched message — see §3.2) | **Car Status** for all cars (22× status per frame); only if product needs it |
| Processing: idempotent write per `(sessionUid, frameId, carIndex)` | Changing **LapData** ingest (already multi-car) |
| Optional **feature flag / config**: “all cars” vs “player only” (rollback) | **Historical backfill** of old sessions (only new data after deploy) |
| Load testing notes; DB retention / sampling strategy | **Spectator / UDP bandwidth** modes in game (assume full packet as today) |

---

## 3. Design

### 3.1 UDP Ingest (`udp-ingest-service`)

| Step | Component | Description |
|------|-----------|-------------|
| 3.1.1 | **Config** | e.g. `f1.telemetry.udp.car-telemetry.mode=PLAYER_ONLY \| ALL_CARS` (default `ALL_CARS` after rollout, or `PLAYER_ONLY` for safe default). Optional: `maxCarIndexExclusive=22` or allowlist. |
| 3.1.2 | **Handler refactor** | `CarTelemetryPacketHandler`: loop `carIdx` from `0` to `21` (or `m_numActiveCars` if available in header/session — verify F1 25 struct). For each slot: `buffer.position(base + carIdx * CAR_TELEMETRY_DATA_SIZE)`, parse `CarTelemetryDto`, skip invalid / empty slots if game pads unused indices (document behaviour). |
| 3.1.3 | **Builder** | `CarTelemetryEventBuilder.build(header, telemetry, carIndex)` — **third parameter** is the **slot index** being published, **not** always `header.getPlayerCarIndex()`. |
| 3.1.4 | **Kafka key** | Use `sessionUID + "-" + carIndex` (or `sessionUID + "-" + frameId + "-" + carIndex` if partition balance needs tuning — prefer stable key per car for ordering). |
| 3.1.5 | **Publish rate** | **22 messages per UDP packet** vs **1 batched message** containing 22 payloads (see §3.2). |

### 3.2 Kafka Contract Decision

**Option A — Multiple messages per frame (simplest migration)**  
- Publish **up to 22** `CarTelemetryEvent` records per incoming packet 6, same topic `telemetry.carTelemetry`, same envelope shape, varying `carIndex`.  
- **Pros:** `CarTelemetryConsumer` and processor change minimally (one consumer record = one car).  
- **Cons:** ~22× Kafka write throughput and consumer CPU for packet 6.

**Option B — Single batched event**  
- New payload type e.g. `CarTelemetryBatchEvent` with `List<CarTelemetrySampleDto>` each with `carIndex` + fields.  
- **Pros:** One Kafka record per UDP packet.  
- **Cons:** New topic or new schema version; consumer must loop samples; idempotency key must cover batch + index.

**Recommendation:** Start with **Option A** for faster delivery and smaller contract risk; add **Option B** only if Kafka or consumer becomes the bottleneck (measure after Phase 1).

### 3.3 Processing (`telemetry-processing-api-service`)

| Step | Component | Description |
|------|-----------|-------------|
| 3.3.1 | **CarTelemetryConsumer** | Already uses `event.getCarIndex()` — verify **no code path forces player index** (e.g. `setPlayerCarIndex` should remain “who is human” for UI, not for overwriting `carIndex` on telemetry). |
| 3.3.2 | **CarTelemetryProcessor.process** | Already keyed by `carIndex`; ensure **watermarks** and **RawTelemetryWriter** use the event’s `carIndex`. |
| 3.3.3 | **Idempotency** | Confirm `markAsProcessed(sessionUid, frameId, packetId, carIndex)` is **per car** (if today it’s only per frame globally, fix to include `carIndex` so the 22 messages don’t dedupe each other). |
| 3.3.4 | **Lap number / lap distance** | `resolveLapNumberForTrace` uses per-car state — verify `SessionRuntimeState` snapshots / lap resolution work for **every** `carIndex` (may need LapData updates per car already in place). |

### 3.4 Database

- **Schema:** `car_telemetry_raw` already has `car_index`; **no migration** required for multi-car inserts.
- **Volume:** Roughly **22×** rows per frame vs today. Revisit:
  - `ProcessedPacketRetentionProperties` / raw retention policies,
  - indexes on `(session_uid, car_index, lap_number)`,
  - optional **downsampling** for non-player cars (e.g. store every 2nd frame for `carIndex != player`) — product decision; document in config.

### 3.5 API and UI

- **REST:** No contract change for comparison; traces fill when data exists.
- **Optional:** Response flag `telemetryCoverage: { carIndexA: "full" \| "laps_only", ... }` or document in OpenAPI that traces may be empty for cars without raw rows.
- **UI (`DriverComparison`):** Short note: *Telemetry traces are available for cars that have raw samples in this session (all cars after multi-car ingest is enabled).*

### 3.6 Testing Strategy

| Layer | What to test |
|-------|----------------|
| **Unit** | Parser: for a fixed buffer, all 22 slots parse; player slot matches previous golden test. |
| **Unit** | Builder: `carIndex` in event equals loop index. |
| **Integration** | Ingest publishes 22 events; or mock Kafka and assert count per frame. |
| **Processing** | Consumer writes 22 distinct `(frameId, carIndex)` rows; idempotency replay does not collapse cars. |
| **E2E** | Two car indices with data → comparison API returns two non-empty speed traces. |

---

## 4. Implementation Phases

### Phase 0 — Preconditions

- [ ] Read F1 25 struct for Packet 6: confirm **number of cars**, stride `CAR_TELEMETRY_DATA_SIZE`, and whether inactive slots are zeroed.
- [ ] Audit **idempotency** key for `telemetry.carTelemetry` (must include `carIndex`).

### Phase 1 — Ingest (all cars)

- [ ] Add configuration property for `PLAYER_ONLY` vs `ALL_CARS`.
- [ ] Refactor `CarTelemetryPacketHandler` to loop all slots; call parser per slot; publish one event per slot (Option A).
- [ ] Update `CarTelemetryEventBuilder` to accept explicit `carIndex`.
- [ ] Update/add tests in `CarTelemetryPacketHandlerTest` / builder tests.

### Phase 2 — Processing verification

- [ ] Verify consumer + processor + writer path for arbitrary `carIndex`.
- [ ] Fix idempotency if needed; add test with two cars same frame.
- [ ] Load test on dev/staging: Kafka lag, DB growth, processing latency.

### Phase 3 — Ops and product

- [ ] Document **DB size** expectations; tune retention or sampling if required.
- [ ] Enable `ALL_CARS` in staging; validate Comparison UI with two non-player cars (e.g. two AI indices with laps).
- [ ] Production rollout: flag default, monitoring alerts on consumer lag.

### Phase 4 — Optional enhancements

- [ ] Batch Kafka message (Option B) if metrics require it.
- [ ] Expose `gear` in comparison trace (extend `PedalTracePoint` / mapper) — separate small plan.
- [ ] Downsample non-player cars via config.

---

## 5. Risks and Mitigations

| Risk | Mitigation |
|------|------------|
| **22× Kafka + DB load** | Feature flag; sampling for non-player; batch publish later; partition scaling. |
| **Idempotency bug** | Duplicate or dropped cars; fix keys and add tests before rollout. |
| **Lap distance null** for some cars | Already mitigated in mapper (synthetic distance); monitor quality per `car_index`. |
| **Game sends garbage in empty slots** | Validate speed/gear ranges or “all zero” skip to avoid noise rows. |

---

## 6. Documentation Updates After Implementation

- [ ] `kafka_contracts_f_1_telemetry.md` — note multiple records per frame for `telemetry.carTelemetry` when `ALL_CARS` is on.
- [ ] `rest_web_socket_api_contracts_f_1_telemetry.md` — comparison trace availability.
- [ ] `block-g-driver-comparison.md` (if present) — align with multi-car raw data.

---

## 7. Checklist (Definition of Done)

- [ ] Config toggles between player-only and all-cars without code redeploy (or with profile).
- [ ] At least two different `car_index` values receive rows in `car_telemetry_raw` for the same session in integration test.
- [ ] Comparison endpoint returns populated `speedTraceA` and `speedTraceB` for two cars that completed laps with telemetry.
- [ ] No regression in existing player-car Session Summary / lap trace endpoints.
- [ ] English comments/Javadoc on new or changed public APIs per project rules.
