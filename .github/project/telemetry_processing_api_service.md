# Telemetry Processing API Service — документація сервісу

> Окремий документ, що описує модуль **telemetry-processing-api-service**: призначення, архітектуру після рефакторингу, шари та зв’язки з іншими документами проєкту.

**Посилання з інших документів:** де в документації йдеться про цей сервіс, використовуйте це посилання: [telemetry_processing_api_service.md](telemetry_processing_api_service.md).

---

## 1. Призначення

**telemetry-processing-api-service** — Spring Boot застосунок (мікросервіс), який:

- **споживає** події з Kafka (топики: `telemetry.session`, `telemetry.lap`, `telemetry.carTelemetry`, `telemetry.carStatus`, `telemetry.carDamage`);
- **не приймає** UDP-пакети (це робить udp-ingest-service);
- веде **state management** (FSM сесії та кола);
- **агрегує** телеметрію (кола, сектори, session summary, tyre wear);
- **зберігає** raw та агреговані дані в PostgreSQL/TimescaleDB;
- надає **REST API** для історичних даних;
- надає **WebSocket** для live-стріму поточної сесії (10 Hz).

Розмежування з udp-ingest-service та UDP Library описано в [ARCHITECTURE_CLARIFICATION.md](ARCHITECTURE_CLARIFICATION.md).

---

## 2. Архітектура після рефакторингу

Рефакторинг виконано згідно з [implementation_phases.md](../../.cursor/plans/implementation_phases.md). Основні зміни:

| Область | До | Після |
|--------|----|-------|
| REST/WS | Контролери зверталися до репозиторіїв і мапперів | **Тонкі контролери** → виклик лише Query-сервісів |
| Read-операції | Розкидана логіка resolve сесії та маппінгу | **SessionQueryService**, **LapQueryService**, **SessionSummaryQueryService**, **WebSocketSubscriptionService** |
| Resolve сесії | Дубльований код по контролерах | **SessionResolveService** — один метод `getSessionByPublicIdOrUid(String)` |
| Маппінг Entity → DTO | Один або неструктурований маппер | **SessionMapper**, **LapMapper**, **SessionSummaryMapper** |
| Помилки | Різні способи обробки | **SessionNotFoundException** + **RestExceptionHandler** (404, RestErrorResponse), WebSocket — WsErrorMessage |
| Kafka | Логіка в консумерах | **Тонкі консумери** → виклик **SessionEventProcessor**, **LapDataProcessor**, **CarTelemetryProcessor**, **CarStatusProcessor**, **CarDamageProcessor** |
| Збір об’єктів | В агрегаторах/стані | **Білдери**: WsSnapshotMessageBuilder, LapBuilder, SessionSummaryBuilder, TyreWearPerLapBuilder |

---

## 3. Структура пакетів (після рефакторингу)

```
telemetry-processing-api-service
└── com.ua.yushchenko.f1.fastlaps.telemetry.processing
    ├── TelemetryProcessingApplication.java
    ├── config
    │   ├── KafkaConsumerConfig.java
    │   └── WebMvcConfig.java
    ├── consumer
    │   ├── SessionEventConsumer.java
    │   ├── LapDataConsumer.java
    │   ├── CarTelemetryConsumer.java
    │   ├── CarStatusConsumer.java
    │   └── CarDamageConsumer.java
    ├── processor
    │   ├── SessionEventProcessor.java
    │   ├── LapDataProcessor.java
    │   ├── CarTelemetryProcessor.java
    │   ├── CarStatusProcessor.java
    │   └── CarDamageProcessor.java
    ├── idempotency
    │   └── IdempotencyService.java
    ├── lifecycle
    │   ├── SessionLifecycleService.java
    │   ├── SessionPersistenceService.java
    │   └── NoDataTimeoutWorker.java
    ├── state
    │   ├── SessionStateManager.java
    │   ├── SessionRuntimeState.java
    │   ├── SessionState.java
    │   ├── EndReason.java
    │   ├── TyreWearSnapshot.java
    │   └── TyreWearState.java
    ├── aggregation
    │   ├── LapAggregator.java
    │   ├── LapRuntimeState.java
    │   ├── SessionSummaryAggregator.java
    │   └── TyreWearRecorder.java
    ├── persistence
    │   ├── RawTelemetryWriter.java
    │   ├── CarStatusRawWriter.java
    │   ├── entity
    │   └── repository
    ├── builder
    │   ├── WsSnapshotMessageBuilder.java
    │   ├── LapBuilder.java
    │   ├── SessionSummaryBuilder.java
    │   └── TyreWearPerLapBuilder.java
    ├── service
    │   ├── SessionResolveService.java
    │   ├── SessionQueryService.java
    │   ├── LapQueryService.java
    │   ├── SessionSummaryQueryService.java
    │   ├── TrackCornerMapService.java
    │   ├── TrackLayoutService.java
    │   └── WebSocketSubscriptionService.java
    ├── mapper
    │   ├── SessionMapper.java
    │   ├── LapMapper.java
    │   └── SessionSummaryMapper.java
    ├── exception
    │   └── SessionNotFoundException.java
    ├── rest
    │   ├── SessionController.java
    │   ├── LapController.java
    │   ├── SessionSummaryController.java
    │   ├── TrackController.java
    │   └── RestExceptionHandler.java
    └── websocket
        ├── WebSocketConfig.java
        ├── WebSocketController.java
        ├── WebSocketEventListener.java
        ├── WebSocketSessionManager.java
        └── LiveDataBroadcaster.java
```

---

## 4. Шари та відповідальність

### 4.1 REST API (read)

- **Контролери** (`rest/`) — лише прийом параметрів і виклик відповідного QueryService; повернення DTO або 404 через exception handler.
- **SessionQueryService** — список сесій (пагінація), сесія по id, активна сесія. Використовує SessionRepository, SessionStateManager, SessionMapper, SessionResolveService.
- **LapQueryService** — getLaps, getPace, getTyreWear, getLapTrace (resolve сесії → репозиторії → маппери).
- **SessionSummaryQueryService** — summary по session id + carIndex.
- **TrackCornerMapService** — corner map по trackId + trackLengthM (GET corner-maps/latest).
- **TrackLayoutService** — 2D track layout по trackId (GET /api/tracks/{trackId}/layout); Block F B8.
- **TrackController** — GET /{trackId}/layout, GET /{trackId}/corner-maps/latest.
- **RestExceptionHandler** — обробка SessionNotFoundException → 404 + RestErrorResponse.

### 4.2 WebSocket

- **WebSocketController** — виклик WebSocketSubscriptionService.subscribe; при помилці — WsErrorMessage (у т.ч. SessionNotFoundException).
- **WebSocketSubscriptionService** — валідація sessionId, resolve сесії, перевірка "session is active", реєстрація в WebSocketSessionManager.
- **LiveDataBroadcaster** — topic id через SessionMapper.toPublicIdString або SessionQueryService (без прямого SessionRepository.findById лише заради public_id).
- **WsSnapshotMessageBuilder** — збір WsSnapshotMessage з SessionRuntimeState.

### 4.3 Kafka

- **Консумери** — десеріалізація, перевірка null, ensureSession, idempotency, watermark → виклик відповідного **Processor**.
- **SessionEventProcessor** — обробка SSTA, SEND, SESSION_INFO, SESSION_TIMEOUT тощо.
- **LapDataProcessor**, **CarTelemetryProcessor**, **CarStatusProcessor**, **CarDamageProcessor** — оновлення стану, запис у БД, виклики агрегаторів.

### 4.4 Resolve сесії та винятки

- **SessionResolveService** — один метод типу `getSessionByPublicIdOrUid(String)`: нормалізація/trim, перевірка порожнього id, `sessionRepository.findByPublicIdOrSessionUid`; повертає Optional або кидає SessionNotFoundException.
- Усі місця (REST, WebSocket, сервіси) використовують цей метод замість дубльованого коду.

### 4.5 Маппери та білдери

- **SessionMapper** — Session → SessionDto, sessionTypeToDisplayString, toPublicIdString; стан (ACTIVE/FINISHED) через toDto(Session, SessionRuntimeState).
- **LapMapper** — Lap → LapResponseDto, PacePointDto, TracePointDto, TyreWearPointDto.
- **SessionSummaryMapper** — SessionSummary → SessionSummaryDto.
- **Білдери** — збір об’єктів без бізнес-логіки: LapBuilder, SessionSummaryBuilder.empty(...), TyreWearPerLapBuilder.

---

## 5. Контракти та суміжні документи

| Документ | Що регулює для сервісу |
|----------|-------------------------|
| [rest_web_socket_api_contracts_f_1_telemetry.md](rest_web_socket_api_contracts_f_1_telemetry.md) | REST та WebSocket API — формат відповідей, коди помилок |
| [kafka_contracts_f_1_telemetry.md](kafka_contracts_f_1_telemetry.md) | Топики, envelope, payload, idempotency, ordering |
| [state_machines_specification_f_1_telemetry.md](state_machines_specification_f_1_telemetry.md) | FSM сесії, кола, timeout, flashback — lifecycle |
| [telemetry_error_and_lifecycle_contract.md](telemetry_error_and_lifecycle_contract.md) | Lifecycle та помилки між шарами, endReason |
| [code_skeleton_java_packages_interfaces.md](code_skeleton_java_packages_interfaces.md) | Пакети, межі відповідальності (оглядово; актуальна структура — у цьому документі) |
| [implementation_phases.md](../../.cursor/plans/implementation_phases.md) | Покроковий план рефакторингу та чеклист класів |

---

## 6. Збірка та запуск

- З кореня репозиторію: `mvn clean install -DskipTests` (включно з модулем `telemetry-processing-api-service`).
- Запуск сервісу: з каталогу модуля `mvn spring-boot:run` або через Docker (інфраструктура в `infra/`).
- Залежності: Kafka, PostgreSQL/TimescaleDB (згідно [f_1_telemetry_project_architecture.md](f_1_telemetry_project_architecture.md)).

---

## 7. Тестування

Юніт-тести модуля підпорядковані [unit_testing_policy.md](unit_testing_policy.md):

- **Стек:** JUnit Jupiter + Mockito; тестові дані — з централізованого класу **TestData**.
- **Структура:** AAA (Arrange–Act–Assert), `@DisplayName` на тестовий клас і кожен тест-метод.
- **Покриття:** мінімум **85%** line coverage; перевірка при `mvn -pl telemetry-processing-api-service verify`.

План покриття по фазах: [unit_test_coverage_plan.md](../../.cursor/plans/unit_test_coverage_plan.md).

---

## 8. Індекс документації

- [documentation_index.md](documentation_index.md) — індекс усієї документації проєкту.
- [ARCHITECTURE_CLARIFICATION.md](ARCHITECTURE_CLARIFICATION.md) — розмежування UDP Library, udp-ingest-service та telemetry-processing-api-service.
