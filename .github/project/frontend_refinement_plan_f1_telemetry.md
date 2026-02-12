## План покращення фронтенду — F1 FastLaps Telemetry

> **Мета файла:** зафіксувати структурований план доведення фронтенду до повної відповідності документації (UI/UX + діаграми телеметрії), а також окреслити наступні ітерації по графікам із PDF.
>
> Блоки:
> - **A. Обов’язкові дрібні фікси для відповідності Stage 11**
> - **B. UX‑покращення (рекомендовані, але не критичні для MVP)**
> - **C. Наступна ітерація: діаграми темпу та педалей за PDF** — **детально**

---

### A. Обов’язкові фікси для повної відповідності Stage 11

#### A.1 Summary: додати lapNumber для best секторів (S1/S2/S3)

**Документація:**
- `telemetry_diagrams_plan.md §4.1–4.3`: у summary мають бути **best S1/S2/S3 + номер кола**, на якому цей сектор показаний.

**Поточний стан:**
- `SessionDetailPage.tsx`:
  - `SessionSummary` містить `bestSector1Ms`, `bestSector2Ms`, `bestSector3Ms`, але **не містить lapNumber цих секторів**.
  - `SummaryItem` для S1/S2/S3 показує лише час, без лап-номера.

**План реалізації:**
1. У `SessionDetailPage.tsx`:
   - Після отримання `laps` і `summary`, додати обчислення:
     - `bestSector1LapNumber`, `bestSector2LapNumber`, `bestSector3LapNumber`.
     - Для кожного сектора:
       - знайти перше коло `lap` з:
         - `!lap.isInvalid`
         - `lap.sectorXMs === summary.bestSectorXMs` (X ∈ {1,2,3}).
       - взяти `lap.lapNumber`.
       - якщо не знайдено (наприклад, всі кола invalid) — залишити `null`.
2. Передати знайдені номери в `SummaryItem.extra`:
   - Для S1/S2/S3:
     - якщо `bestSectorXLapNumber != null` → `extra={`Lap ${bestSectorXLapNumber}`}`.
3. Опціонально: в інтерфейсі `SummaryItemProps` внизу файлу уточнити, що `extra` використовується для lapNumber (док‑коментар).

**Критерій готовності:**
- У summary блокові під назвами `Best S1 / Best S2 / Best S3` відображається час **і** текст `Lap N` для кожного сектора (за наявності валідного кола).

---

#### A.2 Session Detail: 404 та спеціальна обробка помилки

**Документація:**
- `rest_web_socket_api_contracts_f_1_telemetry.md §5.1`: 404 повертається для відсутньої сесії; очікується відповідний UX.
- `ui_ux_specification.md §5.3`:
  - для 404: *«Session not found»* + посилання `Back to sessions`.

**Поточний стан:**
- `SessionDetailPage.tsx`:
  - Будь-яка помилка від `getSession`/`getSessionLaps`/`getSessionSummary` дає загальний текст `Failed to load session details` / `Something went wrong…`.
  - Немає окремого UX для 404 vs інші помилки.

**План реалізації:**
1. В `SessionDetailPage.tsx`:
   - Імпортувати тип/клас `HttpError` (вже є в `api/types.ts`).
   - Усередині `catch (error)` у `load()`:
     - Перевірити:
       ```ts
       if (error instanceof HttpError && error.status === 404) {
         setStatus('error')
         setErrorMessage('NOT_FOUND')
         return
       }
       ```
     - Для інших випадків — залишити поточну логіку (загальна помилка).
2. У рендері:
   - Замість одного блоку `status === 'error'`:
     - Якщо `errorMessage === 'NOT_FOUND'`:
       - Рендерити:
         - текст `Session not found` (клас `text-error` або `text-muted`);
         - кнопку/лінк `Back to sessions` → `/sessions` (secondary або link‑style).
     - Інакше — показувати існуючий текст `Something went wrong…` + (після кроку B.2 — кнопку `Retry`).

**Критерій готовності:**
- Для неіснуючого `sessionUid` у URL UI показує:
  - чіткий текст `Session not found`;
  - явну можливість повернутися до `/sessions`.
- Інші помилки (500, network) поводяться як раніше (загальний error‑стан).

---

#### A.3 Session List: клік по всьому рядку й hover‑ефект

**Документація:**
- `ui_ux_specification.md §5.2`:
  - Табличні рядки повинні бути інтерактивними: hover‑ефект, клік по рядку відкриває деталі.

**Поточний стан:**
- `SessionListPage.tsx`:
  - Клік — тільки по `Link` у першій колонці.
  - CSS `.table` не має `cursor: pointer`/`hover` для рядків.

**План реалізації:**
1. Додати до `index.css`:
   ```css
   .table tbody tr {
     cursor: pointer;
   }

   .table tbody tr:hover {
     background-color: var(--bg-elevated);
   }
   ```
2. В `SessionListPage.tsx`:
   - Імпортувати `useNavigate` з `react-router-dom`.
   - Ініціалізувати `const navigate = useNavigate()`.
   - На `<tr>` додати:
     ```tsx
     <tr
       key={session.sessionUID}
       onClick={() => navigate(`/sessions/${session.sessionUID}`)}
     >
     ```
   - `Link` у першій колонці можна:
     - або залишити (для a11y) з `onClick={e => e.stopPropagation()}`,
     - або замінити простим `<span className="text-mono">shortId…</span>`, залишивши навігацію на рівні `<tr>`.

**Критерій готовності:**
- При наведенні на рядок видно hover‑фон і курсор `pointer`.
- Клік у будь-якій колонці рядка переводить на `/sessions/{sessionUid}`.

---

#### A.4 Live Dashboard: явний банер «Session ended» + CTA

**Документація:**
- `ui_ux_specification.md §5.1`:
  - окремий стан «Session ended» з текстом та, бажано, посиланням на історію сесій.

**Поточний стан:**
- `LiveDashboardPage.tsx`:
  - `sessionEnded` відображається лише як фраза всередині info‑картки.
  - Немає видимого банера та кнопки `View sessions` у цьому стані (вона є лише в «No active session»).

**План реалізації:**
1. У `LiveDashboardPage.tsx`:
   - Над картками, всередині блоку `hasActiveSession`, додати:
     ```tsx
     {sessionEnded && (
       <div className="card" style={{ padding: 'var(--space-2)', borderLeft: '4px solid var(--warning)' }}>
         <p className="text-muted">
           Session ended (reason: <strong>{sessionEnded.endReason}</strong>).{' '}
           <Link to="/sessions">View sessions</Link>
         </p>
       </div>
     )}
     ```
   - Залишити існуючий текст про reason в info‑картці або перенести в банер (щоб не дублювати).

**Критерій готовності:**
- Після завершення сесії користувач явно бачить банер «Session ended…» з причиною та легкий шлях перейти до `/sessions`.

---

### B. UX‑покращення (рекомендовано, але не критично для MVP)

#### B.1 WS‑помилки й майбутній reconnect‑стан (UI рівень)

**Ідея:**
- Підготувати UI до кращого відображення проблем WebSocket, навіть до реалізації самого reconnect.

**План:**
1. У `useLiveTelemetry`:
   - Розширити `LiveTelemetryState` полем:
     ```ts
     connectionMessage: string | null
     ```
   - В `onWebSocketError`:
     - крім `status: 'error'`, встановити `connectionMessage = 'Connection lost. Live data may be outdated.'`.
   - В `onDisconnect`:
     - встановити `status: 'disconnected'`, `connectionMessage = 'Disconnected from live feed.'`.
2. У `LiveDashboardPage.tsx`:
   - Додати умовний рендер над картками:
     ```tsx
     {connectionMessage && (
       <div className="card" style={{ padding: 'var(--space-2)', borderLeft: '4px solid var(--warning)' }}>
         <p className="text-muted">{connectionMessage}</p>
       </div>
     )}
     ```

**Результат:**
- Користувач бачитиме дружні повідомлення про проблеми з live‑з’єднанням, навіть до появи повноцінного reconnect/backoff (який входить у Етап 10 Observability).

---

#### B.2 Кнопка Retry на помилках (Session List + Detail)

**План:**
1. У `SessionListPage.tsx`:
   - Зберегти `load` в окремій функції (вже є всередині `useEffect`), але винести її в scope компонента, наприклад `const reload = () => { /* setStatus+fetch */ }`.
   - Викликати `reload()` в `useEffect`.
   - У блоці `status === 'error'` додати кнопку:
     ```tsx
     {status === 'error' && (
       <div>
         <p className="text-error">{...}</p>
         <button onClick={reload}>Retry</button>
       </div>
     )}
     ```
2. Аналогічно в `SessionDetailPage.tsx`:
   - Винести `load` в окрему функцію, зв’язати з `Retry`‑кнопкою.

**Результат:**
- Користувач зможе перезапустити завантаження без перезавантаження сторінки.

---

#### B.3 Мінімальні поліпшення loading‑станів

**План:**
1. Для таблиць (`SessionList`, `SessionDetail` laps):
   - При `loading`:
     - замість plain‑тексту над таблицею покласти текст усередині `card`:
       ```tsx
       {status === 'loading' && (
         <div className="card card-table">
           <p className="text-muted" style={{ padding: 'var(--space-3)' }}>Loading …</p>
         </div>
       )}
       ```
2. Для Live Dashboard:
   - `loading-active-session` залишити як зараз (`Checking for active session…`), але можна розмістити в `card`.

**Результат:**
- Візуально loading‑стани будуть більш узгоджені з загальним стилем карток/таблиць.

---

### C. Наступна ітерація: діаграми темпу та педалей (з PDF) — детальний план

Цей блок орієнтований на **після-MVP** еволюцію, але опирається на вже наявні бекенд‑заготовки з PDF:

- Діаграма **темпу до/після піт-стопів** (pace chart).
- Діаграма **натиску гальмо/газ по дистанції кола** (pedal trace).

#### C.1 Бекенд: підготовка даних (узгодження з PDF)

> Частина робіт буде на бекенді, але фронтенд план потрібно робити з урахуванням форматів.

1. **Ендпоїнт для темпу по кіл:**
   - На базі опису в PDF:
     - `GET /api/sessions/{sessionId}/laps` уже існує (повертає lapNumber, lapTimeMs, sectorXMs, isInvalid).
   - Для повноцінного темп‑графіка бажано мати:
     - `stintIndex` (номер стінту/відрізку шин);
     - `tyreCompound` (людське позначення типу шин).
   - План:
     - Розширити існуючий REST DTO для laps (або зробити окремий `LapPaceDto`) полями:
       ```json
       {
         "lapNumber": 5,
         "lapTimeMs": 87321,
         "stintIndex": 1,
         "tyreCompound": "SOFT"
       }
       ```
     - Або створити окремий endpoint `GET /api/sessions/{id}/pace` з цим DTO.

2. **Ендпоїнт для профілю педалей на колі:**
   - Відповідно до PDF:
     - `GET /api/sessions/{id}/laps/{lapNum}/trace`
     - Повертає масив:
       ```json
       [
         { "distance": 123.4, "throttle": 0.85, "brake": 0.0 },
         { "distance": 130.2, "throttle": 0.90, "brake": 0.0 },
         ...
       ]
       ```
   - Цей endpoint буде джерелом даних для педалей‑графіка.

**Фронтенд‑ наслідки:** потрібні типи DTO + методи в `api/client.ts`:
- `getSessionPace(sessionUid: string | number): Promise<PaceLapPoint[]>`
- `getLapTrace(sessionUid: string | number, lapNum: number): Promise<PedalTracePoint[]>`

---

#### C.2 Інтеграція бібліотеки графіків (Recharts)

**Вибір бібліотеки:** згідно PDF, рекомендується **Recharts** (React‑friendly, declarative).

**План:**
1. Додати залежність:
   ```bash
   cd ui
   npm install recharts
   ```
2. Створити окрему директорію для діаграм:
   - `ui/src/charts/pace-chart.tsx`
   - `ui/src/charts/throttle-brake-chart.tsx`

3. Додати базову типізацію:
   ```ts
   // src/charts/types.ts
   export interface PacePoint {
     lapNumber: number
     lapTimeMs: number
     stintIndex?: number
     tyreCompound?: string
   }

   export interface PedalTracePoint {
     distance: number
     throttle: number
     brake: number
   }
   ```

---

#### C.3 Діаграма темпу (pace chart) у Session Detail

**UI/UX ціль:** показати час кола по осі Y vs номер кола по осі X, з візуальним розділенням за стінтами/типами шин, як описано в PDF.

**План реалізації:**
1. **Компонент `PaceChart` (фронтенд):**
   - Файл: `src/charts/pace-chart.tsx`.
   - Приймає проп:
     ```ts
     interface PaceChartProps {
       points: PacePoint[]
     }
     ```
   - Використовує Recharts:
     ```tsx
     <ResponsiveContainer width="100%" height={300}>
       <LineChart data={points}>
         <XAxis dataKey="lapNumber" />
         <YAxis tickFormatter={msToSecondsLabel} />
         <Tooltip content={<CustomPaceTooltip />} />
         <Line type="monotone" dataKey="lapTimeSeconds" stroke="#10b981" ... />
       </LineChart>
     </ResponsiveContainer>
     ```
   - Попередньо перетворювати `lapTimeMs` у секунди/формат для осі Y.

2. **Кольори/сегменти за стінтами:**
   - Мінімальний варіант:
     - Одна лінія `lapTimeSeconds` і окремий масив кольорів для точок:
       - можна використовувати `dot` з кастомним `fill` на основі `tyreCompound` (map compound → color).
   - Розширений варіант:
     - Розбити дані на кілька серій (`Line` з різними `dataKey`s по стінтах) — потребує попередньої групування по `stintIndex`.

3. **Інтеграція в `SessionDetailPage`:**
   - Після блоку `Summary` або між `Summary` і `Laps`:
     ```tsx
     <div className="card" style={{ marginBottom: 'var(--space-4)', padding: 'var(--space-4)' }}>
       <h2>Lap pace</h2>
       <PaceChart points={pacePoints} />
     </div>
     ```
   - `pacePoints` можна побудувати:
     - або з розширеного `laps` DTO (якщо воно вже містить `stintIndex` і `tyreCompound`),
     - або з окремого `getSessionPace` endpoint’а.

4. **Додаткові фішки з PDF:**
   - Вертикальні лінії/ReferenceLine для pit stop’ів (переходи між стінтами).
   - Середній час по стінту:
     - обчислити `avgLapTimeMs` для кожного `stintIndex`,
     - над графіком/у легенді вивести `Stint 1 avg: 1:30.2`, `Stint 2 avg: 1:28.7`.

---

#### C.4 Діаграма натиску педалей (throttle/brake trace)

**UI/UX ціль:** показати профіль газ/гальмо по дистанції кола, як описано в PDF.

**План реалізації:**
1. **Компонент `ThrottleBrakeChart`:**
   - Файл: `src/charts/throttle-brake-chart.tsx`.
   - Пропси:
     ```ts
     interface ThrottleBrakeChartProps {
       points: PedalTracePoint[]
     }
     ```
   - Recharts:
     ```tsx
     <ResponsiveContainer width="100%" height={300}>
       <LineChart data={points}>
         <XAxis dataKey="distance" />
         <YAxis domain={[0, 1]} tickFormatter={toPercent} />
         <Tooltip content={<CustomPedalTooltip />} />
         <Line type="monotone" dataKey="throttle" stroke="#22c55e" dot={false} />
         <Line type="monotone" dataKey="brake" stroke="#ef4444" dot={false} />
       </LineChart>
     </ResponsiveContainer>
     ```

2. **API‑інтеграція:**
   - В `api/client.ts`:
     ```ts
     export async function getLapTrace(sessionUid: string | number, lapNum: number): Promise<PedalTracePoint[]> {
       return requestJson<PedalTracePoint[]>(`/api/sessions/${sessionUid}/laps/${lapNum}/trace`)
     }
     ```
   - У `SessionDetailPage`:
     - Додати локальний стан:
       ```ts
       const [selectedLapForTrace, setSelectedLapForTrace] = useState<number | null>(null)
       const [tracePoints, setTracePoints] = useState<PedalTracePoint[] | null>(null)
       const [traceStatus, setTraceStatus] = useState<'idle'|'loading'|'loaded'|'error'>('idle')
       ```
     - При завантаженні сесії:
       - як default вибрати `bestLapNumber` (якщо є), і викликати `loadTrace(bestLapNumber)`.

3. **UI для вибору кола:**
   - Над `ThrottleBrakeChart`:
     ```tsx
     <div>
       <label>
         Lap for pedal trace:
         <select value={selectedLapForTrace ?? ''} onChange={...}>
           {laps.map(lap => (
             <option key={lap.lapNumber} value={lap.lapNumber}>
               Lap {lap.lapNumber}
             </option>
           ))}
         </select>
       </label>
     </div>
     ```

4. **Обробка станів:**
   - `traceStatus === 'loading'` → показати `Loading pedal trace…`.
   - `error` → `Failed to load pedal trace` + `Retry`.
   - `idle`/`loaded` → показати графік, якщо є `points`.

5. **Інтерактивність (за PDF):**
   - Tooltip з текстом:
     - `Distance: 1203 m, Throttle: 100%, Brake: 0%`.
   - Опційно, в майбутньому:
     - додати можливість порівняти 2 кола (overlay двох серій різними кольорами/стилем ліній).

---

#### C.5 Невеликі, але важливі деталі графіків

1. **Формати осей:**
   - Осі Y:
     - Pace chart: показувати секунди (`88.321` → `1:28.321` або `88.3`).
     - Pedal chart: від 0 до 1, tooltip + labels у відсотках (`0.91` → `91%`).
2. **Легенда:**
   - Pace: у легенді показати кольори для типів шин (Soft/Medium/Hard).
   - Pedal: «Throttle» (зелений), «Brake» (червоний).
3. **Performance:**
   - За потреби для pedal trace можна:
     - застосувати легке subsampling (наприклад, брати кожну n‑ту точку, якщо їх тисячі),
     - або обмежити кількість відмальованих точок.

---

### Підсумок

- **Блок A**: точкова доводка нинішнього UI до повної відповідності `ui_ux_specification.md` і `telemetry_diagrams_plan.md` (summary sectors lap, 404 UX, row hover/click, session ended банер).
- **Блок B**: покращений UX без зміни суті функціоналу (кращі error/loading стани, підготовка до reconnect).
- **Блок C**: повноцінна реалізація діаграм темпу та педалей згідно PDF (потребує невеликого розширення бекенду + інтеграції Recharts на фронтенді).

Цей файл можна використовувати як дорожню карту для наступних ітерацій над фронтендом після завершення поточного MVP.
