# План реалізації: Сторінка Session Summary (деталі сесії)

**Тема:** Фронтенд і бекенд — покращення сторінки деталей сесії: Summary (хто на 1-му місці), Lap pace (overflow, медіана), Tyre wear (відсотки, compound), таблиця кіл (дельта, місце, стрілки), ERS (інформація та графік).  
**Джерело ідей:** [improvements-notes-structured.md](../improvements-notes-structured.md) § 2.3.  
**Статус:** реалізовано (етапи 1–6 виконані на гілці `feature/session-summary-page-plan`).

**Статус реалізації (чеклист):**
- [x] **Етап 1** — Summary: `findBySessionUidOrderByFinishingPositionAsc`, SessionSummaryDto `leaderCarIndex`/`leaderIsPlayer`, `enrichWithLeader`, UI Leader (You / Car #N, highlight).
- [x] **Етап 2** — Lap pace: margin у PaceChart, медіана з валідних кіл на фронті, горизонтальна лінія + тултип.
- [x] **Етап 3** — Tyre wear: одиниці 0–1, UI `toPercent()`; compound у LastTyreCompoundState, CarStatusProcessor, міграція 16, TyreWearPerLap/LapMapper/TyreWearPointDto, UI compound у тултипі (Pirelli labels).
- [x] **Етап 4** — Таблиця кіл: LapRuntimeState/LapAggregator `positionAtLapStart`, міграція 17, Lap/LapBuilder/LapResponseDto/LapMapper; UI Delta, Position, стрілки ↑/↓.
- [x] **Етап 5** — ERS: ErsPointDto, `getLapErs` (merge car_telemetry_raw + car_status_raw по ts), GET `/laps/{lapNum}/ers`; UI ErsChart, блок ERS, спільний вибір кола з pedal trace.
- [x] **Етап 6** — Документація: rest_web_socket_api_contracts (leader, positionAtLapStart, pace median, tyre compound, ERS); react_spa_ui_architecture (Session Detail blocks і endpoints).

---

## 1. Поточний стан (коротко)

| Блок | Що є зараз |
|------|-------------|
| **Summary** | Best lap time + lap number, total laps, best sector 1/2/3 + lap numbers. Немає інформації «хто на 1-му місці». |
| **Lap pace** | `PaceChart` (Recharts): ось X = lap number, Y = lap time (секунди). Час кола може виходити за межі графіку / підписи обрізаються. Немає лінії середнього/медіанного часу. |
| **Tyre wear** | `TyreWearChart`: FL, FR, RL, RR у % (front * 100). Скарги: відсотки відображаються некоректно (сумуються або інша логіка). Немає типу компаунду (compound). |
| **Pedal trace** | Вибір кола + ThrottleBrakeChart. Поки залишаємо без змін (кандидат на майбутні покращення). |
| **Таблиця кіл** | Колонки: Lap, Time, S1, S2, S3, Valid. Немає дельти, місця на колі, стрілок зміни позиції. |
| **ERS** | Немає блоку ERS на сторінці Session Summary. |

**Дані:** GET /api/sessions/{id}, /laps, /summary, /pace, /tyre-wear, /laps/{lapNum}/trace. `Lap` (LapResponseDto): lapNumber, lapTimeMs, sector1/2/3Ms, isInvalid — без carPosition. SessionSummaryDto: totalLaps, bestLap*, bestSector* — без лідера. PacePoint: lapNumber, lapTimeMs; TyreWearPoint: lapNumber, wearFL/FR/RL/RR.

---

## 2. Цілі (що хочемо отримати)

1. **Summary:** хто на 1-му місці; якщо гравець — виділити; якщо ні — ім’я гонщика та команда. Джерело: позиції з телеметрії/lap data; оновлення в реальному часі якщо є live.
2. **Lap pace:** виправити вихід часу за межі графіку; додати лінію середнього/медіанного часу кола.
3. **Tyre wear:** коректні відсотки; візуально позначити тип компаунду (Pirelli: soft/medium/hard/inter/wet).
4. **Таблиця кіл:** колонки дельта (до best lap або попереднього кола), місце на колі; біля місця — стрілка вгору/вниз (зміна позиції).
5. **ERS:** інформація про ERS; графік використання (залишок енергії, режим deploy по ходу кола/сесії).

---

## 3. План реалізації (етапи)

### Етап 1: Summary — хто на першому місці

**Мета:** у блоці Summary показувати, хто зараз на 1-му місці; якщо гравець — виділити; якщо ні — ім’я гонщика та команда.

**Кроки:**

1. **Джерело даних**
   - Позиції приходять з телеметрії (LapData carPosition) або з пакета Final Classification. Для історичної сесії (FINISHED) — лідер = той, хто на 1-му місці в результатах (session_finishing_positions або Final Classification). Для імені гонщика/команди: якщо є в телеметрії — зберігати/віддавати; інакше «P1» або car index.
   - **Рішення:** оновлювати «хто на 1-му місці» разом із оновленням графіків та інших даних на сторінці (наприклад, при рефреші в кінці кола). Окрема підписка на live для цієї сторінки не потрібна — достатньо того ж циклу оновлення, що й для pace/tyre wear/таблиці.

2. **Бекенд**
   - Розширити SessionSummaryDto (або окремий endpoint) полями типу: leaderPosition (1), leaderIsPlayer (boolean), leaderDriverName (optional), leaderTeamName (optional). Заповнення з агрегату або з session_finishing_positions + довідника гравців (якщо є).
   - **Доповнення:** У `SessionFinishingPositionRepository` зараз є лише `findBySessionUidAndCarIndex`. Потрібен метод для отримання лідера: наприклад `List<SessionFinishingPosition> findBySessionUidOrderByFinishingPositionAsc(Long sessionUid)` — перший елемент (position = 1) дасть `car_index` лідера. Порівняти з `playerCarIndex` з сесії → `leaderIsPlayer`. Ім'я гонщика/команда: перевірити, чи F1 25 передає participants/Final Classification у телеметрії; якщо ні — fallback: «P1» або «Car #N» (car index лідера).

3. **Фронт**
   - У блоці Summary додати рядок «Leader: …»; якщо leaderIsPlayer — виділити (наприклад, badge «You»); інакше вивести leaderDriverName + leaderTeamName або «Car #N».

4. **Чеклист**
   - Джерело даних визначено; API повертає лідера; на UI відображається коректно.

**Залежності:** наявність позицій/результатів на бекенді (наприклад, після реалізації session_finishing_positions та мульти-кар або довідника учасників).

**Рішення:** див. розділ 5 (питання 1).

---

### Етап 2: Lap pace — overflow і медіана

**Мета:** час кола не виходить за межі графіку; додати лінію середнього/медіанного часу кола.

**Кроки:**

1. **Вихід за межі графіку**
   - У PaceChart (Recharts) переконатися, що YAxis має адекватний domain (вже є yDomain з padding). Якщо підписи осі обрізаються — **збільшити `margin` у `LineChart`** (наприклад `margin={{ left: 50, right: 20 }}`), щоб підписи осі Y не обрізались; опційно tickFormatter з коротшим форматом (наприклад `m:ss`); тултип з повним часом вже є (PaceTooltip). Domain: [min - padding, max + padding]; перевірити, що екстремальні часи кола не виходять за межі.

2. **Медіанний час кола**
   - Обчислення на фронті: медіана по `lapTimeMs` **лише валідних** кіл. Валідні кола: з масиву `laps` (SessionDetailPage вже має laps) — фільтр `laps.filter(l => !l.isInvalid && l.lapTimeMs != null)`. Медіана — одне число на всю сесію; на графіку друга лінія — горизонтальна (одне й те саме значення для кожного lapNumber). Recharts: другий `<Line dataKey="medianTimeSeconds" name="Median" />`, де для кожного елемента `data` поле `medianTimeSeconds = medianMs / 1000`.
   - Альтернатива: бекенд повертає медіанний час у pace API (наприклад у кожному PacePointDto поле `medianTimeMs` для консистентності); фронт тільки малює.

3. **Чеклист**
   - Час кола повністю в межах графіку; **медіанна** лінія відображається; тултипи з повним значенням при потребі.

**Залежності:** тільки фронт (pacePoints вже є) для обчислення медіани; бекенд опційно.

**Рішення:** використовувати **медіану** (питання 2).

---

### Етап 3: Tyre wear — відсотки і compound

**Мета:** коректне відображення відсотків зносу; візуально позначити тип компаунду (Pirelli).

**Кроки:**

1. **Відсотки зносу — конкретний крок (обов’язково виконати перед фіксом відображення)**
   - **Уточнити:** F1 25 Telemetry Output Structures: `m_tyresWear[4]` — "Tyre wear (percentage)". У коді: CarDamageDto → TyreWearSnapshot → TyreWearPerLap (Float); REST contract (§ 3.6) — "float у діапазоні 0..1 (відповідає 0–100%)"; UI (TyreWearChart) робить `p.wearFL * 100`. Тобто очікується, що бекенд віддає 0–1. Потрібно перевірити: парсер зчитує `buffer.getFloat()` — у грі це 0–1 чи вже 0–100? Якщо гра віддає 0–100, при збереженні в TyreWearPerLap потрібно ділити на 100 або не множити на 100 на фронті. Після узгодження — виправити TyreWearRecorder/TyreWearPerLapBuilder або фронт (TyreWearChart). Накопичувальний знос vs знос на кінець кола: зараз зберігається **останній** snapshot на момент finalizeLap — це знос на кінець кола.
   - Рішення: цей пункт зафіксовано як виконуваний крок етапу (питання 3).

2. **Тип компаунду (compound)**
   - **Джерело (підтверджено):** офіційна телеметрія F1 25 — пакет **CarStatus** містить `m_actualTyreCompound` та `m_visualTyreCompound` (byte). У проєкті вже є: CarStatusPacketParser парсить actualTyreCompound/visualTyreCompound, CarStatusDto (tyresCompound, visualTyreCompound), CarStatusRaw.tyres_compound, запис у БД. Треба додати compound до агрегату tyre wear (при фіналізації кола з CarStatus) або зчитувати з car_status_raw по колу і віддавати в GET /tyre-wear (поле у TyreWearPointDto). На фронті: легенда або підсвітка кольорами Pirelli (soft/medium/hard/inter/wet).
   - Рішення: compound є в телеметрії та в пайплайні; залишається інтегрувати його в API tyre-wear та UI (питання 4).

3. **Чеклист**
   - Виконано крок «Уточнити з бекендом» щодо одиниць і формули зносу; відсотки відображаються вірно; compound додано в API та позначено на графіку (Pirelli кольори).

**Залежності:** бекенд для одиниць зносу та compound; фронт для візуалу.

**Рішення:** див. розділ 5 (питання 3–4).

---

### Етап 4: Таблиця кіл — дельта, місце, стрілки

**Мета:** нові колонки: дельта часу (до best lap або попереднього кола), місце на колі; біля місця — зміна позиції (стрілка вгору/вниз).

**Кроки:**

1. **Дельта часу**
   - **Дельта відносно best lap поточного гонщика.** На фронті: для кожного кола delta = lapTimeMs - bestLapTimeMs; bestLapTimeMs визначається з effectiveSummary (поточний best по сесії). При зміні best lap (наприклад, після рефрешу) дельту перераховувати заново. Формат: +0.250 / −0.100 (секунди). Колонка «Delta».
   - Рішення: див. розділ 5 (питання 5).

2. **Місце на колі**
   - Показувати **місце на старті кола.** LapResponseDto та entity Lap зараз не містять position. Потрібно:
     - **LapRuntimeState:** додати поле `positionAtLapStart` (Integer). При зміні номера кола в LapAggregator (вхідний пакет — перший пакет нового кола) зберігати в state: `state.setPositionAtLapStart(lapDto.getCarPosition())` — це позиція на **старті** поточного (нового) кола; при finalizeLap для **попереднього** кола використовувати значення, яке було збережене на початку того кола (тобто при `reset(newLapNumber)` зберігати поточний positionAtLapStart у тимчасову змінну і передавати в LapBuilder для кола, що фіналізується; або зберігати в state position на старті поточного кола і при finalize брати з state перед reset).
     - **Lap entity:** додати колонку `position_at_lap_start` (INTEGER NULL). Міграція: новий скрипт у `infra/init-db/` (наприклад `16-laps-position-at-lap-start.sql`).
     - **LapBuilder.build(...):** додати параметр `Integer positionAtLapStart`; LapMapper.toLapResponseDto — мапити в LapResponseDto поле `positionAtLapStart`.
     - **LapResponseDto:** додати поле `Integer positionAtLapStart` (optional).
   - Стрілки зміни: порівнювати positionAtLapStart кола N з positionAtLapStart кола N−1.
   - Рішення: див. розділ 5 (питання 6).

3. **Стрілки зміни позиції**
   - По місцю на колі N і N-1: якщо position зросла — стрілка вниз (гірше); зменшилась — стрілка вгору (краще). Відображати поруч із числом місця.

4. **Бекенд**
   - Якщо додаємо carPosition (і опційно positionChange) у laps API — оновити LapResponseDto, LapMapper, агрегацію/персистенцію з LapData.

5. **Фронт**
   - Колонки: Delta, Position (місце + стрілка). Дельта обчислюється на фронті з laps + bestLapTimeMs; Position з lap.positionAtLapStart (після додавання поля в LapResponseDto).

6. **Чеклист**
   - Колонки відображаються; дельта та місце коректні; стрілки відображають зміну.

**Залежності:** для місця — зміни в контракті laps та бекенді; дельта може бути тільки на фронті.

**Рішення:** див. розділ 5 (питання 5–6).

---

### Етап 5: ERS — інформація та графік

**Мета:** блок з інформацією про ERS; графік використання (залишок енергії, режим deploy по ходу кола/сесії).

**Кроки:**

1. **Джерело даних**
   - **Графік ERS входить у цю тему (Session Summary).** На Live сторінці ERS — лише віджет (поточне значення), без графіку. Джерело для графіку на Session Summary: **емність (енергія, ersStoreEnergy)** та **дистанція кола** (lap_distance). CarStatus зберігається в car_status_raw; для обраного кола вибірка по lap_distance (корелювати з car_telemetry_raw або тим самим джерелом, де distance). Endpoint: GET /api/sessions/{id}/laps/{lapNum}/ers або аналог. Контракт: відстань по колу + energy % (0–100), опційно deploy.
   - Рішення: див. розділ 5 (питання 7).

2. **Бекенд**
   - У `car_status_raw` немає `lap_distance` — є лише `session_uid`, `car_index`, `ts`, `ers_store_energy`. Тому збірка ERS по колу: (1) отримати Lap за (sessionUid, carIndex, lapNum) — поля `started_at`, `ended_at` (або діапазон по `created_at`/`updated_at` як fallback); (2) вибрати з `car_telemetry_raw` записи за цей lap (lap_number, session_uid, car_index) — отримати пари (ts, lap_distance_m); (3) вибрати з `car_status_raw` записи в діапазоні ts по сесії/машині; (4) **об'єднати по timestamp**: для кожного семплу telemetry (lap_distance_m, ts) знайти найближчий за часом запис car_status_raw і взяти ersStoreEnergy; (5) energy % = ersStoreEnergy / ERS_MAX_ENERGY_J * 100 (ERS_MAX як у CarStatusProcessor). Endpoint GET .../laps/{lapNum}/ers. Контракт: ErsPointDto (lapDistanceM або lapDistance, energyPercent, deployActive опційно).

3. **Фронт**
   - Блок «ERS»: короткий опис (що таке ERS); графік (ось X: дистанція кола, Y: залишок енергії %). Вибір кола — аналогічно Pedal trace.

4. **Чеклист**
   - Дані ERS доступні по колу; графік відображається; інформаційний текст присутній.

**Залежності:** наявність ERS у car_status_raw та логіка вибірки по lap_distance; на Live (02-live-page) — лише віджет, без графіку.

**Рішення:** див. розділ 5 (питання 7).

---

### Етап 6: Оновлення документації

**Мета:** після впровадження змін оновити REST/UI документацію (Session Summary, нові поля, нові endpoints).

**Кроки:**

1. Оновити [rest_web_socket_api_contracts_f_1_telemetry.md](../../project/rest_web_socket_api_contracts_f_1_telemetry.md): SessionSummaryDto (leader*), LapResponseDto (position, delta?), pace (median/mean?), tyre-wear (compound?), ERS endpoint.
2. Оновити [react_spa_ui_architecture.md](../../project/react_spa_ui_architecture.md) / [telemetry_diagrams_plan.md](../../project/telemetry_diagrams_plan.md): опис блоків Session Detail (Summary з лідером, Lap pace з медіаною, Tyre wear з compound, таблиця з дельтою/місцем, ERS блок).

**Залежності:** етапи 1–5.

---

## 4. Питання та рішення (що потрібно уточнити)

---

**Питання 1. Лідер (1-ше місце) — джерело даних** ✅

- Для FINISHED сесії: достатньо взяти «хто на 1-му місці» з session_finishing_positions (мінімальна position = 1) і порівняти з player car? Ім’я гонщика/команда — чи є в телеметрії F1 25 (participants, Final Classification)? Якщо немає — показувати «P1» або «Car #1».
- **Рішення:** оновлювати «хто на 1-му місці» разом із оновленням графіків та інших даних (рефреш у кінці кола). Окрема live-підписка не потрібна.

---

**Питання 2. Lap pace — медіана чи середнє** ✅

- **Рішення:** використовувати **медіану** (стійкіша до викидів).

---

**Питання 3. Tyre wear — одиниці та формула** ✅

- **Рішення:** додано в план як **конкретний виконуваний крок** в Етапі 3: «Уточнити з бекендом: в яких одиницях приходять wearFL/FR/RL/RR (0–1 чи 0–100)? Чи є подвійне підсумовування по колах (накопичувальний знос vs знос на кінець кожного кола)? Після узгодження виправити агрегат або фронт.» Виконати цей крок перед фіксом відображення відсотків.

---

**Питання 4. Compound — наявність у даних** ✅

- **Перевірено:** офіційна структура F1 25 у проєкті (`.github/docs/F1 25 Telemetry Output Structures.txt`): **CarStatusData** містить `m_actualTyreCompound` (uint8, 16=C5…21=C0, 22=C6, 7=inter, 8=wet) та `m_visualTyreCompound`. У коді: CarStatusPacketParser, CarStatusDto (tyresCompound, visualTyreCompound), CarStatusRaw.tyres_compound, запис у БД. **Рішення:** compound є в телеметрії та в пайплайні; залишається додати в API tyre-wear та на фронті кольори Pirelli.

---

**Питання 5. Дельта в таблиці — до чого** ✅

- **Рішення:** дельта відносно **best lap поточного гонщика**; постійно перераховувати дельту при зміні best lap (після рефрешу даних).

---

**Питання 6. Місце на колі — на старті чи на фініші** ✅

- **Рішення:** показувати місце на **старті** кола. Стрілки зміни позиції: порівняння positionAtLapStart поточного кола з попереднім.

---

**Питання 7. ERS на Session Summary — пріоритет** ✅

- **Рішення:** графік ERS входить у цю тему (Session Summary). На Live сторінці — лише відображення у вигляді віджета (емність/режим), без графіку. Джерело даних для графіку: **емність (енергія)** та **дистанція кола** (lap distance).

---

## 5. Прийняті рішення

| # | Питання | Рішення |
|---|---------|---------|
| 1 | Лідер — джерело даних | Оновлювати «хто на 1-му місці» разом із оновленням графіків та даних на сторінці (рефреш у кінці кола). Окрема live-підписка не потрібна. |
| 2 | Lap pace — медіана чи середнє | Використовувати **медіану**. |
| 3 | Tyre wear — одиниці та формула | Додано в Етап 3 як конкретний крок: уточнити з бекендом одиниці (0–1 vs 0–100) та чи є подвійне підсумовування; після узгодження виправити агрегат або фронт. |
| 4 | Compound — наявність у даних | Підтверджено: F1 25 CarStatus має actualTyreCompound/visualTyreCompound; у проєкті вже парситься та зберігається. Залишається додати в API tyre-wear та UI (Pirelli кольори). |
| 5 | Дельта в таблиці — до чого | Дельта відносно best lap поточного гонщика; при зміні best lap перераховувати (після рефрешу). |
| 6 | Місце на колі — старт чи фініш | Місце на **старті** кола; стрілки — порівняння з попереднім колом. |
| 7 | ERS на Session Summary — пріоритет | Графік ERS в цій темі (Session Summary). На Live — лише віджет. Джерело: емність (енергія) + дистанція кола. |

---

## 6. Чеклист перед початком коду

- [x] Усі питання мають заповнені рішення.
- [ ] Визначено порядок етапів (див. рекомендований порядок нижче).
- [ ] Перевірено наявність полів (positionAtLapStart по колах, compound у CarStatus/tyre-wear, ERS по lap_distance) на бекенді та в телеметрії.

**Рекомендований порядок етапів:** 2 → 3 → 4 → 1 → 5 → 6.

- **Етап 2 (Lap pace)** — лише фронт (overflow + медіана з laps), без змін контрактів; швидкий результат.
- **Етап 3 (Tyre wear)** — спочатку уточнити одиниці зносу (0–1 vs 0–100), потім фікс відображення + compound на бекенді та UI.
- **Етап 4 (Таблиця кіл)** — дельта тільки на фронті; місце/стрілки потребують бекенду (Lap entity, LapRuntimeState, міграція, LapMapper, LapResponseDto).
- **Етап 1 (Summary — лідер)** — потребує нового методу в SessionFinishingPositionRepository та розширення SessionSummaryDto/сервісу; логічно після таблиці (вже працює з laps/summary).
- **Етап 5 (ERS)** — новий endpoint і злиття даних за timestamp; найбільш трудомісткий.
- **Етап 6 (Документація)** — після всіх змін.

**Тестування:** Після кожного етапу оновити або додати unit-тести (SessionSummaryMapper, LapMapper, LapQueryService, SessionSummaryQueryService, ERS-сервіс/контролер тощо) і перевірити `mvn -pl telemetry-processing-api-service verify` (покриття ≥ 85%).

**Граничні випадки:**
- Немає записів у session_finishing_positions (сесія завершилась без класифікації) → лідер не показувати або «—» / «Unknown».
- Немає кіл → дельта та місце порожні; медіана не малюється.
- Перше коло (немає попереднього) → стрілка зміни позиції не показується.

Після вибору порядку етапів можна розпочинати реалізацію або переходити до інших тем з improvements-notes.

---

## 7. Виявлені гепи та доповнення (що не було в початковому плані)

Нижче — короткий звіт про гепи між планом і поточною реалізацією; відповідні кроки вже інтегровані в етапи вище, тут — довідково.

| Геп | Поточний стан | Що додано в план |
|-----|----------------|------------------|
| **Лідер — як отримати** | SessionFinishingPositionRepository має лише `findBySessionUidAndCarIndex` | Потрібен метод типу `findBySessionUidOrderByFinishingPositionAsc`; взяти перший запис (position = 1) → car_index лідера; порівняти з playerCarIndex. Джерело імені гонщика/команди — перевірити F1 25; fallback «P1» / «Car #N». |
| **Місце на колі — де зберігати** | Lap entity без position; LapRuntimeState без positionAtLapStart; LapData містить carPosition | Явний потік: LapRuntimeState.positionAtLapStart; встановлювати при переході на нове коло (перший пакет нового кола); при finalizeLap передавати в LapBuilder; міграція — колонка `position_at_lap_start` в `laps`; LapResponseDto + LapMapper. |
| **Pace chart — overflow і медіана** | yDomain є; margin не заданий; медіана не описана детально | Margin у LineChart для підписів осі Y; медіана — одне число з валідних кіл (laps), друга лінія — горизонтальна; валідність через laps (isInvalid). |
| **Tyre wear — одиниці** | REST: 0–1; UI * 100; F1 25 spec: "percentage" (float) | У план додано крок перевірки: гра віддає 0–1 чи 0–100; при невідповідності — виправити запис (TyreWearRecorder/Builder) або відображення (UI). |
| **ERS — джерело даних** | car_status_raw без lap_distance; є ts, ers_store_energy | У план додано: отримати Lap (started_at/ended_at); telemetry по колу для lap_distance_m; car_status_raw за діапазоном ts; об'єднати по timestamp для отримання (lap_distance_m, energyPercent). |
| **Оновлення даних (refresh)** | SessionDetailPage робить background refresh кожні 5 с для ACTIVE | План вже передбачає оновлення разом із графіками; окрема live-підписка не потрібна — достатньо поточного polling refresh. |
| **Порядок етапів і тести** | Чеклист без фіксованого порядку та без явних вимог до тестів | Додано рекомендований порядок 2→3→4→1→5→6 з обґрунтуванням; вимога оновлювати unit-тести та перевіряти verify; граничні випадки (немає positions, немає кіл, перше коло). |
