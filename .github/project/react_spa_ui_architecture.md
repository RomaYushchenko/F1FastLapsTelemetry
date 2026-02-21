# React SPA — Архітектура та UX (F1 FastLaps Telemetry)

> **Мета документа:** опис архітектури React SPA, його місця в системі, структури екранів, потоку даних та зовнішнього вигляду (layout / wireframe) для узгодженої реалізації UI.  
> Джерело API-контрактів: [rest_web_socket_api_contracts_f_1_telemetry.md](rest_web_socket_api_contracts_f_1_telemetry.md).

---

## 1. Місце UI в архітектурі системи

### 1.1 Роль

React SPA є **єдиним клієнтом** backend-сервісу `telemetry-processing-api-service` у MVP (документація сервісу: [telemetry_processing_api_service.md](telemetry_processing_api_service.md)):

- **Read-only:** UI не змінює дані; всі зміни йдуть з гри через UDP → Kafka → Processing.
- **Два канали даних:**
  - **REST API** — історія сесій, кола, сектори, summary (після збереження в БД).
  - **WebSocket** — live-телеметрія поточної сесії (sampling ~10 Hz).

### 1.2 Схема взаємодії

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         telemetry-processing-api-service                │
│  ┌─────────────┐  ┌─────────────┐  ┌──────────────┐  ┌─────────────────┐ │
│  │ REST API    │  │ WebSocket   │  │ Aggregation  │  │ PostgreSQL /    │ │
│  │ /api/*      │  │ /ws/live    │  │ (in-memory)  │  │ TimescaleDB     │ │
│  └──────┬──────┘  └──────┬──────┘  └──────────────┘  └─────────────────┘ │
└─────────┼────────────────┼──────────────────────────────────────────────┘
          │                │
          │ HTTP/JSON      │ WS (JSON: SNAPSHOT, SESSION_ENDED)
          ▼                ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                              React SPA (Browser)                          │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────────────┐ │
│  │ Session List     │  │ Session Detail   │  │ Live Dashboard            │ │
│  │ (REST)           │  │ (REST: laps,     │  │ (WebSocket + optional      │ │
│  │                  │  │  sectors, summary)│  │  GET /api/sessions/active)│ │
│  └──────────────────┘  └──────────────────┘  └──────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────┘
```

### 1.3 Обмеження MVP

- **Один користувач / один браузер:** немає auth, немає мультисесійного стриму на різних клієнтах з різними carIndex.
- **Тільки player car:** `carIndex = 0`; multi-car не в scope MVP.
- **Локальне середовище:** UI орієнтований на localhost / локальну мережу до backend.

---

## 2. Технологічний стек та структура проєкту

### 2.1 Рекомендований стек

| Категорія | Технологія | Примітка |
|-----------|-------------|----------|
| Framework | **React 18+** | Функційні компоненти, hooks |
| Build | **Vite** або Create React App | Швидкий HMR, ES modules |
| Routing | **React Router v6** | `/`, `/sessions`, `/sessions/:sessionUid` |
| HTTP client | **fetch** або **axios** | REST до `/api/*` |
| WebSocket | **native WebSocket** або **SockJS + STOMP** | За контрактом backend (plain WS або STOMP) |
| State (server data) | **React Query (TanStack Query)** або **SWR** | Кеш, refetch, loading/error для REST |
| State (live data) | **React state + useEffect** або **Context** | WebSocket snapshot → локальний state |
| Стилі | **CSS Modules** / **Tailwind** / **styled-components** | На вибір команди |
| Типи (опційно) | **TypeScript** | Рекомендовано для контрактів API |

### 2.2 Базова структура каталогів

```
ui/
├── public/
├── src/
│   ├── api/           # REST client, base URL, типи відповідей
│   ├── ws/             # WebSocket client, subscribe/unsubscribe, message handlers
│   ├── components/     # Переиспользуемые: Layout, Table, Card, Gauge, Badge
│   ├── pages/          # Route-level: LiveDashboard, SessionList, SessionDetail
│   ├── hooks/          # useSessions, useSessionDetail, useLiveTelemetry
│   ├── routes/         # React Router config
│   ├── App.tsx
│   └── main.tsx
├── index.html
├── package.json
└── vite.config.ts
```

---

## 3. Сторінки (екрани) та маршрути

### 3.1 Маршрутизація

| Маршрут | Сторінка | Призначення |
|---------|----------|-------------|
| `/` | **Home / Live Dashboard** | Головна: live-телеметрія, якщо є активна сесія; інакше — заглушка або редірект до списку сесій |
| `/sessions` | **Session List** | Список усіх сесій (історія) |
| `/sessions/:sessionUid` | **Session Detail** | Деталі однієї сесії: таблиця кіл, сектори, summary, опційно графік |

### 3.2 Навігація

- **Header / Nav:** посилання: "Live", "Sessions".
- **Live:** веде на `/` (dashboard).
- **Sessions:** веде на `/sessions`; з списку — клік по рядку/картці → `/sessions/{sessionUid}`.
- **Back:** з деталей сесії — кнопка "Back" до `/sessions`.

---

## 4. Опис екранів та зовнішній вигляд (layout)

### 4.1 Live Dashboard (`/`)

**Призначення:** показувати поточну телеметрію в реальному часі під час активной сесії.

**Потік даних:**

1. При завантаженні: `GET /api/sessions/active` — чи є активна сесія.
2. Якщо так — відкрити WebSocket `ws://…/ws/live`, відправити `{ "type": "SUBSCRIBE", "sessionId": "<id>", "carIndex": 0 }` (sessionId = string з `GET /api/sessions/active`, напр. public id).
3. Отримувати повідомлення `type: "SNAPSHOT"` і оновлювати UI (~10 раз/с).
4. При `type: "SESSION_ENDED"` — показати повідомлення "Session ended", опційно перейти на список сесій або залишити останній snapshot.

**Layout (wireframe):**

```
+------------------------------------------------------------------+
|  F1 FastLaps Telemetry    [ Live ]  [ Sessions ]                  |
+------------------------------------------------------------------+
|                                                                   |
|  LIVE TELEMETRY                                    Session: 123…  |
|  -----------------------------------------------------------------|
|                                                                   |
|   SPEED            RPM              GEAR                           |
|   ___ km/h         _____            _                              |
|   312              10 832           6                              |
|                                                                   |
|   THROTTLE         BRAKE            DRS            ERS              |
|   [=========>   ]  [    ]           ON / OFF       [====] 78% Deploy |
|   91%              0%                                               |
|                                                                   |
|   Current lap: 5 · Sector: 2    Delta to best: +0.213s               |
|                                                                   |
|  -----------------------------------------------------------------|
|  (If no active session: "No active session. Start a session in F1.")|
+------------------------------------------------------------------+
```

**Елементи:**

- **Speed** — велике число (km/h).
- **RPM** — число; опційно progress bar або колірна зона (redline).
- **Gear** — одна цифра.
- **Throttle / Brake** — progress bar або відсотки.
- **DRS** — текст "ON" / "OFF" або індикатор.
- **ERS** — заряд 0–100% (progress bar або %), індикатор "Deploy" коли гравець використовує ERS.
- **Current lap / Sector** — з SNAPSHOT (`currentLap`, `currentSector`).
- **Delta to best** — дельта до кращого часу сесії (±секунди); зелений = швидше best, червоний = повільніше.
- Якщо **немає активной сесії** — повідомлення замість даних; кнопка "View past sessions" → `/sessions`.

Live має **дашбордний** стиль: більші цифри, картки з тінями, компактний session bar. Підтримка світлої/темної теми — окрема задача.

#### 4.1.1 Діаграми та віджети телеметрії (Live)

Перелік live-віджетів, поля з WebSocket SNAPSHOT та критерії готовності описані в **[telemetry_diagrams_plan.md](telemetry_diagrams_plan.md)** (розділ 3) та в **План реалізації діаграм телеметрії EA SPORTS F1 25.pdf**. Під час реалізації Етапу 11 слід дотримуватися цього плану:

- **Speed** (speedKph), **RPM** (engineRpm), **Gear** (gear), **Throttle** (throttle), **Brake** (brake), **DRS** (drs), **ERS** (ersEnergyPercent, ersDeployActive), **Current lap / Sector** (currentLap, currentSector), **Delta to best** (deltaMs).
- Оновлення ~10 Hz з backend; стани екрану: є активна сесія / немає активной сесії.

---

### 4.2 Session List (`/sessions`)

**Призначення:** список збережених сесій для переходу до деталей.

**Потік даних:**

- `GET /api/sessions?limit=50&offset=0` при монтуванні сторінки.
- Відповідь — масив об'єктів з `id` (sessionId для посилань), `sessionDisplayName`, `sessionType`, `trackId`, `startedAt`, `endedAt`, `endReason`, `state`, `finishingPosition` (місце на фініші; null для активних). Редагування назви: `PATCH /api/sessions/{id}` з body `{ "sessionDisplayName": "..." }`.

**Layout (wireframe):**

```
+------------------------------------------------------------------+
|  F1 FastLaps Telemetry    [ Live ]  [ Sessions ]                  |
+------------------------------------------------------------------+
|                                                                   |
|  SESSIONS                                                         |
|  -----------------------------------------------------------------|
|                                                                   |
|  | Session (name)  | Type   | Track   | Started          | Ended    | Place | State  | |
|  |------------------|--------|---------|------------------|----------|-------|--------| |
|  | Monaco Race [Edit]| RACE   | Monaco  | 28.01 20:10      | 20:55    | 3     | FINISHED| |
|  | 1234567891…    | QUALI  | Monaco  | 28.01 19:00      | 19:45    | |
|  | …              |        |         |                  |          | |
|                                                                   |
|  (Click row to open session detail)                                |
+------------------------------------------------------------------+
```

**Елементи:**

- Таблиця: колонка **Session** показує `sessionDisplayName` (за замовчуванням UUID); кнопка Edit відкриває модальне вікно для зміни назви (PATCH). Колонка **Place** — фінішна позиція гравця (число або "—"). Також **Type**, **Track**, **Started**, **Ended**, **State**. Усі посилання та API використовують `session.id` (sessionId).
- Клік по рядку → перехід на `/sessions/{sessionUid}`.
- Стани: **Loading** (skeleton або spinner), **Empty** ("No sessions yet"), **Error** (повідомлення + retry).

---

### 4.3 Session Detail (`/sessions/:sessionUid`)

**Призначення:** повна інформація по одній сесії — кола, сектори, best lap, best sectors. Заголовок сторінки (h1) показує **sessionDisplayName** (або short id як fallback); для запитів laps/summary використовується `session.id` з URL.

**Потік даних:**

- `GET /api/sessions/{sessionUid}` — заголовок сесії (включає `sessionDisplayName`, `finishingPosition` тощо).
- `GET /api/sessions/{sessionUid}/laps?carIndex=0` — кола (включає `positionAtLapStart` для колонки Position і стрілок зміни).
- `GET /api/sessions/{sessionUid}/summary?carIndex=0` — summary (best lap, best sectors, total laps; `leaderCarIndex`, `leaderIsPlayer` для блоку «Leader»).
- `GET /api/sessions/{sessionUid}/pace?carIndex=0` — точки для графіка Lap pace; медіану по валідних колах UI обчислює сам для горизонтальної лінії.
- `GET /api/sessions/{sessionUid}/tyre-wear?carIndex=0` — знос шин по колах (wear 0–1, опційно `compound` для підписів).
- `GET /api/sessions/{sessionUid}/laps/{lapNum}/trace?carIndex=0` — профіль газ/гальмо по обраному колу.
- `GET /api/sessions/{sessionUid}/laps/{lapNum}/ers?carIndex=0` — ERS (енергія %) по тому ж колу.
- Сектори беруться з laps (sector1Ms, sector2Ms, sector3Ms).

**Layout (wireframe):**

```
+------------------------------------------------------------------+
|  F1 FastLaps Telemetry    [ Live ]  [ Sessions ]  [ < Back ]      |
+------------------------------------------------------------------+
|  Session 1234567890…    RACE · Monaco · 28.01.2026 20:10–20:55   |
|  -----------------------------------------------------------------|
|                                                                   |
|  SUMMARY                                                          |
|  Best lap: 1:26.210 (lap 12)   Total laps: 57                     |
|  Best S1: 28.790 (lap 12)  Best S2: 27.912 (lap 5)  Best S3: 29.100 (lap 12) |
|                                                                   |
|  LAPS                                                             |
|  | Lap | Time      | S1      | S2      | S3      | Valid |         |
|  |-----|-----------|---------|---------|---------|-------|         |
|  |  1  | 1:28.321  | 29.100  | 28.500  | 30.721  |  ✓    |         |
|  | ... |           |         |         |         |       |         |
|  | 12  | 1:26.210* | 28.790* | 27.950  | 29.470  |  ✓    |  <- best lap |
|  | ... |           |         |         |         |       |         |
|                                                                   |
|  (Optional) SPEED / RPM chart: time range selector, single metric |
+------------------------------------------------------------------+
```

**Елементи:**

- **Summary block:** Leader (хто на P1; якщо гравець — «You» / highlight); best lap time + lap number; best S1/S2/S3 + lap number; total laps. Best values виділені.
- **Lap pace chart:** графік час кола vs номер кола; опційно горизонтальна лінія медіани (обчислення на клієнті з валідних кіл).
- **Tyre wear chart:** знос FL, FR, RL, RR по колах (0–100%); compound у тултипі/підписах (Pirelli).
- **Pedal trace:** вибір кола + графік throttle/brake по дистанції кола.
- **ERS block:** той самий вибір кола; графік залишку енергії ERS (%) по дистанції кола.
- **Laps table:** Lap, Time, Delta (до best lap), Position (з стрілками ↑/↓ зміни), S1, S2, S3, Valid. Best lap row — highlight; best sector у колонці — виділення.
- Стани: **Loading** (окремо для session / laps / summary / pace / tyre-wear / trace / ERS), **404** (session not found), **Error** (retry).

#### 4.3.1 Діаграми телеметрії (історичні)

Summary block, таблиця кіл, сектори та опційний графік по часу описані в **[telemetry_diagrams_plan.md](telemetry_diagrams_plan.md)** (розділ 4) та в **План реалізації діаграм телеметрії EA SPORTS F1 25.pdf**. Критерії готовності:

- Summary: best lap time + lap number; best S1/S2/S3 + lap number; total laps.
- Таблиця кіл: best lap row виділена; best sector у колонці виділений.
- Історичний графік (якщо endpoint telemetry є) — згідно з планом діаграм.

---

### 4.4 WebSocket протокол (STOMP) — деталі для Етапу 11

Backend використовує **STOMP over SockJS**. Клієнт (React) повинен:

1. **Підключення:** SockJS до `{VITE_WS_URL}/ws/live` (наприклад `http://localhost:8080/ws/live`), потім поверх — STOMP-клієнт.
2. **Відправка SUBSCRIBE:** надіслати повідомлення на **destination** `/app/subscribe` з тілом JSON:
   ```json
   { "type": "SUBSCRIBE", "sessionUID": 1234567890123, "carIndex": 0 }
   ```
3. **Отримання live-даних:** підписатися на **destination** `/topic/live/{sessionUID}` (наприклад `/topic/live/1234567890123`). Сервер надсилає туди повідомлення типу `SNAPSHOT` (10 Hz) та `SESSION_ENDED`.
4. **Помилки:** підписатися на **destination** `/user/queue/errors`. Сервер надсилає туди повідомлення типу `ERROR` (наприклад код `SESSION_NOT_ACTIVE`).
5. **Відправка UNSUBSCRIBE:** destination `/app/unsubscribe`, тіло `{ "type": "UNSUBSCRIBE" }`.

Змінні оточення (приклад для Vite): `VITE_API_BASE_URL` (REST, default `http://localhost:8080`), `VITE_WS_URL` (для SockJS, default той самий origin, тобто `http://localhost:5173` у dev з proxy або `http://localhost:8080` якщо UI обслуговується з backend).

---

## 5. Потік даних (data flow) — зведення

### 5.1 Відповідність API ↔ екрани

| Екран | REST | WebSocket |
|-------|------|-----------|
| **Live Dashboard** | `GET /api/sessions/active` | `WS /ws/live` → SUBSCRIBE → SNAPSHOT, SESSION_ENDED |
| **Session List** | `GET /api/sessions` | — |
| **Session Detail** | `GET /api/sessions/{uid}`, `GET .../laps`, `GET .../summary`, `GET .../pace`, `GET .../tyre-wear`, `GET .../laps/{lapNum}/trace`, `GET .../laps/{lapNum}/ers` | — |

### 5.2 Формат даних з backend (стисло)

- **REST:** JSON; дати в ISO-8601 (UTC); session state: `ACTIVE` | `FINISHED`; endReason: `EVENT_SEND` | `NO_DATA_TIMEOUT` тощо.
- **WebSocket:** JSON messages; `type`: `SNAPSHOT` (speedKph, gear, engineRpm, throttle, brake, currentLap, currentSector, timestamp), `SESSION_ENDED` (sessionUID, endReason), `ERROR` (code, message).

Деталі полів — у [rest_web_socket_api_contracts_f_1_telemetry.md](rest_web_socket_api_contracts_f_1_telemetry.md).

### 5.3 Життєвий цикл Live Dashboard

1. Mount → `GET /api/sessions/active`.
2. **204 / empty** → показати "No active session".
3. **200 + body** → зберегти `sessionUID`, відкрити WS, відправити SUBSCRIBE.
4. Отримувати SNAPSHOT → оновлювати state → re-render (throttling не потрібен, backend вже 10 Hz).
5. Отримати SESSION_ENDED → показати "Session ended", за потреби оновити навігацію.
6. Unmount або перехід на іншу сторінку → UNSUBSCRIBE, close WS.

---

## 6. Компонентна структура (high-level)

### 6.1 Рівні компонентів

| Рівень | Приклад | Опис |
|--------|---------|------|
| **Layout** | `AppLayout`, `Header`, `Nav` | Обгортка, навігація, загальний вигляд |
| **Page** | `LiveDashboardPage`, `SessionListPage`, `SessionDetailPage` | Один екран, оркестрація даних і підключень |
| **Feature** | `LiveTelemetryPanel`, `SessionTable`, `LapsTable`, `SummaryCard` | Логічна частина екрану (REST або WS блок) |
| **UI** | `Gauge`, `ProgressBar`, `DataTable`, `Badge`, `Card` | Переиспользуемые, без знання API |

### 6.2 Рекомендовані розділення

- **REST:** використовувати React Query (або аналог): `useSessions()`, `useSession(sessionUid)`, `useLaps(sessionUid)`, `useSummary(sessionUid)` — кеш, loading, error, refetch.
- **WebSocket:** один хук типу `useLiveTelemetry(sessionUid | null)` — при `sessionUid` відкриває WS, SUBSCRIBE, повертає `{ snapshot, connectionStatus, error }`; при `null` — не підключатися.
- **Маршрути:** React Router; lazy load для сторінок за бажанням.

---

## 7. Обробка помилок та крайні стани

### 7.1 REST

- **404** (session not found) — повідомлення + посилання на список сесій.
- **5xx / мережева помилка** — повідомлення "Something went wrong" + кнопка Retry.
- **Порожній список сесій** — "No sessions yet. Start a session in F1 25."

### 7.2 WebSocket

- **Помилка підключення** — показати "Connection lost" / "Reconnecting…"; автоматичний reconnect з exponential backoff (опційно).
- **Повідомлення `type: "ERROR"`** — показати `message`; при `INVALID_SUBSCRIPTION` — перейти на "No active session".
- **SESSION_ENDED** — не помилка; інформувати про завершення сесії.

### 7.3 Session state у REST

- У відповідях з session завжди є поле **state** (`ACTIVE` | `FINISHED`). UI має його відображати (наприклад, бейдж "Live" для ACTIVE у списку сесій).

---

## 8. Reconnect та узгодження з backend

- Після **reconnect** WebSocket клієнт знову надсилає **SUBSCRIBE** з тим самим `sessionUID` і `carIndex`.
- Backend надсилає **snapshot on connect** — клієнт показує його як поточний стан; окремого "sync" з REST для live не потрібен.
- Для історії (Session List, Session Detail) джерело істини — **REST**; після завершення сесії агрегати фіналізовані, повторний запит дасть актуальні laps/summary.

---

## 9. Нефункціональні вимоги (UI)

- **Sampling:** backend вже обмежує live до ~10 Hz; UI просто відображає кожен отриманий SNAPSHOT без додаткового throttle.
- **Локальний запуск:** base URL API та WS — конфігуровані (env: `VITE_API_BASE_URL`, `VITE_WS_URL` або похожі).
- **Адаптивність (MVP):** мінімум — коректне відображення на десктопі; мобільна верстка — опційно.

---

## 10. Посилання на суміжні документи

| Документ | Що бере UI |
|----------|-------------|
| [rest_web_socket_api_contracts_f_1_telemetry.md](rest_web_socket_api_contracts_f_1_telemetry.md) | Повний контракт REST та WebSocket, формати запитів/відповідей |
| [telemetry_diagrams_plan.md](telemetry_diagrams_plan.md) | План діаграм телеметрії: live-віджети, історичні діаграми, джерела даних, критерії готовності |
| План реалізації діаграм телеметрії EA SPORTS F1 25.pdf | Детальний опис діаграм, макети (первинне джерело для візуалізацій) |
| [mvp-requirements.md](mvp-requirements.md) | Scope фронтенду (розділ 6), критерії готовності MVP |
| [f_1_telemetry_project_architecture.md](f_1_telemetry_project_architecture.md) | Місце UI в загальній архітектурі |
| [implementation_steps_plan.md](implementation_steps_plan.md) | Кроки імплементації UI (Етап 11), включно з підкроками для діаграм |

---

*Цей документ є single source of truth для архітектури та зовнішнього вигляду React SPA у проєкті F1 FastLaps Telemetry.*
