# State Machines Specification — F1 Telemetry

Цей документ формалізує **state machines** для проєкту F1 Telemetry так, щоб реалізація була **детермінованою** і однаково інтерпретованою між ingest → processing → storage → API → UI.

Scope:
- Session lifecycle FSM
- Lap lifecycle FSM (per `(sessionUID, carIndex)`)
- Error → lifecycle mapping
- Timeout FSM / timers
- Flashback (rewind) FSM
- Invariants + приклади коду

---

## 0. Терміни та вхідні сигнали

### 0.1 Ідентифікатори

- `sessionUID` — унікальний ідентифікатор сесії (UDP header).
- `carIndex` — індекс авто (MVP: player car, але модель готова до multi-car).
- `overallFrameIdentifier` — primary ordering key.
- `frameIdentifier` — fallback ordering key.
- `sessionTime` — source of truth для часу (UDP header, seconds).

### 0.2 Вхідні сигнали (events)

Kafka (`telemetry.session`) eventCode:
- `SSTA` — session started
- `SEND` — session ended
- `SESSION_TIMEOUT` — pseudo-event, **але публікується в Kafka**
- `FLBK` — flashback/rewind event

Kafka data topics (envelope + payload):
- `telemetry.lap` (LapData)
- `telemetry.carTelemetry` (CarTelemetry)
- `telemetry.carStatus` (CarStatus)

### 0.3 Станові змінні (processing runtime)

Per session:
- `sessionState` — SessionState enum
- `startedAt`, `endedAt`, `endReason`
- `watermark[carIndex]` — мінімальний `overallFrameIdentifier`, що дозволено для агрегації
- `lastSeenFrame[carIndex]`
- `lastSeenAt` (server time) — для NO_DATA timer
- `confidence` / `qualityFlags`

Per lap (per carIndex):
- `lapState`
- `currentLapNumber`
- `sectorTimes` (S1/S2/S3)
- `lapTimeMs`
- `isInvalid`

---

## 1. Session Lifecycle FSM

### 1.1 Стани

```text
INIT      — сесія невідома або ще не стартувала в processing
ACTIVE    — отримано SSTA, приймаємо й агрегуємо
ENDING    — отримано SEND/SESSION_TIMEOUT, завершуємо flush/finalize
TERMINAL  — фіналізовано, immutable
```

> MVP інтерпретація: `TERMINAL` = `FINISHED` у REST/API.

### 1.2 Діаграма станів

```text
             +----------------+
             |      INIT      |
             +--------+-------+
                      |
                      | on SSTA(sessionUID)
                      v
             +----------------+
             |     ACTIVE     |
             +---+--------+---+
                 |        |
                 |        | on FLBK(sessionUID)
                 |        | (does NOT change state)
                 |        v
                 |   [Flashback handler]
                 |
                 |
   on SEND        | on SESSION_TIMEOUT(NO_DATA)
   (eventCode)    |
                 v
             +----------------+
             |     ENDING     |
             +--------+-------+
                      |
                      | on FINALIZED (internal completion)
                      v
             +----------------+
             |    TERMINAL    |
             +----------------+

Rules:
- INIT -> ACTIVE тільки по SSTA
- ACTIVE -> ENDING по SEND або SESSION_TIMEOUT
- ENDING -> TERMINAL тільки після finalize+flush
- TERMINAL не має виходів
```

### 1.3 Тригери та дії (transition table)

| From | Trigger | Guard | To | Actions |
|---|---|---|---|---|
| INIT | `SSTA` | `sessionUID` not TERMINAL | ACTIVE | create runtime state; upsert `sessions.started_at`; init watermark per carIndex; publish API/UI `SESSION_STARTED` |
| INIT | data packet (`Lap/Telemetry/Status`) | always | INIT | ignore + metric `packets_before_ssta_total` |
| ACTIVE | data packet | frame >= watermark | ACTIVE | update `lastSeenAt/lastSeenFrame`; aggregate; write raw; schedule WS snapshot |
| ACTIVE | data packet | frame < watermark | ACTIVE | write raw; do NOT change aggregates; metric `frames_below_watermark_total` |
| ACTIVE | `SEND` | none | ENDING | mark endReason=`EVENT_SEND`; set endedAt; stop accepting late packets; close WS with `SESSION_ENDING` |
| ACTIVE | `SESSION_TIMEOUT` | none | ENDING | mark endReason=`NO_DATA_TIMEOUT`; set endedAt; close WS with `SESSION_ENDING` |
| ENDING | any data packet | always | ENDING | ignore + metric `late_packets_after_finish_total` |
| ENDING | internal finalize complete | none | TERMINAL | persist final aggregates; publish `SESSION_ENDED` to WS; finalize DB state |
| TERMINAL | any event | always | TERMINAL | ignore (idempotent) |

### 1.4 Пріоритет завершення

Правило: **перший завершуючий тригер виграє**.

- Якщо `ACTIVE → ENDING` вже відбулося по `SESSION_TIMEOUT`, то пізній `SEND` **ігнорується**.
- Якщо `ACTIVE → ENDING` відбулося по `SEND`, то пізній `SESSION_TIMEOUT` **ігнорується**.

### 1.5 Дані після завершення

- Після переходу у `ENDING`:
  - raw НЕ пишемо;
  - агрегати НЕ змінюємо;
  - лише метрики + debug logs.

### 1.6 Відображення в API

REST:
- `state = ACTIVE` коли `sessionState == ACTIVE`
- `state = FINISHED` коли `sessionState ∈ {ENDING, TERMINAL}`

WebSocket:
- `SESSION_STARTED` після INIT→ACTIVE
- `SESSION_ENDING` після ACTIVE→ENDING
- `SESSION_ENDED` після ENDING→TERMINAL

---

## 2. Timeout FSM (NO_DATA)

### 2.1 Модель

- Таймер глобальний: `NO_DATA_TIMEOUT_SECONDS`.
- Перезапускається при отриманні **будь-якого** packet/event для sessionUID у стані ACTIVE.

### 2.2 Діаграма

```text
ACTIVE session
  |
  | on any packet/event
  v
reset noDataTimer
  |
  | if now - lastSeenAt > NO_DATA_TIMEOUT
  v
emit SESSION_TIMEOUT (Kafka telemetry.session)
  |
  v
transition ACTIVE -> ENDING
```

### 2.3 Генерація `SESSION_TIMEOUT` (Kafka)

- Producer: processing service
- Topic: `telemetry.session`
- Key: `sessionUID`
- Payload:

```json
{
  "eventCode": "SESSION_TIMEOUT",
  "reason": "NO_DATA",
  "timeoutSeconds": 30
}
```

---

## 3. Lap Lifecycle FSM (per `(sessionUID, carIndex)`)

### 3.1 Стани

```text
NOT_STARTED — lapNumber ще не визначено
IN_PROGRESS — йде поточне коло
LAP_FINALIZING — detected lap transition або session ENDING
COMPLETED — коло записано (upsert) і immutable
```

> Сектора можна вести в IN_PROGRESS без окремого FSM для sector, або як вкладений mini-state.

### 3.2 Джерело істини для lap/sector

- LapData Packet (UDP) як source of truth:
  - `lapNumber`
  - `currentLapTimeMs`
  - sector times / sector index
  - invalid flags

### 3.3 Діаграма

```text
+-------------+
| NOT_STARTED |
+------+------+
       |
       | on LapData with lapNumber = N
       v
+-------------+
| IN_PROGRESS |
+------+------+
       |
       | on sector completion (derived from LapData)
       | update sector times
       |
       | on lap transition (lapNumber changes N -> N+1)
       v
+---------------+
| LAP_FINALIZING|
+-------+-------+
        |
        | finalize + upsert laps(N)
        v
+-------------+
| COMPLETED   |
+-------------+

Special:
- If session enters ENDING while IN_PROGRESS -> go LAP_FINALIZING
```

### 3.4 Тригери та дії

| From | Trigger | Guard | To | Actions |
|---|---|---|---|---|
| NOT_STARTED | LapData | sessionState==ACTIVE | IN_PROGRESS | init lap runtime (lapNumber, start ts from sessionTime) |
| IN_PROGRESS | LapData | same lapNumber | IN_PROGRESS | update lapTime/sector info/isInvalid |
| IN_PROGRESS | LapData | lapNumber increased | LAP_FINALIZING | finalize previous lap; persist; update summary |
| IN_PROGRESS | sessionState→ENDING | none | LAP_FINALIZING | finalize current lap best-effort |
| LAP_FINALIZING | finalize done | none | COMPLETED | state transitions |

### 3.5 Правила фіналізації

- Lap вважається завершеним, коли:
  - `lapNumber` збільшився (N → N+1), або
  - сесія перейшла в `ENDING`.

- Якщо дані неповні:
  - `sector3` може бути `null`;
  - `confidence` встановлюється `LOW`.

---

## 4. Flashback / Rewind FSM

### 4.1 Вхід

Flashback детектиться при:
- Kafka `FLBK` event, або
- regression `overallFrameIdentifier`.

### 4.2 Принцип

Flashback **не змінює Session lifecycle state**, але змінює допустимість агрегатів.

### 4.3 Діаграма

```text
ACTIVE
  |
  | on FLBK(rewindFrame)
  v
FLASHBACK_HANDLING (internal)
  |
  | invalidate aggregates where frame > rewindFrame
  | rollback in-memory lap/sector state
  | move watermark to rewindFrame
  v
ACTIVE (continue)
```

### 4.4 Правила invalidation

- Усі агрегати (lap/sector/summary) з `frameIdentifier > rewindFrame`:
  - позначаються invalidated;
  - перераховуються з checkpoint або best-effort.

> MVP дозволяє: recompute тільки in-memory, без повного replay raw.

---

## 5. Error → Lifecycle Mapping

### 5.1 Категорії помилок

- `INGEST_MALFORMED`
- `CONTRACT_VIOLATION`
- `OUT_OF_ORDER`
- `PROCESSING_FAILURE`
- `STORAGE_FAILURE`

### 5.2 Мапінг впливу на lifecycle

| Error category | Severity | Default impact | Lifecycle action |
|---|---:|---|---|
| INGEST_MALFORMED | WARN | data drop | no state change; metric + error topic |
| CONTRACT_VIOLATION | ERROR | cannot trust payload | mark session `UNRELIABLE`; may force ENDING if rate high |
| OUT_OF_ORDER | INFO/WARN | reorder within window | update metrics; no state change |
| PROCESSING_FAILURE | ERROR/FATAL | aggregator broken | переходити ACTIVE→ENDING з `endReason=FORCED_TERMINATION` |
| STORAGE_FAILURE | ERROR/FATAL | cannot persist | retry; after retries → DLQ; on fatal → ACTIVE→ENDING `FORCED_TERMINATION` |

### 5.3 endReason policy

- `EVENT_SEND` — якщо завершення по SEND
- `NO_DATA_TIMEOUT` — якщо завершення по SESSION_TIMEOUT
- `FORCED_TERMINATION` — якщо фатальна помилка (processing/storage)

---

## 6. Invariants (must-hold)

1. `TERMINAL` immutable: жодні події не можуть змінити агрегати/сесію.
2. `FINISHED → ACTIVE` заборонено.
3. Всі REST-відповіді з session містять `state`.
4. `sessionTime` (UDP) — source of truth для `ts` у raw.
5. `watermark` монотонний (крім flashback, де може бути знижений до rewindFrame).
6. Після `ENDING` — late packets не пишуться в raw.

---

## 7. Рекомендована реалізація (Java 17) — приклади

### 7.1 Enums

```java
public enum SessionState {
    INIT,
    ACTIVE,
    ENDING,
    TERMINAL
}

public enum EndReason {
    EVENT_SEND,
    NO_DATA_TIMEOUT,
    FORCED_TERMINATION
}

public enum LapState {
    NOT_STARTED,
    IN_PROGRESS,
    LAP_FINALIZING,
    COMPLETED
}
```

### 7.2 Session state reducer (детермінований)

```java
public final class SessionReducer {

    public SessionTransitionResult reduce(SessionRuntimeState s, SessionInputEvent e) {
        return switch (s.getSessionState()) {
            case INIT -> onInit(s, e);
            case ACTIVE -> onActive(s, e);
            case ENDING -> onEnding(s, e);
            case TERMINAL -> SessionTransitionResult.noop();
        };
    }

    private SessionTransitionResult onInit(SessionRuntimeState s, SessionInputEvent e) {
        if (e.type() == SessionInputEvent.Type.SSTA) {
            s.setSessionState(SessionState.ACTIVE);
            s.setStartedAt(e.producedAt());
            return SessionTransitionResult.transition("INIT->ACTIVE");
        }
        // packets before SSTA are ignored
        return SessionTransitionResult.ignored("before_ssta");
    }

    private SessionTransitionResult onActive(SessionRuntimeState s, SessionInputEvent e) {
        return switch (e.type()) {
            case SEND -> {
                s.setSessionState(SessionState.ENDING);
                s.setEndReason(EndReason.EVENT_SEND);
                s.setEndedAt(e.producedAt());
                yield SessionTransitionResult.transition("ACTIVE->ENDING:EVENT_SEND");
            }
            case SESSION_TIMEOUT -> {
                s.setSessionState(SessionState.ENDING);
                s.setEndReason(EndReason.NO_DATA_TIMEOUT);
                s.setEndedAt(e.producedAt());
                yield SessionTransitionResult.transition("ACTIVE->ENDING:NO_DATA_TIMEOUT");
            }
            case FLBK -> {
                // no state change, only internal handler
                yield SessionTransitionResult.internal("flashback");
            }
            case DATA_PACKET -> {
                s.setLastSeenAt(e.producedAt());
                yield SessionTransitionResult.noop();
            }
        };
    }

    private SessionTransitionResult onEnding(SessionRuntimeState s, SessionInputEvent e) {
        if (e.type() == SessionInputEvent.Type.FINALIZED) {
            s.setSessionState(SessionState.TERMINAL);
            return SessionTransitionResult.transition("ENDING->TERMINAL");
        }
        return SessionTransitionResult.ignored("late_event_or_packet");
    }
}
```

### 7.3 Watermark policy (per car)

```java
public final class WatermarkPolicy {

    public boolean canAffectAggregates(SessionRuntimeState s, int carIndex, int overallFrameIdentifier) {
        int wm = s.getWatermark(carIndex);
        return overallFrameIdentifier >= wm;
    }

    public void advanceWatermark(SessionRuntimeState s, int carIndex, int overallFrameIdentifier) {
        int wm = s.getWatermark(carIndex);
        if (overallFrameIdentifier > wm) {
            s.setWatermark(carIndex, overallFrameIdentifier);
        }
    }

    public void rewindWatermark(SessionRuntimeState s, int carIndex, int rewindFrame) {
        // allowed only during FLBK handling
        s.setWatermark(carIndex, rewindFrame);
    }
}
```

### 7.4 NO_DATA timeout worker

```java
@Scheduled(fixedDelayString = "${telemetry.noDataCheckMs:1000}")
public void checkNoDataTimeouts() {
    Instant now = Instant.now();

    for (SessionRuntimeState s : sessionStateManager.activeSessions()) {
        if (s.getSessionState() != SessionState.ACTIVE) continue;

        Duration silent = Duration.between(s.getLastSeenAt(), now);
        if (silent.getSeconds() >= noDataTimeoutSeconds) {
            // publish SESSION_TIMEOUT to Kafka (telemetry.session)
            sessionEventProducer.publishTimeout(s.getSessionUid(), noDataTimeoutSeconds);
            // reducer will move ACTIVE -> ENDING when timeout event is consumed
        }
    }
}
```

---

## 8. Контрольні сценарії (acceptance examples)

### 8.1 Practice → Quali → Race без рестарту

- Для кожної сесії приходить свій `sessionUID` і `SSTA`.
- Processing створює runtime state per session.
- По `SEND`/timeout кожна сесія переходить в TERMINAL, зберігається в DB.

### 8.2 SEND без LapData

- Сесія зберігається.
- `laps` пусто.
- `summary.totalLaps = 0`.
- `endReason = EVENT_SEND`.

### 8.3 Late SEND після timeout

- Сесія вже `ENDING/TERMINAL`.
- SEND ігнорується.

### 8.4 Flashback

- При `FLBK` watermark знижується до `rewindFrame`.
- Агрегати після rewindFrame інвалідовані.

---

## 9. Пункти, що мають бути синхронізовані в контрактах

- Kafka: додати `SESSION_TIMEOUT` як eventCode у `telemetry.session`.
- REST: `state` у всіх session responses.
- DDL: `confidence` у `laps` і `session_summary`.
- Error contract: `FORCED_TERMINATION` як endReason.

---

## Appendix A. Рекомендовані поля WS snapshot для sorting

Мінімум:
- `sessionUID`
- `carIndex`
- `sessionTime` (UDP)
- `overallFrameIdentifier`
- derived `timestamp` (optional)

Цього достатньо, щоб UI міг сортувати і відкидати застарілі кадри.

