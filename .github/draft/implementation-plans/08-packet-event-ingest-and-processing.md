# Packet Event (ID 3) — Ingest and Processing

**Goal:** Handle F1 25 **Packet Event (packetId = 3)** end-to-end: UDP handler → parser → DTO/Kafka → telemetry-processing-api-service. Use **enums** for event detail codes (Retirement Reason, DRS Disabled Reason, Safety Car type/event).

**Status:** Implemented.

**Reference:** `.github/draft/other-enume-types-deep-research-report.md` (Lap Data Packet Enums, Event: Retirement / DRSDisabled / SafetyCar); `.github/docs/F1 25 Telemetry Output Structures.txt` (Event – 45 bytes, union EventDataDetails).

---

## 1. Context

- **Packet Event (3)** is **not** currently read. Session start/end (SSTA/SEND) are inferred from **Session packet (1)** via first bytes, not from Event packet.
- Packet Event carries **session-wide events**: Fastest Lap (FTLP), Retirement (RTMT), DRS Enabled/Disabled (DRSE/DRSD), Safety Car (SCAR), Penalty (PENA), Speed Trap (SPTP), Start Lights (STLG), Lights Out (LGOT), Flashback (FLBK), Red Flag (RDFL), Overtake (OVTK), Collision (COLL), etc.
- Event payload: **4-byte event code string** (e.g. `SSTA`, `DRSD`) + **union EventDataDetails** (layout depends on event type). Total packet = 29 (header) + 45 = 74 bytes (per spec).

---

## 1.1 Why EventEvent and topic telemetry.event are needed

The game sends **Packet Event (packetId = 3)** for **session-wide events** that are not carried in Session/Lap/Telemetry/Status packets:

- **DRSD** — DRS disabled (reason: wet track, safety car, red flag, min lap not reached). Needed to show the user *why* DRS is off (e.g. on Live or Session Detail). See plan **12 (DRS/ERS fix)**.
- **SCAR** — Safety car deployed / returning / returned / resume. Useful for session context and future UI (e.g. “VSC deployed”).
- **RTMT** — Driver retirement (reason: terminal damage, mechanical, etc.). Can be used for session summary or event log.
- **FTLP**, **PENA**, **SPTP**, **OVTK**, **COLL**, etc. — Fastest lap, penalties, speed trap, overtake, collision. Optional: event history, session replay, or analytics.

**EventEvent** is the Kafka message type (envelope + `EventDto` payload) on topic **telemetry.event**. It allows the processing service to consume these events once, with idempotency, and then use them in multiple places (processor, future REST/WS, DB).

---

## 1.2 Where EventEvent is used (and where to use it next)

| Where | Purpose |
|-------|--------|
| **udp-ingest-service** | `EventPacketHandler` parses UDP packet 3 → `EventEvent` → publish to `telemetry.event`. |
| **telemetry-processing-api-service** | **EventConsumer** reads `telemetry.event`, ensures session and idempotency, calls **EventProcessor**. **EventProcessor** logs DRSD/SCAR/RTMT with enum display names; can be extended to update session runtime state or persist to DB. |
| **Plan 12 (DRS/ERS fix)** | On **DRSD** event: set “DRS disabled reason” in session state (or Live snapshot) and expose to UI (e.g. “DRS disabled: Wet track”). Use **DrsDisabledReason** enum for display. |
| **Future** | Optional: **session_events** table (event_code, session_uid, frame_id, detail JSON) for event history; REST `GET /api/sessions/{uid}/events`; Live WebSocket could push DRS disabled / Safety car to the client. |

When adding new consumers or UI that need “why DRS is off” or “safety car status”, use **EventConsumer** output (EventProcessor) or read from session state that EventProcessor updates. Do not duplicate Event parsing in other services.

---

**Enums from report (to implement as reference enums):**

| Enum | Field / union member | Codes (summary) |
|------|----------------------|-----------------|
| Retirement Reason | `EventDataDetails.Retirement.reason` | 0=invalid, 1=retired, 2=finished, 3=terminal damage, 4=inactive, 5=not enough laps, 6=black flagged, 7=red flagged, 8=mechanical failure, 9=session skipped, 10=session simulated |
| DRS Disabled Reason | `EventDataDetails.DRSDisabled.reason` | 0=Wet track, 1=Safety car, 2=Red flag, 3=Min lap not reached |
| Safety Car type | `EventDataDetails.SafetyCar.safetyCarType` | 0=No SC, 1=Full SC, 2=Virtual SC, 3=Formation Lap SC |
| Safety Car event | `EventDataDetails.SafetyCar.eventType` | 0=Deployed, 1=Returning, 2=Returned, 3=Resume Race |

---

## 2. Scope

- **In scope:** Add handler for packetId 3; parser (ByteBuffer → DTO); Kafka event type and topic; optional processing (e.g. persist event log, or only use DRS disabled reason / Safety Car for session context). Enums for Retirement, DRSDisabled, SafetyCar in `telemetry-api-contracts`.
- **Out of scope:** Changing how SSTA/SEND are detected (remain from Session packet); full game logic (e.g. penalties interpretation); UI for event history in this plan (can be follow-up).

---

## 3. Design

### 3.1 UDP Ingest (udp-ingest-service)

| Step | Component | Description |
|------|-----------|-------------|
| 3.1.1 | `PacketId` | Add `EVENT` to enum in `telemetry-api-contracts` (order stable for existing ordinals). |
| 3.1.2 | Event payload parser | New `EventPacketParser`: read 4-byte event code string; switch on code and read union (FastestLap: vehicleIdx, lapTime; Retirement: vehicleIdx, reason; DRSDisabled: reason; SafetyCar: safetyCarType, eventType; etc.). Output **EventDto** (eventCode string, optional detail fields per type). |
| 3.1.3 | EventDto | New DTO in `telemetry-api-contracts`: `eventCode` (String, e.g. "DRSD"), then optional fields: `vehicleIdx`, `lapTime`, `retirementReason`, `drsDisabledReason`, `safetyCarType`, `safetyCarEventType`, etc. Use **integer codes** for reason/type; display mapping via enums in processing or API layer. |
| 3.1.4 | Event builder | `EventEventBuilder` (or `PacketEventEventBuilder`) builds Kafka event with header + EventDto. |
| 3.1.5 | Handler | `EventPacketHandler`: validate payload length ≥ 45; call parser → builder → publish to topic e.g. `telemetry.event`. |
| 3.1.6 | Kafka topic | New topic `telemetry.event` (or reuse existing if policy is one topic per packet type). Contract: envelope + EventDto. |

### 3.2 Reference Enums (telemetry-api-contracts)

- **RetirementReason** — code (int) + displayName.
- **DrsDisabledReason** — 0=Wet track, 1=Safety car, 2=Red flag, 3=Min lap not reached.
- **SafetyCarType** — 0–3 (No SC, Full, VSC, Formation Lap).
- **SafetyCarEventType** — 0–3 (Deployed, Returning, Returned, Resume Race).

### 3.3 Processing (telemetry-processing-api-service)

| Step | Component | Description |
|------|-----------|-------------|
| 3.3.1 | Consumer | `EventConsumer` for `telemetry.event`: deserialize, ensureSession (if needed), idempotency by (sessionUid, frameId), then delegate to processor. |
| 3.3.2 | Processor | `EventProcessor`: optional — persist event to `session_events` table (event_code, detail JSON/codes); or only update session runtime state (e.g. DRS disabled reason, Safety Car status) for Live/API. Prefer enums when writing display strings. |

### 3.4 Idempotency and Ordering

- Event packets have same header (sessionUID, frameIdentifier). Use same idempotency key (sessionUid, packetId, frameId) so duplicate events are skipped.
- Events are sparse; processing can be best-effort (no watermark merge with telemetry).

---

## 4. Implementation Checklist

| # | Task | Location / notes |
|---|------|------------------|
| 1 | Add `EVENT` to `PacketId` enum | `telemetry-api-contracts/.../kafka/PacketId.java` |
| 2 | Add RetirementReason, DrsDisabledReason, SafetyCarType, SafetyCarEventType | `telemetry-api-contracts/.../reference/` (or `api/kafka` if event-specific) |
| 3 | Add EventDto (eventCode + optional detail codes) | `telemetry-api-contracts/.../kafka/EventDto.java` |
| 4 | Add EventEvent (extends AbstractTelemetryEvent&lt;EventDto&gt;) | `telemetry-api-contracts` |
| 5 | Implement EventPacketParser (4-byte code + union) | `udp-ingest-service/.../parser/EventPacketParser.java` |
| 6 | Implement EventEventBuilder | `udp-ingest-service/.../builder/` |
| 7 | Implement EventPacketHandler, register packetId 3 | `udp-ingest-service/.../handler/EventPacketHandler.java` |
| 8 | Create Kafka topic `telemetry.event` (or config) | Infra / application.yml |
| 9 | EventConsumer + EventProcessor | `telemetry-processing-api-service/.../consumer/`, `.../processor/` |
| 10 | Optional: DB table session_events + writer | If product needs event history |
| 11 | **Documentation** | Update project docs (see § 5). |

---

## 5. Documentation updates

Keep project documentation up to date when implementing this plan.

| Document | What to update |
|----------|----------------|
| **.github/project/documentation_index.md** | Ensure draft plans section mentions plan 08 (Packet Event); add link to Kafka topic `telemetry.event` and new PacketId EVENT if documented elsewhere. |
| **udp-ingest-service/README.md** | Document that packetId 3 (Event) is handled; list EventPacketHandler and topic used. |
| **f1-telemetry-udp-spring/README.md** (or **f1-telemetry-udp-core/README.md**) | If Packet IDs table exists, add row: 3 = Event. |
| **.github/docs/** or **project/** Kafka/topics doc | If there is a list of Kafka topics or contracts, add `telemetry.event` and EventDto / event codes (DRSD, SCAR, RTMT, etc.). |
| **.github/draft/other-enume-types-deep-research-report.md** | Optional: add a short note that Event enums (RetirementReason, DrsDisabledReason, SafetyCarType, SafetyCarEventType) are implemented in telemetry-api-contracts (reference). |

---

## 6. Dependencies

- F1 25 spec: Event packet layout (45 bytes after header; union layout per event code).
- Plan **09 (Session data enums)** and **12 (DRS/ERS fix)** can consume DRS Disabled Reason and Safety Car from events for richer UI.

---

## 7. Testing

- Unit tests: EventPacketParser for each event type (DRSD + reason, SCAR + type/eventType, RTMT + reason).
- Handler test: payload length check, publish called with correct envelope.
- Consumer/processor: deserialize and idempotency; optional persistence.
