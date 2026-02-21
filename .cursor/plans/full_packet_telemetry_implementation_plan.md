# Full packet telemetry — implementation plan

Extend UDP ingest models, DTOs, and parsers so they contain **all** data from F1 25 telemetry packets, as defined in the official specification. Currently we only parse a subset of fields; this plan aligns our code with the full packet layout.

---

## Documentation references

| Document | Purpose |
|----------|---------|
| [F1 25 Telemetry Output Structures.txt](../../.github/docs/F1%2025%20Telemetry%20Output%20Structures.txt) | **Primary reference** — C++ structs and byte layout for all packet types (Header, Session, Lap, Car Telemetry, Car Status, Car Damage, etc.). All new fields and parser logic must match this file. |
| [kafka_contracts_f_1_telemetry.md](../../.github/project/kafka_contracts_f_1_telemetry.md) | Kafka topics and payload contracts; extend payload DTOs here consistently. |
| [architecture-layering.mdc](../rules/architecture-layering.mdc) | Parsers: ByteBuffer → DTO only. Builders: header + DTO → event. No business logic in handlers. |
| [udp_ingest_refactoring_plan.md](udp_ingest_refactoring_plan.md) | Ingest service structure; parsers live in `udp-ingest-service/.../parser/`. |
| [unit_testing_policy.md](../../.github/project/unit_testing_policy.md) | Unit tests: JUnit 5, Mockito, AssertJ, TestData, @DisplayName, coverage; update or add tests when DTOs/parsers change. |

---

## Documentation and model updates (required)

As part of this work, the following must be updated so that contracts, docs, and code stay in sync.

### Documentation to update

| Document | What to update | When |
|----------|----------------|------|
| [kafka_contracts_f_1_telemetry.md](../../.github/project/kafka_contracts_f_1_telemetry.md) | Payload schemas for each topic: extend § 5.2 (Lap), § 5.3 (CarTelemetry), § 5.4 (CarStatus), § 5.x (CarDamage if present) with **all new DTO fields** (name, type, optional/required, short description). Add reference to [F1 25 Telemetry Output Structures.txt](../../.github/docs/F1%2025%20Telemetry%20Output%20Structures.txt) for field semantics. | After each phase (1–4) when DTO is extended. |
| [documentation_index.md](../../.github/project/documentation_index.md) | If any new doc is added (e.g. ingest payload reference), list it. | If new docs created. |
| [udp_ingest_refactoring_plan.md](udp_ingest_refactoring_plan.md) or udp-ingest-service README | State that parsers and DTOs cover **full F1 25 packet payload** per `.github/docs/F1 25 Telemetry Output Structures.txt`; list packet types and byte sizes. | After Phases 1–4 are done. |
| [F1 25 Telemetry Output Structures.txt](../../.github/docs/F1%2025%20Telemetry%20Output%20Structures.txt) | No edits; it is the **source of truth**. All new fields in DTOs/parsers must match this file. | — |

### Models to add or update

| Layer | What | Where | When |
|-------|------|--------|------|
| **Kafka payload (DTOs)** | Extend `CarTelemetryDto`, `LapDto`, `CarStatusDto`, `CarDamageDto` with all packet fields from the F1 25 spec. | `telemetry-api-contracts` → `api/kafka/*Dto.java` | Phases 1–4 (see steps below). |
| **Persistence entities** | If new fields are persisted: add columns and map them in `CarTelemetryRaw`, `CarStatusRaw`, or other raw/aggregate entities. | `telemetry-processing-api-service` → `persistence/entity/`, DB migrations | Phase 5 (optional). |
| **REST/API response models** | If any REST or WebSocket DTO exposes telemetry/lap/status/damage data, extend those DTOs when new fields are needed for the API. | `telemetry-api-contracts` → `api/rest/`, `api/ws/` | When API contract is extended. |

- **DTOs (Kafka payload models):** Adding fields is mandatory in Phases 1–4; Javadoc on each new field should reference the F1 25 doc (e.g. “F1 25 CarTelemetryData m_brakesTemperature”).
- **Persistence models:** Add or update only when we decide to store new fields (Phase 5 or later); include Flyway/Liquibase migration and repository/entity changes.
- **REST/WS models:** Update only when the public API must expose the new data.

### Unit tests to update or add

Existing unit tests must stay green after DTO and parser changes; new behaviour must be covered. Follow [unit_testing_policy.md](../../.github/project/unit_testing_policy.md) and [unit-testing-policy.mdc](../rules/unit-testing-policy.mdc) (JUnit 5, Mockito, AssertJ, TestData, @DisplayName, 85% coverage where applicable).

| Scope | What to do | When |
|-------|------------|------|
| **udp-ingest-service** | **Parsers:** Update or add parser unit tests so that (1) a ByteBuffer with known bytes produces a DTO with expected values for **all** mapped fields, including new ones; (2) buffer position advances by exactly the struct size (60 / 57 / 55 / 46 bytes). **Handlers:** If handler tests stub the parser, ensure they still pass; extend handler tests if they assert on DTO shape or parser behaviour. Use test data from a central TestData (or equivalent) where it exists. | Each phase (1–4) when the parser and DTO change. |
| **telemetry-processing-api-service** | **Processors / writers:** If code starts using new DTO fields, add or update tests for that behaviour. If only existing getters are used, existing tests should still pass; run `mvn -pl telemetry-processing-api-service verify` to keep coverage ≥ 85%. | Phase 5 or when new fields are used. |
| **telemetry-api-contracts** | DTOs are data holders; no unit tests required for the DTOs themselves. Builder/usage is covered by parser and consumer tests. | — |

- **Update existing tests:** When a parser’s return type gains new fields, adjust tests that build or assert on the DTO (e.g. assertions on parsed values, mocks returning DTOs) so they remain valid and, where useful, assert new fields.
- **Add new tests:** Add tests for new parser behaviour (e.g. parsing full 60/57/55/46 bytes, all new fields populated). One test class per parser is sufficient; use @DisplayName and AAA (Arrange/Act/Assert).

---

## Current state vs specification (gap analysis)

### 1. Session (packetId = 1)

- **Handler:** `SessionPacketHandler` — `@F1PacketHandler(packetId = 1)`.
- **Doc:** Packet type 1 = **PacketSessionData** (753 bytes): weather, temperatures, totalLaps, trackLength, sessionType, trackId, sessionTimeLeft, sessionDuration, pitSpeedLimit, marshalZones, weatherForecastSamples, and many session/assist/game settings (see doc lines 116–202).
- **Current parser:** `SessionPacketParser` parses **4-byte event code + skip 20 + sessionType, trackId, totalLaps** — this layout matches **PacketEventData** (event packet, id=3), not PacketSessionData. So either:
  - The handler is used with a different payload in practice (e.g. event payload forwarded as “session”), or
  - There is a mismatch: packetId 1 in the doc is Session (753 bytes), not Event.
- **Current DTO:** `SessionEventDto` — eventCode, sessionType, sessionTypeId, trackId, totalLaps only.
- **Gap:** If we are to support **full Session packet (id=1)** as per doc, we need a separate **SessionData** DTO and parser for PacketSessionData (all fields from doc § Session). SSTA/SEND events come from **Event packet (id=3)** with event code + EventDataDetails; that can stay as today (SessionEventDto) or be extended with event-specific details. **Recommendation:** Keep current Session handler semantics (SSTA/SEND + session info); add an optional **SessionDataDto** and **SessionDataPacketParser** for packetId=1 full payload if we want to ingest full session data in the future. For this plan we **do not change** Session parser/DTO (out of scope) unless we explicitly add “full Session packet” as a separate task.

### 2. Car Telemetry (packetId = 6)

- **Doc:** `PacketCarTelemetryData` — header + `CarTelemetryData[22]` (60 bytes per car) + MFD/suggested gear (doc lines 363–386). **CarTelemetryData:** speed, throttle, steer, brake, clutch, gear, engineRPM, drs, revLightsPercent, revLightsBitValue, brakesTemperature[4], tyresSurfaceTemperature[4], tyresInnerTemperature[4], engineTemperature, tyresPressure[4], surfaceType[4].
- **Current DTO:** `CarTelemetryDto` — speedKph, throttle, brake, steer, gear, engineRpm, drs (7 fields).
- **Missing in DTO:** clutch, revLightsPercent, revLightsBitValue, brakesTemperature[4], tyresSurfaceTemperature[4], tyresInnerTemperature[4], engineTemperature, tyresPressure[4], surfaceType[4]. Packet-level: mfdPanelIndex, mfdPanelIndexSecondaryPlayer, suggestedGear.
- **Parser:** Reads and discards the missing fields; only maps the 7 fields above.

### 3. Lap Data (packetId = 2)

- **Doc:** `PacketLapData` — header + `LapData[22]` (57 bytes per car) + timeTrialPBCarIdx, timeTrialRivalCarIdx (doc lines 248–255). **LapData:** lastLapTimeInMS, currentLapTimeInMS, sector1/2 (ms+min parts), deltaToCarInFront (ms+min), deltaToRaceLeader (ms+min), lapDistance, totalDistance, safetyCarDelta, carPosition, currentLapNum, pitStatus, numPitStops, sector, currentLapInvalid, penalties, totalWarnings, cornerCuttingWarnings, numUnservedDriveThroughPens, numUnservedStopGoPens, gridPosition, driverStatus, resultStatus, pitLaneTimerActive, pitLaneTimeInLaneInMS, pitStopTimerInMS, pitStopShouldServePen, speedTrapFastestSpeed, speedTrapFastestLap.
- **Current DTO:** `LapDto` — lapNumber, lapDistance, lastLapTimeMs, currentLapTimeMs, sector1TimeMs, sector2TimeMs, sector, isInvalid, penaltiesSeconds (9 fields).
- **Missing in DTO:** sector2 time (we have sector1/sector2 but not sector3), deltaToCarInFront, deltaToRaceLeader, totalDistance, safetyCarDelta, carPosition, pitStatus, numPitStops, totalWarnings, cornerCuttingWarnings, numUnservedDriveThroughPens, numUnservedStopGoPens, gridPosition, driverStatus, resultStatus, pitLaneTimerActive, pitLaneTimeInLaneInMS, pitStopTimerInMS, pitStopShouldServePen, speedTrapFastestSpeed, speedTrapFastestLap. Packet-level: timeTrialPBCarIdx, timeTrialRivalCarIdx. Note: sector3 is not in LapData in the doc (only sector1 and sector2 time parts in the struct).
- **Parser:** Skips many bytes with `buffer.getShort()`, `buffer.get()` etc.; maps only the fields present in LapDto.

### 4. Car Status (packetId = 7)

- **Doc:** `PacketCarStatusData` — header + `CarStatusData[22]` (55 bytes per car) (doc lines 396–402). **CarStatusData:** tractionControl, antiLockBrakes, fuelMix, frontBrakeBias, pitLimiterStatus, fuelInTank, fuelCapacity, fuelRemainingLaps, maxRPM, idleRPM, maxGears, drsAllowed, drsActivationDistance, actualTyreCompound, visualTyreCompound, tyresAgeLaps, vehicleFIAFlags, enginePowerICE, enginePowerMGUK, ersStoreEnergy, ersDeployMode, ersHarvestedThisLapMGUK, ersHarvestedThisLapMGUH, ersDeployedThisLap, networkPaused.
- **Current DTO:** `CarStatusDto` — tractionControl, abs, fuelInTank, fuelMix, drsAllowed, tyresCompound, tyresAgeLaps, ersStoreEnergy (8 fields).
- **Missing in DTO:** frontBrakeBias, pitLimiterStatus, fuelCapacity, fuelRemainingLaps, maxRPM, idleRPM, maxGears, drsActivationDistance, visualTyreCompound, vehicleFIAFlags, enginePowerICE, enginePowerMGUK, ersDeployMode, ersHarvestedThisLapMGUK, ersHarvestedThisLapMGUH, ersDeployedThisLap, networkPaused.
- **Parser:** Reads and skips the missing fields (e.g. CAR_STATUS_TAIL_SKIP); maps only the 8 fields above.

### 5. Car Damage (packetId = 10)

- **Doc:** `PacketCarDamageData` — header + `CarDamageData[22]` (46 bytes per car) (doc lines 451–457). **CarDamageData:** tyresWear[4], tyresDamage[4], brakesDamage[4], tyreBlisters[4], frontLeftWingDamage, frontRightWingDamage, rearWingDamage, floorDamage, diffuserDamage, sidepodDamage, drsFault, ersFault, gearBoxDamage, engineDamage, engineMGUHWear, engineESWear, engineCEWear, engineICEWear, engineMGUKWear, engineTCWear, engineBlown, engineSeized.
- **Current DTO:** `CarDamageDto` — tyresWearFL, tyresWearFR, tyresWearRL, tyresWearRR (4 fields).
- **Missing in DTO:** tyresDamage[4], brakesDamage[4], tyreBlisters[4], frontLeftWingDamage, frontRightWingDamage, rearWingDamage, floorDamage, diffuserDamage, sidepodDamage, drsFault, ersFault, gearBoxDamage, engineDamage, engineMGUHWear, engineESWear, engineCEWear, engineICEWear, engineMGUKWear, engineTCWear, engineBlown, engineSeized.
- **Parser:** Reads only 4 floats (tyre wear); does not advance through the rest of CarDamageData (22 cars × 46 bytes); handler seeks to player car then parses 16 bytes. So the handler is correct (seek + parse one car); parser must be extended to read full 46 bytes per car and fill full DTO.

---

## Implementation steps (detailed)

### Phase 0: Preparation and validation

1. **Confirm byte sizes** from [F1 25 Telemetry Output Structures.txt](../../.github/docs/F1%2025%20Telemetry%20Output%20Structures.txt):
   - CarTelemetryData: 2+4+4+4+1+1+2+1+1+2+8+4+4+2+16+4 = **60 bytes** (already used in handler).
   - LapData: 4+4+2+1+2+1+2+1+2+1+4+4+4+1+1+1+1+1+1+1+1+1+1+1+1+1+2+2+1+4+1 = **57 bytes** (already used).
   - CarStatusData: **55 bytes** (already used).
   - CarDamageData: 16+4+4+4+5×1+10×1+2×1 = **46 bytes** (already used).
2. **Unit tests:** In udp-ingest-service, ensure each parser has (or will get in its phase) tests that use a ByteBuffer of the correct size and assert the parser advances by that many bytes and returns a DTO with expected values. See section “Unit tests to update or add” above.

---

### Phase 1: Car Telemetry — full CarTelemetryData and packet trailer

**Doc:** [F1 25 Telemetry Output Structures.txt](../../.github/docs/F1%2025%20Telemetry%20Output%20Structures.txt) lines 363–386 (CarTelemetryData, PacketCarTelemetryData).

**Steps:**

1. **Extend `CarTelemetryDto`** (telemetry-api-contracts):
   - Add: `clutch` (Integer 0–100), `revLightsPercent` (Integer), `revLightsBitValue` (Integer), `brakesTemperatureRL/RR/FL/FR` (int[] or four Integers), `tyresSurfaceTemperatureRL/RR/FL/FR` (int[] or four Integers), `tyresInnerTemperatureRL/RR/FL/FR` (int[] or four Integers), `engineTemperature` (Integer), `tyresPressureRL/RR/FL/FR` (float[] or four Floats), `surfaceTypeRL/RR/FL/FR` (int[] or four Integers).
   - Add packet-level (optional, if we parse after the 22 cars): `mfdPanelIndex` (Integer), `mfdPanelIndexSecondaryPlayer` (Integer), `suggestedGear` (Integer). These live after the 22×60 bytes; handler currently only seeks to player car and parses one car — so we can either (a) add a second parser method that parses full packet and returns DTO + trailer, or (b) leave trailer for a later step. For “full data from packet” we at least extend **per-car** DTO and parser; trailer can be Phase 1b.
   - Keep all existing fields; use boxed types for new fields (nullable where appropriate).
2. **Update `CarTelemetryPacketParser`** (udp-ingest-service):
   - Read in order: speed, throttle, steer, brake, clutch, gear, engineRPM, drs, revLightsPercent, revLightsBitValue; then brakesTemperature[4], tyresSurfaceTemperature[4], tyresInnerTemperature[4], engineTemperature, tyresPressure[4], surfaceType[4]. Map all into the extended DTO. Reference doc for exact field order and types (uint16, float, uint8, int8, etc.).
   - Do not change handler contract: still parse **one car** (buffer positioned at that car by handler). Trailer (mfdPanelIndex, etc.) is after all 22 cars; if we need it, handler would need to either parse full packet or call a separate “parse trailer” after parsing player car (and skip remaining cars). For minimal change, Phase 1 = full **per-car** DTO only.
3. **Update documentation and models:** In [kafka_contracts_f_1_telemetry.md](../../.github/project/kafka_contracts_f_1_telemetry.md) extend the Car Telemetry payload schema (§ 5.3) with all new fields. Add Javadoc on new `CarTelemetryDto` fields referencing the F1 25 spec (e.g. `m_brakesTemperature`, `m_tyresPressure`).
4. **Update or add unit tests:** In udp-ingest-service, update existing parser/handler tests for the new DTO fields and parser behaviour. Add or extend parser tests: ByteBuffer of 60 bytes with known values → assert all DTO fields (including new ones) and that buffer position advances by 60. Fix any handler tests that depend on the previous DTO shape. See “Unit tests to update or add” and [unit_testing_policy.md](../../.github/project/unit_testing_policy.md).
5. **Build and tests:** `mvn -pl telemetry-api-contracts,udp-ingest-service clean compile test`. Fix any usages in telemetry-processing-api-service (processors/writers use only existing getters; no change required if we only add fields).

---

### Phase 2: Lap Data — full LapData

**Doc:** [F1 25 Telemetry Output Structures.txt](../../.github/docs/F1%2025%20Telemetry%20Output%20Structures.txt) lines 212–255 (LapData, PacketLapData).

**Steps:**

1. **Extend `LapDto`** (telemetry-api-contracts):
   - Add: sector2 (we already have sector1TimeMs, sector2TimeMs; doc has sector1MinutesPart+MsPart and sector2MinutesPart+MsPart — we can add sector3TimeMs if we compute from sector3 parts when present; doc LapData does not list sector3 time parts, only sector1 and sector2). So add: deltaToCarInFrontMs (Integer), deltaToRaceLeaderMs (Integer), totalDistance (Float), safetyCarDelta (Float), carPosition (Integer), pitStatus (Integer), numPitStops (Integer), totalWarnings (Integer), cornerCuttingWarnings (Integer), numUnservedDriveThroughPens (Integer), numUnservedStopGoPens (Integer), gridPosition (Integer), driverStatus (Integer), resultStatus (Integer), pitLaneTimerActive (Integer), pitLaneTimeInLaneInMs (Integer), pitStopTimerInMs (Integer), pitStopShouldServePen (Integer), speedTrapFastestSpeed (Float), speedTrapFastestLap (Integer). Packet-level (after 22 cars): timeTrialPBCarIdx (Integer), timeTrialRivalCarIdx (Integer) — only if we parse full packet; for “one car” parser we don’t read these.
   - Keep existing fields; use boxed types for new ones.
2. **Update `LapDataPacketParser`** (udp-ingest-service):
   - Read fields in the exact order of LapData in the doc; map each into the extended DTO. Compute delta times from minute+ms parts (e.g. deltaToCarInFrontMs = minutesPart * 60_000 + msPart). Advance buffer by exactly 57 bytes per car.
3. **Update documentation and models:** In [kafka_contracts_f_1_telemetry.md](../../.github/project/kafka_contracts_f_1_telemetry.md) extend the Lap Data payload schema (§ 5.2) with all new fields. Add Javadoc on new `LapDto` fields referencing the F1 25 spec.
4. **Update or add unit tests:** Update udp-ingest-service parser/handler tests for extended LapDto and LapDataPacketParser; add or extend tests with a 57-byte ByteBuffer and assert all mapped fields and buffer advance by 57.
5. **Build and tests:** `mvn -pl telemetry-api-contracts,udp-ingest-service clean compile test`. Processing service (LapDataProcessor, LapAggregator, etc.) uses existing LapDto getters; no breaking change.

---

### Phase 3: Car Status — full CarStatusData

**Doc:** [F1 25 Telemetry Output Structures.txt](../../.github/docs/F1%2025%20Telemetry%20Output%20Structures.txt) lines 396–402 (CarStatusData).

**Steps:**

1. **Extend `CarStatusDto`** (telemetry-api-contracts):
   - Add: frontBrakeBias (Integer), pitLimiterStatus (Integer), fuelCapacity (Float), fuelRemainingLaps (Float), maxRPM (Integer), idleRPM (Integer), maxGears (Integer), drsActivationDistance (Integer), visualTyreCompound (Integer), vehicleFIAFlags (Integer), enginePowerICE (Float), enginePowerMGUK (Float), ersDeployMode (Integer), ersHarvestedThisLapMGUK (Float), ersHarvestedThisLapMGUH (Float), ersDeployedThisLap (Float), networkPaused (Integer). Keep existing field names (e.g. tyresCompound = actualTyreCompound).
   - All new fields boxed/nullable as appropriate.
2. **Update `CarStatusPacketParser`** (udp-ingest-service):
   - Read all 55 bytes in doc order; remove CAR_STATUS_TAIL_SKIP and map every field into the DTO.
3. **Update documentation and models:** In [kafka_contracts_f_1_telemetry.md](../../.github/project/kafka_contracts_f_1_telemetry.md) extend the Car Status payload schema (§ 5.4) with all new fields. Add Javadoc on new `CarStatusDto` fields referencing the F1 25 spec.
4. **Update or add unit tests:** Update udp-ingest-service parser/handler tests for extended CarStatusDto and CarStatusPacketParser; add or extend tests with a 55-byte ByteBuffer and assert all mapped fields and buffer advance by 55.
5. **Build and tests:** `mvn -pl telemetry-api-contracts,udp-ingest-service clean compile test`. CarStatusRawWriter and CarStatusProcessor use existing getters; can later be extended to persist new fields if DB schema is extended.

---

### Phase 4: Car Damage — full CarDamageData

**Doc:** [F1 25 Telemetry Output Structures.txt](../../.github/docs/F1%2025%20Telemetry%20Output%20Structures.txt) lines 437–457 (CarDamageData).

**Steps:**

1. **Extend `CarDamageDto`** (telemetry-api-contracts):
   - Add: tyresDamage (array or FL/FR/RL/RR), brakesDamage (array or per corner), tyreBlisters (array or per corner), frontLeftWingDamage, frontRightWingDamage, rearWingDamage, floorDamage, diffuserDamage, sidepodDamage, drsFault, ersFault, gearBoxDamage, engineDamage, engineMGUHWear, engineESWear, engineCEWear, engineICEWear, engineMGUKWear, engineTCWear, engineBlown, engineSeized (all Integer or Boolean where doc says 0/1).
   - Keep existing tyre wear fields.
2. **Update `CarDamagePacketParser`** (udp-ingest-service):
   - Parse full 46 bytes per car: 4×float (tyre wear), 4×uint8 (tyre damage), 4×uint8 (brakes damage), 4×uint8 (tyre blisters), then single-byte fields in doc order. Handler already seeks to player car and expects parser to consume one car’s data; ensure parser advances by 46 bytes.
3. **Update documentation and models:** In [kafka_contracts_f_1_telemetry.md](../../.github/project/kafka_contracts_f_1_telemetry.md) add or extend the Car Damage payload schema with all new fields. Add Javadoc on new `CarDamageDto` fields referencing the F1 25 spec.
4. **Update or add unit tests:** Update udp-ingest-service parser/handler tests for extended CarDamageDto and CarDamagePacketParser; add or extend tests with a 46-byte ByteBuffer and assert all mapped fields (including damage arrays and single-byte fields) and buffer advance by 46.
5. **Build and tests:** `mvn -pl telemetry-api-contracts,udp-ingest-service clean compile test`. CarDamageProcessor only uses tyre wear; other consumers can use new fields later.

---

### Phase 5: Processing service and persistence (optional follow-up)

- **CarTelemetryProcessor / RawTelemetryWriter:** Currently use speedKph, throttle, brake, gear, engineRpm, lapDistance (from snapshot). No change required for Phase 1; we can later add persistence of brakes temps, tyre temps, tyre pressure if we add DB columns.
- **CarStatusRawWriter:** Uses tractionControl, abs, fuelMix, tyresCompound, tyresAgeLaps, fuelInTank, ersStoreEnergy, etc. New DTO fields are available for future columns.
- **CarDamageProcessor:** Uses only tyre wear; new damage fields available for future use.
- **LapDataProcessor / LapAggregator:** Use lapNumber, lapDistance, sector times, invalid, penalties; new LapDto fields available for analytics or storage later.

No mandatory changes to telemetry-processing-api-service in Phases 1–4; only additive DTO extensions.

---

### Phase 6: Session packet (optional / separate scope)

- If we want **full PacketSessionData** (packetId=1, 753 bytes): introduce `SessionDataDto` and `SessionDataPacketParser`, and either a new topic or an extended session event. Current `SessionPacketParser` + `SessionEventDto` appear to target event-like payload (SSTA/SEND); keep them as-is unless we explicitly decide to parse packetId=1 as full session data.
- Document in this plan: “Session (packetId=1) full payload deferred; current behaviour unchanged.”

---

## Checklist summary

| Packet        | DTO            | Parser                      | Handler              | Doc reference (line range) |
|---------------|----------------|-----------------------------|----------------------|----------------------------|
| Car Telemetry | CarTelemetryDto| CarTelemetryPacketParser    | CarTelemetryPacketHandler | 363–386                 |
| Lap Data      | LapDto         | LapDataPacketParser         | LapDataPacketHandler | 212–255                    |
| Car Status    | CarStatusDto   | CarStatusPacketParser       | CarStatusPacketHandler   | 396–402                 |
| Car Damage    | CarDamageDto   | CarDamagePacketParser       | CarDamagePacketHandler   | 437–457                 |
| Session       | (no change)    | (no change)                 | SessionPacketHandler | 92–202 (Session); 258–358 (Event) |

---

## Order of implementation

1. **Phase 1** — Car Telemetry (DTO + parser + **documentation and model** + **update/add unit tests**).
2. **Phase 2** — Lap Data (DTO + parser + **documentation and model** + **update/add unit tests**).
3. **Phase 3** — Car Status (DTO + parser + **documentation and model** + **update/add unit tests**).
4. **Phase 4** — Car Damage (DTO + parser + **documentation and model** + **update/add unit tests**).
5. **Phase 5** — Optional: extend persistence/processors and **persistence entities** (DB schema, migrations) to use new fields; update or add processor/writer tests if new fields are used.
6. **Phase 6** — Optional: full Session packet support (SessionDataDto, parser, docs, tests).

After each phase: run `mvn clean compile test` from repo root (and `mvn -pl telemetry-processing-api-service verify` if that module uses the DTOs) to ensure no regressions and that existing tests pass. After Phases 1–4 are complete, update [udp_ingest_refactoring_plan.md](udp_ingest_refactoring_plan.md) or udp-ingest-service README to state that parsers and DTOs cover the full F1 25 packet payload.
