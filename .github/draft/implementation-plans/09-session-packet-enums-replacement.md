# Session Packet (PacketSessionData) — Enum Replacement

**Goal:** Replace raw integer fields from **PacketSessionData** (packet 1) with **reference enums** where values have defined meanings. Ingest continues to send raw codes (Integer); processing and API use enums for mapping and display.

**Status:** Implemented.

**Reference:** `.github/draft/other-enume-types-deep-research-report.md` (Weather, Track/Air Temp Change, Zone Flag, Formula, SLI Pro, Safety Car Status, Network Game, Forecast Accuracy, Steering/Braking/Gearbox/Pit/ERS/DRS Assist, Dynamic Racing Line, Session Length, Speed/Temperature Units, etc.).

---

## 1. Context

- **SessionDataDto** (and SessionDataPacketParser) already read all PacketSessionData fields as `Integer` or `int[]` (e.g. `weather`, `formula`, `safetyCarStatus`, `marshalZoneFlags`, `ersAssist`, `drsAssist`).
- Plan **07** establishes a single place for session type and track display (F1SessionType, F1Track). This plan extends the same pattern to **all other session enums** from the F1 25 spec.
- **Principle:** Keep Kafka/ingest DTOs with raw codes (Integer) for compatibility and size; introduce **reference enums** in `telemetry-api-contracts` and use them in processing when persisting or exposing to REST (e.g. future "session settings" or admin API).

---

## 2. Enums to Add (telemetry-api-contracts)

All in package `com.ua.yushchenko.f1.fastlaps.telemetry.api.reference` (or a dedicated `session` subpackage). Each enum: code (int) + displayName (String); static `fromCode(int)` returning UNKNOWN for invalid.

| Enum | SessionDataDto field(s) | Codes (from report) |
|------|-------------------------|----------------------|
| **Weather** | weather, weatherForecastWeather[] | 0=clear, 1=light cloud, 2=overcast, 3=light rain, 4=heavy rain, 5=storm |
| **TemperatureChange** | trackTemperatureChange, airTemperatureChange (in forecast); wfTrackTempChange, wfAirTempChange | 0=up, 1=down, 2=no change |
| **MarshalZoneFlag** | marshalZoneFlags[] | -1=invalid, 0=none, 1=green, 2=blue, 3=yellow |
| **Formula** | formula | 0=F1 Modern, 1=F1 Classic, 2=F2, 3=F1 Generic, 4=Beta, 6=Esports, (+ 8,9 if in spec) |
| **SliProSupport** | sliProNativeSupport | 0=inactive, 1=active |
| **SafetyCarStatus** | safetyCarStatus | 0=no safety car, 1=full, 2=virtual, 3=formation lap |
| **NetworkGame** | networkGame | 0=offline, 1=online |
| **ForecastAccuracy** | forecastAccuracy | 0=Perfect, 1=Approximate |
| **SteeringAssist** | steeringAssist | 0=off, 1=on |
| **BrakingAssist** | brakingAssist | 0=off, 1=low, 2=medium, 3=high |
| **GearboxAssist** | gearboxAssist | 1=manual, 2=manual+suggested gear, 3=auto |
| **PitAssist**, **PitReleaseAssist**, **ERSAssist**, **DRSAssist** | pitAssist, pitReleaseAssist, ersAssist, drsAssist | 0=off, 1=on |
| **DynamicRacingLine** | dynamicRacingLine | 0=off, 1=corners only, 2=full |
| **DynamicRacingLineType** | dynamicRacingLineType | 0=2D, 1=3D |
| **SessionLength** | sessionLength | 0=None, 2=Very Short, 3=Short, 4=Medium, 5=Medium Long, 6=Long, 7=Full |
| **SpeedUnits**, **TemperatureUnits** | speedUnitsLeadPlayer, temperatureUnitsLeadPlayer, speedUnitsSecondaryPlayer, temperatureUnitsSecondaryPlayer | 0=MPH/Celsius, 1=KPH/Fahrenheit |
| **EqualCarPerformance** | equalCarPerformance | 0=Off, 1=On |
| **RecoveryMode** | recoveryMode | 0=None, 1=Flashbacks, 2=Auto-recovery |
| **FlashbackLimit** | flashbackLimit | 0=Low, 1=Medium, 2=High, 3=Unlimited |
| **SurfaceType** | surfaceType | 0=Simplified, 1=Realistic |
| **LowFuelMode** | lowFuelMode | 0=Easy, 1=Hard |
| **RaceStarts** | raceStarts | 0=Manual, 1=Assisted |
| **TyreTemperature** (pit) | tyreTemperature | 0=Surface only, 1=Surface & Carcass |
| **PitLaneTyreSim** | pitLaneTyreSim | 0=On, 1=Off |
| **CarDamage** (setting) | carDamage | 0=Off, 1=Reduced, 2=Standard, 3=Simulation |
| **CarDamageRate** | carDamageRate | 0=Reduced, 1=Standard, 2=Simulation |
| **Collisions** | collisions | 0=Off, 1=Player-to-Player Off, 2=On |
| **CollisionsOffForFirstLapOnly** | collisionsOffForFirstLapOnly | 0=Disabled, 1=Enabled |
| **MpUnsafePitRelease** | mpUnsafePitRelease | 0=On, 1=Off |
| **MpOffForGriefing** | mpOffForGriefing | 0=Disabled, 1=Enabled |
| **CornerCuttingStringency** | cornerCuttingStringency | 0=Regular, 1=Strict |
| **ParcFermeRules** | parcFermeRules | 0=Off, 1=On |
| **PitStopExperience** | pitStopExperience | 0=Automatic, 1=Broadcast, 2=Immersive |
| **SafetyCar** (setting) | safetyCar | 0=Off, 1=Reduced, 2=Standard, 3=Increased |
| **SafetyCarExperience** | safetyCarExperience | 0=Broadcast, 1=Immersive |
| **FormationLap** | formationLap | 0=Off, 1=On |
| **FormationLapExperience** | formationLapExperience | 0=Broadcast, 1=Immersive |
| **RedFlags** | redFlags | 0=Off, 1=Reduced, 2=Standard, 3=Increased |
| **AffectsLicenceLevel** | affectsLicenceLevelSolo, affectsLicenceLevelMP | 0=Off, 1=On |

---

## 3. Design

### 3.1 Ingest (udp-ingest-service)

- **No change** to SessionDataPacketParser or SessionDataDto. Continue sending Integer/int[] over Kafka.

### 3.2 Processing and API

- When **persisting** session data (if we add a session_settings or session_data table): map each code via the corresponding enum and store either code or display string as needed.
- When **exposing** session data via REST (e.g. GET session with options): add optional display fields (e.g. `weatherDisplayName`) by resolving from enum in a mapper.
- **SessionMapper** (or a dedicated SessionDataMapper): for any SessionDataDto field that has an enum, provide `XxxDisplayName fromCode(Integer code)` usage in one place.

### 3.3 Phasing

- **Phase 1:** Add enums for the most used/visible fields: Weather, SafetyCarStatus, Formula, MarshalZoneFlag, SessionLength.
- **Phase 2:** Add remaining enums; use in mapper when building REST DTOs that include session settings.

---

## 4. Implementation Checklist

| # | Task | Location / notes |
|---|------|------------------|
| 1 | Add Weather, TemperatureChange, MarshalZoneFlag, Formula | telemetry-api-contracts/.../reference/ |
| 2 | Add SliProSupport, SafetyCarStatus, NetworkGame, ForecastAccuracy | Same package |
| 3 | Add assist enums (Steering, Braking, Gearbox, Pit, ERS, DRS), DynamicRacingLine, SessionLength, Units | Same package |
| 4 | Add game rules enums (RecoveryMode, FlashbackLimit, SurfaceType, CarDamage, Collisions, etc.) | Same package |
| 5 | SessionDataMapper or extend SessionMapper: map codes → display names for REST | telemetry-processing-api-service/.../mapper/ |
| 6 | Optional: persist session settings with enum-resolved display names | If product needs session options in DB |
| 7 | **Documentation** | Update project docs (see § 5). |

---

## 5. Documentation updates

Keep project documentation up to date when implementing this plan.

| Document | What to update |
|----------|----------------|
| **.github/project/documentation_index.md** | If a "Reference enums" or "Session data" subsection exists, add reference to Session Packet enums (Weather, Formula, MarshalZoneFlag, etc.) and link to this plan. |
| **.github/docs/session_type_mapping.md** | Note that session type and track are in plan 07; **session options** (weather, formula, safety car status, assists, etc.) are mapped via enums from this plan (09) in SessionMapper / SessionDataMapper. |
| **.github/draft/other-enume-types-deep-research-report.md** | Optional: add a short note that Session Packet enums are implemented in telemetry-api-contracts (reference package) and used in processing mappers. |

---

## 6. Dependencies

- Plan **07** (single place session type/track) — same pattern.
- F1 25 spec and report for exact code lists (some gaps in report marked "absent"; use UNKNOWN for out-of-range).

---

## 7. Testing

- Unit tests for each enum: fromCode for every code, UNKNOWN for -1/255/out-of-range.
- Mapper tests: SessionDataDto with sample codes → display names present where expected.
