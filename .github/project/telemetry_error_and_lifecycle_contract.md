# Telemetry Error and Lifecycle Contract

## Мета документа

Цей документ формалізує **контракти життєвого циклу сесії, семантику помилок та сигналів** у проєкті **F1 Telemetry**.

Документ є доповненням до:
- `kafka_contracts_f_1_telemetry.md`
- `rest_web_socket_api_contracts_f_1_telemetry.md`

Мета — зробити обробку телеметрії, помилок і завершення сесій **детермінованою, прозорою та однаково інтерпретованою** між ingest, processing, API та UI шарами.

---

## 1. Session Lifecycle Contract (State Machine як API)

### 1.1 Джерело істини

- Джерелом істини життєвого циклу є **події `telemetry.session`**
- Стан сесії визначається **послідовністю подій**, а не наявністю/відсутністю даних

- Source of truth lifecycle:
    - **internal processing state**.

- Timeout:
    - глобальний (configurable).

- FINISHED → ACTIVE:
    - **заборонено**.

- Aggregation error:
    - може змінити lifecycle state (наприклад → UNRELIABLE).

---

### 1.2 State Machine

```text
┌──────────┐
│ INIT     │
└────┬─────┘
     │ SSTA
     ▼
┌──────────┐
│ ACTIVE   │◄───────────────┐
└────┬─────┘                │
     │ SEND                 │ Flashback
     │ Timeout              │ (rewind)
     ▼                      │
┌──────────┐                │
│ ENDING   │────────────────┘
└────┬─────┘
     ▼
┌──────────┐
│ TERMINAL │
└──────────┘
```

---

### 1.3 Опис станів

| State | Опис |
|------|------|
| INIT | Сесія відома, але не активна |
| ACTIVE | Отримано `SSTA`, приймаються всі пакети |
| ENDING | Сесія завершується (SEND або timeout) |
| TERMINAL | Сесія фіналізована, стан immutable |

---

### 1.4 Контракт подій

| Event | Source | Effect |
|------|--------|--------|
| SSTA | UDP → Kafka | INIT → ACTIVE |
| SEND | UDP → Kafka | ACTIVE → ENDING |
| TIMEOUT | Processing | ACTIVE → ENDING |
| FINALIZED | Processing | ENDING → TERMINAL |

---

## 2. Error Semantics між сервісами

### 2.1 Класи помилок

| Category | Опис | Retry |
|--------|------|-------|
| INGEST_MALFORMED | Некоректний UDP payload | No |
| CONTRACT_VIOLATION | Порушення Kafka контракту | No |
| OUT_OF_ORDER | Out-of-order frameIdentifier | Yes (soft) |
| PROCESSING_FAILURE | Внутрішня помилка агрегації | Yes |
| STORAGE_FAILURE | Помилка БД | Yes |

---

### 2.2 Error Event Contract

Topic: `telemetry.errors`

```json
{
  "schemaVersion": 1,
  "errorCode": "CONTRACT_VIOLATION",
  "severity": "ERROR",
  "sessionUID": 123456789,
  "frameIdentifier": 10234,
  "packetId": "CAR_TELEMETRY",
  "message": "Unexpected null field",
  "producedAt": "2026-01-29T10:15:00Z"
}
```

---

### 2.3 Error Severity

| Severity | Meaning |
|--------|---------|
| INFO | Діагностичний сигнал |
| WARN | Дані частково деградовані |
| ERROR | Дані непридатні |
| FATAL | Сесія не може продовжуватись |

---

## 3. Timeout → endReason Mapping

### 3.1 Timeout типи

| Timeout | Опис |
|--------|------|
| NO_DATA | Відсутність UDP пакетів |
| INACTIVITY | Відсутність прогресу frameIdentifier |
| GRACEFUL_WAIT | Очікування SEND після inactivity |

---

### 3.2 endReason Enum

```text
EVENT_SEND
TIMEOUT_NO_DATA
TIMEOUT_INACTIVITY
FORCED_TERMINATION
```

---

### 3.3 Mapping

| Condition | endReason |
|---------|-----------|
| SEND received | EVENT_SEND |
| No UDP > T1 | TIMEOUT_NO_DATA |
| No frame progress > T2 | TIMEOUT_INACTIVITY |
| Fatal error | FORCED_TERMINATION |

---

## 4. Signals для UI / API / Monitoring

### 4.1 Kafka → API Signals

| Signal | Meaning |
|------|---------|
| SESSION_STARTED | Сесія активна |
| SESSION_ENDING | Початок завершення |
| SESSION_ENDED | TERMINAL стан |
| DATA_DEGRADED | Часткові втрати |

---

### 4.2 API → UI Signals

WebSocket `type`:

```text
SESSION_STARTED
SESSION_ENDING
SESSION_ENDED
ERROR
WARNING
```

---

### 4.3 Monitoring Signals

| Metric | Description |
|-------|-------------|
| telemetry_udp_drops | Втрачені UDP пакети |
| telemetry_out_of_order | Out-of-order кадри |
| telemetry_processing_lag | Kafka → DB lag |
| telemetry_session_timeouts | Кількість timeout |

---

## 5. Invariants

- TERMINAL сесія **ніколи не змінюється**
- Дані після ENDING **можуть бути проігноровані**
- Flashback **не скидає** sessionUID
- Помилки **не змінюють** state напряму

---

## 6. Versioning

- Контракт версіонується незалежно
- Будь-яка зміна state machine = **breaking change**
- Додавання нових errorCode = non-breaking

