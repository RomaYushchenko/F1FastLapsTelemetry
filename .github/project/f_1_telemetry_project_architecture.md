# F1 25 Telemetry – Архітектура проєкту

## 1. Загальний огляд

Проєкт призначений для збору, обробки, зберігання та відображення телеметрії з гри **EA SPORTS™ F1® 25** у реальному часі.

Архітектура побудована з урахуванням:
- високочастотних UDP‑даних;
- можливих втрат пакетів;
- потреби у live‑відображенні;
- майбутнього горизонтального масштабування.

MVP реалізується як **один інстанс** (Docker Compose), але всі рішення закладені так, щоб у майбутньому безболісно перейти до distributed‑деплою.

---

## 2. Архітектурні підходи та принципи

### 2.1 Архітектурний стиль
- Event‑driven architecture
- Streaming pipeline (UDP → Kafka → Processing)
- Hexagonal Architecture (Ports & Adapters) всередині сервісів
- Stateless ingestion

### 2.2 Ключові патерни
- Message Envelope
- Idempotent Consumer
- CQRS (light)
- Backpressure через Kafka
- Sampling / batching

---

## 3. Розбиття на сервіси

### 3.1 UDP Ingest Service

**Відповідальність:**
- слухає UDP‑порт F1 25;
- парсить binary пакети;
- нормалізує дані;
- публікує події у Kafka.

**Особливості:**
- не пише у БД;
- мінімальна логіка;
- готовий до масштабування.

**Реалізація:** Вся бізнес-логіка ingest (парсинг пакетів, хендлери, Kafka-публішер) знаходиться в модулі **udp-ingest-service**. Бібліотека **f1-telemetry-udp-spring** надає лише інфраструктуру: аннотації, адаптер методів, реєстр та диспетчер (маршрутизація UDP → виклик методів).

---

### 3.2 Telemetry Processing & API Service

**Детальна документація сервісу:** [telemetry_processing_api_service.md](telemetry_processing_api_service.md)

**Відповідальність:**
- споживає Kafka‑події;
- агрегує телеметрію;
- зберігає raw та aggregated дані;
- віддає REST API;
- пушить live‑дані через WebSocket.

---

### 3.3 UI (Web)

**Відповідальність:**
- live‑дашборд;
- перегляд історії сесій;
- порівняння кіл та секторів.

**Архітектура, екрани, layout та потік даних (REST + WebSocket)** описані в окремому документі: [react_spa_ui_architecture.md](react_spa_ui_architecture.md). Там же — структура React SPA, маршрути, компоненти та узгодження з REST/WS контрактами.

---

### 3.4 Service layout (MVP)

- Для MVP дозволено:
  - **обʼєднання ingest + processing в один Spring Boot app**.
  - Kafka використовується логічно, навіть локально.

### 3.5 UDP ingestion

- Один UDP listener достатній для MVP.
- Якщо Kafka недоступна:
  - ingestion припиняє publish;
  - packets дропаються з метрикою `udp_dropped_due_to_backpressure`.

### 3.6 State & restart

- Runtime state **in-memory**.
- При рестарті сервісу:
  - state починається з нуля;
  - raw використовується **лише для debug**, не для replay в MVP.

### 3.7 Flashback

- Processing **готовий до FLBK**:
  - invalidate агрегати;
  - rollback state;
  - recompute з checkpoint.


---

## 4. Multi‑module Maven структура

```
root
 ├─ telemetry-api-contracts
 ├─ f1-telemetry-udp-core
 ├─ f1-telemetry-udp-spring
 ├─ f1-telemetry-udp-starter
 ├─ udp-ingest-service
 ├─ telemetry-processing-api-service
 ├─ ui
 └─ infra
```

### 4.1 telemetry-api-contracts
- Kafka envelope (`KafkaEnvelope<T>`)
- DTO (Session, Lap, CarTelemetry, CarStatus)
- Enum: `PacketId`, `EventCode`
- Schema versioning: `SchemaVersion`
- REST/WebSocket DTO contracts

### 4.2 f1-telemetry-udp-core
**Pure Java module (no Spring dependencies)**
- UDP packet reception via `DatagramChannel`
- Packet header decoding (`PacketHeader`, `PacketHeaderDecoder`)
- Dispatcher pattern (`UdpPacketDispatcher`, `UdpPacketConsumer`)
- Binary parsing: UDP → ByteBuffer → structured data
- Little Endian parsing for F1 25 format

### 4.3 f1-telemetry-udp-spring
**Spring integration layer**
- Annotations: `@F1UdpListener`, `@F1PacketHandler`
- `BeanPostProcessor` for handler discovery
- Method adapter: business methods → `UdpPacketConsumer`
- Handler registry and Spring-aware wiring

### 4.4 f1-telemetry-udp-starter
**Spring Boot autoconfiguration**
- AutoConfiguration for UDP listener
- Configuration properties binding
- Lifecycle management (start on ready, stop on shutdown)
- Conditional bean creation based on properties

**Детальний опис UDP бібліотеки:** [udp_telemetry_ingest_as_reusable_library_implementation_guide.md](udp_telemetry_ingest_as_reusable_library_implementation_guide.md)

---

## 5. Комунікація та Data Flow

### 5.1 High-level flow (огляд)

```
+---------+        UDP        +------------------+        Kafka        +--------------------------------+
| F1 Game |  ─────────────▶  | udp-ingest-svc   |  ───────────────▶  | telemetry-processing-api-svc   |
+---------+                  | (parse + publish)|                    | (consume + aggregate + store) |
                                                                    │
                                                                    ▼
                                                     +------------------------------+
                                                     | TimescaleDB / PostgreSQL     |
                                                     +------------------------------+
                                                                    │
                                                     REST / WebSocket│
                                                                    ▼
                                                              +-----------+
                                                              |   UI      |
                                                              +-----------+
```

---

### 5.2 Sequence: старт сесії (SSTA)

```
F1 Game        Ingest            Kafka                Processing            DB
  |              |                 |                     |                  |
  |-- UDP:SSTA ->|                 |                     |                  |
  |              | parse header    |                     |                  |
  |              | publish event   |-- SSTA ------------>|                  |
  |              |                 |                     | upsert session   |
  |              |                 |                     | started_at=now() |
  |              |                 |                     | insert cars      |
```

---

### 5.3 Sequence: live телеметрія (batched + sampled)

```
F1 Game        Ingest            Kafka            Processing              WS/UI
  |              |                 |                  |                     |
  |-- UDP:frame->|                 |                  |                     |
  |              | parse packet    |                  |                     |
  |              | batch + publish |-- telemetry ---->|                     |
  |              |                 |                  | aggregate in-mem    |
  |              |                 |                  | batch insert raw    |
  |              |                 |                  | (Timescale)         |
  |              |                 |                  |                     |
  |              |                 |                  |-- snapshot (10Hz) ->|-- render -->
```

---

### 5.4 Sequence: lap aggregation

```
Processing Service
        |
        | receive LapData packet
        |
        | check processed_packets (idempotent)
        |
        | update current lap state
        |
        | sector completed?
        |    ├─ yes → update sector time
        |    └─ no
        |
        | lap completed?
        |    ├─ yes → upsert telemetry.laps
        |    |        update session_summary
        |    └─ no
        |
```

---

### 5.5 Sequence: завершення сесії (SEND або timeout)

```
F1 Game        Ingest            Kafka            Processing                DB
  |              |                 |                  |                       |
  |-- UDP:SEND ->|                 |                  |                       |
  |              | publish event   |-- SEND -------->|                       |
  |              |                 |                  | finalize aggregates  |
  |              |                 |                  | ended_at=now()       |
  |              |                 |                  | end_reason=EVENT     |
  |              |                 |                  | flush buffers        |
  |              |                 |                  | close WS streams     |
```

Fallback (no-data-timeout):

```
Processing Scheduler
        |
        | no packets for sessionUID > X sec
        |
        | mark session ended
        | end_reason = NO_DATA_TIMEOUT
        |
        | flush & close
```

---

## 6. Kafka дизайн

### 6.1 Topics (MVP)
- telemetry.session
- telemetry.lap
- telemetry.carTelemetry
- telemetry.carStatus

Топіки створюються автоматично брокером Kafka при першій публікації (у інфраструктурі ввімкнено `KAFKA_AUTO_CREATE_TOPICS_ENABLE: true`). Ручне створення не потрібне.

### 6.2 Key strategy
- Kafka key = sessionUID

### 6.3 Message Envelope

```json
{
  "schemaVersion": 1,
  "sessionUID": "long",
  "sessionTime": "float",
  "frameIdentifier": "int",
  "packetId": "enum",
  "carIndex": "int",
  "producedAt": "timestamp",
  "payload": {}
}
```

---

## 7. Batching та performance

### 7.1 Kafka producer
- linger.ms
- batch.size

### 7.2 Database
- batch insert
- hypertables

### 7.3 WebSocket
- sampling 10 Hz
- агрегований snapshot

---

## 8. Session lifecycle

### 8.1 Основний механізм
- SSTA → старт сесії
- SEND → завершення сесії

### 8.2 Fallback
- no‑data timeout (10–30 сек)

### 8.3 Packets до SSTA
- усі telemetry / lap / status пакети до отримання SSTA для sessionUID ігноруються;
- такі пакети не зберігаються у raw та не впливають на агрегати;
- факт фіксується у метриці packets_before_ssta.

### 8.4 Packets під час ACTIVE сесії
- усі пакети з валідним sessionUID приймаються;
- обробка підпорядковується правилам reorder та rewind;
- агрегати оновлюються лише для frames ≥ watermark.

### 8.5 Packets після SEND / FINISHED
- пакети, отримані після SEND або NO_DATA_TIMEOUT, ігноруються;
- raw дані не зберігаються;
- інкрементується метрика late_packets_after_finish.

---

## 9. Data Storage та DDL (PostgreSQL + TimescaleDB)

> Примітка: DDL нижче розрахований на **PostgreSQL + TimescaleDB** в одному інстансі. Timescale використовується для високочастотних raw-снапшотів, PostgreSQL-таблиці — для агрегатів та довідників.

### 9.1 Ініціалізація TimescaleDB

```sql
-- Увімкнути TimescaleDB (потрібно, щоб extension був доступний у збірці Postgres)
CREATE EXTENSION IF NOT EXISTS timescaledb;

-- (Опційно) UUID генерація, якщо буде потрібна
CREATE EXTENSION IF NOT EXISTS pgcrypto;
```

---

### 9.2 Схема

```sql
CREATE SCHEMA IF NOT EXISTS telemetry;
```

---

### 9.3 Довідники та сесії (PostgreSQL)

#### 9.3.1 sessions

```sql
CREATE TABLE IF NOT EXISTS telemetry.sessions (
  session_uid        BIGINT PRIMARY KEY,

  -- з PacketSessionData / header
  packet_format      SMALLINT      NOT NULL,
  game_major_version SMALLINT      NOT NULL,
  game_minor_version SMALLINT      NOT NULL,

  -- бізнес-атрибути сесії (MVP мінімум)
  session_type       SMALLINT      NULL,
  track_id           SMALLINT      NULL,
  track_length_m     INTEGER       NULL,
  total_laps         SMALLINT      NULL,
  ai_difficulty      SMALLINT      NULL,

  -- lifecycle
  started_at         TIMESTAMPTZ   NULL,
  ended_at           TIMESTAMPTZ   NULL,
  end_reason         VARCHAR(32)   NULL, -- EVENT_SEND | NO_DATA_TIMEOUT | MANUAL

  created_at         TIMESTAMPTZ   NOT NULL DEFAULT now(),
  updated_at         TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_sessions_started_at ON telemetry.sessions(started_at DESC);
```

#### 9.3.2 session_cars (закладка під multi-car)

```sql
CREATE TABLE IF NOT EXISTS telemetry.session_cars (
  session_uid   BIGINT      NOT NULL REFERENCES telemetry.sessions(session_uid) ON DELETE CASCADE,
  car_index     SMALLINT    NOT NULL,
  is_player     BOOLEAN     NOT NULL DEFAULT false,

  created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),

  PRIMARY KEY (session_uid, car_index)
);

CREATE INDEX IF NOT EXISTS idx_session_cars_player ON telemetry.session_cars(session_uid) WHERE is_player = true;
```

---

### 9.4 Ідемпотентність та контроль обробки (PostgreSQL)

Ця таблиця потрібна, щоб processing був **Idempotent Consumer** при можливих повторних повідомленнях/рестартах.

```sql
CREATE TABLE IF NOT EXISTS telemetry.processed_packets (
  session_uid      BIGINT      NOT NULL,
  frame_identifier INTEGER     NOT NULL,
  packet_id        SMALLINT    NOT NULL,
  car_index        SMALLINT    NOT NULL,

  processed_at     TIMESTAMPTZ NOT NULL DEFAULT now(),

  PRIMARY KEY (session_uid, frame_identifier, packet_id, car_index)
);

CREATE INDEX IF NOT EXISTS idx_processed_packets_time ON telemetry.processed_packets(processed_at DESC);
```

---

### 9.5 Raw telemetry (TimescaleDB hypertables)

#### 9.5.1 car_telemetry_raw

Зберігає високочастотні snaphots з `CarTelemetry` для **carIndex**. Для MVP можна писати лише player car, але схема одразу multi-car.

```sql
CREATE TABLE IF NOT EXISTS telemetry.car_telemetry_raw (
  ts               TIMESTAMPTZ NOT NULL,

  session_uid      BIGINT      NOT NULL,
  frame_identifier INTEGER     NOT NULL,
  car_index        SMALLINT    NOT NULL,

  -- основні поля (MVP)
  speed_kph        SMALLINT    NULL,
  throttle         REAL        NULL,
  steer            REAL        NULL,
  brake            REAL        NULL,
  gear             SMALLINT    NULL,
  engine_rpm       INTEGER     NULL,
  drs              SMALLINT    NULL,

  -- для дебагу/traceability
  session_time_s   REAL        NULL,

  PRIMARY KEY (ts, session_uid, frame_identifier, car_index)
);

SELECT create_hypertable('telemetry.car_telemetry_raw', 'ts', if_not_exists => TRUE);

CREATE INDEX IF NOT EXISTS idx_car_tel_raw_session_time ON telemetry.car_telemetry_raw(session_uid, ts DESC);
CREATE INDEX IF NOT EXISTS idx_car_tel_raw_frame ON telemetry.car_telemetry_raw(session_uid, frame_identifier);
```

#### 9.5.2 car_status_raw

```sql
CREATE TABLE IF NOT EXISTS telemetry.car_status_raw (
  ts               TIMESTAMPTZ NOT NULL,

  session_uid      BIGINT      NOT NULL,
  frame_identifier INTEGER     NOT NULL,
  car_index        SMALLINT    NOT NULL,

  -- основні поля (MVP)
  traction_control SMALLINT    NULL,
  abs              SMALLINT    NULL,
  fuel_in_tank     REAL        NULL,
  fuel_mix         SMALLINT    NULL,
  drs_allowed      BOOLEAN     NULL,
  tyres_compound   SMALLINT    NULL,
  ers_store_energy REAL        NULL,

  session_time_s   REAL        NULL,

  PRIMARY KEY (ts, session_uid, frame_identifier, car_index)
);

SELECT create_hypertable('telemetry.car_status_raw', 'ts', if_not_exists => TRUE);

CREATE INDEX IF NOT EXISTS idx_car_status_raw_session_time ON telemetry.car_status_raw(session_uid, ts DESC);
```

---

### 9.6 Агрегати кіл/секторів (PostgreSQL)

#### 9.6.1 laps

```sql
CREATE TABLE IF NOT EXISTS telemetry.laps (
  session_uid          BIGINT      NOT NULL REFERENCES telemetry.sessions(session_uid) ON DELETE CASCADE,
  car_index            SMALLINT    NOT NULL,

  lap_number           SMALLINT    NOT NULL,

  lap_time_ms          INTEGER     NULL,
  sector1_time_ms      INTEGER     NULL,
  sector2_time_ms      INTEGER     NULL,
  sector3_time_ms      INTEGER     NULL,

  is_invalid           BOOLEAN     NOT NULL DEFAULT false,
  penalties_seconds    SMALLINT    NULL,

  started_at           TIMESTAMPTZ NULL,
  ended_at             TIMESTAMPTZ NULL,

  created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),

  PRIMARY KEY (session_uid, car_index, lap_number)
);

CREATE INDEX IF NOT EXISTS idx_laps_session_car ON telemetry.laps(session_uid, car_index, lap_number DESC);
CREATE INDEX IF NOT EXISTS idx_laps_session_time ON telemetry.laps(session_uid, ended_at DESC);
```

#### 9.6.2 session_summary

```sql
CREATE TABLE IF NOT EXISTS telemetry.session_summary (
  session_uid           BIGINT      NOT NULL REFERENCES telemetry.sessions(session_uid) ON DELETE CASCADE,
  car_index             SMALLINT    NOT NULL,

  total_laps            SMALLINT    NULL,
  best_lap_time_ms      INTEGER     NULL,
  best_lap_number       SMALLINT    NULL,

  best_sector1_ms       INTEGER     NULL,
  best_sector2_ms       INTEGER     NULL,
  best_sector3_ms       INTEGER     NULL,

  last_updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),

  PRIMARY KEY (session_uid, car_index)
);

CREATE INDEX IF NOT EXISTS idx_session_summary_best_lap ON telemetry.session_summary(best_lap_time_ms);
```

---

### 9.7 Retention / cleanup (TimescaleDB)

#### 9.7.1 Retention для raw

```sql
-- Приклад: тримати raw 14 днів (конфігуровано)
SELECT add_retention_policy('telemetry.car_telemetry_raw', INTERVAL '14 days', if_not_exists => TRUE);
SELECT add_retention_policy('telemetry.car_status_raw',    INTERVAL '14 days', if_not_exists => TRUE);
```

#### 9.7.2 Compression (опційно, якщо потрібно зменшити диск)

```sql
-- Для великих обсягів: компресувати чанки старші за 1 день
ALTER TABLE telemetry.car_telemetry_raw SET (
  timescaledb.compress,
  timescaledb.compress_segmentby = 'session_uid,car_index'
);

ALTER TABLE telemetry.car_status_raw SET (
  timescaledb.compress,
  timescaledb.compress_segmentby = 'session_uid,car_index'
);

SELECT add_compression_policy('telemetry.car_telemetry_raw', INTERVAL '1 day', if_not_exists => TRUE);
SELECT add_compression_policy('telemetry.car_status_raw',    INTERVAL '1 day', if_not_exists => TRUE);
```

---

### 9.8 Мінімальні принципи запису/оновлення

- `udp-ingest-service` **не пише** в БД.
- `telemetry-processing-api-service`:
  - перед записом у raw перевіряє `telemetry.processed_packets` (idempotency).
  - raw пише батчами.
  - `laps` та `session_summary` — upsert (оновлення при надходженні нових даних).

---

# 10. Kafka Contracts (DTO + JSON)

### 10.1 Загальні принципи
- Формат: JSON
- Версіонування: `schemaVersion`
- Ідемпотентність: `(sessionUID, frameIdentifier, packetId, carIndex)`
- Усі події загорнуті в єдиний envelope

---

### 10.2 Kafka Envelope

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

---

### 10.3 Payload DTO приклади

#### 10.3.1 CarTelemetryDto

```json
{
  "speedKph": 312,
  "throttle": 0.87,
  "brake": 0.0,
  "steer": -0.12,
  "gear": 7,
  "engineRpm": 11543,
  "drs": 1
}
```

#### 10.3.2 LapDto

```json
{
  "lapNumber": 5,
  "lapDistance": 4231.4,
  "currentLapTimeMs": 87321,
  "sector": 2,
  "isInvalid": false,
  "penaltiesSeconds": 0
}
```

#### 10.3.3 SessionEventDto (SSTA / SEND)

```json
{
  "eventCode": "SSTA",
  "sessionType": "RACE",
  "trackId": 12,
  "totalLaps": 57
}
```

---

## 11. REST + WebSocket API Contracts

### 11.1 REST API (read-only)

#### Отримання списку сесій
```
GET /api/sessions
```

Response:
```json
[
  {
    "sessionUID": 1234567890,
    "trackId": 12,
    "sessionType": "RACE",
    "startedAt": "2026-01-28T20:10:00Z",
    "endedAt": "2026-01-28T20:55:12Z"
  }
]
```

---

#### Деталі сесії
```
GET /api/sessions/{sessionUid}
```

---

#### Кола сесії
```
GET /api/sessions/{sessionUid}/laps?carIndex=0
```

---

### 11.2 WebSocket API

Endpoint:
```
/ws/live
```

#### Subscribe message

```json
{
  "type": "SUBSCRIBE",
  "sessionUID": 1234567890,
  "carIndex": 0
}
```

#### Live snapshot message (10 Hz)

```json
{
  "type": "SNAPSHOT",
  "ts": "2026-01-28T21:15:44.000Z",
  "speedKph": 298,
  "gear": 6,
  "rpm": 10832,
  "throttle": 0.91,
  "brake": 0.0,
  "currentLap": 5,
  "currentSector": 2
}
```

---

## 12. Internal State Machines

### 12.1 Session State Machine

```
[IDLE]
   |
   | SSTA
   v
[ACTIVE]
   |
   | SEND
   v
[FINISHED]

Fallback:
[ACTIVE] -- no-data-timeout --> [FINISHED]
```

**ACTIVE state responsibilities:**
- прийом telemetry
- агрегація lap/sector
- live streaming

---

### 12.2 Lap State Machine (per carIndex)

```
[NOT_STARTED]
     |
     | lap start detected
     v
[IN_PROGRESS]
     |
     | sector complete
     v
[SECTOR_COMPLETED]
     |
     | lap complete
     v
[COMPLETED]
```

**Пояснення:**
- lap start/complete визначається з `LapData`
- invalid lap може перейти в COMPLETED з прапором `isInvalid=true`

---

### 12.3 Aggregation rules

- агрегат живе в memory під час ACTIVE сесії
- persistence:
  - raw → TimescaleDB
  - aggregates → PostgreSQL
- при FINISHED:
  - flush агрегатів
  - закриття WebSocket stream

---

## 13. API

### 13.1 REST
- GET /api/sessions
- GET /api/sessions/{sessionUid}
- GET /api/laps
- GET /api/sectors
- GET /api/summary

### 13.2 WebSocket
- /ws/live
- live telemetry snapshot

---

## 14. Single car → Multi car evolution

MVP:
- обробляється лише player car

Закладено:
- carIndex у всіх DTO
- агрегати по (sessionUID, carIndex)

---

## 15. Deployment

### 15.1 Docker Compose
- kafka
- postgres + timescaledb
- ingest service
- processing/api service
- ui

### 15.2 Volumes
- Kafka data
- Postgres data

---

## 16. Database Schema (DDL)

### 16.1 Загальні принципи

- Raw телеметрія зберігається у **TimescaleDB hypertables**
- Агреговані та довгоживучі дані — у звичайних PostgreSQL таблицях
- Усі таблиці привʼязані до `session_uid`
- Часова колонка завжди `ts TIMESTAMPTZ`

---

## 17. TimescaleDB: Raw Telemetry

### 17.1 car_telemetry_raw

```sql
CREATE TABLE car_telemetry_raw (
    session_uid        BIGINT NOT NULL,
    car_index          SMALLINT NOT NULL,
    frame_identifier   INT NOT NULL,
    ts                 TIMESTAMPTZ NOT NULL,

    speed              SMALLINT,
    throttle           REAL,
    steer              REAL,
    brake              REAL,
    gear               SMALLINT,
    engine_rpm         INT,
    drs                BOOLEAN,

    PRIMARY KEY (session_uid, car_index, frame_identifier)
);

SELECT create_hypertable('car_telemetry_raw', 'ts');
```

---

### 17.2 car_status_raw

```sql
CREATE TABLE car_status_raw (
    session_uid        BIGINT NOT NULL,
    car_index          SMALLINT NOT NULL,
    frame_identifier   INT NOT NULL,
    ts                 TIMESTAMPTZ NOT NULL,

    fuel_in_tank       REAL,
    fuel_mix           SMALLINT,
    drs_allowed        BOOLEAN,
    ers_store_energy   REAL,
    tyre_compound      SMALLINT,

    PRIMARY KEY (session_uid, car_index, frame_identifier)
);

SELECT create_hypertable('car_status_raw', 'ts');
```

---

### 17.3 Retention policy

```sql
SELECT add_retention_policy('car_telemetry_raw', INTERVAL '7 days');
SELECT add_retention_policy('car_status_raw', INTERVAL '7 days');
```

---

## 18. PostgreSQL: Aggregates & Metadata

### 18.1 sessions

```sql
CREATE TABLE sessions (
    session_uid        BIGINT PRIMARY KEY,
    track_id           SMALLINT,
    session_type       SMALLINT,
    total_laps         SMALLINT,
    track_length       INT,

    started_at         TIMESTAMPTZ,
    ended_at           TIMESTAMPTZ,
    finished           BOOLEAN DEFAULT FALSE
);
```

---

### 18.2 laps

```sql
CREATE TABLE laps (
    id                 BIGSERIAL PRIMARY KEY,
    session_uid        BIGINT NOT NULL,
    car_index          SMALLINT NOT NULL,
    lap_number         SMALLINT NOT NULL,

    lap_time_ms        INT,
    sector1_ms         INT,
    sector2_ms         INT,
    sector3_ms         INT,

    is_valid           BOOLEAN,

    UNIQUE (session_uid, car_index, lap_number)
);
```

---

### 18.3 sectors

```sql
CREATE TABLE sectors (
    id                 BIGSERIAL PRIMARY KEY,
    session_uid        BIGINT NOT NULL,
    car_index          SMALLINT NOT NULL,
    lap_number         SMALLINT NOT NULL,
    sector_index       SMALLINT NOT NULL,

    sector_time_ms     INT,

    UNIQUE (session_uid, car_index, lap_number, sector_index)
);
```

---

### 18.4 session_summary

```sql
CREATE TABLE session_summary (
    session_uid            BIGINT PRIMARY KEY,
    best_lap_ms            INT,
    best_sector1_ms        INT,
    best_sector2_ms        INT,
    best_sector3_ms        INT,

    total_laps_completed   SMALLINT
);
```

---

## 19. Індекси

```sql
CREATE INDEX idx_laps_session ON laps(session_uid);
CREATE INDEX idx_sectors_session ON sectors(session_uid);
```

---

## 20. Еволюція схеми

- Raw таблиці можуть змінюватися без міграцій
- Aggregates — через Flyway
- Нові поля додаються nullable

---

## 21. Aggregation Contract (raw → derived → aggregated)

### 21.1 Data layers

**Raw**
- 1:1 UDP frames;
- immutable;
- не перераховуються;
- використовуються для replay та дебагу.

**Derived**
- нормалізовані DTO після UDP ingest;
- immutable;
- гарантують єдиний формат для processing.

**Aggregated**
- laps / sectors / session summary;
- можуть бути invalidated та recomputed.

### 21.2 Finalization rules
- lap фіналізується при:
  - старті наступного кола;
  - завершенні сесії.
- session summary фіналізується **тільки** при завершенні сесії.

---

## 22. Reorder та out‑of‑order frames

### 22.1 Ordering model
- primary ordering key: `overallFrameIdentifier`;
- fallback: `frameIdentifier`.

### 22.2 Reorder window
- допустиме вікно out‑of‑order: `N` frames (конфігуровано);
- frame < watermark → ігнорується на рівні агрегації.

### 22.3 Вплив на агрегацію
- raw зберігається завжди;
- агрегати оновлюються лише для frames ≥ watermark;
- порушення фіксуються у метриках.

---

## 23. Flashback / Rewind (state rollback)

### 23.1 Detection
- `FLBK` event;
- regression `overallFrameIdentifier`.

### 23.2 Поведінка processing
- invalidate всі агрегати з `frameIdentifier > rewindFrame`;
- rollback lap / sector state;
- recompute з останнього стабільного checkpoint.

---

## 24. UDP packet loss – формалізація

### 24.1 Метрики
- expectedFrames;
- receivedFrames;
- packet_loss_ratio.

### 24.2 Thresholds

| Packet loss | Статус |
|------------|--------|
| < 1% | OK |
| 1–5% | Degraded |
| > 5% | Unreliable |

### 24.3 Вплив
- raw дані зберігаються без змін;
- агрегати маркуються `confidence = LOW`.

### 24.4 Packet loss detection
- packet loss визначається за gap’ами overallFrameIdentifier;
- розрахунок виконується на рівні processing.

### 24.5 Signalization
- при packet_loss_ratio > 5%:
  - сесія маркується як UNRELIABLE;
  - поле confidence = LOW встановлюється у session_summary;
  - WebSocket клієнтам надсилається warning event.

### 24.6 Downstream impact
- UI зобовʼязаний відображати warning для такої сесії;
- best lap / sectors вважаються приблизними.

---

## 25. Observability Contract

### 25.1 Metrics
- udp_packets_received
- udp_packets_dropped
- out_of_order_frames
- rewind_events
- kafka_lag
- aggregation_invalidations

### 25.2 Critical metrics

|  Metric | Impact |
|---|---|
| kafka_lag  | деградація live UI  |
| udp_packets_dropped  | потенційна втрата даних  |
| out_of_order_frames  | ризик неточних агрегатів  |
| rewind_events  | індикатор нестабільної сесії  |

### 25.3 Health & readiness
- `/actuator/health`
- `/actuator/metrics`

### 25.4 Health semantics
- /actuator/health = DOWN, якщо:
  - Kafka недоступна; 
  - БД недоступна; 
  - processing loop зупинений.
  
### 25.5 Readiness semantics
- сервіс не readiness, якщо:
  - kafka_lag > threshold;
  - ingestion backlog перевищує допустиме значення.

---

## 26. Multi‑Car Scope (explicit)

### 26.1 Підтримується
- всі DTO містять `carIndex`;
- агрегати ідентифікуються `(sessionUID, carIndex)`.

### 26.2 MVP обмеження
- processing лише `playerCarIndex`.

### 26.3 Не підтримується зараз
- міжмашинна агрегація;
- порівняльна аналітика авто.

---

## 27. Майбутні покращення

- Continuous aggregates (Timescale)
- Downsampling policies
- Partitioning по track / session type

