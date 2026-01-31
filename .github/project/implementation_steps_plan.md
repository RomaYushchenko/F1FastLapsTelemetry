# F1 FastLaps Telemetry — План імплементації

> **Мета документа:** аналіз документації проєкту, підсумок архітектури та дрібні покрокові кроки імплементації для *поступової* реалізації MVP.  
> Навігація по документах: [documentation_index.md](documentation_index.md).

---

## Частина 1. Аналіз документації проєкту

### 1.1 Ієрархія документів

Документація в `.github/project/` є **single source of truth**. Залежності між документами:

```
mvp-requirements.md
        │
        ▼
f_1_telemetry_project_architecture.md
        │
        ▼
kafka_contracts_f_1_telemetry.md ◄──────────────┐
        │                                        │
        ▼                                        │
state_machines_specification_f_1_telemetry.md   │
        │                                        │
        ▼                                        │
telemetry_error_and_lifecycle_contract.md ───────┘
        │
        ▼
rest_web_socket_api_contracts_f_1_telemetry.md
        │
        ▼
react_spa_ui_architecture.md
```

---

### 1.2 Призначення кожного документа

| Документ | Призначення | Використовується для |
|----------|-------------|----------------------|
| **mvp-requirements.md** | Scope MVP, функціональні та нефункціональні межі | Планування реалізації, non-goals, edge-cases |
| **f_1_telemetry_project_architecture.md** | Архітектура, data flow, сервіси, DDL, FSM | Розуміння системи, DDL, Kafka envelope, REST/WS |
| **kafka_contracts_f_1_telemetry.md** | Контракт Kafka: envelope, payload, ordering, idempotency | Producers/consumers, schema evolution |
| **state_machines_specification_f_1_telemetry.md** | FSM: Session, Lap, Timeout, Flashback | Processing state, reducer-логіка, тести |
| **telemetry_error_and_lifecycle_contract.md** | Lifecycle та помилки між шарами | Session lifecycle, timeout → endReason |
| **rest_web_socket_api_contracts_f_1_telemetry.md** | REST та WebSocket API | Backend API, React SPA |
| **react_spa_ui_architecture.md** | Архітектура React SPA, екрани, layout (wireframe), потік даних | Реалізація UI, маршрути, компоненти |
| **backend_implementation_plan_f_1_telemetry.md** | Фази реалізації бекенду | Послідовність Phase 0–11 |
| **code_skeleton_java_packages_interfaces.md** | Пакети Java, інтерфейси, модулі | Структура коду, імена класів |
| **documentation_index.md** | Індекс та навігація по документах | Швидкий пошук секцій |
| **ai_agent_instruction_f_1_telemetry_project.md** | Правила для AI-агента | Дотримання документації, формат рішень |

---

### 1.3 Ключові контракти (витяги)

- **Kafka key:** `sessionUID`
- **Idempotency key:** `(sessionUID, frameIdentifier, packetId, carIndex)`
- **Topics:** `telemetry.session`, `telemetry.lap`, `telemetry.carTelemetry`, `telemetry.carStatus`
- **Session FSM:** `INIT → ACTIVE → ENDING → TERMINAL`
- **Lap FSM:** `NOT_STARTED → IN_PROGRESS → SECTOR_COMPLETED → COMPLETED`
- **MVP:** тільки player car (`carIndex = 0`)

---

## Частина 2. Архітектура проєкту

### 2.1 High-level схема

```
┌─────────┐   UDP      ┌────────────────────────────┐
│ F1 Game │ ────────►  │ udp-ingest-service         │
└─────────┘            │ (uses UDP Library:         │
                       │  - parse UDP packets       │
                       │  - publish to Kafka)       │
                       └────────┬───────────────────┘
                                │ Kafka (4 topics)
                                ▼
                    ┌──────────────────────────────────────┐
                    │ telemetry-processing-api-service     │
                    │ (Kafka consumers → FSM →              │
                    │  aggregation → DB → REST/WebSocket)  │
                    └──────────────┬───────────────────────┘
                                   │
                                   ▼
                    ┌──────────────────────────────┐
                    │ PostgreSQL + TimescaleDB     │
                    └──────────────┬───────────────┘
                                   │ REST / WebSocket
                                   ▼
                        ┌─────────────────────┐
                        │    React SPA        │
                        └─────────────────────┘
```

**⚠️ ВАЖЛИВО:** UDP Library **не є окремим сервісом**. Це інфраструктурна бібліотека (як Spring Boot Starter), яка **використовується** udp-ingest-service. Див. [ARCHITECTURE_CLARIFICATION.md](ARCHITECTURE_CLARIFICATION.md) для детального роз'яснення.

---

### 2.2 Компоненти

| Компонент | Відповідальність | Стек | Де знаходиться |
|-----------|------------------|------|----------------|
| **UDP Library (3 модулі)** | Інфраструктура: UDP listener, парсинг, Kafka publishing | Java 17, Pure Java + Spring | f1-telemetry-udp-core, f1-telemetry-udp-spring, f1-telemetry-udp-starter |
| **UDP Ingest Service** | Deployment wrapper для UDP Library, конфігурація | Java 17, Spring Boot | udp-ingest-service |
| **Telemetry Processing & API Service** | Kafka consumers, FSM, агрегація, persistence, REST + WebSocket | Java 17, Spring Boot, JPA, WebSocket | telemetry-processing-api-service |
| **PostgreSQL + TimescaleDB** | Raw telemetry (hypertables), sessions/laps/sectors/summary | PostgreSQL, TimescaleDB | infra/docker-compose.yml |
| **Kafka** | Буфер подій, decoupling, backpressure | Kafka | infra/docker-compose.yml |
| **React SPA** | Live dashboard, перегляд сесій, laps/sectors | React | ui/ (майбутній модуль) |

---

### 2.3 Maven-модулі (фактична структура)

| Модуль | Призначення | Статус |
|--------|-------------|--------|
| **telemetry-api-contracts** | DTO, Kafka envelope, enum (PacketId, EventCode), REST/WS DTO | ✅ Частково (потрібні REST DTOs) |
| **f1-telemetry-udp-core** | Pure Java: UDP listener, dispatcher, packet header decoder | ✅ ЗАВЕРШЕНО (14 тестів) |
| **f1-telemetry-udp-spring** | Spring integration: annotations, handlers, Kafka publisher | ✅ ЗАВЕРШЕНО (32 тести) |
| **f1-telemetry-udp-starter** | Spring Boot autoconfiguration | ✅ ЗАВЕРШЕНО (4 тести) |
| **udp-ingest-service** | Spring Boot app (uses UDP starter) | ✅ Базова структура готова |
| **telemetry-processing-api-service** | Kafka consumers, FSM, aggregation, REST/WS | ❌ ТО DO |
| **infra** | docker-compose (Kafka, PostgreSQL+TimescaleDB), DDL scripts | ❌ TO DO |

---

### 2.4 Data flow (детально)

1. **F1 Game** → UDP пакети (Session, LapData, CarTelemetry, CarStatus) на порт 20777.

2. **udp-ingest-service** (uses UDP Library):
   - UDP Library отримує пакети
   - Built-in handlers парсять binary → DTO
   - Built-in handlers публікують у Kafka (4 топіки)
   - **Не зберігає в БД, не агрегує**

3. **Kafka topics:**
   - `telemetry.session` (key = sessionUID)
   - `telemetry.lap` (key = sessionUID-carIndex)
   - `telemetry.carTelemetry` (key = sessionUID-carIndex)
   - `telemetry.carStatus` (key = sessionUID-carIndex)

4. **telemetry-processing-api-service:**
   - 4 Kafka consumers читають з топіків
   - Idempotency check (processed_packets table)
   - Session FSM (INIT → ACTIVE → ENDING → TERMINAL)
   - Lap aggregation (sector times, lap finalization)
   - Session summary (best lap, best sectors)
   - Batch insert raw telemetry (TimescaleDB hypertables)
   - Upsert aggregated data (laps, sessions, summary)

5. **API layer (telemetry-processing-api-service):**
   - REST endpoints: `GET /api/sessions`, `/sessions/{uid}/laps`, `/summary`
   - WebSocket: `/ws/live` → 10 Hz snapshots для live dashboard

6. **React SPA:**
   - HTTP calls для історичних даних
   - WebSocket для live оновлень

---

## Частина 3. Покроковий план імплементації

Кожен **крок** — це один невеликий обсяг робіт з чітким результатом. Кроки згруповані по **етапах** (Phase).

---

### Етап 0. Bootstrap та репозиторій

| # | Крок | Дії | Критерій готовності |
|---|------|-----|---------------------|
| 0.1 | Створити root POM | Maven multi-module, Java 17, Spring Boot BOM, encoding UTF-8 | `mvn clean install` на root без модулів — OK |
| 0.2 | Додати модуль `telemetry-api-contracts` | Порожній модуль, тільки Lombok | Модуль збирається |
| 0.3 | Додати модуль `udp-ingest-service` | Залежність на contracts, Spring Boot starter | Модуль збирається |
| 0.4 | Додати модуль `telemetry-processing-api-service` | Залежність на contracts, Spring Boot, Kafka, JPA | Модуль збирається |
| 0.5 | Додати модуль `infra` | Тільки папки/файли (без Java) | — |
| 0.6 | Налаштувати CI (GitHub Actions) | `mvn clean install` на push/PR | CI зелений |

---

### Етап 1. Контракти (telemetry-api-contracts)

| # | Крок | Дії | Критерій готовності |
|---|------|-----|---------------------|
| 1.1 | Enum `PacketId` | CAR_TELEMETRY, LAP_DATA, CAR_STATUS, SESSION (за контрактом) | Компіляція |
| 1.2 | Enum `EventCode` | SSTA, SEND, SESSION_TIMEOUT, FLBK | Компіляція |
| 1.3 | Enum / constant `SchemaVersion` | schemaVersion = 1 | Компіляція |
| 1.4 | Клас `KafkaEnvelope<T>` | Поля: schemaVersion, packetId, sessionUID, frameIdentifier, sessionTime, carIndex, producedAt, payload | Відповідає Kafka contract |
| 1.5 | DTO `SessionEventDto` | eventCode, sessionType, trackId, totalLaps (за контрактом) | Компіляція |
| 1.6 | DTO `LapDto` (Kafka) | lapNumber, lapDistance, currentLapTimeMs, sector, isInvalid, penaltiesSeconds | Компіляція |
| 1.7 | DTO `CarTelemetryDto` | speedKph, throttle, brake, steer, gear, engineRpm, drs | Компіляція |
| 1.8 | DTO `CarStatusDto` | tractionControl, abs, fuelInTank, fuelMix, drsAllowed, tyresCompound, ersStoreEnergy (мінімум MVP) | Компіляція |
| 1.9 | REST DTO `SessionDto` | sessionUID, trackId, sessionType, startedAt, endedAt, endReason тощо | За REST контрактом |
| 1.10 | REST DTO `LapDto` (REST) або `LapResponseDto` | lapNumber, lapTimeMs, sector1/2/3, isInvalid | За REST контрактом |
| 1.11 | REST DTO `SessionSummaryDto` | bestLapTimeMs, bestLapNumber, bestSector1/2/3Ms, totalLaps | За REST контрактом |
| 1.12 | WS: `WsSubscribeMessage`, `WsSnapshotMessage`, `WsSessionEndedMessage` | Поля за WebSocket контрактом | Компіляція |

---

### Етап 2. Інфраструктура (infra)

| # | Крок | Дії | Критерій готовності                               |
|---|------|-----|---------------------------------------------------|
| 2.1 | docker-compose: Zookeeper + Kafka | Порти, volumes | `docker-compose up -d` — Kafka доступна           |
| 2.2 | docker-compose: PostgreSQL + TimescaleDB | Extension timescaledb, порт, volume | Підключення до DB, `CREATE EXTENSION timescaledb` |
| 2.3 | Init SQL: схема `telemetry` | `CREATE SCHEMA IF NOT EXISTS telemetry` | Виконується при старті контейнера                 |
| 2.4 | Init SQL: таблиця `telemetry.sessions` | DDL з architecture doc | Таблиця створюється                               |
| 2.5 | Init SQL: таблиця `telemetry.session_cars` | DDL | Таблиця створюється                               |
| 2.6 | Init SQL: таблиця `telemetry.processed_packets` | DDL для idempotency | Таблиця створюється                               |
| 2.7 | Init SQL: таблиця `telemetry.car_telemetry_raw` + hypertable | DDL + create_hypertable | Таблиця створюється                               |
| 2.8 | Init SQL: таблиця `telemetry.car_status_raw` + hypertable | DDL + create_hypertable | Таблиця створюється                               |
| 2.9 | Init SQL: таблиця `telemetry.laps` | DDL | Таблиця створюється                               |
| 2.10 | Init SQL: таблиця `telemetry.session_summary` | DDL | Таблиця створюється                               |
| 2.11 | Retention policy (опційно) | add_retention_policy для raw таблиць | Політика додана                                   |
| 2.12 | Скрипт створення Kafka topics | telemetry.session, .lap, .carTelemetry, .carStatus | Topics створюються автоматично                    |

---

### Етап 3. UDP Ingestion (UDP Library + udp-ingest-service) ✅ ЗАВЕРШЕНО

> **Статус:** ✅ **ПОВНІСТЮ РЕАЛІЗОВАНО**  
> **Детальна документація:** [UDP_LIBRARY_SUMMARY.md](../UDP_LIBRARY_SUMMARY.md)  
> **Покроковий план:** [udp_library_implementation_plan.md](udp_library_implementation_plan.md)  
> **Роз'яснення архітектури:** [ARCHITECTURE_CLARIFICATION.md](ARCHITECTURE_CLARIFICATION.md)

#### Що вже реалізовано

**UDP Бібліотека (3 модулі):**
- ✅ **f1-telemetry-udp-core** — Pure Java, UDP listener, dispatcher (14 тестів)
- ✅ **f1-telemetry-udp-spring** — Spring інтеграція, анотації, built-in handlers (32 тести)
- ✅ **f1-telemetry-udp-starter** — Spring Boot autoconfiguration (4 тести)
- ✅ **Всього 50 тестів, всі проходять**

**Вбудовані обробники (в бібліотеці):**
- ✅ SessionPacketHandler (packetId=1) → Kafka topic: `telemetry.session`
- ✅ LapDataPacketHandler (packetId=2) → Kafka topic: `telemetry.lap`
- ✅ CarTelemetryPacketHandler (packetId=6) → Kafka topic: `telemetry.carTelemetry`
- ✅ CarStatusPacketHandler (packetId=7) → Kafka topic: `telemetry.carStatus`

**udp-ingest-service:**
- ✅ Spring Boot application з dependency на UDP starter
- ✅ Конфігурація (application.yml): UDP port 20777, Kafka settings
- ✅ Автоматичний запуск UDP listener при старті
- ✅ Автоматична реєстрація built-in handlers
- ✅ Готовий до deployment

#### Що робить UDP Library

**Інфраструктура (не бізнес-логіка):**
1. Отримує UDP пакети з F1 Game
2. Парсить packet header (sessionUID, frameIdentifier, packetId, etc.)
3. Диспетчеризує пакети за packetId
4. Built-in handlers парсять binary payload → DTO
5. Будують KafkaEnvelope з метаданими
6. Публікують у Kafka з retry/throttling
7. **НЕ ЗБЕРІГАЄ в БД, не агрегує, не має REST API**

#### Роль udp-ingest-service

**Deployment wrapper (мінімальний код):**
- Main class: `UdpIngestApplication.java` (~10 рядків)
- Конфігурація: UDP port, Kafka bootstrap servers
- **Опціонально:** Кастомні handlers (якщо потрібні інші типи пакетів)
- **Опціонально:** Метрики, health checks, custom business logic

**Чому потрібен окремий сервіс:**
- ✅ Deployment boundary (мікросервіс з власним lifecycle)
- ✅ Production configuration (порти, Kafka URL)
- ✅ Розширюваність (custom handlers)
- ✅ Масштабування (кілька інстансів)
- ✅ Ізоляція від processing logic

**Аналогія:** UDP library = Spring Boot Starter, udp-ingest-service = ваш Spring Boot application

#### Ключові переваги реалізації

- ✅ **Відсутність Spring залежностей у core** — Pure Java, повністю тестований
- ✅ **Декларативна модель** — `@F1UdpListener`, `@F1PacketHandler` annotations
- ✅ **Decorator pattern** — Retry, throttling для Kafka publisher
- ✅ **Відсутність циклічних залежностей** — Manual bean wiring
- ✅ **Переповторне використання** — Бібліотека може використовуватись в інших проєктах
- ✅ **50 тестів** — Unit + integration тести

#### Залишається зробити (мінімально)

| # | Завдання | Зусилля | Пріоритет |
|---|----------|---------|-----------|
| 3.1 | Health checks в udp-ingest-service | 30 хв | LOW (працює з Spring Actuator) |
| 3.2 | Metrics integration (Micrometer) | 1-2 год | MEDIUM (для production) |
| 3.3 | Docker image для udp-ingest-service | 1 год | HIGH (для docker-compose) |

**Висновок:** Етап 3 фактично завершений, udp-ingest-service готовий до використання.

---

### Етап 4. Processing — State та Lifecycle

| # | Крок | Дії | Критерій готовності |
|---|------|-----|---------------------|
| 4.1 | Enum `SessionState` | INIT, ACTIVE, ENDING, TERMINAL | За FSM doc |
| 4.2 | Enum `EndReason` | EVENT_SEND, NO_DATA_TIMEOUT, MANUAL | За lifecycle contract |
| 4.3 | Клас `SessionRuntimeState` | sessionUID, state, startedAt, endedAt, endReason, lastSeenAt, watermark (per carIndex) — thread-safe | Доступ по sessionUID |
| 4.4 | SessionStateManager: getOrCreate(sessionUID) | In-memory map, створення нового SessionRuntimeState при першому зверненні | Один state на sessionUID |
| 4.5 | SessionStateManager: close(sessionUID) | Перехід у TERMINAL, очищення з мапи (або зберігати для history) | Стан не оновлюється |
| 4.6 | SessionLifecycleService.onSessionStarted(SessionEventDto) | Перехід INIT→ACTIVE, збереження startedAt, upsert session у DB (етап 7) — поки тільки state | State = ACTIVE |
| 4.7 | SessionLifecycleService.onSessionEnded(SessionEventDto) | Перехід ACTIVE→ENDING | State = ENDING |
| 4.8 | Internal: FINALIZED transition | ENDING → TERMINAL після flush buffers, closed WS | State = TERMINAL |
| 4.9 | NoDataTimeoutWorker (scheduler) | Кожні N сек перевірка lastSeenAt; якщо > timeout → генерувати SESSION_TIMEOUT (або викликати onSessionEnded з pseudo-event) | Сесія переходить у ENDING по timeout |
| 4.10 | Ігнорування пакетів до SSTA | Пакети з невідомим sessionUID або до отримання SSTA — не обробляти (метрика packets_before_ssta) | Метрика зростає |
| 4.11 | Ігнорування пакетів після SEND/TERMINAL | late_packets_after_finish не зберігати в raw | Метрика late_packets_after_finish |

---

### Етап 5. Kafka Consumers та Idempotency

| # | Крок | Дії | Критерій готовності |
|---|------|-----|---------------------|
| 5.1 | Kafka consumer config | bootstrap-servers, group-id, JSON deserializer для envelope | Consumer підключається |
| 5.2 | ProcessedPacketRepository | INSERT (session_uid, frame_identifier, packet_id, car_index) ON CONFLICT DO NOTHING; перевірка exists | JdbcTemplate або JPA |
| 5.3 | Idempotency check: перед обробкою перевірити (sessionUID, frameId, packetId, carIndex) | Якщо вже є в processed_packets — skip | Повторне повідомлення не змінює агрегати |
| 5.4 | Consumer telemetry.session | Deserialize envelope, виклик SessionLifecycleService.onSessionStarted/onSessionEnded по eventCode | State змінюється |
| 5.5 | Consumer telemetry.lap | Deserialize, idempotency check, передача в LapAggregator (етап 6) | Laps оновлюються |
| 5.6 | Consumer telemetry.carTelemetry | Idempotency, передача в RawTelemetryWriter + оновлення SessionRuntimeState (snapshot для WS) | Raw + state |
| 5.7 | Consumer telemetry.carStatus | Idемпотентність, RawTelemetryWriter (car_status_raw) | Raw записується |
| 5.8 | Watermark: оновлювати lastSeenFrame; агрегувати тільки frame ≥ watermark | За state_machines_spec | Out-of-order не ламає агрегати |

---

### Етап 6. Агрегація (Lap, Session Summary)

| # | Крок | Дії | Критерій готовності |
|---|------|-----|---------------------|
| 6.1 | LapRuntimeState (per carIndex) | lapNumber, sectorTimes[], currentLapTimeMs, isInvalid | In-memory |
| 6.2 | LapAggregator: оновлення по LapDto | Sector complete → збереження sector time; lap complete → фіналізація кола | Laps table upsert |
| 6.3 | Lap finalization rules | При старті наступного кола або при завершенні сесії — запис lap у DB | За FSM |
| 6.4 | SessionSummaryAggregator | При фіналізації lap — оновлення best_lap_time_ms, best_sector_*_ms, best_lap_number, total_laps | session_summary upsert |
| 6.5 | RawTelemetryWriter: batch insert car_telemetry_raw | Batch з N записів, ts від sessionTime (або derived), тільки для ACTIVE | Записи в TimescaleDB |
| 6.6 | RawTelemetryWriter: batch insert car_status_raw | Аналогічно | Записи в TimescaleDB |
| 6.7 | Confidence flag (опційно MVP) | При packet_loss > 5% встановлювати confidence = LOW у session_summary | За observability contract |

---

### Етап 7. Persistence (Repositories)

| # | Крок | Дії | Критерій готовності |
|---|------|-----|---------------------|
| 7.1 | SessionWriteRepository | Upsert session (started_at, ended_at, end_reason, track_id, session_type тощо) | Після SSTA та SEND |
| 7.2 | SessionReadRepository | findById(sessionUid), findAll(limit, offset) | Для REST |
| 7.3 | LapWriteRepository | Upsert lap (session_uid, car_index, lap_number, lap_time_ms, sector*_time_ms, is_invalid) | Після lap finalization |
| 7.4 | LapReadRepository | findBySessionUid(sessionUid, carIndex) | Для REST |
| 7.5 | SessionSummaryWriteRepository | Upsert session_summary | Після агрегації |
| 7.6 | SessionSummaryReadRepository або включити в SessionReadRepository | Для GET /api/sessions/{uid}/summary | Для REST |
| 7.7 | Flyway migrations (опційно) | Якщо DDL веде Flyway замість init SQL — перенести DDL у V1__schema.sql | Міграції застосовуються |

---

### Етап 8. REST API ✅ ЗАВЕРШЕНО

> **Статус:** ✅ **ПОВНІСТЮ РЕАЛІЗОВАНО** (February 1, 2026)  
> **Детальна документація:** [IMPLEMENTATION_PROGRESS.md](IMPLEMENTATION_PROGRESS.md#-stage-8-rest-api-complete)  
> **Файли:** 4 controllers (SessionController, LapController, SessionSummaryController, RestExceptionHandler)

| # | Крок | Дії | Критерій готовності | Статус |
|---|------|-----|---------------------|--------|
| 8.1 | GET /api/sessions | List SessionDto, limit/offset | Відповідь за контрактом | ✅ |
| 8.2 | GET /api/sessions/{sessionUid} | Один SessionDto | 404 якщо немає | ✅ |
| 8.3 | GET /api/sessions/{sessionUid}/laps | List laps, carIndex=0 default | Відповідь за контрактом | ✅ |
| 8.4 | GET /api/sessions/{sessionUid}/sectors | Сектори по колах (можна з laps) | За контрактом | ✅ |
| 8.5 | GET /api/sessions/{sessionUid}/summary | SessionSummaryDto | Відповідь за контрактом | ✅ |
| 8.6 | Поле state в відповідях | ACTIVE / FINISHED (TERMINAL) завжди присутнє де потрібно | За REST contract | ✅ |
| 8.7 | (Опційно) GET /api/sessions/{uid}/telemetry?from=&to=&metric= | Читання з car_telemetry_raw | Для графіків | ⚠️ Skipped (MVP) |

**Реалізовано:**
- ✅ SessionController з endpoints: list, get by ID, get active
- ✅ LapController з endpoints: laps, sectors
- ✅ SessionSummaryController з endpoint: summary
- ✅ RestExceptionHandler для стандартизованих помилок
- ✅ Entity-to-DTO конвертація
- ✅ Інтеграція з SessionStateManager для визначення ACTIVE state
- ✅ Pagination підтримка для списку сесій

---

### Етап 9. WebSocket Live ✅ ЗАВЕРШЕНО

> **Статус:** ✅ **ПОВНІСТЮ РЕАЛІЗОВАНО** (February 1, 2026)  
> **Детальна документація:** [IMPLEMENTATION_PROGRESS.md](IMPLEMENTATION_PROGRESS.md#-stage-9-websocket-live-feed-complete)  
> **Файли:** 5 components (Config, SessionManager, Controller, Broadcaster, EventListener)

| # | Крок | Дії | Критерій готовності | Статус |
|---|------|-----|---------------------|--------|
| 9.1 | Endpoint /ws/live (або /ws/telemetry/live) | STOMP або plain WebSocket за контрактом | Підключення встановлюється | ✅ |
| 9.2 | Subscribe message: sessionUID, carIndex | Зберегти підписку клієнта | Клієнт отримує тільки свою сесію | ✅ |
| 9.3 | Snapshot builder | З SessionRuntimeState → WsSnapshotMessage (speed, rpm, gear, throttle, brake, currentLap, currentSector) | 1 snapshot на виході | ✅ |
| 9.4 | Scheduler 10 Hz | Кожні 100 ms: для активних підписок взяти state, зібрати snapshot, відправити | UI отримує ~10 msg/s | ✅ |
| 9.5 | Reconnect: snapshot on connect | При новому підключенні відправити поточний snapshot | Клієнт одразу бачить стан | ✅ |
| 9.6 | Session ended → WsSessionEndedMessage | При переході в TERMINAL відправити і закрити stream | Клієнт бачить завершення | ✅ |

**Реалізовано:**
- ✅ WebSocketConfig: STOMP over WebSocket з SockJS fallback
- ✅ WebSocketSessionManager: Thread-safe subscription tracking
- ✅ WebSocketController: Subscribe/unsubscribe handlers з session validation
- ✅ LiveDataBroadcaster: @Scheduled broadcaster на 10 Hz
- ✅ WebSocketEventListener: Auto-cleanup на disconnect
- ✅ Інтеграція з SessionLifecycleService для SESSION_ENDED notifications
- ✅ SessionRuntimeState.getLatestSnapshot() method

---

### Етап 10. Observability

| # | Крок | Дії | Критерій готовності |
|---|------|-----|---------------------|
| 10.1 | Метрики: udp_packets_received, udp_packets_dropped | Ingest: counter при отриманні; dropped при backpressure | /actuator/metrics |
| 10.2 | Метрики: kafka_lag | Consumer lag по групах | Відображається |
| 10.3 | Метрики: packet_loss_ratio, late_packets_after_finish, packets_before_ssta | Processing | Відображаються |
| 10.4 | Health: Kafka DOWN → DOWN | Spring Kafka health indicator | /actuator/health |
| 10.5 | Health: DB DOWN → DOWN | DataSource health | /actuator/health |
| 10.6 | Structured logging (опційно) | JSON або key-value для важливих подій | Логи зрозумілі |

---

### Етап 11. React SPA (UI)

| # | Крок | Дії | Критерій готовності |
|---|------|-----|---------------------|
| 11.1 | Проєкт React (Vite/CRA) | Базова структура, роутинг | Сторінки: /, /sessions, /sessions/:id |
| 11.2 | API client | fetch/axios до /api/sessions, /api/sessions/:id/laps, /summary | Дані приходять |
| 11.3 | Live dashboard: підключення WebSocket | /ws/live, subscribe по sessionUID | Отримання snapshot |
| 11.4 | Live dashboard: відображення Speed, RPM, Gear, Throttle, Brake, DRS | Компоненти + state з WS | Оновлення в реальному часі |
| 11.5 | Сторінка списку сесій | GET /api/sessions, таблиця/картки | Список сесій |
| 11.6 | Сторінка сесії: таблиця кіл | GET /api/sessions/:id/laps | Best lap highlight |
| 11.7 | Сторінка сесії: сектори | GET /api/sessions/:id/sectors або з laps | Fastest S1/S2/S3 + lap number |
| 11.8 | Сторінка сесії: summary | GET /api/sessions/:id/summary | Best lap, best sectors |
| 11.9 | (Опційно) Графік speed/rpm по часу | GET telemetry?from=&to=&metric= | Графік |

---

### Етап 12. Фінальна валідація MVP

| # | Крок | Дії | Критерій готовності |
|---|------|-----|---------------------|
| 12.1 | Сценарій: Practice → Quali → Race без рестарту | Три сесії підряд, різні sessionUID | Усі три збережені, коректні laps/summary |
| 12.2 | Сценарій: SEND vs NO_DATA timeout | Завершення через SEND; окремо — вимкнення гри без SEND | endReason EVENT_SEND / NO_DATA_TIMEOUT |
| 12.3 | Сценарій: late packets | Відправити пакет після SEND | Не потрапляє в raw, метрика late_packets |
| 12.4 | Сценарій: partial session (timeout) | Немає даних 30 сек → timeout | Сесія з end_reason NO_DATA_TIMEOUT |
| 12.5 | Перевірка: UI live + history | Відкрити dashboard під час гри; потім переглянути сесію | Live оновлюється; історія коректна |

---

## Частина 4. Порядок виконання (оновлено February 1, 2026)

### Поточний статус
- ✅ **Етап 0** — ГОТОВО (Maven multi-module structure)
- ✅ **Етап 1** — ГОТОВО (19 DTOs: Kafka, REST, WebSocket)
- ✅ **Етап 2** — ГОТОВО (Infrastructure: docker-compose + 9 DDL scripts)
- ✅ **Етап 3** — ГОТОВО (UDP Library + udp-ingest-service, 50 tests)
- ✅ **Етап 4** — ГОТОВО (State Management: FSM, lifecycle, timeout)
- ✅ **Етап 5** — ГОТОВО (Kafka Consumers + Idempotency)
- ✅ **Етап 6** — ГОТОВО (Aggregation: Lap tracking, session summary)
- ✅ **Етап 7** — ГОТОВО (Persistence: JPA entities + repositories)
- ✅ **Етап 8** — ГОТОВО (REST API: 4 controllers, all endpoints)
- ✅ **Етап 9** — ГОТОВО (WebSocket: STOMP, 10 Hz broadcast)
- ❌ **Етап 10** — TO DO (Observability)
- ❌ **Етап 11** — TO DO (React UI)
- ❌ **Етап 12** — TO DO (Final Validation)

**Backend Implementation: 75% Complete**

### Вже реалізовано (Stages 0-9)

#### ✅ **Stage 1: Foundation (COMPLETE)**
1. ✅ Етап 0: Maven multi-module structure
2. ✅ Етап 1: All API contracts (Kafka, REST, WebSocket DTOs)
3. ✅ Етап 2: Infrastructure (docker-compose, PostgreSQL, Kafka, DDL scripts)
4. ✅ Етап 3: UDP Library with 50 passing tests

**Результат:** Повна інфраструктура + UDP ingestion ready

---

#### ✅ **Stage 2: Core Processing Logic (COMPLETE)**
5. ✅ Етап 4: State Management (FSM: INIT→ACTIVE→ENDING→TERMINAL)
6. ✅ Етап 5: Kafka Consumers (4 topics) + Idempotency service
7. ✅ Етап 7: Persistence Layer (JPA entities, repositories)

**Результат:** Дані з Kafka зберігаються в БД з коректним state management

---

#### ✅ **Stage 3: Business Aggregation (COMPLETE)**
8. ✅ Етап 6: Aggregation (LapAggregator, SessionSummaryAggregator)

**Результат:** Laps, sectors, summary коректно агрегуються

---

#### ✅ **Stage 4: API Layer (COMPLETE)**
9. ✅ Етап 8: REST API (SessionController, LapController, SummaryController)
10. ✅ Етап 9: WebSocket Live (STOMP, 10 Hz snapshots, subscription management)

**Результат:** Backend API готове для UI

---

### Залишається реалізувати (Stages 10-12)

#### ❌ **Stage 5: Observability (NOT STARTED)**
11. **Етап 10:** Observability (metrics, health checks, logging)
   - Spring Actuator endpoints
   - Micrometer metrics
   - Health indicators for Kafka and DB
   - Custom metrics (packet loss, lag, etc.)

**Результат:** Production monitoring ready

**Estimated Effort:** 4-6 hours

---

#### ❌ **Stage 6: Frontend (NOT STARTED)**
12. **Етап 11:** React SPA (UI для live dashboard та історії)
   - Session list screen
   - Live dashboard with WebSocket
   - Historical lap analysis
   - Session summary view

**Результат:** Повний end-to-end MVP

**Estimated Effort:** 20-30 hours

---

#### ❌ **Stage 7: Testing & Validation (NOT STARTED)**
13. **Етап 12:** Final Validation (scenarios, end-to-end testing)
   - Integration tests
   - F1 game testing scenarios
   - Performance validation
   - Error handling verification

**Результат:** Production-ready MVP

**Estimated Effort:** 6-8 hours

---

### Паралелізація

**Можна робити паралельно:**
- ~~Етап 1 (REST DTOs) + Етап 2 (Infrastructure)~~ ✅ DONE
- ~~Етап 4 (State) + Етап 7 (Repositories)~~ ✅ DONE
- ~~Етап 8 (REST) + Етап 9 (WebSocket)~~ ✅ DONE
- **Етап 10 (Observability) + Етап 11 (React UI)** ← Можна робити паралельно

**Залежності (must be sequential):**
- ~~Етап 1 → Етапи 4, 5, 8, 9~~ ✅ DONE
- ~~Етап 2 → Етапи 5, 6, 7~~ ✅ DONE
- ~~Етап 4 → Етап 5~~ ✅ DONE
- ~~Етап 5 → Етап 6~~ ✅ DONE
- ~~Етап 6, 7 → Етап 8~~ ✅ DONE
- ~~Етап 4, 6 → Етап 9~~ ✅ DONE
- Етапи 8, 9 → Етап 11 (UI потребує API) ← **READY TO START**
- Етапи 10, 11 → Етап 12 (Final testing потребує всі компоненти)

---

### Оціночний час виконання (залишається)

| Етап | Опис | Початковий час | Статус | Залишилося |
|------|------|----------------|--------|------------|
| ~~0~~ | ~~Bootstrap~~ | ~~4 год~~ | ✅ | 0 |
| ~~1~~ | ~~REST/WS DTOs~~ | ~~4-6 год~~ | ✅ | 0 |
| ~~2~~ | ~~Infrastructure~~ | ~~6-8 год~~ | ✅ | 0 |
| ~~3~~ | ~~UDP Library~~ | ~~30-40 год~~ | ✅ | 0 |
| ~~4~~ | ~~State Management~~ | ~~10-12 год~~ | ✅ | 0 |
| ~~5~~ | ~~Kafka Consumers~~ | ~~8-10 год~~ | ✅ | 0 |
| ~~6~~ | ~~Aggregation~~ | ~~12-15 год~~ | ✅ | 0 |
| ~~7~~ | ~~Persistence~~ | ~~8-10 год~~ | ✅ | 0 |
| ~~8~~ | ~~REST API~~ | ~~6-8 год~~ | ✅ | 0 |
| ~~9~~ | ~~WebSocket~~ | ~~8-10 год~~ | ✅ | 0 |
| 10 | Observability | 4-6 год | ❌ | **4-6 год** |
| 11 | React UI | 20-30 год | ❌ | **20-30 год** |
| 12 | Validation | 6-8 год | ❌ | **6-8 год** |

**Виконано:** ~100-125 годин  
**Залишилося:** ~30-44 години  
**Загальна економія:** UDP Library була реалізована окремо (~30-40 год)

---

## Частина 5. Чеклист готовності MVP (з mvp-requirements)

- [ ] UDP ingestion працює стабільно і публікує події в Kafka.
- [ ] Дані сесії зберігаються в БД: raw telemetry (обмежено), laps + sectors + summary.
- [ ] UI показує: live dashboard, список сесій, перегляд сесії з колами/секторами, fastest lap і fastest sectors з прив'язкою до номера кола.
- [ ] Lifecycle детермінований: SSTA → ACTIVE → SEND/Timeout → ENDING → TERMINAL.
- [ ] Локальний запуск одним docker-compose (infra + сервіси за бажанням у compose).

---

---

## Посилання на документацію

| Документ | Опис |
|----------|------|
| [mvp-requirements.md](mvp-requirements.md) | Scope MVP, функціональні та нефункціональні межі |
| [f_1_telemetry_project_architecture.md](f_1_telemetry_project_architecture.md) | Архітектура, data flow, DDL, FSM |
| [kafka_contracts_f_1_telemetry.md](kafka_contracts_f_1_telemetry.md) | Kafka envelope, payload, ordering |
| [state_machines_specification_f_1_telemetry.md](state_machines_specification_f_1_telemetry.md) | Session/Lap/Timeout FSM |
| [rest_web_socket_api_contracts_f_1_telemetry.md](rest_web_socket_api_contracts_f_1_telemetry.md) | REST та WebSocket API |
| [react_spa_ui_architecture.md](react_spa_ui_architecture.md) | Архітектура React SPA, екрани, layout, потік даних |
| [backend_implementation_plan_f_1_telemetry.md](backend_implementation_plan_f_1_telemetry.md) | Фази реалізації (Phase 0–11) |
| [code_skeleton_java_packages_interfaces.md](code_skeleton_java_packages_interfaces.md) | Пакети Java, інтерфейси |

*Документ узгоджений з усіма переліченими документами вище.*
