# F1 FastLaps Telemetry (EA SPORTS™ F1®25) 

## 1. Мета проєкту
Локальний застосунок для:
- збору телеметрії F1 25 через UDP,
- доставки даних через Kafka,
- збереження raw та агрегованих даних у PostgreSQL/TimescaleDB,
- відображення **live-метрик** у React SPA,
- аналізу **сесії / гран-прі**: кола, сектори, найшвидші відрізки, історія.

## 2. Технологічний стек
- **Java 17**
- **Spring Boot**
- **PostgreSQL + TimescaleDB**
- **Kafka**
- **Docker / docker-compose**
- **GitHub Actions**
- **React (SPA)**

## 3. Джерело даних та протокол
- UDP Telemetry Output з F1 25.
- Ключові атрибути для кореляції:
    - `sessionUID`
    - `sessionTime`
    - `frameIdentifier` / `overallFrameIdentifier`
    - `playerCarIndex`
- Типи пакетів у MVP:
    - Session
    - Lap Data
    - Car Telemetry
    - Car Status  
      Деталі структур та частот — у специфікації. :contentReference[oaicite:0]{index=0} :contentReference[oaicite:1]{index=1}

## 4. Високорівнева архітектура (локально)

### 4.1 Компоненти
1. **UDP Ingest Service**
    - слухає UDP порт,
    - парсить пакети,
    - нормалізує DTO,
    - публікує події у Kafka.

2. **Telemetry Processing + API Service** ([документація сервісу](telemetry_processing_api_service.md))
    - Kafka consumer(и),
    - персистить raw дані (Timescale),
    - рахує агрегати (laps/sectors/best),
    - надає HTTP API та WebSocket/SSE для UI.

3. **PostgreSQL + TimescaleDB**
    - time-series storage для telemetry snapshots,
    - реляційні таблиці для session/lap/sector/summary,
    - retention для raw telemetry.

4. **Kafka**
    - буфер для high-frequency подій,
    - decoupling ingestion/processing,
    - можливість replay в межах локального запуску.

5. **React SPA**
    - live dashboard,
    - перегляд сесій,
    - аналітика кіл/секторів.

## 5. Первинні вимоги (MVP)

### 5.1 Збір та доставка даних
- Конфігуровані UDP:
    - host/ip, port, формат (2025), send rate.
- Обробка лише даних **player car** (`playerCarIndex`).
- Публікація подій у Kafka з ключем `sessionUID`.
- Ідемпотентність ingestion/processing по ключу:
    - `(sessionUID, frameIdentifier, packetId)`.

### 5.2 Kafka: топіки та події
#### 5.2.1 Topics (MVP)
- `telemetry.session`
- `telemetry.lap`
- `telemetry.carTelemetry`
- `telemetry.carStatus`

#### 5.2.2 Message envelope
- `sessionUID`
- `sessionTime`
- `frameIdentifier`
- `packetId`
- payload (нормалізований)

### 5.3 Збереження в БД
#### 5.3.1 Raw telemetry (Timescale hypertable)
- snapshots з частотою, близькою до sendRate
- поля (MVP):
    - speed, rpm, gear, throttle, brake, drs
    - timestamp = `sessionTime` (або derived time)

#### 5.3.2 Реляційні дані
- **sessions**
    - `session_uid`, `track_id`, `session_type`, start/end markers
- **laps**
    - lap number, lap time, invalid flag, позиція, penalties (мінімально)
- **sectors**
    - S1/S2/S3 time на колі
- **session_summary**
    - best lap, best S1/S2/S3 + lap number для кожного best sector

#### 5.3.3 Retention (MVP)
- raw telemetry: обмежений retention (конфігурований).
- агрегати (laps/sectors/summary): зберігаються довше.

### 5.4 Аналітика (lap/sector)
- Підтримати:
    - таблицю кіл (lap time по колах),
    - визначення **fastest lap**,
    - збереження секторів по колах (S1/S2/S3),
    - визначення **fastest S1/S2/S3** та на якому колі це було,
    - відображення історії в межах сесії та гран-прі.

> Джерело lap/sector часу: Lap Data Packet (`m_sector1Time*`, `m_sector2Time*`, поточний lap time), історія — Session History Packet може використовуватись у наступній ітерації. :contentReference[oaicite:2]{index=2} :contentReference[oaicite:3]{index=3}

### 5.5 API (MVP)
#### 5.5.1 Real-time
- WebSocket або SSE:
    - `/ws/telemetry/live` або `/api/telemetry/stream`
- Throttling для UI (sampling), щоб не рендерити кожен frame.

#### 5.5.2 Історія та перегляд
- `GET /api/sessions`
- `GET /api/sessions/{sessionUid}`
- `GET /api/sessions/{sessionUid}/laps`
- `GET /api/sessions/{sessionUid}/sectors`
- `GET /api/sessions/{sessionUid}/summary`
- (опційно) `GET /api/sessions/{sessionUid}/telemetry?from=&to=&metric=`

## 6. Frontend (React SPA)

> **Детальна архітектура UI, layout (wireframe), потік даних та структура екранів описані в окремому документі:** [react_spa_ui_architecture.md](react_spa_ui_architecture.md).

### 6.1 Live Dashboard (MVP)
- Speed, RPM, Gear
- Throttle, Brake
- DRS status
- Поточне коло та сектор (якщо доступно)

### 6.2 Session View (MVP)
- список сесій
- детальна сторінка сесії:
    - таблиця кіл
    - best lap highlight
    - графік speed або rpm по часу (в межах rolling range або обраного відрізку)

### 6.3 Sector View (MVP)
- таблиця секторів по колах
- fastest S1/S2/S3 (виділення + lap number)
- базова візуалізація (timeline/heatmap — мінімально)

## 7. Нефункціональні вимоги
- Стійкість до UDP packet loss.
- Відсутність блокуючої обробки у ingestion.
- Контроль навантаження:
    - Kafka як backpressure,
    - sampling у UI.
- Локальний запуск одним docker-compose.

## 8. Критерії готовності MVP
- UDP ingestion працює стабільно і публікує події у Kafka.
- Дані сесії зберігаються в БД:
    - raw telemetry (обмежено),
    - laps + sectors + summary.
- UI показує:
    - live dashboard,
    - список сесій,
    - перегляд конкретної сесії з колами/секторами,
    - fastest lap і fastest sectors з привʼязкою до номера кола.
## 9. Scope та очікування (MVP)

### 9.1 Явні межі MVP

**У MVP НЕ входить:**
- Flashback / rewind обробка (`FLBK` events, state rollback)
- Replay історичних сесій або відтворення telemetry timeline
- Multi-car UI (підтримується лише `carIndex = 0`, player car)
- Порівняння декількох сесій між собою
- Аналітика між різними треками або сезонами

### 9.2 Edge-cases, які свідомо ігноруються

- Часткові або пошкоджені UDP пакети
- Out-of-order пакети між різними типами (`Lap` vs `Telemetry`)
- Flashback без подальшого `SEND`
- Невідповідність `lapDistance` через packet loss
- Різка зміна конфігурації UDP під час активної сесії

## 10. Обмеження щодо packet loss

- UDP packet loss вважається **нормальним сценарієм**
- Система **не намагається відновлювати пропущені пакети**
- Агрегація працює на основі *best-effort data*

Допустимі втрати:
- До ~5–10% telemetry пакетів без критичного впливу
- Пропуски lap/sector telemetry можуть впливати на точність, але не ламають сесію

Недопустимі сценарії:
- Повна втрата `SSTA` або `SEND` подій
- Довготривала відсутність `LapData` (> декількох кіл)

## 11. Гарантії точності агрегатів

Система **НЕ гарантує абсолютну точність**, еквівалентну in-game UI.

Гарантується:
- Коректна послідовність кіл (`lapNumber`)
- Найкращі lap / sector значення на основі отриманих даних
- Узгодженість агрегатів *в межах однієї обробленої сесії*
- Ідентичність результатів із офіційною телеметрією гри

Не гарантується:
- Мілісекундна точність при packet loss
- Відновлення стану після rewind / flashback

## 12. Non-goals (нецілі проєкту)

Ці речі **свідомо не є цілями MVP**, щоб уникнути хибних очікувань:

- Побудова повноцінної racing analytics платформи
- eSports-рівень точності даних
- Підтримка live-мультиплеєра з декількома авто
- Складна кореляція між packet-ами різних типів
- Повна симуляція race control логіки (SC, VSC, penalties)

MVP фокусується на:
- Стабільному зборі telemetry
- Базовій агрегації lap / sector / speed
- Зрозумілій та прозорій архітектурі для подальшого розширення

### 13.1 Session lifecycle

- **Підтримується декілька сесій підряд без рестарту сервісів**:
    - Practice → Qualifying → Race → Sprint
    - Кожна сесія ідентифікується унікальним `sessionUID` (UDP header, PacketSessionData).

- **Рестарт гри без `SEND`**:
    - Класифікується як **edge-case**.
    - Сесія завершується через `NO_DATA_TIMEOUT`.

- **Partial sessions**:
    - Сесії, завершені по timeout, **зберігаються**.
    - Маркуються `end_reason = NO_DATA_TIMEOUT`.

### 13.2 Time semantics

- **Source of truth для часу**:
    - `sessionTime` з UDP header (`m_sessionTime`).
    - Server time (`producedAt`) використовується лише для traceability.

### 13.3 Persistence rules

- Сесії **зберігаються незалежно від того**, чи відкривав їх UI.
- Агрегати (laps / sectors / summary) **можуть оновлюватись до моменту фіналізації**.
- Після `FINISHED`:
    - агрегати immutable;
    - дозволені лише read-операції.

## 14. Observability / Monitoring

### 14.1 MVP metrics

- lap_time
- sector_time
- lap_distance
- avg_speed_per_lap
- avg_speed_per_sector
- packet_loss_ratio
- kafka_lag

### 14.2 Health

- Kafka DOWN → service DOWN.

- Packet drops:
    - логуються агреговано.

- Live UI latency:
    - **best-effort**, не SLA-bound у MVP.

---

## 15. Scope / Non-goals

- Проєкт = **dev-tool**.
- Breaking changes між версіями **заборонені**.
- Backward compatibility для збережених сесій — **обовʼязкова**.



