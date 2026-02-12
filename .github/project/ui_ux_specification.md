# UI/UX Специфікація — F1 FastLaps Telemetry

> **Мета:** одне джерело правди для зовнішнього вигляду та поведінки фронтенду. При реалізації Етапу 11 (React SPA) слід дотримуватися цієї специфікації.  
> **Пов’язані документи:** [react_spa_ui_architecture.md](react_spa_ui_architecture.md), [telemetry_diagrams_plan.md](telemetry_diagrams_plan.md), [implementation_steps_plan.md](implementation_steps_plan.md).  
> **Оновлено:** February 2026

---

## 1. Призначення документа

- Фіксує **стиль** (кольори, типографіка, відступи, радіуси).
- Описує **елементи інтерфейсу** по екранах і їх ієрархію.
- Задає **поведінку** компонентів і стани (loading, empty, error).
- Дозволяє реалізувати фронтенд без додаткових питань щодо «як має виглядати».

Розділ **«Рішення від тебе»** (§ 8) — місце для твоїх переваг; якщо нічого не змінювати, використовуються пропозиції з цього документа.

---

## 2. Дизайн-напрямок (пропозиція)

- **Настрій:** темна, технічна панель телеметрії; асоціація з F1 / cockpit / дашборд.
- **Контраст:** темний фон, світлий текст; акценти для важливих значень (швидкість, best lap).
- **Читабельність:** великі цифри для live-метрик; чіткі лейбли; достатні відступи.
- **Мінімалізм:** без зайвих декорацій; фокус на даних.

Якщо хочеш інший стиль (світла тема, інший настрій) — вкажи в § 8.

---

## 3. Дизайн-токени (Design Tokens)

Усі значення — **пропозиція**. Їх можна змінити в одному місці (CSS-змінні або theme) під твої вподобання.

### 3.1 Кольори

| Токен | Значення (приклад) | Використання |
|--------|---------------------|--------------|
| `--bg-page` | `#0d0d0f` | Фон сторінки |
| `--bg-surface` | `#16161a` | Картки, панелі, таблиці |
| `--bg-elevated` | `#1e1e24` | Хедер, підняті елементи |
| `--border` | `#2a2a32` | Рамки, роздільники |
| `--text-primary` | `#f0f0f2` | Основний текст |
| `--text-secondary` | `#9a9aa6` | Підписи, другорядний текст |
| `--accent` | `#e10600` або `#c41e3a` | Акцент (напр. лого, активний nav, DRS ON) |
| `--accent-muted` | `#ff4444` з opacity | Підсвітка best lap / best sector |
| `--success` | `#22c55e` | Valid lap, успіх, підключення |
| `--warning` | `#eab308` | Попередження, реконнект |
| `--error` | `#ef4444` | Помилки, invalid |
| `--live-badge` | `#22c55e` | Бейдж ACTIVE / Live |

**Примітка:** `--accent` можна взяти у червоної гами (F1-асоціація); альтернатива — синій/бірюзовий для технічного вигляду.

### 3.2 Типографіка

| Токен | Значення (приклад) | Використання |
|--------|---------------------|--------------|
| `--font-sans` | `'Inter', 'Segoe UI', system-ui, sans-serif` | Основний текст, UI |
| `--font-mono` | `'JetBrains Mono', 'Fira Code', monospace` | Числа телеметрії, коди, session UID |
| `--text-xs` | `0.75rem` | Дрібні підписи |
| `--text-sm` | `0.875rem` | Другорядний текст |
| `--text-base` | `1rem` | Основний текст |
| `--text-lg` | `1.125rem` | Підзаголовки |
| `--text-xl` | `1.25rem` | Заголовки блоків |
| `--text-2xl` | `1.5rem` | Заголовок сторінки |
| `--text-display-speed` | `3rem` або `clamp(2rem, 5vw, 3.5rem)` | Велике число Speed (km/h) |
| `--text-display-rpm` | `2rem` – `2.5rem` | RPM, Gear |
| `--font-weight-normal` | `400` | Звичайний текст |
| `--font-weight-medium` | `500` | Лейбли |
| `--font-weight-semibold` | `600` | Заголовки |
| `--font-weight-bold` | `700` | Best lap, акценти |

### 3.3 Відступи та сітка

| Токен | Значення | Використання |
|--------|----------|--------------|
| `--space-1` | `4px` | Дрібні відступи |
| `--space-2` | `8px` | Внутрішні відступи кнопок, комірки |
| `--space-3` | `12px` | Відступи між елементами |
| `--space-4` | `16px` | Відступи між блоками |
| `--space-5` | `24px` | Відступ контенту від країв, між секціями |
| `--space-6` | `32px` | Великі блоки |
| `--radius-sm` | `4px` | Кнопки, бейджі, інпути |
| `--radius-md` | `8px` | Картки, панелі |
| `--radius-lg` | `12px` | Модалки, великі блоки |
| `--content-max-width` | `1200px` | Максимальна ширина контенту по центру |

### 3.4 Тіні (опційно)

| Токен | Значення | Використання |
|--------|----------|--------------|
| `--shadow-sm` | `0 1px 2px rgba(0,0,0,0.3)` | Картки, таблиці |
| `--shadow-md` | `0 4px 12px rgba(0,0,0,0.4)` | Підняті панелі |

---

## 4. Глобальний layout

### 4.1 Структура

- **Header** (фіксований або sticky): логотип/назва «F1 FastLaps Telemetry», навігація (Live, Sessions), опційно індикатор WebSocket (підключено / відключено).
- **Main** (контент): одна колонка, max-width `--content-max-width`, центрування, padding `--space-5`.
- **Footer** (опційно в MVP): мінімальний або відсутній.

### 4.2 Header

- Висота: ~56px (або 3.5rem).
- Фон: `--bg-elevated`, нижня border `--border`.
- Елементи в один ряд: зліва — назва/лого; справа — посилання «Live», «Sessions». На сторінці Session Detail додатково — кнопка «Back» (веде на `/sessions`).
- **Active route:** посилання поточної сторінки виділити (наприклад `--accent` або підкреслення / background).

### 4.3 Назва / лого

- Текст: **«F1 FastLaps Telemetry»** (або логотип, якщо буде).
- Стиль: `--text-lg`, `--font-weight-semibold`, `--text-primary`. Клік по назві — перехід на `/`.

---

## 5. Екрани та елементи

### 5.1 Live Dashboard (`/`)

**Призначення:** показувати live-телеметрію або стан «немає активної сесії».

#### Блок «Немає активної сесії»

- Центрований блок (картка або просто текст).
- Текст: *«No active session. Start a session in F1 25.»*
- Кнопка/посилання: *«View past sessions»* → `/sessions`. Стиль: кнопка secondary (outline) або link з `--accent`.

#### Блок «Є активна сесія»

- Заголовок: *«Live telemetry»*; справа (опційно) — короткий session ID або «Session: …» (truncate).
- Сітка віджетів (наприклад 2–3 колонки на desktop). Кожен віджет — **картка** (фон `--bg-surface`, border `--border`, `--radius-md`, padding `--space-4`).

**Віджети (порядок і стиль):**

| Віджет | Лейбл | Значення | Стиль значення |
|--------|--------|----------|-----------------|
| **Speed** | SPEED (малими, `--text-secondary`, `--text-xs`) | число + « km/h» | `--text-display-speed`, `--font-mono`, `--text-primary` |
| **RPM** | RPM | число | `--text-display-rpm`, `--font-mono`; опційно progress bar (0–~15000), redline зона > ~12000 червоним |
| **Gear** | GEAR | 1–8, N, R | Велике число/символ, `--font-mono` |
| **Throttle** | THROTTLE | 0–100% | Progress bar (горизонтальний); заповнення `--accent` або зелений; під баром опційно «XX%» |
| **Brake** | BRAKE | 0–100% | Progress bar; заповнення червоний/помаранчевий |
| **DRS** | DRS | ON / OFF / — | Текст або бейдж: ON — `--accent` або зелений; OFF — `--text-secondary`; null — «—» |
| **Lap / Sector** | Current lap | «Lap {n} · Sector {s}» | `--text-base`, `--text-primary`; якщо null — «—» |

- **Progress bar:** висота ~8px, фон `--bg-elevated`, заповнення зліва направо, `--radius-sm`.

#### Стан «Session ended»

- Після отримання SESSION_ENDED: показати банер або повідомлення *«Session ended»* (наприклад `--text-secondary` або `--warning`), опційно кнопка «View sessions» → `/sessions`. Останній snapshot можна залишити на екрані або прибрати.

#### Помилки WebSocket

- «Connection lost» / «Reconnecting…» — невеликий банер під header або над віджетами (`--warning`). Помилка `SESSION_NOT_ACTIVE` — обробити як перехід у стан «No active session».

---

### 5.2 Session List (`/sessions`)

**Заголовок сторінки:** *«Sessions»* (`--text-2xl`, `--font-weight-semibold`).

**Контейнер:** таблиця або сітка карток. Рекомендація: **таблиця** для швидкого скану (Session, Type, Track, Started, Ended, State).

- **Таблиця:**
  - Фон `--bg-surface`, border `--border`, `--radius-md`, overflow-x auto.
  - Header row: фон трохи темніший, `--text-secondary`, `--text-sm`, `--font-weight-medium`. Колонки: Session, Type, Track, Started, Ended, State.
  - Рядки: при hover — фон `--bg-elevated`; клік по рядку — навігація на `/sessions/{sessionUid}`. Курсор pointer.
  - **Session:** короткий UID (наприклад останні 8 символів) або дата старту в локальному форматі (DD.MM HH:mm).
  - **Type:** RACE, QUALIFYING, PRACTICE (як є в API).
  - **Track:** trackId або назва з мапи (наприклад 12 → «Monaco»); якщо мапи немає — показувати trackId.
  - **Started / Ended:** дата/час у форматі DD.MM HH:mm або коротше; ended може бути «—» якщо null.
  - **State:** бейдж — ACTIVE (`--live-badge`) або FINISHED (`--text-secondary`).

**Стани:**

- **Loading:** skeleton рядків або spinner по центру блоку таблиці.
- **Empty:** текст *«No sessions yet. Start a session in F1 25.»* по центру, стиль `--text-secondary`.
- **Error:** повідомлення *«Something went wrong»* + кнопка «Retry» (primary або outline).

---

### 5.3 Session Detail (`/sessions/:sessionUid`)

**Заголовок сторінки:** один ряд: короткий session UID або «Session»; під ним підзаголовок: тип · траса · діапазон дат (наприклад *«RACE · Monaco · 28.01.2026 20:10–20:55»*), `--text-secondary`, `--text-sm`.

**Кнопка Back:** зліва або в header — «← Back» або «Back to sessions» → `/sessions`. Стиль: link або secondary button.

#### Summary block

- Заголовок секції: *«Summary»* (`--text-xl`, `--font-weight-semibold`).
- Картка (або панель) з полями:
  - **Best lap:** час у форматі M:SS.mmm (наприклад «1:26.210») + «(lap {n})»; значення виділити (`--font-weight-bold` або `--accent-muted`).
  - **Total laps:** число.
  - **Best S1 / S2 / S3:** час у секундах (наприклад 28.790) + «(lap {n})»; best sector у таблиці кіл виділяти так само.

#### Laps table

- Заголовок секції: *«Laps»*.
- Таблиця: **Lap** | **Time** | **S1** | **S2** | **S3** | **Valid**.
- **Time, S1, S2, S3:** формат з lapTimeMs/sectorXMs — M:SS.mmm або S.mmm (секунди з мілісекундами).
- **Valid:** галочка (✓) або «Yes» для valid; «—» або хрестик для invalid.
- **Best lap row:** фон рядка `--accent-muted` з низькою opacity або border-left `--accent`; опційно іконка/зірочка біля часу.
- **Best sector у колонці:** комірка з best S1 (або S2, S3) виділити так само (жирний або кольором).

#### Сектори (окрема підсекція або частина summary)

- Можна показати best S1/S2/S3 з номерами кіл у блоці Summary; окрема таблиця «Sectors» не обов’язкова, якщо дані вже є в Laps та Summary.

**Стани:**

- **Loading:** окремо для session / laps / summary — skeleton або spinner у відповідних блоках.
- **404:** *«Session not found»* + посилання «Back to sessions».
- **Error:** *«Something went wrong»* + кнопка «Retry».

---

## 6. Переиспользуємі компоненти (стиль)

### 6.1 Кнопки

- **Primary:** фон `--accent`, колір тексту білий, padding `--space-2` `--space-4`, `--radius-sm`, hover трохи світліший.
- **Secondary / Outline:** прозорий фон, border `--border`, текст `--text-primary`, hover фон `--bg-elevated`.
- **Link:** без рамки, текст `--accent` або `--text-primary`, підкреслення при hover.

### 6.2 Посилання (Nav)

- Звичайний стан: `--text-secondary` або `--text-primary`.
- Active: `--accent` або `--font-weight-semibold` + підкреслення.
- Hover: `--text-primary` або підкреслення.

### 6.3 Картки (віджети, панелі)

- Фон `--bg-surface`, border `1px solid var(--border)`, `--radius-md`, padding `--space-4`, опційно `--shadow-sm`.

### 6.4 Бейджі (State, DRS)

- Невеликий padding `--space-1` `--space-2`, `--radius-sm`, `--text-xs`, `--font-weight-medium`. ACTIVE — зелений фон/текст; FINISHED — сірий.

### 6.5 Таблиці

- Header: `--text-secondary`, `--text-sm`. Рядки: border-bottom `--border`; hover — фон `--bg-elevated`. Комірки: padding `--space-2` `--space-3`.

### 6.6 Progress bar (Throttle / Brake)

- Контейнер: висота ~8px, фон `--bg-elevated`, `--radius-sm`. Заповнення: висота 100%, `--radius-sm`, Throttle — зелений або `--accent`, Brake — червоний/помаранчевий.

### 6.7 Числа телеметрії (Speed, RPM, Gear)

- Шрифт `--font-mono`, щоб цифри не «стрибали» при зміні (fixed width). Відповідні розміри з § 3.2.

---

## 7. Адаптивність та доступність (MVP)

- **Breakpoint (опційно):** до 768px — один стовпчик віджетів, таблиця з горизонтальним скролом. Header — можливий burger menu замість двох посилань.
- **Контраст:** переконатися, що `--text-primary` на `--bg-page` та `--bg-surface` відповідає WCAG AA (мінімум для важливого тексту).
- **Фокус:** видимий outline для кнопок і посилань (keyboard focus).
- **Семантика:** заголовки H1/H2 по сторінках, кнопки — `<button>`, навігація — `<nav>` та посилання.

---

## 8. Рішення від тебе (опційно)

Якщо хочеш щось змінити — заповни нижче. Інакше при реалізації використовуються пропозиції з § 2–7.

| Питання | Пропозиція в документі | Твій вибір (заповни при потребі) |
|---------|------------------------|----------------------------------|
| **Тема** | Темна (cockpit-style) | □ залишити темну  □ світла  □ інше: ___ |
| **Акцентний колір** | Червоний (#e10600 / #c41e3a) | □ залишити  □ інший: ___ |
| **Шрифт для цифр** | Monospace (JetBrains Mono тощо) | □ залишити  □ інший: ___ |
| **Віджети Live** | Картки в сітці 2–3 колонки | □ залишити  □ один стовпчик  □ інше: ___ |
| **Список сесій** | Таблиця | □ залишити  □ картки  □ інше: ___ |
| **Референси** (сайти/скріншоти, як «хочу щоб так») | — | ___ |
| **Інше** | — | ___ |

---

## 9. Чеклист для реалізації (Етап 11)

При імплементації перевіряти:

- [ ] Усі кольори/відступи/шрифти взяті з токенів (§ 3) або з твоїх перевизначень (§ 8).
- [ ] Header однаковий на всіх сторінках; active route виділений.
- [ ] Live: усі 7 віджетів у заданому стилі; стан «no active session» і «session ended» оброблені.
- [ ] Session List: таблиця з усіма колонками; loading / empty / error.
- [ ] Session Detail: summary block, laps table з виділенням best lap і best sectors; back, 404, error.
- [ ] Кнопки та посилання відповідають § 6.1–6.2.
- [ ] Формати: час M:SS.mmm з ms; дати DD.MM HH:mm; DRS ON/OFF/—.

---

*Цей документ є частиною документації проєкту F1 FastLaps Telemetry і узгоджений з [react_spa_ui_architecture.md](react_spa_ui_architecture.md) та [telemetry_diagrams_plan.md](telemetry_diagrams_plan.md).*
