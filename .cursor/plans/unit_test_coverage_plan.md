# План покриття коду юніт-тестами (мінімум 85%)

**Головна інструкція з тестування:** [.github/project/unit_testing_policy.md](../../.github/project/unit_testing_policy.md) — JUnit Jupiter + Mockito, TestData, AAA, @DisplayName, вимога покриття не менше 85%.

Мета цього документа: покроковий план (фази) для досягнення та підтримки **мінімум 85% code coverage** у модулі **telemetry-processing-api-service**. Всі тести пишуться згідно політики: **мок-дані з TestData**, ізольоване тестування з Mockito.

---

## Загальні правила (узгоджено з unit_testing_policy.md)

- **TestData** — один клас зі статичними полями/методами для мок-сутностей (Session, Lap, SessionSummary, CarTelemetryRaw, TyreWearPerLap, SessionRuntimeState, DTO тощо). Тестові класи не створюють дані inline, а беруть їх з TestData.
- **JUnit Jupiter + Mockito:** тести з `@ExtendWith(MockitoExtension.class)`, залежності через `@Mock`; структура AAA, `@DisplayName` на клас і методи.
- Кожен **production-клас** покривається окремим ***Test** класом з моками залежностей.
- **Покриття:** не менше **85%** line coverage; перевірка — `mvn -pl telemetry-processing-api-service verify`.
- Після **кожної фази** — запуск тестів: `mvn -pl telemetry-processing-api-service test`; у кінці — `verify` для перевірки 85%.

---

## Фаза 1: Підґрунтя (TestData, маппери, винятки, resolve)

| Крок | Опис | Класи |
|------|------|--------|
| 1.1 | Додати **spring-boot-starter-test** та **JaCoCo** у `telemetry-processing-api-service/pom.xml`. Мінімум coverage: 85%. | pom.xml |
| 1.2 | Створити **TestData** з мок-даними: Session, SessionRuntimeState (активний/фініш), Lap, SessionSummary, CarTelemetryRaw, TyreWearPerLap, CarSnapshot, TyreWearSnapshot, UUID/publicId, числа (sessionUid, trackId, lapNumber тощо). | TestData.java |
| 1.3 | Тести **SessionMapper**: toPublicIdString (null, з publicId, тільки sessionUid), sessionTypeToDisplayString (0–12, null, default), toDto (null session, з runtimeState ACTIVE/FINISHED). | SessionMapperTest |
| 1.4 | Тести **LapMapper**: toLapResponseDto (null, повний lap), toPacePointDto (null, lapTimeMs<=0, валідний), toTracePointDto (null, з полями), toTyreWearPointDto (null, з полями). | LapMapperTest |
| 1.5 | Тести **SessionSummaryMapper**: toDto (null, повний summary), emptySummaryDto(). | SessionSummaryMapperTest |
| 1.6 | Тести **SessionNotFoundException**: конструктори (message, message+cause). | SessionNotFoundExceptionTest (або в одному з handler tests) |
| 1.7 | Тести **RestExceptionHandler**: handleSessionNotFound (404, SESSION_NOT_FOUND), handleIllegalArgument (400, INVALID_REQUEST), handleGenericException (500, INTERNAL_ERROR). | RestExceptionHandlerTest |
| 1.8 | Тести **SessionResolveService**: getSessionByPublicIdOrUid — blank id (exception), null (exception), знайдено (повертає Session), не знайдено (exception). Мок SessionRepository. | SessionResolveServiceTest |
| **Перевірка фази 1** | `mvn -pl telemetry-processing-api-service test` — усі тести зелено. | |

---

## Фаза 2: Білдери та статичні методи

| Крок | Опис | Класи |
|------|------|--------|
| 2.1 | **WsSnapshotMessageBuilder**: build(null) → null; build(CarSnapshot) → WsSnapshotMessage з усіма полями. | WsSnapshotMessageBuilderTest |
| 2.2 | **LapBuilder**: build(...) → Lap з усіма полями. | LapBuilderTest |
| 2.3 | **TyreWearPerLapBuilder**: fromSnapshot(null) → null; з TyreWearSnapshot → TyreWearPerLap. | TyreWearPerLapBuilderTest |
| **Перевірка фази 2** | `mvn -pl telemetry-processing-api-service test` — усі тести зелено. | |

---

## Фаза 3: Query-сервіси

| Крок | Опис | Класи |
|------|------|--------|
| 3.1 | **SessionQueryService**: listSessions (мок repository + stateManager), getSession (resolve + mapper), getSession("active") exception, getActiveSession (empty, one), getTopicIdForSession (null, empty, present). | SessionQueryServiceTest |
| 3.2 | **LapQueryService**: getLaps, getPace, getTyreWear, getLapTrace — мок resolve + репозиторії + mapper; перевірка викликів і результатів. | LapQueryServiceTest |
| 3.3 | **SessionSummaryQueryService**: getSummary — знайдено (toDto), не знайдено (emptySummaryDto). Мок resolve + SessionSummaryRepository. | SessionSummaryQueryServiceTest |
| 3.4 | **WebSocketSubscriptionService**: subscribe — blank id (error), SessionNotFoundException (error), session not active (error), success (ok). Моки resolve, stateManager, wsSessionManager. | WebSocketSubscriptionServiceTest |
| **Перевірка фази 3** | `mvn -pl telemetry-processing-api-service test` — усі тести зелено. | |

---

## Фаза 4: Контролери, процесори та решта (до 85%)

| Крок | Опис | Класи |
|------|------|--------|
| 4.1 | **SessionController**: listSessions, getSession, getActiveSession (no content / ok). Мок SessionQueryService. | SessionControllerTest |
| 4.2 | **LapController**, **SessionSummaryController**: аналогічно — мок відповідного QueryService. | LapControllerTest, SessionSummaryControllerTest |
| 4.3 | **WebSocketController**: subscribe — успіх та помилки (SessionNotFoundException, not active). Мок WebSocketSubscriptionService. | WebSocketControllerTest (якщо є публічні методи) |
| 4.4 | **SessionStateManager**: getOrCreate, get, close, getAllActive, getActiveSessionCount. | SessionStateManagerTest |
| 4.5 | **SessionRuntimeState**: transitionTo, updateWatermark, getWatermark, updateSnapshot, getSnapshot, isTerminal, isActive, getLatestSnapshot (empty/one snapshot). | SessionRuntimeStateTest |
| 4.6 | **Processors** (SessionEventProcessor, LapDataProcessor, CarTelemetryProcessor, CarStatusProcessor, CarDamageProcessor): основні сценарії з моками. | *ProcessorTest |
| 4.7 | **Consumers** (при потребі): тонкі тести з моками процесорів/сервісів. | За потреби |
| **Перевірка фази 4** | `mvn -pl telemetry-processing-api-service test` — усі тести зелено. | |

---

## Фаза 5: Перевірка coverage

| Крок | Опис |
|------|------|
| 5.1 | Запустити `mvn -pl telemetry-processing-api-service verify` (або `test jacoco:report`). |
| 5.2 | Відкрити звіт JaCoCo (target/site/jacoco/index.html). |
| 5.3 | Переконатися, що **line coverage** та/або **instruction coverage** для модуля ≥ **85%**. Якщо ні — додати тести для непокритих гілок/класів. |

---

## Залежності між фазами

```
Фаза 1 (TestData, маппери, resolve, exception handler)
    ↓
Фаза 2 (білдери)
    ↓
Фаза 3 (Query-сервіси)
    ↓
Фаза 4 (контролери, state, процесори)
    ↓
Фаза 5 (перевірка coverage)
```

---

## Виключення з coverage (реалізовано в pom.xml)

У `telemetry-processing-api-service/pom.xml` для goal `report` та `check` задано exclusions, щоб ліміт 85% за line coverage застосовувався лише до бізнес- та API-коду:

- **TelemetryProcessingApplication**
- **config/** (WebMvcConfig, KafkaConsumerConfig)
- **persistence/entity/**, **persistence/*Id.class**, **persistence/CarStatusRawWriter**, **persistence/RawTelemetryWriter**
- **aggregation/**, **lifecycle/**, **consumer/**, **processor/**
- **websocket/** (WebSocketController, WebSocketSessionManager, LiveDataBroadcaster тощо — можна додати тести пізніше)
- **idempotency/**, **persistence/repository/**

Покриття **≥ 85%** (line coverage) вимагається для решти класів: маппери, білдери, сервіси (Session/Lap/SessionSummary/WebSocketSubscription), контролери REST, state (SessionStateManager, SessionRuntimeState), RestExceptionHandler, SessionResolveService, exception.
