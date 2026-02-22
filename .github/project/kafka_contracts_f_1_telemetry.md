# Kafka Contracts – F1 25 Telemetry

## 1. Мета документа

Цей документ описує **контракти повідомлень Kafka**, які використовуються між сервісами проєкту F1 Telemetry.

Контракти є стабільним API між:
- `udp-ingest-service`
- `telemetry-processing-api-service` ([документація сервісу](telemetry_processing_api_service.md))

Будь‑які зміни тут вважаються **breaking / non‑breaking** і повинні проходити контроль версій.

## 1.1 Scope

Документ покриває:
- життєвий цикл сесії на рівні подій Kafka;
- error handling та DLQ контракти;
- ordering / out-of-order поведінку;
- flashback / rewind семантику;
- правила schema evolution;
- еволюцію multi-car messaging.

## 1.2 Терміни

| Термін | Опис |
|------|------|
| SSTA | Подія старту сесії |
| SEND | Подія завершення сесії |
| Watermark | Мінімальний frameIdentifier, дозволений для агрегації |
| Reorder window | Допустиме вікно out-of-order frame |
| Flashback | Відкат стану через rewind frame |

---

## 2. Загальні правила

- Формат повідомлень: **JSON**
- Версіонування: `schemaVersion`
- Усі повідомлення загорнуті в **Kafka Envelope**
- Порядок повідомлень гарантується **в межах `sessionUID`**
- Ідемпотентність: `(sessionUID, frameIdentifier, packetId, carIndex)`

---

## 3. Topics

| Topic name | Опис | Producer | Consumer |
|-----------|------|----------|----------|
| `telemetry.session` | Події життєвого циклу сесії (SSTA/SEND) | ingest | processing |
| `telemetry.sessionData` | Повний PacketSessionData (724 B) при packetId=1 | ingest | optional |
| `telemetry.lap` | Дані по колах | ingest | processing |
| `telemetry.carTelemetry` | Live телеметрія авто | ingest | processing |
| `telemetry.carStatus` | Статус авто | ingest | processing |
| `telemetry.carDamage` | Пошкодження/знос шин (tyre wear % per wheel) | ingest | processing |
| `telemetry.event` | Session-wide events (DRSD, SCAR, RTMT, FTLP, PENA, etc.). See [Plan 08](../draft/implementation-plans/08-packet-event-ingest-and-processing.md) § 1.1–1.2 (why and where to use). | ingest | processing |

---

## 4. Kafka Message Envelope

### 4.1 Envelope schema

```json
{
  "schemaVersion": 1,
  "packetId": "CAR_TELEMETRY",
  "sessionUID": 123456789012345678,
  "frameIdentifier": 10234,
  "sessionTime": 345.67,
  "carIndex": 0,
  "producedAt": "2026-01-28T21:15:43.123Z",
  "payload": {}
}
```

### 4.2 Опис полів

| Field | Type | Description |
|------|------|-------------|
| schemaVersion | int | Версія контракту envelope |
| packetId | enum | Тип UDP пакета (Lap, Telemetry, Status, Event) |
| sessionUID | long | Унікальний ідентифікатор сесії |
| frameIdentifier | int | Ідентифікатор frame з UDP |
| sessionTime | float | Час сесії у грі (сек) |
| carIndex | int | Індекс авто (0 = player) |
| producedAt | ISO-8601 | Серверний timestamp |
| payload | object | DTO пакета |

---

## 5. Payload Contracts

### 5.1 SessionEventDto

Topic: `telemetry.session`

```json
{
  "eventCode": "SSTA",
  "sessionType": "RACE",
  "trackId": 12,
  "totalLaps": 57
}
```

**eventCode:**
- `SSTA` – session started
- `SEND` – session ended

---

### 5.2 LapDto

Topic: `telemetry.lap`

Payload maps F1 25 **LapData** (57 bytes per car). Field semantics: `.github/docs/F1 25 Telemetry Output Structures.txt` (PacketLapData, LapData).

| Field | Type | Description |
|-------|------|--------------|
| lapNumber | int | m_currentLapNum |
| lapDistance | float | Distance around current lap (m) |
| lastLapTimeMs | int | m_lastLapTimeInMS |
| currentLapTimeMs | int | m_currentLapTimeInMS |
| sector1TimeMs, sector2TimeMs | int | Sector times (computed from min+ms parts) |
| sector | int | 0 = sector1, 1 = sector2, 2 = sector3 |
| isInvalid | bool | m_currentLapInvalid |
| penaltiesSeconds | int | m_penalties |
| deltaToCarInFrontMs | int | Time delta to car in front (ms) |
| deltaToRaceLeaderMs | int | Time delta to race leader (ms) |
| totalDistance | float | Total distance in session (m) |
| safetyCarDelta | float | Safety car delta (s) |
| carPosition | int | Race position |
| pitStatus | int | 0 = none, 1 = pitting, 2 = in pit area |
| numPitStops | int | Pit stops taken |
| totalWarnings, cornerCuttingWarnings | int | Warnings |
| numUnservedDriveThroughPens, numUnservedStopGoPens | int | Unserved penalties |
| gridPosition | int | Grid position at start |
| driverStatus | int | 0 = garage, 1 = flying lap, 2 = in lap, 3 = out lap, 4 = on track |
| resultStatus | int | 0 = invalid, 1 = inactive, 2 = active, 3 = finished, etc. |
| pitLaneTimerActive | int | 0 = inactive, 1 = active |
| pitLaneTimeInLaneInMs, pitStopTimerInMs | int | Pit timing (ms) |
| pitStopShouldServePen | int | Serve penalty at this stop |
| speedTrapFastestSpeed | float | Fastest speed trap (km/h) |
| speedTrapFastestLap | int | Lap of fastest speed, 255 = not set |

Example (subset of fields):

```json
{
  "lapNumber": 5,
  "lapDistance": 4231.4,
  "lastLapTimeMs": 84500,
  "currentLapTimeMs": 87321,
  "sector1TimeMs": 28500,
  "sector2TimeMs": 30000,
  "sector": 2,
  "isInvalid": false,
  "penaltiesSeconds": 0,
  "carPosition": 3,
  "totalDistance": 15000.0,
  "pitStatus": 0,
  "numPitStops": 1
}
```

---

### 5.3 CarTelemetryDto

Topic: `telemetry.carTelemetry`

Payload maps F1 25 **CarTelemetryData** (60 bytes per car). Field semantics: `.github/docs/F1 25 Telemetry Output Structures.txt` (PacketCarTelemetryData, CarTelemetryData).

| Field | Type | Description |
|-------|------|--------------|
| speedKph | int | Speed of car in km/h (m_speed) |
| throttle | float | Throttle applied (0.0–1.0) |
| brake | float | Brake applied (0.0–1.0) |
| steer | float | Steering (-1.0 full left to 1.0 full right) |
| gear | int | Gear (1–8, N=0, R=-1) |
| engineRpm | int | Engine RPM |
| drs | int | DRS 0=off, 1=on |
| clutch | int | Clutch applied (0–100) |
| revLightsPercent | int | Rev lights indicator (percentage) |
| revLightsBitValue | int | Rev lights bitmask (bit 0 = leftmost LED) |
| brakesTemperature | int[4] | Brakes temperature °C, order RL, RR, FL, FR |
| tyresSurfaceTemperature | int[4] | Tyres surface temperature °C, order RL, RR, FL, FR |
| tyresInnerTemperature | int[4] | Tyres inner temperature °C, order RL, RR, FL, FR |
| engineTemperature | int | Engine temperature °C |
| tyresPressure | float[4] | Tyre pressure PSI, order RL, RR, FL, FR |
| surfaceType | int[4] | Driving surface per appendices, order RL, RR, FL, FR |

Example (full payload):

```json
{
  "speedKph": 312,
  "throttle": 0.87,
  "brake": 0.0,
  "steer": -0.12,
  "gear": 7,
  "engineRpm": 11543,
  "drs": 1,
  "clutch": 0,
  "revLightsPercent": 85,
  "revLightsBitValue": 16383,
  "brakesTemperature": [ 420, 418, 380, 382 ],
  "tyresSurfaceTemperature": [ 105, 106, 102, 104 ],
  "tyresInnerTemperature": [ 108, 109, 105, 107 ],
  "engineTemperature": 95,
  "tyresPressure": [ 23.1, 23.0, 22.9, 23.2 ],
  "surfaceType": [ 0, 0, 0, 0 ]
}
```

---

### 5.4 CarStatusDto

Topic: `telemetry.carStatus`

Payload maps F1 25 **CarStatusData** (55 bytes per car). Field semantics: `.github/docs/F1 25 Telemetry Output Structures.txt` (PacketCarStatusData, CarStatusData).

| Field | Type | Description |
|-------|------|--------------|
| tractionControl | int | 0 = off, 1 = medium, 2 = full |
| abs | int | 0 = off, 1 = on |
| fuelInTank | float | Current fuel mass |
| fuelMix | int | 0 = lean, 1 = standard, 2 = rich, 3 = max |
| drsAllowed | bool | 0 = not allowed, 1 = allowed |
| tyresCompound | int | m_actualTyreCompound (e.g. 16 = C5, 7 = inter, 8 = wet) |
| tyresAgeLaps | int | Age in laps of current set |
| ersStoreEnergy | float | ERS energy store (J) |
| frontBrakeBias | int | Front brake bias (%) |
| pitLimiterStatus | int | 0 = off, 1 = on |
| fuelCapacity | float | Fuel capacity |
| fuelRemainingLaps | float | Fuel remaining in laps (MFD value) |
| maxRpm, idleRpm | int | Max / idle RPM |
| maxGears | int | Maximum number of gears |
| drsActivationDistance | int | Metres; 0 = DRS not available |
| visualTyreCompound | int | Visual compound (can differ from actual) |
| vehicleFiaFlags | int | -1 = invalid, 0 = none, 1 = green, 2 = blue, 3 = yellow |
| enginePowerIce, enginePowerMguk | float | Power output (W) |
| ersDeployMode | int | 0 = none, 1 = medium, 2 = hotlap, 3 = overtake |
| ersHarvestedThisLapMguk, ersHarvestedThisLapMguh | float | ERS harvested this lap |
| ersDeployedThisLap | float | ERS deployed this lap |
| networkPaused | int | Car paused in network game |

Example (subset):

```json
{
  "tractionControl": 2,
  "abs": 1,
  "fuelInTank": 18.4,
  "fuelMix": 1,
  "drsAllowed": true,
  "tyresCompound": 16,
  "tyresAgeLaps": 5,
  "ersStoreEnergy": 2.34,
  "frontBrakeBias": 55,
  "fuelCapacity": 110.0,
  "maxRpm": 12000,
  "idleRpm": 7000,
  "maxGears": 8
}
```

---

### 5.5 CarDamageDto

Topic: `telemetry.carDamage`

Payload maps F1 25 **CarDamageData** (46 bytes per car). Field semantics: `.github/docs/F1 25 Telemetry Output Structures.txt` (PacketCarDamageData, CarDamageData).

| Field | Type | Description |
|-------|------|--------------|
| tyresWearFL, tyresWearFR, tyresWearRL, tyresWearRR | float | Tyre wear (%), order in packet RL, RR, FL, FR |
| tyresDamage | int[4] | Tyre damage (%), order RL, RR, FL, FR |
| brakesDamage | int[4] | Brakes damage (%), order RL, RR, FL, FR |
| tyreBlisters | int[4] | Tyre blisters (%), order RL, RR, FL, FR |
| frontLeftWingDamage, frontRightWingDamage, rearWingDamage | int | Wing damage (%) |
| floorDamage, diffuserDamage, sidepodDamage | int | Damage (%) |
| drsFault, ersFault | int | 0 = OK, 1 = fault |
| gearBoxDamage, engineDamage | int | Damage (%) |
| engineMguhWear, engineEsWear, engineCeWear, engineIceWear, engineMgukWear, engineTcWear | int | Engine wear (%) |
| engineBlown, engineSeized | int | 0 = OK, 1 = fault |

Example (subset):

```json
{
  "tyresWearFL": 0.08,
  "tyresWearFR": 0.10,
  "tyresWearRL": 0.12,
  "tyresWearRR": 0.15,
  "tyresDamage": [ 0, 0, 5, 3 ],
  "brakesDamage": [ 0, 0, 0, 0 ],
  "tyreBlisters": [ 0, 0, 0, 0 ],
  "frontLeftWingDamage": 0,
  "frontRightWingDamage": 0,
  "rearWingDamage": 0,
  "drsFault": 0,
  "ersFault": 0
}
```

---

### 5.6 EventDto (EventEvent)

Topic: `telemetry.event`

Payload for **Packet Event (packetId = 3)**. Message type: **EventEvent** (envelope + EventDto). Used for session-wide game events: DRS disabled (DRSD), Safety car (SCAR), Retirement (RTMT), Fastest lap (FTLP), Penalty (PENA), Speed trap (SPTP), Overtake (OVTK), Collision (COLL), etc.

**Why and where to use:** See [Plan 08](../draft/implementation-plans/08-packet-event-ingest-and-processing.md) § 1.1 (why EventEvent is needed) and § 1.2 (where it is used and where to use it next). Main use: **DRS disabled reason** for Live/Session UI (plan 12); optional: event history, Safety car status.

| Field | Type | Description |
|-------|------|--------------|
| eventCode | string | 4-char code: DRSD, SCAR, RTMT, FTLP, PENA, SPTP, STLG, OVTK, COLL, … |
| vehicleIdx | int | Per-event (e.g. retiring car, fastest lap car). |
| retirementReason | int | RTMT: 0–10 (see RetirementReason enum). |
| drsDisabledReason | int | DRSD: 0–3 (see DrsDisabledReason enum). |
| safetyCarType, safetyCarEventType | int | SCAR: type 0–3, event 0–3 (see SafetyCarType, SafetyCarEventType). |
| lapTime | float | FTLP: lap time in seconds. |
| (other) | … | penaltyType, speedTrapSpeedKph, numLights, flashbackFrameIdentifier, vehicle1Idx/2Idx, etc. |

---

# 6. Session Lifecycle – Extended Contract

## 6.1 Packets BEFORE `SSTA`

### Правило
Усі пакети (`telemetry`, `lap`, `status`), отримані **до першого `SSTA` для sessionUID**, ігноруються.

### Наслідки
- Raw дані **не зберігаються**;
- Агрегати **не змінюються**;
- Інкрементується метрика:
```
packets_before_ssta_total
```

---

## 6.2 Packets DURING ACTIVE session

### Правило
Усі пакети з валідним `sessionUID` приймаються до обробки.

### Обмеження
- Обробка підпорядковується reorder / rewind правилам;
- Агрегація дозволена лише для frames ≥ watermark.

---

## 6.3 `SEND` / `SSTA` semantics

`SEND` означає **логічне завершення сесії**.

### Обовʼязкові дії processing service
- Фіналізувати всі lap / sector агрегати;
- Оновити `session_summary`;
- Закрити всі live WebSocket streams;
- Зафіксувати:
```
end_reason = EVENT_SEND
```

- **`SSTA` (Session Start)**:
  - В офіційній телеметрії надсилається **рівно один раз** на сесію.
  - Повторний `SSTA` з тим самим `sessionUID` вважається invalid → log + ignore.

- **`SEND` без LapData**:
  - Допустимий сценарій (наприклад, user одразу вийшов із сесії).
  - Сесія зберігається з `total_laps = 0`.

- **`SEND` після timeout**:
  - Якщо `NO_DATA_TIMEOUT` вже зафіксований → `SEND` ігнорується.
  - Пріоритет має **перший завершуючий тригер**.

---

## 6.4 Session timeout (pseudo-event)

### Умова
```
no packets for sessionUID > T seconds
```

### Поведінка
- Генерується внутрішній pseudo-event `SESSION_TIMEOUT`;
- Подія **не публікується** у Kafka;
- Використовується виключно у processing service.

- `SESSION_TIMEOUT`:
  - **Публікується в Kafka** у topic `telemetry.session`.
  - Дозволяє UI та іншим сервісам синхронно дізнатись про завершення.

### Контракт
```json
{
  "eventCode": "SESSION_TIMEOUT",
  "reason": "NO_DATA",
  "timeoutSeconds": 30
}
```

### Результат
```
end_reason = NO_DATA_TIMEOUT
```

---

# 7. Flashback / Rewind Contract

## 7.1 Detection

Flashback детектиться, якщо:
- Отримано `FLBK` event;
- Виявлено regression `overallFrameIdentifier`.

---

## 5.2 Flashback Payload

```json
{
  "eventCode": "FLBK",
  "rewindFrame": 9123,
  "rewindSessionTime": 312.45
}
```

---

## 7.3 Processing Behavior

- Інвалідуються всі агрегати з `frameIdentifier > rewindFrame`;
- Rollback in-memory state;
- Recompute агрегатів з останнього стабільного checkpoint.

---

# 8. Schema Evolution Contract

## 8.1 Non-breaking Changes

- Додавання optional полів;
- Додавання нових `packetId`;
- Nullable поля.

Не потребують координації між сервісами.

---

## 8.2 Breaking Changes

- Видалення полів;
- Зміна типів;
- Зміна семантики.

### Обовʼязковий порядок rollout
1. Зміна `schemaVersion`;
2. Rollout consumer;
3. Rollout producer.

---

## 9. Ordering & Idempotency

### 9.1 Ordering
- Kafka key = `sessionUID`
- Всі повідомлення однієї сесії потрапляють в одну партицію

На основі офіційного опису frame counters:

- Primary ordering key: `overallFrameIdentifier`.
- Watermark:
  - **per sessionUID + carIndex**.
  - Монотонно зростає.

- LapData < watermark:
  - raw зберігається;
  - агрегати **не оновлюються**.

### 9.2 Idempotency

Повідомлення вважається унікальним за ключем:

```
(sessionUID, frameIdentifier, packetId, carIndex)
```

Consumer зобовʼязаний перевіряти, чи повідомлення вже було оброблене.

- `processed_packets`:
  - Дані зберігаються **поки існує сесія**.
  - Після archival / retention сесії — можуть бути видалені.

- Ingestion:
  - Ідемпотентність **лише у processing service**.

---

## 10. Versioning strategy

- `schemaVersion` змінюється тільки при **breaking changes**
- Non-breaking зміни:
  - додавання optional полів
  - додавання нових `packetId`

---

## 11. Error handling

- Некоректний payload → log + skip
- Невідома `schemaVersion` → reject + metric
- Порушення ordering не вважається помилкою (UDP)
- DB write error:
  - retry (configurable);
  - після вичерпання → DLQ.
---

# 12. Error Handling Contract

## 12.1 Error / DLQ Topics

| Topic | Призначення |
|------|-------------|
| telemetry.error | Контрактні помилки |
| telemetry.dlq | Фатальні / non-retriable помилки |

---

## 12.2 Error Payload Schema

```json
{
  "errorType": "DESERIALIZATION_ERROR",
  "originalTopic": "telemetry.carTelemetry",
  "sessionUID": 123456789,
  "packetId": "CAR_TELEMETRY",
  "frameIdentifier": 10234,
  "schemaVersion": 1,
  "message": "Invalid payload structure",
  "producedAt": "2026-01-28T21:16:00Z"
}
```

---

## 12.3 Error Classification

| Error type | Action |
|-----------|--------|
| Invalid JSON | DLQ |
| Unknown schemaVersion | DLQ |
| Missing optional fields | Skip |
| Duplicate packet | Skip |
| Out-of-order frame | Accept |

---

# 13. Ordering & Out-of-Order Frames

## 13.1 Ordering Model

Primary ordering key:
```
overallFrameIdentifier
```

Fallback key:
```
frameIdentifier
```

---

## 13.2 Reorder Window

```
reorder_window_frames = N (configurable)
```

### Правила
- Frame < watermark → агрегація заборонена;
- Frame ≥ watermark → нормальна обробка;
- Raw telemetry зберігається завжди.

---

## 13.3 Watermark Semantics

Watermark монотонно зростає та визначає мінімальний frameIdentifier,
який може змінювати агрегати.

---
# 14. Multi-Car Messaging (Future)

## 14.1 Поточний стан (MVP)
- Один Kafka message = один car;
- `carIndex` є обовʼязковим.

---

## 14.2 Batch Payload (Future)

```json
{
  "cars": [
    { "carIndex": 0, "payload": {} },
    { "carIndex": 1, "payload": {} }
  ]
}
```

### Правила
- Новий `packetId` або новий `schemaVersion`;
- Можливий окремий topic;
- Silent upgrade заборонений.

---

# 15. Compatibility Rules

- Processing service зобовʼязаний підтримувати legacy single-car payloads;
- Batch payloads є opt-in і не активуються автоматично.

---

# 16. Status

- Документ є **нормативним доповненням**;
- `kafka_contracts_f_1_telemetry.md` залишається єдиним базовим контрактом.

---

## 17. Future extensions

- Multi-car payloads (22 авто)
- Protobuf / Avro
- Schema Registry
- Replay з Kafka

