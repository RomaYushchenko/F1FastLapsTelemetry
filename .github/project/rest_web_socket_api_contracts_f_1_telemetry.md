# REST & WebSocket API Contracts – F1 25 Telemetry

## 1. Мета документа

Цей документ описує **публічний API** сервісу `telemetry-processing-api-service`. Детальна документація самого сервісу (архітектура, шари): [telemetry_processing_api_service.md](telemetry_processing_api_service.md).

API використовується UI (Web) та потенційно сторонніми клієнтами для:
- перегляду історії сесій;
- отримання агрегованих даних;
- live-відображення телеметрії через WebSocket.

> **Реалізація клієнта (React SPA):** архітектура екранів, layout, потік даних та використання цих endpoint'ів описані в [react_spa_ui_architecture.md](react_spa_ui_architecture.md).

---

## 2. Загальні принципи

- Протокол: HTTP + WebSocket
- Формат: JSON
- API переважно read-only; єдиний запис: **PATCH /api/sessions/{id}** для оновлення sessionDisplayName.
- Timezone: UTC (ISO-8601)
- MVP: single user / single session stream

---

## 3. REST API

### 3.1 Sessions

#### 3.1.1 Отримати список сесій

```
GET /api/sessions
```

Query params (optional):
- `limit` (int, default 50) — page size; max 100.
- `offset` (int, default 0) — number of records to skip.
- `sessionType` (string) — filter by session type **code** (e.g. `RACE`, `QUALIFYING_1`). Matches `session_type` in DB. Omit = all types.
- `trackId` (int) — filter by F1 track id. Omit = all tracks.
- `search` (string) — search across: (1) session display name (LIKE, case-insensitive), (2) session type **display name** (e.g. "Race" → RACE), (3) track **display name** (e.g. "Monaco" → trackId 5). Combined with OR.
- `sort` (string) — sort order. Values: `startedAt_asc`, `startedAt_desc` (default), `finishingPosition_asc`, `bestLap_asc`, `bestLap_desc`. Default: `startedAt_desc`.
- `state` (string) — filter by session state: `ACTIVE` (endedAt null) or `FINISHED` (endedAt non-null). Omit = all.
- `dateFrom` (string) — ISO date (YYYY-MM-DD). Sessions included only if **both** startedAt and endedAt fall in [dateFrom, dateTo]. For active sessions (endedAt null), include if startedAt in range.
- `dateTo` (string) — ISO date (YYYY-MM-DD). Use with dateFrom.

**Date filter semantics:** A session is included if: startedAt is within [dateFrom, dateTo] **and** (endedAt is within [dateFrom, dateTo] **or** endedAt is null). So active sessions are included when their start date is in range.

Response:
- **Body:** array of SessionDto (unchanged shape).
- **Header:** `X-Total-Count` (integer) — total number of sessions matching the filters (before pagination). Use for "Showing X–Y of Z".

Each SessionDto includes:
- `id` (string) — public session identifier (UUID). Use in URLs and WebSocket subscribe.
- `sessionDisplayName` (string) — user-facing display name; max 64 characters; not empty. Editable via PATCH. Defaults to UUID at creation.
- `sessionType`, `trackId`, `trackDisplayName`, `startedAt`, `endedAt`, `endReason`, `state` (ACTIVE | FINISHED), `playerCarIndex`, `finishingPosition` (integer, optional) — race position at session end; null if session active or no LapData received. Source: last carPosition from LapData at session end; stored in `session_finishing_positions` for multi-car support.

Example request:
```
GET /api/sessions?limit=20&offset=0&sort=startedAt_desc
GET /api/sessions?search=Monaco&state=FINISHED&dateFrom=2026-01-01&dateTo=2026-01-31
GET /api/sessions?sessionType=RACE&sort=bestLap_asc
```

Example response (body + header):
```
X-Total-Count: 142
```
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "sessionDisplayName": "550e8400-e29b-41d4-a716-446655440000",
    "sessionType": "RACE",
    "trackId": 12,
    "startedAt": "2026-01-28T20:10:00Z",
    "endedAt": "2026-01-28T20:55:12Z",
    "endReason": "EVENT_SEND",
    "state": "FINISHED"
  }
]
```

---

#### 3.1.2 Отримати деталі сесії

```
GET /api/sessions/{sessionUid}
```

Response: SessionDto with `id`, `sessionDisplayName`, `sessionType`, `trackId`, `trackLengthM`, `totalLaps`, `aiDifficulty`, `startedAt`, `endedAt`, `endReason`, `state`, `playerCarIndex`, `finishingPosition` (integer or null).

---

#### 3.1.3 Оновити відображувану назву сесії

```
PATCH /api/sessions/{id}
```

Path: `id` — public session id (UUID) or session_uid string.

Request body:
```json
{
  "sessionDisplayName": "Monaco Race 2026"
}
```

- `sessionDisplayName` (string, required): not blank, max 64 characters. Uniqueness not required.

Response: 200 OK, body = full SessionDto (updated session).  
Errors: 404 if session not found; 400 if validation fails (blank or length > 64).

---

### 3.2 Laps

#### 3.2.1 Кола сесії

```
GET /api/sessions/{sessionUid}/laps
```

Query params:
- `carIndex` (int, default 0)

Response: array of lap objects (LapResponseDto). Each item includes:
- `lapNumber`, `lapTimeMs`, `sector1Ms`, `sector2Ms`, `sector3Ms`, `isInvalid`
- `positionAtLapStart` (integer, optional) — race position at the start of this lap; null if unknown. Used for position column and delta arrows on the session detail page.

```json
[
  {
    "lapNumber": 5,
    "lapTimeMs": 87321,
    "sector1Ms": 29123,
    "sector2Ms": 28344,
    "sector3Ms": 29854,
    "isInvalid": false,
    "positionAtLapStart": 3
  }
]
```

---

### 3.3 Session Summary

#### 3.3.1 Summary по сесії

```
GET /api/sessions/{sessionUid}/summary
```

Query params:
- `carIndex` (int, default 0)

Response (SessionSummaryDto):
- `totalLaps`, `bestLapTimeMs`, `bestLapNumber`, `bestSector1Ms`, `bestSector2Ms`, `bestSector3Ms`
- `leaderCarIndex` (integer, optional) — car index of the session leader (P1). Source: `session_finishing_positions` (position = 1) for finished sessions.
- `leaderIsPlayer` (boolean) — true if the leader is the player car (playerCarIndex === leaderCarIndex). UI uses this to highlight "You" in the Summary block. If leader is unknown, API may omit these fields or client may fall back to "P1" / "Car #N".

```json
{
  "totalLaps": 57,
  "bestLapTimeMs": 86210,
  "bestLapNumber": 12,
  "bestSector1Ms": 28790,
  "bestSector2Ms": 27912,
  "bestSector3Ms": 29100,
  "leaderCarIndex": 0,
  "leaderIsPlayer": true
}
```

---

### 3.4 Pace (діаграма темпу по колах)

```
GET /api/sessions/{sessionUid}/pace
```

Query params:
- `carIndex` (int, default 0)

Response: масив точок для графіка «час кола vs номер кола».
```json
[
  { "lapNumber": 1, "lapTimeMs": 87321 },
  { "lapNumber": 2, "lapTimeMs": 86210 }
]
```

- Якщо сесії немає — 404.
- Якщо сесія є, але кіл з часом немає — 200 і порожній масив `[]`.

Джерело даних: агреговані кола (таблиця `lap`); лише кола з `lapTimeMs > 0`.

- UI may compute **median lap time** from valid laps (filter `!isInvalid && lapTimeMs != null`) and display it as a horizontal reference line on the pace chart; no dedicated backend field.

---

### 3.5 Pedal trace (профіль газ/гальмо по колу)

```
GET /api/sessions/{sessionUid}/laps/{lapNum}/trace
```

Query params:
- `carIndex` (int, default 0)

Response: масив семплів по дистанції кола (distance, throttle, brake).
```json
[
  { "distance": 123.4, "throttle": 0.85, "brake": 0.0 },
  { "distance": 130.2, "throttle": 0.90, "brake": 0.0 }
]
```

- Джерело даних: таблиця `telemetry.car_telemetry_raw` (семпли throttle/brake з прив'язкою до кола через `lap_number`, `lap_distance_m`). Під час активної сесії CarTelemetryConsumer записує семпли разом із поточним lap_number та lap_distance з LapData.
- Якщо для обраного кола записів немає — 200 і порожній масив `[]`. 404 якщо сесії немає.

---

#### 3.5.1 Speed trace (швидкість по дистанції кола)

```
GET /api/sessions/{sessionUid}/laps/{lapNum}/speed-trace
```

Query params:
- `carIndex` (int, default 0)

Response: масив семплів (distanceM, speedKph), відсортований по дистанції. Для графіка «швидкість vs дистанція» на Session Summary (план 13-session-summary-speed-corner-graph).
```json
[
  { "distanceM": 0.0, "speedKph": 120 },
  { "distanceM": 50.5, "speedKph": 285 }
]
```

- Джерело даних: таблиця `telemetry.car_telemetry_raw` (поля `lap_distance_m`, `speed_kph` за session_uid, car_index, lap_number). Якщо для обраного кола записів немає або всі точки мають null distance/speed — 200 і порожній масив `[]`. 404 якщо сесії немає.

---

#### 3.5.2 Corners (метрики по поворотах для кола)

```
GET /api/sessions/{sessionUid}/laps/{lapNum}/corners
```

Query params:
- `carIndex` (int, default 0)

Response: масив поворотів з entry/apex/exit швидкостями (steer-based детекція). Для маркерів і номерів поворотів на графіку speed vs distance (план 13 Phase 2).
```json
[
  {
    "cornerIndex": 1,
    "startDistanceM": 120.5,
    "endDistanceM": 260.0,
    "apexDistanceM": 170.0,
    "entrySpeedKph": 305,
    "apexSpeedKph": 138,
    "exitSpeedKph": 226,
    "durationMs": null
  }
]
```

- Джерело: `car_telemetry_raw` (lap_distance_m, speed_kph, steer); детекція поворотів по порогу керма (steer_on/steer_off). Якщо немає точок з усіма полями — 200 і `[]`. 404 якщо сесії немає.
- **Phase 3:** метрики зберігаються в `lap_corner_metrics`; при наявності карти поворотів треку кожен об'єкт може містити поле `name` (наприклад "T1", "T2") з `track_corners`.

---

#### 3.5.3 Track corner map (остання карта поворотів треку)

```
GET /api/tracks/{trackId}/corner-maps/latest?trackLengthM=
```

Query params:
- `trackLengthM` (int, required)

Response: карта поворотів для треку (версія, список кутів з name, start/end/apex distance). 404 якщо карти немає.
```json
{
  "trackId": 7,
  "trackLengthM": 5891,
  "version": 1,
  "corners": [
    { "cornerIndex": 1, "name": "T1", "startDistanceM": 120.5, "endDistanceM": 260.0, "apexDistanceM": 170.0 }
  ]
}
```

---

#### 3.5.4 Track layout (2D map)

```
GET /api/tracks/{trackId}/layout
```

Path param:
- `trackId` (integer) — F1 track id (e.g. 8 = Silverstone).

Response **200:** 2D track outline/centreline for drawing the Live Track Map.
```json
{
  "trackId": 8,
  "points": [
    { "x": 100, "y": 300 },
    { "x": 250, "y": 100 },
    { "x": 700, "y": 250 },
    { "x": 100, "y": 300 }
  ],
  "bounds": {
    "minX": 100,
    "minY": 100,
    "maxX": 700,
    "maxY": 500
  }
}
```

- `points` — array of `{ x: number, y: number }` (centreline or outline). Client builds SVG path from these; order preserves track direction.
- `bounds` (optional) — `{ minX, minY, maxX, maxY }` for client viewBox so the UI can scale without computing from points. Omit if not stored.

Response **404:** No layout for this track (unknown trackId or layout not imported).

---

### 3.6 Tyre wear (діаграма зносу шин по колах)

```
GET /api/sessions/{sessionUid}/tyre-wear
```

Query params:
- `carIndex` (int, default 0)

Response: масив точок для графіка «знос шин (% по колесах FL, FR, RL, RR) vs номер кола». Кожна точка може містити `compound` (string, optional) — Pirelli compound label (e.g. soft, medium, hard) for tooltip/labels.
```json
[
  { "lapNumber": 1, "wearFL": 0.02, "wearFR": 0.02, "wearRL": 0.03, "wearRR": 0.03, "compound": "medium" },
  { "lapNumber": 2, "wearFL": 0.05, "wearFR": 0.05, "wearRL": 0.07, "wearRR": 0.07, "compound": "medium" }
]
```

- Значення wear — float у діапазоні 0..1 (відповідає 0–100%).
- Якщо сесії немає — 404.
- Якщо даних зносу немає (наприклад, у грі вимкнено пошкодження/знос) — 200 і порожній масив `[]`.

Джерело даних: таблиця `telemetry.tyre_wear_per_lap`; записується при фіналізації кола з останнього семплу Car Damage (packet 10) для цього session+car. Для появи даних у F1 25 потрібно увімкнене відображення пошкоджень/зносу шин.

---

#### 3.6.1 Pit stops (піт-стопи по сесії/авто)

```
GET /api/sessions/{sessionUid}/pit-stops
```

Query params:
- `carIndex` (int, default 0)

Response: array of PitStopDto. Each item: `lapNumber` (int, out-lap number), `inLapTimeMs` (Integer, optional), `pitDurationMs` (Integer, optional, null in MVP), `outLapTimeMs` (Integer), `compoundIn` (Integer, F1 25 code), `compoundOut` (Integer). 404 if session not found; 200 and `[]` if no pit stops. Detection: compound change between consecutive laps (from `tyre_wear_per_lap`); in/out lap times from `lap` table.

---

#### 3.6.2 Stints (шини по стінтах)

```
GET /api/sessions/{sessionUid}/stints
```

Query params:
- `carIndex` (int, default 0)

Response: array of StintDto. Each item: `stintIndex` (int), `compound` (Integer, F1 25 code), `startLap` (int), `lapCount` (int), `avgLapTimeMs` (Integer, optional), `degradationIndicator` (optional, "high"|"medium"|"low" or null; UI shows "—" when null). 404 if session not found; 200 and `[]` if no laps. Consecutive laps with same compound form one stint.

---

### 3.7 ERS (Energy Recovery System — запас енергії по колу)

```
GET /api/sessions/{sessionUid}/laps/{lapNum}/ers
```

Query params:
- `carIndex` (int, default 0)

Response: array of ErsPointDto — stored energy along the lap (same lap as pedal trace).
```json
[
  { "lapDistanceM": 0.0, "energyPercent": 100.0 },
  { "lapDistanceM": 250.5, "energyPercent": 87.2 },
  { "lapDistanceM": 501.2, "energyPercent": 72.1 }
]
```

- `lapDistanceM` (number) — distance along the lap in metres.
- `energyPercent` (number) — stored ERS energy as percentage (0–100). Computed as `ersStoreEnergy / ERS_MAX_ENERGY_J * 100` from car status; car_status_raw has no lap_distance, so points are built by merging with car_telemetry_raw by nearest timestamp within the lap time range.
- 404 if session not found. 200 and empty array `[]` if no data for the selected lap.

---

### 3.8 Live leaderboard (active session)

```
GET /api/sessions/active/leaderboard
```

Returns the current leaderboard for the **active** session only (same session as `GET /api/sessions/active`). Source: runtime state (positions from LapData), last completed lap per car from `laps`, compound from CarStatus snapshot, driver label from `session_drivers` (fallback "Car N" if null).

Response: **204 No Content** if there is no active session. Otherwise **200 OK** with body: array of `LeaderboardEntryDto`:

- `position` (integer) — race position (1-based).
- `carIndex` (integer) — car index (0–19).
- `driverLabel` (string, optional) — display label (e.g. "VER"); null = UI shows "Car N".
- `compound` (string) — tyre compound for display: "S", "M", "H", or "—".
- `gap` (string) — "LEAD" for P1, "+1.234" for others, or "—" if no lap time yet.
- `lastLapTimeMs` (integer, optional) — last completed lap time in ms.
- `sector1Ms`, `sector2Ms`, `sector3Ms` (integer, optional) — sector times of last lap.

WebSocket: same payload is sent as message type **LEADERBOARD** on `/topic/live/{sessionId}` when LapData/position/snapshot changes (see § 4.5.3).

---

### 3.9 Session events (timeline)

```
GET /api/sessions/{sessionUid}/events
```

Query params:
- `fromLap` (int, optional) — minimum lap (inclusive).
- `toLap` (int, optional) — maximum lap (inclusive).
- `limit` (int, optional) — max number of events to return (default 100, max 500).

Response: array of `SessionEventDto`. Each item:
- `lap` (integer, optional) — lap number when event occurred; null if unknown.
- `eventCode` (string) — event code (e.g. "FTLP", "PENA", "SCAR").
- `carIndex` (integer, optional) — car index when applicable.
- `detail` (object, optional) — event-specific fields (e.g. FTLP: `lapTime`, `vehicleIdx`; PENA: `penaltyTime`, `penaltyLapNum`).
- `createdAt` (string) — ISO-8601 timestamp.

Order: by `lap` ascending, then by frame. 404 if session not found; 200 and `[]` if no events.

---

## 4. WebSocket API

### 4.1 Endpoint

```
/ws/live
```

---

### 4.2 REST

- REST API повертає:
  - finalized дані для FINISHED сесій;
  - live snapshot для ACTIVE.

- `GET /sessions/{id}` для ACTIVE:
  - `endedAt = null`;
  - `state = ACTIVE`.

- Pagination для `/sessions` у MVP **не потрібна**.

### 4.3 WebSocket

- Reconnect:
  - якщо session ACTIVE → resume з snapshot;
  - якщо ні → initial snapshot.

- Ordering:
  - не гарантується;
  - кожен message містить `ts` / `frameIdentifier` для client-side sort.

- Session end:
  - WS event `SESSION_ENDED`.

---

### 4.4 Client → Server messages

#### 4.4.1 Subscribe

Client sends **sessionId** (string): public_id or session UID as string, so the topic `/topic/live/{topicId}` matches the id used in REST (e.g. `GET /api/sessions/active` returns `id`).

```json
{
  "type": "SUBSCRIBE",
  "sessionId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "carIndex": 0
}
```

- `sessionId` (string, required): session identifier — public UUID or numeric UID as string. Resolved server-side via `SessionResolveService.getSessionByPublicIdOrUid`.
- `carIndex` (number, optional): player car index; default 0.

---

#### 4.4.2 Unsubscribe

```json
{
  "type": "UNSUBSCRIBE"
}
```

---

### 4.5 Server → Client messages

#### 4.5.1 Live Snapshot (sampling 10 Hz)

```json
{
  "type": "SNAPSHOT",
  "timestamp": "2026-01-28T21:15:44.000Z",
  "speedKph": 298,
  "gear": 6,
  "engineRpm": 10832,
  "throttle": 0.91,
  "brake": 0.0,
  "drs": false,
  "currentLap": 5,
  "currentSector": 2,
  "currentLapTimeMs": 45230,
  "bestLapTimeMs": 43100,
  "deltaMs": 2130,
  "ersEnergyPercent": 78,
  "ersDeployActive": false,
  "tyresSurfaceTempC": [95, 99, 102, 98],
  "fuelRemainingPercent": 67
}
```

- `drs` (boolean, optional): DRS active; may be null until first car status packet received.
- `currentLap`, `currentSector`: from lap data (F1 25 LapData); may be null until first lap packet.
- `currentLapTimeMs` (integer, optional): current lap time in ms (F1 25 LapData `m_currentLapTimeInMS`). Used for delta to best.
- `bestLapTimeMs` (integer, optional): best lap time in session (ms). From SessionSummary; enriched on server when building snapshot.
- `deltaMs` (integer, optional): delta to best lap in ms = currentLapTimeMs − bestLapTimeMs. Negative = faster than best; positive = slower. Null if either lap time missing.
- `ersEnergyPercent` (integer, optional): ERS energy store 0–100%. From CarStatus `m_ersStoreEnergy` (Joules) ÷ ERS max capacity (4 MJ). Null until first car status packet.
- `ersDeployActive` (boolean, optional): driver using ERS (F1 25 CarStatus `m_ersDeployMode` > 0). Null until first car status packet.
- `tyresSurfaceTempC` (array of 4 integers, optional): tyre surface temperatures in °C, order **RL, RR, FL, FR**. From CarTelemetry `m_tyresSurfaceTemperature`. Null until first car telemetry packet.
- `fuelRemainingPercent` (integer, optional): fuel remaining 0–100%. From CarStatus `m_fuelInTank` ÷ `m_fuelCapacity`. Null until first car status packet with valid capacity.

---

#### 4.5.2 Session End Notification

```json
{
  "type": "SESSION_ENDED",
  "sessionId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "endReason": "EVENT_SEND"
}
```

- `sessionId` (string): same identifier as in topic (`/topic/live/{sessionId}`) and REST (session public id or UID as string).
- `endReason` (string): reason code (e.g. EVENT_SEND, NO_DATA_TIMEOUT).

---

#### 4.5.3 New session event (push)

Sent on `/topic/live/{sessionId}` when a new session event is persisted (EventProcessor). Client appends the event to the timeline. Payload: `SessionEventDto` (lap, eventCode, carIndex, detail, createdAt).

```json
{
  "type": "SESSION_EVENT",
  "event": {
    "lap": 24,
    "eventCode": "FTLP",
    "carIndex": 0,
    "detail": { "vehicleIdx": 0, "lapTime": 84.532 },
    "createdAt": "2026-01-28T21:16:00.000Z"
  }
}
```

---

#### 4.5.4 Live leaderboard (push)

Sent on `/topic/live/{sessionId}` when leaderboard data changes (LapData, position, or CarStatus snapshot). Same payload shape as `GET /api/sessions/active/leaderboard` (array of LeaderboardEntryDto).

```json
{
  "type": "LEADERBOARD",
  "entries": [
    {
      "position": 1,
      "carIndex": 0,
      "driverLabel": "VER",
      "compound": "S",
      "gap": "LEAD",
      "lastLapTimeMs": 84432,
      "sector1Ms": 28100,
      "sector2Ms": 27900,
      "sector3Ms": 28432
    }
  ]
}
```

---

## 5. Error handling

### 5.1 REST errors

| HTTP Code | Description |
|----------|-------------|
| 400 | Invalid request |
| 404 | Session not found |
| 500 | Internal error |

Response format:
```json
{
  "error": "SESSION_NOT_FOUND",
  "message": "Session not found"
}
```

---

### 5.2 WebSocket errors

```json
{
  "type": "ERROR",
  "code": "INVALID_SUBSCRIPTION",
  "message": "Session does not exist"
}
```

---

## 6. Versioning

- API versioned via URL prefix (future): `/api/v1/...`
- Backward-compatible changes allowed

---

## 7. Discovery активної сесії

### 7.1 Endpoint

```
GET /api/sessions/active
```

### 7.2 Поведінка

- Повертає **поточну активну сесію**, якщо така існує.
- Якщо активної сесії немає — повертається `204 No Content`.
- В системі може існувати **не більше однієї ACTIVE сесії**.

### 7.3 Response (ACTIVE)

```json
{
  "sessionUID": 1234567890123,
  "sessionType": "RACE",
  "trackId": 12,
  "startedAt": "2026-01-28T20:10:00Z",
  "state": "ACTIVE"
}
```

---

## 8. Явна модель стану сесії

### 8.1 Session State Model

| State | Опис |
|------|------|
| `ACTIVE` | Сесія триває, дані надходять |
| `FINISHED` | Сесія завершена (SEND або timeout) |

### 8.2 REST-представлення

Усі REST-відповіді, що повертають session, **повинні містити поле `state`**.

```json
{
  "sessionUID": 1234567890123,
  "state": "FINISHED",
  "endReason": "TIMEOUT"
}
```

---

## 9. WebSocket Reconnect Contract

### 9.1 Загальні принципи

- WebSocket зʼєднання є **stateless**.
- При кожному reconnect клієнт зобовʼязаний повторно виконати `SUBSCRIBE`.

---

### 9.2 Snapshot on Connect

Після успішного `SUBSCRIBE` сервер **обовʼязково** надсилає повний snapshot поточного стану.

```json
{
  "type": "SNAPSHOT",
  "snapshotType": "FULL",
  "currentLap": 12,
  "currentSector": 2,
  "speedKph": 301,
  "engineRpm": 11400
}
```

---

### 9.3 Last Known State

Клієнт **не передає offset / frameIdentifier**.

Сервер:
- визначає актуальний стан самостійно;
- snapshot завжди є **authoritative source of truth**.

---

### 9.4 Mid-lap Reconnect

При reconnect у середині кола:
- `currentLap` — номер поточного кола;
- `lapProgress` — відсоток проходження кола (derived);
- сектори визначаються сервером.

```json
{
  "currentLap": 8,
  "lapProgress": 0.63,
  "currentSector": 2
}
```

---

## 10. Поведінка API при незавершеній сесії

### 10.1 Unfinished session

REST endpoints (`/laps`, `/summary`):
- повертають **часткові агрегати**;
- поля можуть бути `null`, якщо дані ще не фіналізовані.

### 10.2 Timeout-ended session

Якщо `SEND` не отримано:
- сесія переводиться у `FINISHED` з `endReason = TIMEOUT`;
- WebSocket надсилає:

```json
{
  "type": "SESSION_ENDED",
  "endReason": "TIMEOUT"
}
```

---

## 11. Узгодженість API з агрегатами

### 11.1 Consistency Model

| Тип даних | Model |
|---------|-------|
| Live WebSocket snapshots | Eventual consistency |
| Laps / Sectors | Eventual → Strong (post-finalization) |
| Session Summary | Strong consistency |

### 11.2 Гарантії

- Після `SESSION_ENDED` агрегати вважаються **final**.
- До завершення сесії можливі корекції (recompute).

---

## 12. Multi-car Support

### 12.1 Scope (MVP)

- Підтримується **один carIndex (player)**.
- Multi-car telemetry **не гарантується**.

### 12.2 API Contract

- `carIndex` параметр присутній, але:
    - значення `>0` може повертати `501 Not Implemented`.

```json
{
  "error": "MULTI_CAR_NOT_SUPPORTED"
}
```

### 12.3 Future Extension

- Multi-car sessions (up to 22 cars)
- WebSocket multiplexing per carIndex
- Aggregates per car



