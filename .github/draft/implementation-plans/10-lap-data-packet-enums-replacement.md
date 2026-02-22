# Lap Data Packet (PacketLapData) — Enum Replacement

**Goal:** Replace raw integer fields from **PacketLapData** (packet 2) with **reference enums** for Pit Status, Sector, Current Lap Invalid, Driver Status, Result Status, and Pit Lane Timer Active. Ingest keeps sending raw codes; processing and API use enums for display and logic.

**Status:** Implemented.

**Reference:** `.github/draft/other-enume-types-deep-research-report.md` (§ Lap Data Packet Enums); `.github/docs/F1 25 Telemetry Output Structures.txt` (LapData struct).

---

## 1. Context

- **LapDto** and **LapDataPacketParser** already read: `pitStatus`, `sector`, `currentLapInvalid` (mapped to `isInvalid` boolean), `driverStatus`, `resultStatus`, `pitLaneTimerActive`.
- These are sent over Kafka and used in processing (LapDataProcessor, SessionRuntimeState snapshot). Currently exposed as numbers or booleans; no human-readable labels.

---

## 2. Enums to Add (telemetry-api-contracts)

Package: `com.ua.yushchenko.f1.fastlaps.telemetry.api.reference`. Each: code (int) + displayName (String); `fromCode(int)` with UNKNOWN for invalid.

| Enum | LapDto field | Codes (F1 25 spec) |
|------|--------------|---------------------|
| **PitStatus** | pitStatus | 0=none, 1=pitting, 2=in pit area |
| **Sector** | sector | 0=sector1, 1=sector2, 2=sector3 |
| **CurrentLapValid** | (currentLapInvalid 0/1) | 0=valid, 1=invalid — optional, we already have isInvalid boolean |
| **DriverStatus** | driverStatus | 0=in garage, 1=flying lap, 2=in lap, 3=out lap, 4=on track |
| **ResultStatus** | resultStatus | 0=invalid, 1=inactive, 2=active, 3=finished, 4=did not finish, 5=disqualified, 6=not classified, 7=retired |
| **PitLaneTimerActive** | pitLaneTimerActive | 0=inactive, 1=active |

---

## 3. Design

### 3.1 Ingest (udp-ingest-service)

- **No change** to LapDataPacketParser or LapDto. Keep Integer/sector, pitStatus, driverStatus, resultStatus, pitLaneTimerActive as int.

### 3.2 Processing and API

- **LapMapper** (or LapDto mapper in processing): when building REST DTOs (e.g. lap list or lap detail), add optional display fields: `pitStatusDisplayName`, `driverStatusDisplayName`, `resultStatusDisplayName`, `sectorDisplayName` using the new enums.
- **SessionRuntimeState.CarSnapshot**: currently has `currentSector` (Integer). Can remain; if we expose "current sector" as text (Sector 1/2/3), use Sector enum.
- **Live page / WebSocket**: if we show "Driver: In lap" or "Pit: In pit area", resolve via enums in WsSnapshotMessageBuilder or a small helper.

### 3.3 Backward compatibility

- Kafka contract and DB (lap table) keep storing codes (or no change if we don't persist these fields today). Enums are used only where we interpret or display.

---

## 4. Implementation Checklist

| # | Task | Location / notes |
|---|------|------------------|
| 1 | Add PitStatus, Sector, DriverStatus, ResultStatus, PitLaneTimerActive enums | telemetry-api-contracts/.../reference/ |
| 2 | LapMapper: add display name resolution for lap DTOs (pitStatus, driverStatus, resultStatus, sector) | telemetry-processing-api-service/.../mapper/LapMapper.java |
| 3 | Optional: WsSnapshotMessageBuilder — currentSector as display string | If Live UI shows sector as text |
| 4 | Unit tests for enums and mapper | Same module |
| 5 | **Documentation** | Update project docs (see § 5). |

---

## 5. Documentation updates

Keep project documentation up to date when implementing this plan.

| Document | What to update |
|----------|----------------|
| **.github/project/documentation_index.md** | If "Lap" or "REST API" sections describe lap DTOs, mention that pit/driver/result status and sector can be exposed as display names via reference enums (PitStatus, DriverStatus, ResultStatus, Sector). |
| **.github/project/react_spa_ui_architecture.md** | If Live or Session Detail shows lap status (pit/driver/sector) as text, note that display names come from LapMapper using enums from this plan. |

---

## 6. Dependencies

- Report and F1 25 spec for exact code lists.
- Plan **07** / **09** — same enum pattern (fromCode, getDisplayName).

---

## 7. Testing

- Enum tests: fromCode for each code, UNKNOWN for out-of-range.
- LapMapper test: Lap entity/DTO with pitStatus=1 → "Pitting" (or equivalent) when mapping to REST DTO.
