# REST & WebSocket API Contracts – F1 25 Telemetry

## 1. Мета документа

Цей документ описує **публічний API** сервісу `telemetry-processing-api-service`.

API використовується UI (Web) та потенційно сторонніми клієнтами для:
- перегляду історії сесій;
- отримання агрегованих даних;
- live-відображення телеметрії через WebSocket.

> **Реалізація клієнта (React SPA):** архітектура екранів, layout, потік даних та використання цих endpoint'ів описані в [react_spa_ui_architecture.md](react_spa_ui_architecture.md).

---

## 2. Загальні принципи

- Протокол: HTTP + WebSocket
- Формат: JSON
- API **read-only**
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
- `limit` (int, default 50)
- `offset` (int, default 0)

Response:
```json
[
  {
    "sessionUID": 1234567890123,
    "sessionType": "RACE",
    "trackId": 12,
    "startedAt": "2026-01-28T20:10:00Z",
    "endedAt": "2026-01-28T20:55:12Z",
    "endReason": "EVENT_SEND"
  }
]
```

---

#### 3.1.2 Отримати деталі сесії

```
GET /api/sessions/{sessionUid}
```

Response:
```json
{
  "sessionUID": 1234567890123,
  "sessionType": "RACE",
  "trackId": 12,
  "trackLengthM": 5300,
  "totalLaps": 57,
  "aiDifficulty": 95,
  "startedAt": "2026-01-28T20:10:00Z",
  "endedAt": "2026-01-28T20:55:12Z"
}
```

---

### 3.2 Laps

#### 3.2.1 Кола сесії

```
GET /api/sessions/{sessionUid}/laps
```

Query params:
- `carIndex` (int, default 0)

Response:
```json
[
  {
    "lapNumber": 5,
    "lapTimeMs": 87321,
    "sector1Ms": 29123,
    "sector2Ms": 28344,
    "sector3Ms": 29854,
    "isInvalid": false
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

Response:
```json
{
  "totalLaps": 57,
  "bestLapTimeMs": 86210,
  "bestLapNumber": 12,
  "bestSector1Ms": 28790,
  "bestSector2Ms": 27912,
  "bestSector3Ms": 29100
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

### 3.6 Tyre wear (діаграма зносу шин по колах)

```
GET /api/sessions/{sessionUid}/tyre-wear
```

Query params:
- `carIndex` (int, default 0)

Response: масив точок для графіка «знос шин (% по колесах FL, FR, RL, RR) vs номер кола».
```json
[
  { "lapNumber": 1, "wearFL": 0.02, "wearFR": 0.02, "wearRL": 0.03, "wearRR": 0.03 },
  { "lapNumber": 2, "wearFL": 0.05, "wearFR": 0.05, "wearRL": 0.07, "wearRR": 0.07 }
]
```

- Значення wear — float у діапазоні 0..1 (відповідає 0–100%).
- Якщо сесії немає — 404.
- Якщо даних зносу немає (наприклад, у грі вимкнено пошкодження/знос) — 200 і порожній масив `[]`.

Джерело даних: таблиця `telemetry.tyre_wear_per_lap`; записується при фіналізації кола з останнього семплу Car Damage (packet 10) для цього session+car. Для появи даних у F1 25 потрібно увімкнене відображення пошкоджень/зносу шин.

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

```json
{
  "type": "SUBSCRIBE",
  "sessionUID": 1234567890123,
  "carIndex": 0
}
```

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
  "currentSector": 2
}
```

- `drs` (boolean, optional): DRS active; may be null until first car status packet received.
- `currentLap`, `currentSector`: from lap data; may be null until first lap packet.

---

#### 4.5.2 Session End Notification

```json
{
  "type": "SESSION_ENDED",
  "sessionUID": 1234567890123,
  "endReason": "EVENT_SEND"
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



