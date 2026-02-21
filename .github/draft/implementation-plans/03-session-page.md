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
   - Додати колонку `session_display_name` у таблицю `telemetry.sessions`. Тип: VARCHAR(64), NOT NULL.
   - **Міграції:** у проєкті використовується `infra/init-db/` (не Flyway/Liquibase). Додати новий скрипт, наприклад `14-session-display-name.sql`: `ALTER TABLE telemetry.sessions ADD COLUMN session_display_name VARCHAR(64);` → оновити існуючі рядки `UPDATE telemetry.sessions SET session_display_name = public_id::text WHERE session_display_name IS NULL;` → `ALTER TABLE telemetry.sessions ALTER COLUMN session_display_name SET NOT NULL;`

2. **Entity і DTO**
   - У `Session` entity додати поле `sessionDisplayName` (String, не null). **За замовчуванням при INSERT:** у `@PrePersist` встановлювати `if (sessionDisplayName == null) sessionDisplayName = publicId.toString();` (publicId вже заданий у тому ж @PrePersist). Так обидва місця створення сесії (`SessionLifecycleService.onSessionStarted` та `ensureSessionActive`) отримають значення без дублювання логіки в білдері.
   - У `SessionDto` (telemetry-api-contracts) додати поле `sessionDisplayName` (String).

3. **PATCH для оновлення назви**
   - **Endpoint:** PATCH /api/sessions/{id}, body `{ "sessionDisplayName": "..." }`. Повертати оновлений SessionDto; при відсутності сесії — 404 (існуючий `SessionNotFoundException` та exception handler).
   - **Request DTO:** окремий DTO (наприклад `UpdateSessionDisplayNameRequest`) з полем `sessionDisplayName`, Bean Validation: `@NotBlank`, `@Size(max = 64)`.
   - **Сервіс:** додати метод оновлення display name. За архітектурою (orchestration у services): створити `SessionUpdateService.updateDisplayName(String id, String sessionDisplayName)` або аналогічний метод у існуючому сервісі; контролер лишається тонким — валідує вхід, викликає сервіс, повертає DTO. Репозиторій оновлення — `SessionRepository.save` після `findById` та `setSessionDisplayName`.

4. **Документація**
   - Оновити REST контракт (`.github/project/rest_web_socket_api_contracts_f_1_telemetry.md`): опис SessionDto з полем `sessionDisplayName` (max 64, not empty); опис PATCH /api/sessions/{id} для зміни назви.

**Залежності:** немає. Можна робити першим.

---

### Етап 2: sessionDisplayName — фронт

**Мета:** у таблиці та в UI показувати sessionDisplayName; можливість редагувати; всі запити до API лишають за sessionId.

**Кроки:**

1. **Типи і API**
   - У `Session` (ui/src/api/types.ts) додати `sessionDisplayName?: string | null`. У `getSessions()` і `getSession()` відповіді вже міститимуть поле після етапу 1.
   - Додати в `ui/src/api/client.ts` виклик `updateSessionDisplayName(sessionId: string, sessionDisplayName: string)` → PATCH /api/sessions/{id} з body `{ sessionDisplayName }`. Обробляти 404 як і інші endpoints.

2. **Таблиця сесій**
   - У колонці "Session" показувати `session.sessionDisplayName ?? session.id` (fallback на id якщо старий бекенд). При наведенні або в тултипі показувати повний `session.id` для копіювання.
   - **Редагування:** окрема кнопка в таблиці (іконка олівця або "Edit") відкриває **модальне вікно** з полем sessionDisplayName; після збереження викликати PATCH і оновити стан списку (refetch або оновити відповідний елемент у state). Валідація на фронті: максимум 64 символи, порожнє не допустиме (наприклад, перед submit).

3. **Інші екрани**
   - **SessionDetailPage (ui/src/pages/SessionDetailPage.tsx):** заголовок h1 зараз "Session {shortId}…". Змінити на відображення назви: `session.sessionDisplayName ?? titleIdPart` (де titleIdPart — поточний shortId для fallback). Підзаголовок (sessionType, track, startedAt) залишити без змін. Для всіх запитів (laps, summary, trace, pace, tyre wear) продовжувати використовувати `session.id` з URL.
   - **AppLayout (ui/src/components/AppLayout.tsx):** зараз для деталей сесії показується лише "← Back to Sessions". Опційно: у блоці навігації показувати "Sessions" + поточну назву сесії (sessionDisplayName), якщо вона доступна в контексті (наприклад, через передачу з роута або окремий міні-запит). Для MVP достатньо лише оновлення h1 на SessionDetailPage; breadcrumbs можна залишити як майбутнє покращення.

4. **Чеклист готовності**
   - У таблиці відображається display name; користувач може змінити його; після зміни дані зберігаються і відображаються коректно; всі запити (laps, summary, WebSocket) продовжують використовувати sessionId.

**Залежності:** етап 1.

---

### Етап 3: Місце на фініші — бекенд

**Мета:** мати фінішну позицію гравця по сесії і повертати її в SessionDto. Джерело: **спочатку** використовувати **carPosition з LapDto** (Kafka LapData) — останнє значення під час сесії; опційно в майбутньому — пакет **Final Classification** F1 25, якщо він існує в специфікації. Зберігання: **окрема таблиця** для майбутнього мульти-кар.

**Кроки:**

1. **Джерело даних (carPosition з LapData)**
   - **LapDto** (telemetry-api-contracts) вже містить `carPosition` (F1 25 LapData m_carPosition). Lap entity **не** зберігає carPosition; значення потрібно накопичувати в runtime і при завершенні сесії записати в БД.
   - У **SessionRuntimeState** додати поле для останньої позиції гравця: наприклад `lastCarPositionByCarIndex` (Map carIndex → position) або для MVP одне поле `lastCarPosition` (Integer), яке оновлюється для поточного playerCarIndex.
   - У **LapDataProcessor** після `lapAggregator.processLapData(...)` оновлювати в `SessionRuntimeState` останнє значення carPosition для відповідного carIndex (з `lapDto.getCarPosition()`). Так під час сесії завжди є актуальне "останнє місце" гравця.
   - **Final Classification:** у поточній кодовій базі пакет Final Classification не використовується. Реалізувати **тільки шлях через carPosition з LapData**. Якщо пізніше в специфікації F1 25 буде знайдено пакет Final Classification, можна додати окремий consumer/парсер і при завершенні сесії перезаписати фінішну позицію з нього (пріоритет вище за carPosition).

2. **Зберігання — окрема таблиця**
   - Створити таблицю (наприклад, `telemetry.session_finishing_positions`): `session_uid` (PK/FK), `car_index` (для мульти-кар), `finishing_position` (Integer NOT NULL). Міграція: новий скрипт у `infra/init-db/`, наприклад `15-session-finishing-positions.sql`.
   - Entity + Repository (наприклад `SessionFinishingPosition`, `SessionFinishingPositionRepository`). При переході сесії в FINISHED у **SessionLifecycleService.onSessionEnded** після `lapAggregator.finalizeAllLaps(sessionUID)` і оновлення session (ended_at, end_reason): отримати з `SessionRuntimeState` останнє carPosition для playerCarIndex (або з session entity); якщо не null — створити запис у `session_finishing_positions` і зберегти. Якщо lastCarPosition відсутній (наприклад, не було LapData) — не записувати рядок; API тоді поверне `finishingPosition: null`.

3. **API і збір DTO**
   - У `SessionDto` додати поле `finishingPosition` (Integer, optional/null).
   - **SessionMapper** лишається без залежностей від репозиторіїв: не додавати туди запит finishing position. У **SessionQueryService** при зборі DTO (listSessions і getSession): після `sessionMapper.toDto(session, runtimeState)` отримати з репозиторія finishing position по `session_uid` та player car_index (або по session_uid для одного гравця в MVP); встановити `dto.setFinishingPosition(...)`. Для getActiveSession аналогічно, якщо потрібно (зазвичай активна сесія ще не має finishing position).

4. **Документація**
   - REST контракт: опис поля `finishingPosition` у SessionDto; коли воно null (сесія ще активна, не FINISHED, або дані не надійшли). Джерело: останнє carPosition з LapData при завершенні сесії; окрема таблиця для мульти-кар.

**Залежності:** логіка завершення сесії (SessionLifecycleService), LapDataProcessor. Можна паралельно з етапами 1–2.

---

### Етап 4: Місце на фініші — фронт

**Мета:** нова колонка "Place" (або "Finish") у таблиці сесій з фінішною позицією.

**Кроки:**

1. **Типи і дані**
   - У `Session` (ui/src/api/types.ts) додати `finishingPosition?: number | null`. Після етапу 3 API вже повертає це поле.

2. **Таблиця**
   - Додати колонку (наприклад, "Place" або "Finish") у заголовок таблиці; у тілі виводити `session.finishingPosition ?? '—'` для сесій без даних або ACTIVE.

3. **Чеклист готовності**
   - Колонка відображається; для FINISHED сесій з заповненою фінішною позицією показується число; для інших — "—" або порожньо.

**Залежності:** етап 3.

---

### Етап 5: Оновлення документації та тести

**Мета:** після впровадження sessionDisplayName та місця на фініші оновити документацію та переконатися в покритті тестами.

**Кроки:**

1. **REST контракт**
   - `.github/project/rest_web_socket_api_contracts_f_1_telemetry.md`: SessionDto — поля `sessionDisplayName` (max 64, not empty), `finishingPosition` (Integer, optional); опис PATCH /api/sessions/{id} для оновлення display name; джерело finishingPosition (останнє carPosition з LapData при завершенні сесії; окрема таблиця session_finishing_positions для мульти-кар).

2. **Архітектура / UI**
   - `.github/project/react_spa_ui_architecture.md` або `documentation_index.md`: коротко відобразити, що список сесій показує display name (з можливістю редагування в модальному вікні) і колонку фінішної позиції; заголовок деталей сесії — sessionDisplayName; API ідентифікатор лишається sessionId.

3. **Unit-тести (політика покриття 85%)**
   - **SessionMapper:** оновити `SessionMapperTest` — toDto включає `sessionDisplayName` (з entity); для `finishingPosition` маппер його не встановлює (це робить SessionQueryService), тому достатньо перевіряти інші поля. Якщо маппер отримає overload з finishingPosition — додати тест.
   - **PATCH display name:** тести для сервісу оновлення (наприклад `SessionUpdateServiceTest`) — успішне оновлення, 404 для неіснуючого id, валідація (порожнє, > 64). Контролер: `SessionControllerTest` — виклик PATCH повертає 200 та оновлений DTO.
   - **SessionQueryService:** при поверненні списку/сесії DTO містить finishingPosition, коли воно є в БД; при відсутності — null. Покриття через існуючий або оновлений `SessionQueryServiceTest`.
   - **Finishing position persistence:** тест сценарію "при onSessionEnded зберігається lastCarPosition у session_finishing_positions" (інтеграційний або unit з моками SessionLifecycleService + state + repository).
   - Команда перевірки: `mvn -pl telemetry-processing-api-service verify`.

4. **Чеклист готовності**
   - Документація відповідає поточній поведінці API та UI; тести проходять; coverage не падає нижче 85%.

**Залежності:** етапи 1–4.

---

## 4. Прийняті рішення (підсумок)

| # | Питання | Рішення |
|---|---------|---------|
| 1 | sessionDisplayName — значення за замовчуванням | **Запам'ятовувати при INSERT:** заповнювати колонку при вставці нової сесії (наприклад, значенням public_id::text / UUID у коді персистенції). |
| 2 | Редагування назви — унікальність і обмеження | **sessionDisplayName не повинно бути унікальним.** Максимальна довжина **64 символи**. **Порожнє значення не допустиме.** |
| 3 | Редагування на фронті — UX | **Окрема кнопка в таблиці** та **модальне вікно** для редагування назви. |
| 4 | Фінішна позиція — джерело даних | **Спочатку** брати **carPosition з LapDto** (останнє значення під час сесії, накопичувати в SessionRuntimeState). **Final Classification** — опційно в майбутньому, якщо пакет є в специфікації F1 25. |
| 5 | Фінішна позиція — де зберігати | **Окрема таблиця** (наприклад, session_finishing_positions або session_results) для майбутнього мульти-кар; структура з session_uid + car_index + finishing_position. |

---

## 5. Чеклист перед початком коду

- [x] Усі питання мають заповнені рішення.
- [x] Порядок етапів: **1 → 2 → 3 → 4 → 5**.
- [x] Джерело фінішної позиції: **carPosition з LapDto** (вже є в LapDto та LapDataPacketParser); Final Classification — опційно в майбутньому.
- [ ] Перед етапом 1: переконатися, що наступний номер скрипту в `infra/init-db/` відповідає (наприклад 14, 15).

Після підтвердження теми можна переходити до наступної (наприклад, Сторінка Session Summary — блоки Summary, Lap pace, Tyre wear тощо).

---

## 6. Розширення плану (усунені прогалини)

При аналізі поточної реалізації та плану було додано таке, щоб план був однозначним і без прогалин:

| Прогалина | Доповнення |
|-----------|------------|
| Міграції | У проєкті немає Flyway/Liquibase; використовується `infra/init-db/`. Вказано конкретні номери скриптів (14, 15) та кроки ALTER/UPDATE. |
| sessionDisplayName за замовчуванням | Встановлення в entity `@PrePersist` (якщо null → publicId.toString()), щоб не дублювати логіку в двох місцях створення сесії (onSessionStarted, ensureSessionActive). |
| PATCH — хто робить оновлення | Request DTO з валідацією; окремий сервіс (наприклад SessionUpdateService) або метод у сервісі; контролер тонкий; 404 через існуючий exception handler. |
| Фінішна позиція — звідки брати | Lap entity не зберігає carPosition. Накопичувати в SessionRuntimeState при обробці LapData в LapDataProcessor; при onSessionEnded зберегти в session_finishing_positions. |
| Final Classification | У кодовій базі відсутній. Реалізувати тільки carPosition з LapData; Final Classification — майбутнє розширення за наявності в специфікації F1 25. |
| Збір finishingPosition у DTO | SessionMapper без репозиторіїв; SessionQueryService після toDto запитує finishing position і встановлює на DTO. |
| SessionDetailPage / breadcrumbs | Конкретизовано: h1 = sessionDisplayName ?? shortId; AppLayout — опційно breadcrumbs, для MVP достатньо оновлення h1. |
| Тести | Додано етап 5: SessionMapperTest, тести PATCH (сервіс + контролер), SessionQueryService з finishingPosition, перевірка coverage (mvn verify). |
| Шляхи до файлів | Уточнено шляхи: ui/src/api/types.ts, client.ts, SessionDetailPage.tsx, AppLayout.tsx; .github/project/rest_web_socket_api_contracts_f_1_telemetry.md. |
