# Pedal trace — аналіз та план реалізації

## 1. Мета документа

Цей документ описує **аналіз функції Pedal trace** (профіль газ/гальмо по колу), **детальний план реалізації** та узгодження з існуючою документацією (.github/project та rest_web_socket_api_contracts).

---

## 2. Поточний стан (gap analysis)

### 2.1 Що вже є

| Компонент | Статус | Примітка |
|-----------|--------|----------|
| **REST контракт** | ✅ | § 3.5: `GET /api/sessions/{sessionUid}/laps/{lapNum}/trace` — масив `{ distance, throttle, brake }` |
| **LapController** | ✅ | Endpoint повертає `200` + порожній масив `[]` (MVP stub) |
| **TracePointDto** | ✅ | `distance`, `throttle`, `brake` (telemetry-api-contracts) |
| **UI** | ✅ | SessionDetailPage: секція "Pedal trace", dropdown по колах, виклик `getLapTrace()` |
| **ThrottleBrakeChart** | ✅ | Recharts: дві лінії (throttle, brake) по X = distance; показує "No pedal trace data" якщо `points.length === 0` |
| **Таблиця car_telemetry_raw** | ✅ | DDL у `infra/init-db/05-car-telemetry-raw.sql` (TimescaleDB hypertable) |
| **CarTelemetry UDP/Kafka** | ✅ | CarTelemetryPacketHandler → telemetry.carTelemetry (throttle, brake) |
| **LapData lap_distance** | ✅ | LapDataPacketHandler парсить `m_lapDistance`, LapDto містить `lapDistance` |

### 2.2 Чого не вистачає

| Компонент | Проблема |
|-----------|----------|
| **Запис у БД** | У `car_telemetry_raw` ніхто не пише: у CarTelemetryConsumer лише TODO «Pass to RawTelemetryWriter». |
| **Прив’язка до кола** | У таблиці немає `lap_number` та `lap_distance` — неможливо вибрати семпли по колу. |
| **Кореляція LapData ↔ CarTelemetry** | Потрібно при записі телеметрії знати поточне коло та дистанцію: брати з runtime state, який оновлює LapDataConsumer. |

---

## 3. Джерела даних (F1 25)

- **CarTelemetry (packetId=6):** throttle, brake (float 0–1), надходить високою частотою; **немає** lap_distance.
- **LapData (packetId=2):** lap_number, **lap_distance** (m), sector, current_lap_time; надходить разом із телеметрією в тому ж фреймі гри.
- Кореляція: один і той самий **frame_identifier** у заголовку UDP відповідає одному моменту часу; LapData і CarTelemetry для того ж frame можна зіставити по (session_uid, frame_identifier, car_index). Тому при обробці CarTelemetry достатньо взяти **останній** lap_number та lap_distance з runtime state (що щойно оновив LapDataConsumer для цього car).

---

## 4. План реалізації

### Етап A. Схема даних та runtime state

| Крок | Опис | Критерій готовності |
|------|------|---------------------|
| A.1 | Додати в таблицю `telemetry.car_telemetry_raw` колонки `lap_number` (SMALLINT), `lap_distance_m` (REAL). | Міграція/init-db; існуючі індекси за потреби доповнити для запитів по (session_uid, car_index, lap_number). |
| A.2 | У `SessionRuntimeState` зберігати per-car останні значення з LapData: `lastLapNumber`, `lastLapDistanceM`. | LapDataConsumer при обробці події оновлює ці поля для відповідного car_index. |
| A.3 | (Опційно) Індекс для trace-запитів: `(session_uid, car_index, lap_number, frame_identifier)`. | Швидкий SELECT для getLapTrace. |

### Етап B. Запис raw telemetry

| Крок | Опис | Критерій готовності |
|------|------|---------------------|
| B.1 | Реалізувати сервіс/репозиторій для запису в `car_telemetry_raw`: session_uid, frame_identifier, car_index, ts (або session_time), throttle, brake, **lap_number**, **lap_distance_m** (з runtime state). | Після обробки CarTelemetry викликається write з поточними lap_number/lap_distance з state. |
| B.2 | У CarTelemetryConsumer: після оновлення snapshot викликати запис у RawTelemetryWriter (batch або по одному — за вибором, з урахуванням навантаження). | Записи з’являються в БД під час активної сесії. |
| B.3 | Запис тільки для ACTIVE сесій; не писати після transition до ENDING/TERMINAL. | Уникнути сміття та зберегти консистентність. |

### Етап C. REST GET trace

| Крок | Опис | Критерій готовності |
|------|------|---------------------|
| C.1 | LapController.getLapTrace: замість повернення порожнього списку робити SELECT з `car_telemetry_raw` WHERE session_uid = ? AND car_index = ? AND lap_number = ? ORDER BY frame_identifier. | Повертати `List<TracePointDto>` з полями distance (lap_distance_m), throttle, brake. |
| C.2 | Перетворення рядка БД → TracePointDto: distance ← lap_distance_m, throttle/brake як у контракті (0–1). | Відповідь відповідає § 3.5 контракту. |
| C.3 | Якщо сесії немає — 404; якщо записів немає — 200 і `[]`. | Існуюча поведінка для «немає даних» збережена. |

### Етап D. Документація

| Крок | Опис | Критерій готовності |
|------|------|---------------------|
| D.1 | Оновити **rest_web_socket_api_contracts_f_1_telemetry.md** § 3.5: зазначити, що trace заповнюється з raw car telemetry (car_telemetry_raw); MVP-стаб замінено на реальні дані. | Контракт відображає фактичну реалізацію. |
| D.2 | Оновити **implementation_steps_plan.md** / **IMPLEMENTATION_GAPS_ANALYSIS.md**: pedal trace — реалізовано (запис raw + GET trace з БД). | Прогрес та прогалини задокументовані. |
| D.3 | **frontend_refinement_plan_f1_telemetry.md** § C.4: зазначити, що бекенд для pedal trace реалізований, джерело — car_telemetry_raw. | Узгодження з оригінальною документацією. |
| D.4 | **documentation_index.md**: за потреби додати посилання на цей документ (Pedal trace analysis & plan). | Навігація по docs актуальна. |

---

## 5. Технічні деталі

### 5.1 Формат відповіді GET trace (повтор контракту)

```json
[
  { "distance": 123.4, "throttle": 0.85, "brake": 0.0 },
  { "distance": 130.2, "throttle": 0.90, "brake": 0.0 }
]
```

- **distance** — метри по колу (lap_distance з гри).
- **throttle**, **brake** — 0.0–1.0.

### 5.2 Частота запису

- CarTelemetry надходить часто; запис кожного семплу може створити велике навантаження. Допустимі варіанти:
  - Запис кожного пакета (простіше, більше об’єму).
  - Throttling (наприклад, кожен N-й frame або не частіше 10–20 Hz) — опційно на наступній ітерації.

### 5.3 Retention

- Існуюча політика retention для `car_telemetry_raw` (наприклад, 14 днів) застосовується й до trace-даних.

---

## 6. Залежності від інших документів

- **rest_web_socket_api_contracts_f_1_telemetry.md** — § 3.5 Pedal trace.
- **implementation_steps_plan.md** — Етап 6 (aggregation), Етап 8 (REST), згадка RawTelemetryWriter.
- **IMPLEMENTATION_GAPS_ANALYSIS.md** — прогалина pace/trace.
- **frontend_refinement_plan_f1_telemetry.md** — § C.1, C.4 (trace endpoint та діаграма).
- **f_1_telemetry_project_architecture.md** — § 9.5.1 car_telemetry_raw (схема).

---

## 7. Чеклист готовності фічі

- [x] car_telemetry_raw містить lap_number, lap_distance_m (міграція 11-pedal-trace.sql).
- [x] LapDataConsumer оновлює runtime state (lapDistanceM у CarSnapshot) per car.
- [x] CarTelemetryConsumer записує семпли в car_telemetry_raw через RawTelemetryWriter з lap_number/lap_distance з snapshot.
- [x] GET /api/sessions/{id}/laps/{lapNum}/trace повертає реальні точки з БД (CarTelemetryRawRepository).
- [x] UI вже показує графік throttle/brake по дистанції (ThrottleBrakeChart); при наявності даних відображаються.
- [x] Документація (REST контракт § 3.5, IMPLEMENTATION_GAPS_ANALYSIS, infra README) оновлена.
