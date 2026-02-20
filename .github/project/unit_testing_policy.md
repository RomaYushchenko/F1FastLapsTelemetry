# Політика юніт-тестування

> Єдиний референс для написання та підтримки юніт-тестів у проєкті F1 FastLaps Telemetry.  
> Усі нові тести та зміни в існуючих мають відповідати цій політиці.

**Інструкція для агента (Cursor):** правило [.cursor/rules/unit-testing-policy.mdc](../.cursor/rules/unit-testing-policy.mdc) — агент використовує його при роботі з тестами та питаннях про покриття.

**Посилання з інших документів:** [unit_testing_policy.md](unit_testing_policy.md)

---

## 1. Вимоги до покриття

- **Мінімальне покриття коду: 85%** (за лініями, line coverage).
- Поріг перевіряється при збірці: `mvn -pl telemetry-processing-api-service verify`.
- JaCoCo виключає з розрахунку частину коду (Application, config, entity, websocket, consumer, processor тощо); 85% вимагається для решти — маппери, сервіси, контролери, state, exception handler, білдери.

Якщо після змін `verify` падає через недостатнє покриття — потрібно додати або оновити тести.

---

## 2. Стек та підхід

### 2.1 JUnit Jupiter + Mockito

- **Фреймворк тестів:** JUnit 5 (Jupiter).
- **Моки:** Mockito. Залежності тестованого класу мокаються через `@Mock`; тестовий клас підключає Mockito через `@ExtendWith(MockitoExtension.class)`.
- **Ініціалізація:** тестований об’єкт створюється в `@BeforeEach`, йому передаються моки (або нові екземпляри мапперів/білдерів без моків).

### 2.2 Статичні тестові дані (TestData)

- **Один централізований клас** (наприклад, `TestData`) зберігає всі мок-дані та константи для тестів: сутності (Session, Lap, SessionSummary, CarTelemetryRaw, TyreWearPerLap), стани (SessionRuntimeState активний/термінальний), DTO, числа (sessionUid, trackId, lapNumber тощо).
- Тестові класи **не створюють** тестові сутності inline; вони використовують методи/поля з `TestData` (наприклад, `TestData.session()`, `TestData.lap()`, `TestData.SESSION_UID`).
- Мета: чисті тести, єдине місце зміни тестових даних, узгодженість між тестами.

### 2.3 AAA (Arrange – Act – Assert)

Кожен тест має чітку структуру:

- **Arrange** — підготовка: дані з TestData, налаштування моків (`when(...).thenReturn(...)`).
- **Act** — виклик методу, який тестується.
- **Assert** — перевірки (`assertThat(...)`, `verify(...)`).

Рекомендовано відділяти блоки коментарями `// Arrange`, `// Act`, `// Assert` або порожніми рядками, щоб структура була видима.

### 2.4 @DisplayName

- На **тестовий клас** — `@DisplayName("Ім’я тестованого класу")` (наприклад, `@DisplayName("SessionMapper")`).
- На **кожен тест-метод** — `@DisplayName("короткий опис сценарію українською")`, наприклад:
  - `@DisplayName("toPublicIdString повертає null коли session null")`
  - `@DisplayName("getSessionByPublicIdOrUid кидає виняток коли id null")`

Це покращує читання звітів та навігацію в IDE.

---

## 3. Правила написання тестів

| Правило | Опис |
|--------|------|
| Ізоляція | Кожен production-клас покривається окремим тестовим класом; залежності мокаються, не використовуються реальні БД/Kafka. |
| TestData | Мок-дані та константи беруться з класу TestData (або аналогічного централізованого класу). |
| Mockito | Для класів з залежностями: `@ExtendWith(MockitoExtension.class)`, `@Mock` для залежностей, у `@BeforeEach` створення тестованого об’єкта з моками. |
| AAA | У кожному тесті явно виділити Arrange, Act, Assert. |
| @DisplayName | Клас і кожен тест-метод мають зрозумілий опис. |
| Assertions | Використовувати AssertJ (`assertThat(...)`). Для перевірки винятків — `assertThatThrownBy(...)`. |
| Verify | При необхідності перевіряти виклики моків через `verify(mock).method(...)`. |

---

## 4. Команди

```bash
# Запуск тестів модуля
mvn -pl telemetry-processing-api-service test

# Збірка + тести + перевірка покриття (85%)
mvn -pl telemetry-processing-api-service verify

# Звіт JaCoCo (після test)
# Відкрити: telemetry-processing-api-service/target/site/jacoco/index.html
```

---

## 5. Пов’язані документи

| Документ | Призначення |
|----------|-------------|
| [unit_test_coverage_plan.md](../../.cursor/plans/unit_test_coverage_plan.md) | Покроковий план покриття по фазах, перелік тестових класів, виключення з JaCoCo. |
| [telemetry_processing_api_service.md](telemetry_processing_api_service.md) | Документація модуля telemetry-processing-api-service (архітектура, пакети). |
| [documentation_index.md](documentation_index.md) | Індекс усієї документації проєкту. |

---

## 6. Підсумок

- **Стек:** JUnit Jupiter + Mockito.
- **Дані:** централізований клас статичних тестових даних (TestData).
- **Структура тесту:** AAA, @DisplayName на клас і методи.
- **Покриття:** не менше **85%** (line coverage) для коду, що враховується JaCoCo; перевірка — `mvn verify`.
