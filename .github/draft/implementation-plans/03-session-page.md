# План реалізації: Сторінка Session (список сесій)

**Тема:** Фронтенд і бекенд — відображувана назва сесії (sessionDisplayName), місце на фініші.  
**Джерело ідей:** [improvements-notes-structured.md](../improvements-notes-structured.md) § 2.2.  
**Статус:** рішення прийняті; готово до реалізації.

---

## 1. Поточний стан (коротко)

| Аспект | Що є зараз |
|--------|-------------|
| **Сторінка** | `SessionListPage.tsx`: таблиця сесій (Session, Type, Track, Started, Ended, State). Колонка "Session" показує **короткий UUID** (перші 8 символів + "…"); клік веде на `/sessions/{id}`. |
| **API** | GET /api/sessions → список `SessionDto`. GET /api/sessions/{id} — деталі по `id`. Поле `id` у DTO — завжди **sessionId** (UUID або session_uid string). |
| **Бекенд** | `Session` entity: `session_uid`, `public_id`, sessionType, trackId, startedAt, endedAt, endReason тощо. **Немає** поля display name; **немає** поля фінішної позиції. `SessionMapper.toDto()` повертає id = public_id (UUID). |
| **Фронт типи** | `Session` в `api/types.ts`: `id`, sessionType, trackId, startedAt, endedAt, state тощо. Усі запити (laps, summary, WebSocket) використовують `session.id`. |

**Висновок:** для sessionDisplayName потрібно додати поле на бекенді (entity + DTO), за замовчуванням заповнювати UUID; на фронті — показувати display name в таблиці та дати можливість редагувати; всі запити лишають за `sessionId`. Для місця на фініші потрібне джерело даних на бекенді (фінішна позиція гравця по сесії).

---

## 2. Цілі (що хочемо отримати)

1. **sessionDisplayName** — замість одного UUID у колонці Session мати зрозумілішу назву. На бекенді поле `sessionDisplayName`, за замовчуванням = UUID; на фронті можливість редагувати; в таблиці та в UI скрізь показувати display name, але всі запити до API лишають за ключем `sessionId` (UUID).
2. **Місце на фініші** — нова колонка в таблиці: місце, на якому гравець закінчив сесію. Потребує даних з бекенду (фінішна позиція по сесії).

---

## 3. План реалізації (етапи)

### Етап 1: sessionDisplayName — бекенд

**Мета:** додати поле відображуваної назви сесії в БД, контракт і API; за замовчуванням = UUID.

**Кроки:**

1. **База даних**
   - Додати колонку `session_display_name` у таблицю `telemetry.sessions`. Тип: VARCHAR(64), NOT NULL. **Заповнювати при INSERT** нової сесії значенням `public_id::text` (UUID) у місці, де створюється Session (наприклад, SessionPersistenceService).
   - Міграція (Flyway/Liquibase): один скрипт додавання колонки та оновлення існуючих рядків (SET session_display_name = public_id::text WHERE session_display_name IS NULL), потім NOT NULL.

2. **Entity і DTO**
   - У `Session` entity додати поле `sessionDisplayName` (String, не null). При збереженні нової сесії встановлювати за замовчуванням = `publicId.toString()`.

3. **PATCH або PUT для оновлення назви**
   - Додати endpoint оновлення display name (наприклад PATCH /api/sessions/{id} з body `{ "sessionDisplayName": "..." }`). Валідація: **максимум 64 символи**, **порожнє значення не допустиме**; **унікальність не потрібна**. Повертати оновлений SessionDto.

4. **Документація**
   - Оновити REST контракт (rest_web_socket_api_contracts): опис SessionDto з новим полем; опис PATCH/PUT для зміни назви.

**Залежності:** немає. Можна робити першим.

---

### Етап 2: sessionDisplayName — фронт

**Мета:** у таблиці та в UI показувати sessionDisplayName; можливість редагувати; всі запити до API лишають за sessionId.

**Кроки:**

1. **Типи і API**
   - У `Session` (api/types.ts) додати `sessionDisplayName?: string | null`. У `getSessions()` і `getSession()` відповіді вже міститимуть поле після етапу 1.
   - Додати виклик API для оновлення назви (наприклад, `updateSessionDisplayName(sessionId: string, sessionDisplayName: string)` → PATCH /api/sessions/{id} з body).

2. **Таблиця сесій**
   - У колонці "Session" показувати `session.sessionDisplayName` (завжди заповнене з бекенду). При наведенні або в тултипі можна показувати повний sessionId для копіювання.
   - **Редагування: окрема кнопка в таблиці** (наприклад, іконка олівця або "Edit" у рядку) відкриває **модальне вікно** з полем sessionDisplayName; після збереження викликати PATCH і оновити стан списку. Валідація на фронті: максимум 64 символи, порожнє не допустиме.

3. **Інші екрани**
   - У заголовках деталей сесії, breadcrumbs тощо використовувати sessionDisplayName де показується "назва" сесії; для посилань і API завжди використовувати `session.id`.

4. **Чеклист готовності**
   - У таблиці відображається display name; користувач може змінити його; після зміни дані зберігаються і відображаються коректно; всі запити (laps, summary, WebSocket) продовжують використовувати sessionId.

**Залежності:** етап 1.

---

### Етап 3: Місце на фініші — бекенд

**Мета:** мати фінішну позицію гравця по сесії і повертати її в SessionDto. Джерело: якщо в F1 25 є пакет **Final Classification** — використовувати його і брати дані звідти; якщо немає — брати **carPosition з LapDto** (останнє значення при завершенні сесії). Зберігання: **окрема таблиця** для майбутнього мульти-кар.

**Кроки:**

1. **Джерело даних**
   - Перевірити наявність пакета **Final Classification** у F1 25 телеметрії. Якщо є — додати парсер/обробник і брати фінішну позицію гравця звідти при завершенні сесії.
   - Якщо Final Classification немає — використовувати **carPosition з LapDto** (Kafka LapData): при переході сесії в FINISHED зберегти останнє відоме carPosition гравця (playerCarIndex) як фінішну позицію.

2. **Зберігання — окрема таблиця**
   - Створити таблицю (наприклад, `telemetry.session_finishing_positions` або `session_results`): `session_uid` (PK/FK), `car_index` (для майбутнього мульти-кар), `finishing_position` (Integer). Для MVP один рядок на сесію (гравець = один car_index). Заповнювати при переході сесії в FINISHED (з Final Classification або з LapDataProcessor).
   - Міграція: створення таблиці.

3. **API**
   - У `SessionDto` додати поле `finishingPosition` (Integer, optional). При зборі DTO (SessionMapper або SessionQueryService) джойнити/запитувати фінішну позицію для гравця з нової таблиці. Список сесій і GET by id повертатимуть це поле.

4. **Документація**
   - REST контракт: опис поля `finishingPosition` у SessionDto; коли воно null (сесія ще активна або дані не надійшли).

**Залежності:** наявність carPosition у LapDto або Final Classification у F1 25; логіка завершення сесії. Можна паралельно з етапами 1–2.

---

### Етап 4: Місце на фініші — фронт

**Мета:** нова колонка "Place" (або "Finish") у таблиці сесій з фінішною позицією.

**Кроки:**

1. **Типи і дані**
   - У `Session` (api/types.ts) додати `finishingPosition?: number | null`. Після етапу 3 API вже повертає це поле.

2. **Таблиця**
   - Додати колонку (наприклад, "Place" або "Finish") у заголовок таблиці; у тілі виводити `session.finishingPosition ?? '—'` для сесій без даних або ACTIVE.

3. **Чеклист готовності**
   - Колонка відображається; для FINISHED сесій з заповненою фінішною позицією показується число; для інших — "—" або порожньо.

**Залежності:** етап 3.

---

### Етап 5: Оновлення документації

**Мета:** після впровадження sessionDisplayName та місця на фініші оновити існуючу документацію проєкту.

**Кроки:**

1. **REST контракт**
   - [rest_web_socket_api_contracts_f_1_telemetry.md](../../project/rest_web_socket_api_contracts_f_1_telemetry.md): SessionDto — поля `sessionDisplayName` (max 64, not empty), `finishingPosition`; опис PATCH для оновлення display name; джерело finishingPosition (Final Classification або LapData.carPosition); окрема таблиця для результатів (мульти-кар).

2. **Архітектура / UI**
   - [react_spa_ui_architecture.md](../../project/react_spa_ui_architecture.md) або [documentation_index.md](../../project/documentation_index.md): коротко відобразити, що список сесій показує display name (з можливістю редагування) і фінішну позицію; API ідентифікатор лишається sessionId.

3. **Чеклист готовності**
   - Документація відповідає поточній поведінці API та UI.

**Залежності:** етапи 1–4.

---

## 4. Прийняті рішення (підсумок)

| # | Питання | Рішення |
|---|---------|---------|
| 1 | sessionDisplayName — значення за замовчуванням | **Запам'ятовувати при INSERT:** заповнювати колонку при вставці нової сесії (наприклад, значенням public_id::text / UUID у коді персистенції). |
| 2 | Редагування назви — унікальність і обмеження | **sessionDisplayName не повинно бути унікальним.** Максимальна довжина **64 символи**. **Порожнє значення не допустиме.** |
| 3 | Редагування на фронті — UX | **Окрема кнопка в таблиці** та **модальне вікно** для редагування назви. |
| 4 | Фінішна позиція — джерело даних | Якщо в F1 25 є **Final Classification** — використовувати його і брати дані звідти. Якщо немає — брати **carPosition з LapDto** (якщо воно є). |
| 5 | Фінішна позиція — де зберігати | **Окрема таблиця** (наприклад, session_finishing_positions або session_results) для майбутнього мульти-кар; структура з session_uid + car_index + finishing_position. |

---

## 5. Чеклист перед початком коду

- [x] Усі питання мають заповнені рішення.
- [x] Порядок етапів: **1 → 2 → 3 → 4 → 5**.
- [ ] Перевірити наявність Final Classification у F1 25 та carPosition у LapDto; визначити, яке джерело використовувати першим.

Після підтвердження теми можна переходити до наступної (наприклад, Сторінка Session Summary — блоки Summary, Lap pace, Tyre wear тощо).
