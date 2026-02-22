# Car Telemetry, Car Status, Car Damage — Enum Replacement

**Goal:** Replace raw integer fields in **CarTelemetryDto**, **CarStatusDto**, and **CarDamageDto** with **reference enums** where values have defined meanings. Ingest keeps sending raw codes; processing uses enums for snapshot, REST, and Live UI.

**Status:** Implemented.

**Reference:** `.github/docs/F1 25 Telemetry Output Structures.txt` (CarTelemetryData, CarStatusData, CarDamageData); `.github/draft/other-enume-types-deep-research-report.md`.

---

## 1. Context

- **CarTelemetryDto:** `drs` (Integer 0/1) — DRS wing open/closed.
- **CarStatusDto:** `drsAllowed` (Boolean), `ersDeployMode` (Integer). Traction control, fuel mix, pit limiter are also coded (see spec).
- **CarDamageDto:** `drsFault`, `ersFault` (Integer 0/1) — fault indicators.
- Plan **12 (DRS/ERS fix)** will correct **data source** for Live DRS (use telemetry `drs` for "open", status `drsAllowed` for "allowed"). This plan adds **enums** for type-safe handling and display.

---

## 2. Enums to Add (telemetry-api-contracts)

Package: `com.ua.yushchenko.f1.fastlaps.telemetry.api.reference`.

| Enum | DTO / field | Codes (F1 25 spec) |
|------|-------------|---------------------|
| **DrsState** (telemetry) | CarTelemetryDto.drs | 0=off, 1=on |
| **ErsDeployMode** | CarStatusDto.ersDeployMode | 0=none, 1=medium, 2=hotlap, 3=overtake |
| **FaultStatus** | CarDamageDto.drsFault, ersFault | 0=OK, 1=fault |
| **TractionControl** (optional) | CarStatusDto tractionControl | 0=off, 1=medium, 2=full |
| **FuelMix** (optional) | CarStatusDto.fuelMix | 0=lean, 1=standard, 2=rich, 3=max |
| **PitLimiterStatus** (optional) | CarStatusDto.pitLimiterStatus | 0=off, 1=on |

---

## 3. Design

### 3.1 Ingest (udp-ingest-service)

- **No change** to parsers or DTOs. CarTelemetryDto.drs stays Integer; CarStatusDto.ersDeployMode stays Integer; CarDamageDto.drsFault/ersFault stay Integer.

### 3.2 Processing and API

- **CarStatusProcessor:** when setting snapshot ERS deploy active, use `ErsDeployMode.fromCode(status.getErsDeployMode())` and e.g. `!= ErsDeployMode.NONE` for consistency; optional: set a display label for "ERS mode" (Medium/Hotlap/Overtake) in snapshot if Live UI shows it.
- **CarTelemetryProcessor:** when merging DRS into snapshot (see plan 12), use `DrsState.fromCode(telemetry.getDrs())` and set snapshot DRS from that.
- **REST / WebSocket:** WsSnapshotMessageBuilder can expose `ersDeployModeDisplayName` (or keep boolean ersDeployActive only); damage API can expose `drsFaultDisplayName`, `ersFaultDisplayName` if we add a damage endpoint.
- **CarStatusRaw / CarTelemetryRaw:** persist raw codes as today; if we add a "session car status" REST that returns last status, mapper can add display names via enums.

### 3.3 Backward compatibility

- Kafka and DB keep Integer/short; enums used only in Java logic and when building display strings.

---

## 4. Implementation Checklist

| # | Task | Location / notes |
|---|------|------------------|
| 1 | Add DrsState, ErsDeployMode, FaultStatus enums | telemetry-api-contracts/.../reference/ |
| 2 | Optional: TractionControl, FuelMix, PitLimiterStatus | Same package (if we expose in API) |
| 3 | CarStatusProcessor: use ErsDeployMode for ersDeployActive and optional display | telemetry-processing-api-service/.../processor/CarStatusProcessor.java |
| 4 | After plan 12: CarTelemetryProcessor use DrsState for snapshot DRS | CarTelemetryProcessor.java |
| 5 | Optional: WsSnapshotMessageBuilder / REST DTO add ersDeployModeDisplayName | If Live shows ERS mode text |
| 6 | Unit tests for enums | telemetry-api-contracts or processing |
| 7 | **Documentation** | Update project docs (see § 5). |

---

## 5. Documentation updates

Keep project documentation up to date when implementing this plan.

| Document | What to update |
|----------|----------------|
| **.github/project/documentation_index.md** | If "Live" or "WebSocket" sections describe snapshot fields, note that DRS/ERS/fault display can use reference enums (DrsState, ErsDeployMode, FaultStatus). |
| **.github/project/react_spa_ui_architecture.md** | If § Live dashboard lists DRS/ERS: note that display labels (e.g. ERS mode "Hotlap", "Overtake") come from enums in telemetry-api-contracts. |

---

## 6. Dependencies

- **Plan 12 (DRS/ERS fix):** DRS snapshot source will use Car Telemetry; both plans share DrsState and ErsDeployMode enums.

---

## 7. Testing

- Enum fromCode for each code; UNKNOWN for out-of-range.
- Processor/snapshot: ersDeployMode 2 → ersDeployActive true and optional "Hotlap" label.
