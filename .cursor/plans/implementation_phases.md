# Покроковий план реалізації — фази

Порядок кроків зведений з таблиці проблем та додаткових вимог. Виконувати по черзі, щоб не ламати залежності.

---

## Документація (референс бізнес-логіки)

**Перед і під час реалізації обов’язково орієнтуватися на ці документи**, щоб не порушувати бізнес-логіку та контракти:

| Документ | Призначення |
|----------|-------------|
| [.github/project/documentation_index.md](../../.github/project/documentation_index.md) | Індекс усієї документації, порядок читання |
| [.github/project/mvp-requirements.md](../../.github/project/mvp-requirements.md) | Scope MVP, обмеження, non-goals |
| [.github/project/state_machines_specification_f_1_telemetry.md](../../.github/project/state_machines_specification_f_1_telemetry.md) | FSM сесії, кола, timeout, flashback — **джерело істини для lifecycle** |
| [.github/project/kafka_contracts_f_1_telemetry.md](../../.github/project/kafka_contracts_f_1_telemetry.md) | Контракт Kafka (топики, envelope, payload, idempotency, ordering) |
| [.github/project/telemetry_error_and_lifecycle_contract.md](../../.github/project/telemetry_error_and_lifecycle_contract.md) | Lifecycle та помилки між шарами, endReason, timeout mapping |
| [.github/project/rest_web_socket_api_contracts_f_1_telemetry.md](../../.github/project/rest_web_socket_api_contracts_f_1_telemetry.md) | **REST та WebSocket API** — контракти read-model та live-stream |
| [.github/project/f_1_telemetry_project_architecture.md](../../.github/project/f_1_telemetry_project_architecture.md) | Архітектура, data flow, сервіси |
| [.github/project/code_skeleton_java_packages_interfaces.md](../../.github/project/code_skeleton_java_packages_interfaces.md) | Пакети, інтерфейси, межі відповідальності модуля telemetry-processing-api-service |

**Правило:** зміни в контрактах (REST/WS відповіді, коди помилок, форматування) узгоджувати з `rest_web_socket_api_contracts_f_1_telemetry.md`; зміни в обробці подій Kafka та lifecycle — з `state_machines_specification_f_1_telemetry.md` та `kafka_contracts_f_1_telemetry.md`.

---

## Перевірка збірки після кожної фази

**Перед завершенням кожної фази обов’язково:**

1. **Збірка всього проекту** — з кореня репозиторію виконати:
   - `mvn clean compile` — компіляція всіх модулів, або
   - `mvn clean install -DskipTests` — повна збірка без тестів (якщо потрібні артефакти).
2. Переконатися, що збірка завершується **успішно** (exit code 0).
3. Якщо збірка падає — виправити помилки **до переходу до наступної фази**, щоб фаза не поламала інший код проекту.

Цей крок описано в кінці кожної фази як **«Перевірка збірки»**.

---

## Фаза 1: Підґрунтя (маппери, винятки, session resolve)

### 1.1 Маппери Entity → DTO

- Створити окремі класи для маппінгу:
  - **SessionMapper** — Session → SessionDto, включно з `sessionTypeToDisplayString(Short)` та `toPublicIdString(Session)`.
  - **LapMapper / LapDtoMapper** — Lap → LapResponseDto, PacePointDto, TracePointDto, TyreWearPointDto.
  - **SessionSummaryMapper** — SessionSummary → SessionSummaryDto.
- Маппінг стану сесії (ACTIVE/FINISHED) робити в `SessionMapper.toDto(Session, SessionRuntimeState)` або в сервісі, що викликає маппер.

### 1.2 SessionNotFoundException та обробка помилок

- Додати користувацький **SessionNotFoundException**.
- У **RestExceptionHandler** — обробник, що повертає 404 і єдиний формат **RestErrorResponse**.
- Для WebSocket — обробка того ж винятку з відправкою **WsErrorMessage** (наприклад, у WebSocketController або окремому advice/handler).

### 1.3 Централізований resolve сесії за id

- Ввести один метод типу **getSessionByPublicIdOrUid(String id)**:
  - нормалізація/trim;
  - перевірка на порожній id;
  - виклик `sessionRepository.findByPublicIdOrSessionUid`.
- Повертати `Optional<Session>` або кидати `SessionNotFoundException`.
- Використовувати його у всіх місцях замість дубльованого коду (REST, WebSocket, далі — у сервісах).

### 1.4 Перевірка збірки (в кінці фази)

- Виконати з кореня репозиторію: `mvn clean compile` (або `mvn clean install -DskipTests`).
- Переконатися, що збірка проходить успішно; при помилках — виправити перед Фазою 2.

---

## Фаза 2: Application service шар (query-сервіси)

### 2.1 SessionQueryService

- Сервіс для read-операцій по сесіях:
  - список сесій (пагінація);
  - отримання сесії по id (через централізований resolve);
  - активна сесія.
- Використовує SessionRepository, SessionStateManager, SessionMapper.
- Методи повертають вже DTO або `Optional<SessionDto>` / кидають SessionNotFoundException.

### 2.2 LapQueryService

- Методи: **getLaps**, **getPace**, **getTyreWear**, **getLapTrace** (по session id + carIndex, при потребі lapNum).
- Всередині — resolve сесії (з кроку 1.3), виклики LapRepository, CarTelemetryRawRepository, TyreWearPerLapRepository, маппери для DTO.
- Фільтрація (наприклад, lapTimeMs > 0) — у сервісі або маппері, не в контролері.

### 2.3 SessionSummaryQueryService

- Отримання summary по session id + carIndex.
- Resolve сесії → SessionSummaryRepository → маппер → DTO.
- Включно з порожнім summary, коли сесія є, але laps ще не агреговані.

### 2.4 WebSocketSubscriptionService

- Метод типу **subscribe(wsSessionId, sessionIdStr)**:
  - валідація sessionId;
  - resolve сесії (кидає SessionNotFoundException);
  - перевірка "session is active" через SessionStateManager;
  - реєстрація в WebSocketSessionManager.
- Повертає результат або тип помилки для відправки клієнту.

### 2.5 Перевірка збірки (в кінці фази)

- Виконати з кореня репозиторію: `mvn clean compile` (або `mvn clean install -DskipTests`).
- Переконатися, що збірка проходить успішно; при помилках — виправити перед Фазою 3.

---

## Фаза 3: Тонкі контролери та LiveDataBroadcaster

### 3.1 Рефакторинг REST-контролерів

- **SessionController**, **LapController**, **SessionSummaryController** — лише:
  - прийом параметрів;
  - виклик відповідного QueryService;
  - повернення DTO або 404 (через exception handler).
- Прибрати прямі залежності від репозиторіїв і мапперів.

### 3.2 Рефакторинг WebSocketController

- Виклик **WebSocketSubscriptionService.subscribe**.
- При помилці — відправка **WsErrorMessage** (у т.ч. при SessionNotFoundException).
- Без прямого доступу до SessionRepository та перевірки active в контролері.

### 3.3 LiveDataBroadcaster

- Topic id для broadcast брати з одного місця: **SessionMapper.toPublicIdString(Session)** або метод у SessionQueryService (наприклад, **getTopicIdForSession(sessionUid)**).
- Без прямого виклику SessionRepository.findById тільки ради public_id/session_uid.

### 3.4 Перевірка збірки (в кінці фази)

- Виконати з кореня репозиторію: `mvn clean compile` (або `mvn clean install -DskipTests`).
- Переконатися, що збірка проходить успішно; при помилках — виправити перед Фазою 4.

---

## Фаза 4: Білдери (збір даних без бізнес-логіки)

### 4.1 Білдери замість "голого" збирання об'єктів

- Якщо є код, який **лише збирає або агрегує дані** в один об'єкт і **не містить бізнес-логіки**, винести його в окремі класи-білдери.
- **Не плутати з мапінгом:** маппер — перетворення моделі в іншу модель/DTO; білдер — конструювання одного об'єкта з уже готових частин.

**Приклади:**

| Місце | Поточна логіка | Білдер |
|-------|----------------|--------|
| SessionRuntimeState | збір WsSnapshotMessage з CarSnapshot | **WsSnapshotMessageBuilder** |
| LapAggregator | збір Lap з фінальних значень | **LapBuilder** |
| SessionSummaryAggregator | порожній SessionSummary | **SessionSummaryBuilder.empty(...)** |
| TyreWearRecorder | збір TyreWearPerLap з TyreWearSnapshot | **TyreWearPerLapBuilder** |

- Переглянути також: persistence (RawTelemetryWriter, CarStatusRawWriter), lifecycle, idempotency — де є лише "збирання" полів у сутність/повідомлення.

### 4.2 Перевірка збірки (в кінці фази)

- Виконати з кореня репозиторію: `mvn clean compile` (або `mvn clean install -DskipTests`).
- Переконатися, що збірка проходить успішно; при помилках — виправити перед Фазою 5.

---

## Фаза 5: Kafka — розділення обробки подій і логіки

### 5.1 Винесення логіки з Kafka-хендлерів у окремі файли

Логіку обробки подій і даних винести з консумерів у окремі класи/файли.

**Роутинг/обробка подій:**

- Наприклад **SessionEventProcessor** — обробка EventCode: SSTA, SEND, SESSION_INFO, SESSION_TIMEOUT тощо.
- Викликається з **SessionEventConsumer** після idempotency.

**Обробка даних по топиках:**

- **LapDataProcessor**, **CarTelemetryProcessor**, **CarStatusProcessor**, **CarDamageProcessor** — приймають уже "очищений" контекст (sessionUid, carIndex, watermark, payload) і виконують:
  - оновлення стану;
  - запис у БД;
  - виклики агрегаторів.

**Консумери залишаються тонкими:**

- Десеріалізація, перевірка null, ensureSession, shouldProcess, idempotency, watermark — потім виклик відповідного Processor.
- Вся бізнес-логіка (sector completion, lap finalization, merge snapshot, lap trace state тощо) — у процесорах або вже існуючих сервісах/агрегаторах.

### 5.2 Перевірка збірки (в кінці фази)

- Виконати з кореня репозиторію: `mvn clean compile` (або `mvn clean install -DskipTests`).
- Переконатися, що збірка проходить успішно; при помилках — виправити перед завершенням реалізації плану.

---

## Залежності між фазами

```
Фаза 1 (маппери, винятки, resolve)
    ↓
Фаза 2 (Query-сервіси використовують маппери та resolve)
    ↓
Фаза 3 (контролери використовують тільки сервіси)
    ↓
Фаза 4 (білдери — незалежно, можна паралельно з 2–3)
Фаза 5 (Kafka — незалежно від REST/WS, після стабільного API)
```

---

## Покриття модуля telemetry-processing-api-service (чеклист)

Усі класи модуля перевірені; жоден не повинен бути пропущений при реалізації. Якщо клас не згаданий у фазах явно — він або використовується як залежність, або залишається без змін.

### REST

| Клас | Фаза / дія |
|------|------------|
| **SessionController** | Фаза 3 — рефакторинг: лише виклик SessionQueryService, без репозиторіїв/мапперів |
| **LapController** | Фаза 3 — рефакторинг: лише виклик LapQueryService |
| **SessionSummaryController** | Фаза 3 — рефакторинг: лише виклик SessionSummaryQueryService |
| **RestExceptionHandler** | Фаза 1 — додати обробник SessionNotFoundException → 404 + RestErrorResponse |

### WebSocket

| Клас | Фаза / дія |
|------|------------|
| **WebSocketController** | Фаза 3 — виклик WebSocketSubscriptionService.subscribe; обробка SessionNotFoundException → WsErrorMessage |
| **WebSocketEventListener** | Без змін — тільки connect/disconnect та unsubscribe; помилки підписки в контролері/сервісі |
| **WebSocketConfig** | Без змін — конфігурація STOMP |
| **WebSocketSessionManager** | Без змін — використовується WebSocketSubscriptionService (Фаза 2) |
| **LiveDataBroadcaster** | Фаза 3 — topic id брати з SessionMapper.toPublicIdString або SessionQueryService.getTopicIdForSession, без SessionRepository.findById |

### Консумери Kafka (Фаза 5)

| Клас | Фаза / дія |
|------|------------|
| **SessionEventConsumer** | Фаза 5 — залишити тонким: десеріалізація, ensureSession, idempotency → виклик **SessionEventProcessor** |
| **LapDataConsumer** | Фаза 5 — тонкий → виклик **LapDataProcessor** |
| **CarTelemetryConsumer** | Фаза 5 — тонкий → виклик **CarTelemetryProcessor** |
| **CarStatusConsumer** | Фаза 5 — тонкий → виклик **CarStatusProcessor** |
| **CarDamageConsumer** | Фаза 5 — тонкий → виклик **CarDamageProcessor** |

### Сервіси стану та lifecycle (використовуються, не рефакторяться планом)

| Клас | Примітка |
|------|----------|
| **SessionStateManager** | Використовується в SessionQueryService, WebSocketSubscriptionService, LiveDataBroadcaster; логіку не змінювати |
| **SessionRuntimeState** | Фаза 4 — збір WsSnapshotMessage винести в WsSnapshotMessageBuilder; інше без змін |
| **SessionLifecycleService** | Викликається з SessionEventProcessor (Фаза 5); не змінювати бізнес-логіку |
| **SessionPersistenceService** | Викликається з lifecycle/процесорів; без змін |
| **NoDataTimeoutWorker** | Lifecycle; без змін |
| **IdempotencyService** | Використовується в консумерах до виклику процесорів; без змін |

### Агрегація (Фаза 4 — білдери)

| Клас | Фаза / дія |
|------|------------|
| **LapAggregator** | Фаза 4 — збір Lap з фінальних значень винести в **LapBuilder** |
| **LapRuntimeState** | Внутрішній стан LapAggregator; без змін структури |
| **SessionSummaryAggregator** | Фаза 4 — порожній SessionSummary винести в **SessionSummaryBuilder.empty(...)** |
| **TyreWearRecorder** | Фаза 4 — збір TyreWearPerLap з TyreWearSnapshot винести в **TyreWearPerLapBuilder** |

### Persistence

| Клас | Фаза / дія |
|------|------------|
| **RawTelemetryWriter** | Фаза 4 — переглянути: якщо лише збір полів у сутність — винести в білдер |
| **CarStatusRawWriter** | Фаза 4 — переглянути: якщо лише збір полів — білдер |

### Репозиторії та сутності (Entity / Id)

| Категорія | Класи | Примітка |
|-----------|--------|----------|
| Репозиторії | SessionRepository, LapRepository, SessionSummaryRepository, CarTelemetryRawRepository, TyreWearPerLapRepository, CarStatusRawRepository, ProcessedPacketRepository | Використовуються в Query-сервісах (Фаза 2) та процесорах (Фаза 5); централізований resolve сесії (Фаза 1) використовує SessionRepository |
| Entity | Session, Lap, SessionSummary, CarTelemetryRaw, CarStatusRaw, TyreWearPerLap, ProcessedPacket | Без змін; маппери (Фаза 1) переводять у DTO |
| Id / PK | Session (Long), LapId, SessionSummaryId, CarTelemetryRawId, CarStatusRawId, TyreWearPerLapId, ProcessedPacketId | Без змін |

### State / enum (доменні)

| Клас | Примітка |
|------|----------|
| **SessionState** | Enum; без змін |
| **EndReason** | Enum; без змін |
| **TyreWearSnapshot**, **TyreWearState** | Використовуються в TyreWearRecorder / білдері; без змін логіки |

### Конфігурація та Application

| Клас | Примітка |
|------|----------|
| **WebMvcConfig** | Без змін |
| **KafkaConsumerConfig** | Без змін |
| **TelemetryProcessingApplication** | Без змін |

### Нові класи (створити за планом)

| Клас | Фаза |
|------|------|
| **SessionNotFoundException** | 1 |
| **SessionMapper** | 1 |
| **LapMapper** / **LapDtoMapper** | 1 |
| **SessionSummaryMapper** | 1 |
| Метод **getSessionByPublicIdOrUid(String)** (в сервісі або окремому helper) | 1 |
| **SessionQueryService** | 2 |
| **LapQueryService** | 2 |
| **SessionSummaryQueryService** | 2 |
| **WebSocketSubscriptionService** | 2 |
| **WsSnapshotMessageBuilder** | 4 |
| **LapBuilder** | 4 |
| **SessionSummaryBuilder** | 4 |
| **TyreWearPerLapBuilder** | 4 |
| **SessionEventProcessor** | 5 |
| **LapDataProcessor** | 5 |
| **CarTelemetryProcessor** | 5 |
| **CarStatusProcessor** | 5 |
| **CarDamageProcessor** | 5 |

Якщо з’явиться новий клас у модулі — додати його в цей чеклист і визначити фазу або позначити «без змін».
