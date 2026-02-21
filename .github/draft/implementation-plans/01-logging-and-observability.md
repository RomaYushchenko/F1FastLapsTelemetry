# План реалізації: Логування та спостережність

**Тема:** Бекенд — логування в файли, розділені лог-файли, лог запитів до БД, трасування (traceId).  
**Джерело ідей:** [improvements-notes-structured.md](../improvements-notes-structured.md) § 1.1.  
**Статус:** рішення прийняті; готово до реалізації.

---

## 1. Поточний стан (коротко)

| Аспект | telemetry-processing-api-service | udp-ingest-service |
|--------|----------------------------------|--------------------|
| **Куди пишуться логи** | Тільки консоль | Тільки консоль |
| **Конфіг логування** | `application.yml`: `logging.level`, `logging.pattern.console` | `application.yml`: тільки `logging.level` |
| **traceId у логах** | Є: `[%X{traceId:-}]` у pattern | Немає (немає MDC/traceId) |
| **Звідки traceId** | REST: `TraceIdFilter` (X-Trace-Id або UUID); Kafka: кожен consumer ставить `kafka-<topic>-<sessionUid>-<frameId>` у MDC | — |
| **Файлові апендери** | Немає (немає logback-spring.xml) | Немає |
| **Окремий лог БД** | Немає | — |

**Висновок:** логи лише в консоль, після закриття сесії пропадають; трасування є тільки в processing-api (REST + Kafka), в ingest — немає; розділених файлів (inbound/outbound, DB) немає.

---

## 2. Цілі (що хочемо отримати)

1. **Логи в файли** — щоб не губити їх після закриття; з ротацією та retention.
2. **Розділені лог-файли:**
   - загальний application log (все що зараз у консолі);
   - inbound/outbound (події/пакети: що входить і виходить із сервісу) — можливо окремо Kafka, окремо UDP де це застосовно;
   - окремий файл для запитів до БД (тільки processing-api).
3. **Трасування:** один traceId на запит/повідомлення, однаковий формат у всіх логах, щоб по одному traceId можна було зібрати логи з різних файлів (application, inbound/outbound, DB).

---

## 3. План реалізації (етапи)

### Етап 1: Файлове логування і ротація (база)

**Мета:** логи писати в файл (окрім консолі), з ротацією по дню та retention 5 днів (див. рішення 1–2 у розділі 5).

**Кроки:**

1. **Визначити де зберігати файли логів**
   - Локація: напр. `./logs/` відносно working directory, або змінна середовища `LOG_PATH`.
   - Рішення: обрати один варіант і задокументувати (див. питання 1 нижче).

2. **Додати logback-spring.xml (або logback.xml) у кожен сервіс**
   - Processing-api: `telemetry-processing-api-service/src/main/resources/logback-spring.xml`.
   - Ingest: `udp-ingest-service/src/main/resources/logback-spring.xml`.
   - У кожному: один File appender з RollingPolicy (наприклад, SizeAndTimeBased: макс розмір файлу + щоденна ротація), один Console appender (поточний pattern з traceId де вже є).
   - Retention: скільки днів/архівів зберігати (див. питання 2).

3. **Перевірити**
   - Після старту сервісу з’являються файли в обраній директорії, логи пишуться і в консоль, і в файл.
   - Pattern у файлі містить traceId там, де він уже використовується (processing-api).

**Залежності:** немає; можна робити першим.

---

### Етап 2: Розділені файли (application vs inbound/outbound)

**Мета:** окремий файл для подій/пакетів inbound та outbound, щоб не розбирати їх серед загального потоку.

**Кроки:**

1. **Визначити, які логери куди пишуть**
   - **Application:** все як зараз (root або пакет сервісу) → файл `application.log` (або як вирішимо в питанні 3).
   - **Inbound/outbound:** лише повідомлення про вхід/вихід подій. Потрібно визначити:
     - **Processing-api:** consumer’и (вхід з Kafka) — один логер; producer’ів у цьому сервісі немає; REST вхід можна вважати inbound (опційно).
     - **UDP ingest:** handler’и (вхід UDP пакетів) — один логер; publisher (вихід у Kafka) — один логер.
   - Рішення: ввести окремі логери (наприклад, `com.ua.yushchenko.f1.fastlaps.telemetry.processing.consumer` для Kafka inbound; аналогічно для ingest handler/publisher). Або створити один логер типу `inbound-outbound` і писати туди лише вибрані повідомлення (див. питання 4).

2. **У logback-spring.xml додати**
   - Окремі appender'и для inbound/outbound з **префіксом сервісу** в імені файлу (напр. `telemetry-processing-api-service-inbound-events.log`, `udp-ingest-service-inbound-udp.log`, `udp-ingest-service-outbound-events.log`), щоб не плутати логи різних сервісів.
   - Async appender за бажанням, щоб не гальмувати основний потік.

3. **У коді**
   - Переконатися, що логи “вхід/вихід” (наприклад, “Received …”, “Published …”) йдуть саме з тих пакетів/класів, які прив’язані до нового логера. Якщо зараз вони змішані з загальним логером — або змінити логер по класу, або додати окремий named logger (наприклад, `LoggerFactory.getLogger("inbound-outbound")`) і використовувати його тільки для цих рядків.

4. **Формат рядка**
   - У файлі inbound/outbound обов’язково включити traceId (і дату/час), щоб потім збирати з application.log по одному traceId.

**Залежності:** етап 1 (щоб загальна схема appender’ів вже була).

**Відкрите питання:** один файл “inbound-outbound” на сервіс чи два (inbound.log / outbound.log)? Див. питання 5.

---

### Етап 3: Окремий лог запитів до БД (тільки telemetry-processing-api-service)

**Мета:** один файл з усією необхідною інформацією по фреймворку (Hibernate/JPA) для аналізу запитів до БД (див. рішення 6 у розділі 5).

**Кроки:**

1. **Джерело логів БД**
   - Spring Boot + JPA/Hibernate: увімкнути логування SQL (наприклад, `spring.jpa.show-sql` або Hibernate `use_sql_comments`, format_sql вже є).
   - Направляти логер Hibernate/Spring JDBC у окремий appender. Типово це логери типу `org.hibernate.SQL`, `org.hibernate.type.descriptor.sql.BasicBinder` (параметри) — останній дуже об’ємний, зазвичай його вимикають або пишуть у окремий “trace” файл.
   - Рішення: які саме логери пишуть у `db.log` (лише SQL, чи ще параметри) — див. питання 6.

2. **logback-spring.xml**
   - Новий appender з **префіксом сервісу** в імені файлу (напр. `telemetry-processing-api-service-db.log`), щоб не плутати з іншими сервісами; ротація по дню, 5 днів.
   - Logger: `org.hibernate.SQL` (та за потреби інші) з additivity=false, щоб не дублювати в application.

3. **traceId у логах БД**
   - Запити до БД виконуються в контексті того ж потоку (REST запит або Kafka consumer), тому MDC з traceId вже має бути заповнений. Переконатися, що pattern для db appender містить `%X{traceId:-}`.

**Залежності:** етап 1.

---

### Етап 4: Трасування (traceId) — узгодження і перевірка

**Мета:** один формат traceId у всіх лог-файлах; для кожного запиту/повідомлення traceId однаковий у application, inbound/outbound і DB; в ingest-сервісі теж мати traceId де це має сенс.

**Кроки:**

1. **Формат traceId (стандартизувати)**
   - Зараз у processing-api: REST — UUID або з заголовка; Kafka — `kafka-<shortTopic>-<sessionUid>-<frameId>`.
   - Рішення: залишити ці формати чи ввести один стиль (наприклад, завжди UUID, а в Kafka передавати його в заголовку)? Див. питання 7.

2. **Перевірити pattern у всіх appender’ах**
   - У кожному logback appender (console, file application, file inbound-outbound, file db) однаковий фрагмент для traceId, напр. `[%X{traceId:-}]`, щоб grep по traceId знаходив однакові поля в усіх файлах.

3. **UDP ingest: додати traceId**
   - В ingest немає HTTP, тому traceId = “один UDP пакет” або “один publish”. Варіанти:
     - На кожен пакет генерувати UUID і класти в MDC перед обробкою; після publish очищати MDC.
     - Або traceId = `udp-<sessionUid>-<frameId>` (якщо є в пакеті), щоб корелювати з processing-api по sessionUid+frameId.
   - Рішення: який варіант обрати для ingest — див. питання 8.

4. **Документація**
   - Коротко описати: які файли куди пишуться, як шукати по traceId (приклад grep), як формується traceId для REST / Kafka / UDP.

**Залежності:** етапи 1–3 (щоб усі файли вже були).

---

### Етап 5: Оновлення документації та правил логування

**Мета:** зафіксувати схему логування в документації проєкту та в правилах для агента, щоб при додаванні нового сервісу або написанні нового функціоналу ці зміни враховувались.

**Кроки:**

1. **Оновити політику логування** (`.cursor/rules/logging-policy.mdc`)
   - Додати розділ про файлове логування: де зберігаються логи (папка `logs` в корені проєкту; профайл `dev` — `logs-dev` з підпапками по сервісах).
   - Імена файлів: **префікс сервісу для всіх** типів логів (application, inbound-events, inbound-udp, outbound-events, db), щоб не плутати між сервісами.
   - Ротація по дню, retention 5 днів.
   - traceId у всіх файлах (однаковий pattern), як шукати по traceId (grep).
   - Для **нового сервісу**: додати logback-spring.xml з тими ж правилами (шлях, префікс, ротація); для консьюмерів/хендлерів/publisher — використовувати named logger для inbound/outbound логів; встановлювати traceId у MDC на початку обробки, очищати в finally.
   - Для **нового функціоналу** (новий consumer, handler, publisher): логувати прийом/відправку через відповідний named logger (inbound-events, inbound-udp, outbound-events), щоб записи потрапляли в правильний лог-файл.

2. **Оновити документацію проєкту**
   - У [documentation_index.md](.github/project/documentation_index.md) (або в архітектурному документі) додати посилання на схему логування: план реалізації (цей документ), політика логування (.cursor/rules/logging-policy.mdc).
   - Коротко: де лежать логи, які файли (з префіксами), як додати логування в новий сервіс / новий consumer або publisher.

3. **Перевірити**
   - Новий розробник або агент за інструкцією з правил і документації може зрозуміти, куди пишуться логи і як додати логування в новий код.

**Залежності:** етапи 1–4 виконані (схема логування вже працює).

---

## 4. Прийняті рішення — довідка

Усі рішення зібрані в таблиці нижче (розділ 5). Раніше відкриті питання 1–8 закриті; деталі реалізації відображені в етапах плану (розділ 3).


---

## 5. Прийняті рішення (підсумок)

| # | Питання | Рішення |
|---|---------|---------|
| 1 | Де зберігати файли логів? | Папка `logs` в корені проекту (де лежать сервіси). **Профайл `dev`:** писати в `logs-dev`, всередині розбивати по сервісах (`logs-dev/telemetry-processing-api-service/`, `logs-dev/udp-ingest-service/`). **За замовчуванням:** усі логи в папці `logs` без підпапок по сервісах. |
| 2 | Ротація та retention | Ротація **по дню**. Retention **5 днів**. |
| 3 | Імена файлів | **Так**, префікс сервісу для **усіх** лог-файлів, щоб не плутати між сервісами: application, inbound-events, inbound-udp, outbound-events, db — кожен з префіксом, напр. `telemetry-processing-api-service-application.log`, `telemetry-processing-api-service-inbound-events.log`, `telemetry-processing-api-service-db.log`, `udp-ingest-service-inbound-udp.log`, `udp-ingest-service-outbound-events.log`. |
| 4 | Як фільтрувати inbound/outbound? | **Окремий named logger.** Переробити класи (consumers, handlers, publisher), щоб вони логували прийом/відправку через цей logger; ці логи пишуться у відповідні inbound/outbound файли. |
| 5 | Один файл чи окремо inbound/outbound? | **Окремо:** inbound/outbound для **івентів** (Kafka) + **inbound для UDP пакетів**. processing-api — inbound-events; ingest — inbound-udp та outbound-events. |
| 6 | Що писати в db.log? | **Вся необхідна інформація по фреймворку** (Hibernate/JPA): SQL, параметри та інші корисні логери за документацією Hibernate. |
| 7 | Єдиний формат traceId? | **Залишаємо як зараз:** REST — UUID або заголовок; Kafka — `kafka-<shortTopic>-<sessionUid>-<frameId>`. |
| 8 | traceId в udp-ingest-service | **UUID на пакет:** на кожен UDP пакет генерувати UUID, класти в MDC перед обробкою, очищати після. |

---

## 6. Чеклист перед початком коду

- [x] Усі питання мають заповнені рішення.
- [ ] Визначити порядок етапів (рекомендація: 1 → 3 → 2 → 4 → 5).
- [ ] Визначити, чи робити обидва сервіси паралельно чи спочатку один.
- [ ] Етап 5 виконувати останнім (після того як логи вже пишуться за новою схемою).

**Довідка після реалізації:** шлях (default) — `<project-root>/logs/`; (dev) — `logs-dev/telemetry-processing-api-service/`, `logs-dev/udp-ingest-service/`. Усі імена файлів — **з префіксом сервісу** (щоб не плутати між сервісами): `telemetry-processing-api-service-application.log`, `telemetry-processing-api-service-inbound-events.log`, `telemetry-processing-api-service-db.log`, `udp-ingest-service-application.log`, `udp-ingest-service-inbound-udp.log`, `udp-ingest-service-outbound-events.log`. Ротація по дню, 5 днів.

Після підтвердження теми можна переходити до наступної (наприклад, фронтенд — сторінка Live).
