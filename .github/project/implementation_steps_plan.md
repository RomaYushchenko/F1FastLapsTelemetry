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
┌─────────┐    UDP     ┌──────────────────┐    Kafka     ┌─────────────────────────────────┐
│ F1 Game │ ────────► │ udp-ingest-svc   │ ───────────► │ telemetry-processing-api-svc    │
└─────────┘           │ (parse+publish)  │              │ (consume+aggregate+store+API)    │
                      └──────────────────┘              └──────────────┬──────────────────┘
                                                                        │
                                                                        ▼
                                                         ┌──────────────────────────────┐
                                                         │ PostgreSQL + TimescaleDB     │
                                                         └──────────────┬───────────────┘
                                                                        │ REST / WebSocket
                                                                        ▼
                                                              ┌─────────────────┐
                                                              │   React SPA     │
                                                              └─────────────────┘
```

---

### 2.2 Компоненти

| Компонент | Відповідальність | Стек |
|-----------|------------------|------|
| **UDP Ingest Service** | Слухати UDP, парсити пакети, нормалізувати DTO, публікувати в Kafka | Java 17, Spring Boot |
| **Telemetry Processing & API Service** | Kafka consumers, агрегація, persistence, REST + WebSocket | Java 17, Spring Boot |
| **PostgreSQL + TimescaleDB** | Raw telemetry (hypertables), sessions/laps/sectors/summary | PostgreSQL, TimescaleDB |
| **Kafka** | Буфер подій, decoupling, backpressure | Kafka |
| **React SPA** | Live dashboard, перегляд сесій, laps/sectors | React |

---

### 2.3 Maven-модулі (з code skeleton)

| Модуль | Призначення |
|--------|-------------|
| **telemetry-api-contracts** | DTO, Kafka envelope, enum (PacketId, EventCode), REST/WS DTO — без Spring/Kafka/DB |
| **telemetry-parser-f125** | Binary UDP → DTO, Little Endian (опційно окремий модуль або в ingest) |
| **udp-ingest-service** | UDP server, parser, Kafka producer |
| **telemetry-processing-api-service** | Kafka consumers, FSM, aggregation, persistence, REST, WebSocket |
| **infra** | docker-compose (Kafka, PostgreSQL+TimescaleDB), скрипти |

---

### 2.4 Data flow (стисло)

1. **F1 Game** → UDP пакети (Session, LapData, CarTelemetry, CarStatus).
2. **Ingest** → парсинг → `KafkaEnvelope` + payload → Kafka (key = sessionUID).
3. **Processing** → consumer → idempotency check → FSM (session/lap) → raw batch insert + aggregates upsert.
4. **API** → REST (sessions, laps, summary) + WebSocket (live snapshot 10 Hz).
5. **UI** → GET /api/sessions, /laps, /summary + WS /ws/live.

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

### Етап 3. UDP Ingest Service — основа

| # | Крок | Дії | Критерій готовності |
|---|------|-----|---------------------|
| 3.1 | Конфіг UDP | host, port, packet format (2025) у application.yml | Значення читаються |
| 3.2 | Клас `UdpPacketListener` / `UdpServer` | Слухати UDP порт, отримувати DatagramPacket → ByteBuffer | Лог "received N bytes" |
| 3.3 | Визначення packetId з UDP header | Читати packetId з буфера (за F1 25 spec) | Правильний packetId для тестових байтів |
| 3.4 | Інтерфейс `PacketParser<T>` | `boolean supports(int packetId); T parse(ByteBuffer buffer);` | Компіляція |
| 3.5 | Session packet parser | Парсинг Session → SessionEventDto (або внутрішня структура) | Unit-тест на прикладі байтів |
| 3.6 | LapData parser | Парсинг LapData → LapDto | Unit-тест |
| 3.7 | CarTelemetry parser | Парсинг CarTelemetry → CarTelemetryDto | Unit-тест |
| 3.8 | CarStatus parser | Парсинг CarStatus → CarStatusDto | Unit-тест |
| 3.9 | Dispatcher: packetId → parser | switch(packetId) → виклик відповідного parser | Один DTO на виході |
| 3.10 | Маппер Packet → KafkaEnvelope | sessionUID, frameIdentifier, sessionTime, carIndex з header; producedAt = now() | Envelope заповнений |
| 3.11 | Kafka producer config | bootstrap-servers, key-serializer, value-serializer (JSON), linger.ms, batch.size | Producer створюється |
| 3.12 | TopicResolver | packetId / eventCode → topic name | Правильний topic на envelope |
| 3.13 | TelemetryKafkaProducer.send(KafkaEnvelope) | Serialize JSON, key = sessionUID, send | Повідомлення в Kafka (перевірити kafka-console-consumer) |
| 3.14 | Інтеграція: UDP → Parser → Envelope → Kafka | Тільки player car (carIndex з конфігу або з пакета) | Пакети з гри потрапляють у Kafka |

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

### Етап 8. REST API

| # | Крок | Дії | Критерій готовності |
|---|------|-----|---------------------|
| 8.1 | GET /api/sessions | List SessionDto, limit/offset | Відповідь за контрактом |
| 8.2 | GET /api/sessions/{sessionUid} | Один SessionDto | 404 якщо немає |
| 8.3 | GET /api/sessions/{sessionUid}/laps | List laps, carIndex=0 default | Відповідь за контрактом |
| 8.4 | GET /api/sessions/{sessionUid}/sectors | Сектори по колах (можна з laps) | За контрактом |
| 8.5 | GET /api/sessions/{sessionUid}/summary | SessionSummaryDto | Відповідь за контрактом |
| 8.6 | Поле state в відповідях | ACTIVE / FINISHED (TERMINAL) завжди присутнє де потрібно | За REST contract |
| 8.7 | (Опційно) GET /api/sessions/{uid}/telemetry?from=&to=&metric= | Читання з car_telemetry_raw | Для графіків |

---

### Етап 9. WebSocket Live

| # | Крок | Дії | Критерій готовності |
|---|------|-----|---------------------|
| 9.1 | Endpoint /ws/live (або /ws/telemetry/live) | STOMP або plain WebSocket за контрактом | Підключення встановлюється |
| 9.2 | Subscribe message: sessionUID, carIndex | Зберегти підписку клієнта | Клієнт отримує тільки свою сесію |
| 9.3 | Snapshot builder | З SessionRuntimeState → WsSnapshotMessage (speed, rpm, gear, throttle, brake, currentLap, currentSector) | 1 snapshot на виході |
| 9.4 | Scheduler 10 Hz | Кожні 100 ms: для активних підписок взяти state, зібрати snapshot, відправити | UI отримує ~10 msg/s |
| 9.5 | Reconnect: snapshot on connect | При новому підключенні відправити поточний snapshot | Клієнт одразу бачить стан |
| 9.6 | Session ended → WsSessionEndedMessage | При переході в TERMINAL відправити і закрити stream | Клієнт бачить завершення |

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

## Частина 4. Порядок виконання (залежності)

- **Етап 0** — перший (репозиторій і модулі).
- **Етап 1** — потрібен перед етапами 3, 4, 5, 8, 9 (контракти).
- **Етап 2** — можна паралельно з 1; потрібен перед 5, 6, 7.
- **Етапи 3, 4, 5** — 3 перед 5 (Kafka messages); 4 перед 5 і 6 (state).
- **Етапи 5 → 6 → 7** — consumers → aggregation → persistence.
- **Етапи 7 → 8** — repositories перед REST.
- **Етапи 4, 6 → 9** — state + snapshot для WebSocket.
- **Етапи 8, 9 → 11** — API та WS перед UI.
- **Етап 10** — можна впроваджувати поступово після 3 і 5.
- **Етап 12** — після готовності 3–11.

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
