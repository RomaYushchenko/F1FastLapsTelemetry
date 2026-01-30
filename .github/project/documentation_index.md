# DOCUMENTATION INDEX

> Стислий ієрархічний зміст усіх `.md` файлів проєкту **F1 Telemetry**.  
> Формат: **H1–H4**, з короткими описами на рівні файлів.  
> Призначення: архітектурний reference та навігація для розробки.

---

## Which document to read first

### Рекомендований порядок читання

1. **mvp-requirements.md**  
   Формує ментальну модель системи: що входить у MVP, які обмеження та non-goals. Без цього документа неможливо коректно інтерпретувати всі інші контракти.

2. **f_1_telemetry_project_architecture.md**  
   Дає high-level уявлення про архітектуру, сервіси, data flow та зони відповідальності. Пояснює *як* система побудована.

3. **kafka_contracts_f_1_telemetry.md**  
   Нормативний контракт взаємодії між сервісами. Фіксує message-level API та правила доставки, ordering і еволюції.

4. **state_machines_specification_f_1_telemetry.md**  
   Формалізує поведінку processing layer. Дає відповідь *як система поводиться* у часі.

5. **telemetry_error_and_lifecycle_contract.md**  
   Уточнює lifecycle та error semantics між шарами. Синхронізує FSM, Kafka та API.

6. **rest_web_socket_api_contracts_f_1_telemetry.md**  
   Проєкція внутрішнього стану системи у зовнішній API та UI.

7. **react_spa_ui_architecture.md**  
   Архітектура React SPA: екрани, layout (wireframe), потік даних (REST + WebSocket), структура компонентів та як це виглядає для користувача.

---

## Documentation Dependency Graph

### Логічні залежності між документами

```
[mvp-requirements]
        |
        v
[f_1_telemetry_project_architecture]
        |
        v
[kafka_contracts_f_1_telemetry] <----------------+
        |                                          |
        v                                          |
[state_machines_specification_f_1_telemetry]      |
        |                                          |
        v                                          |
[telemetry_error_and_lifecycle_contract] ----------+
        |
        v
[rest_web_socket_api_contracts_f_1_telemetry]
        |
        v
[react_spa_ui_architecture]
```

### Пояснення

- `mvp-requirements.md` — корінь усієї документації.
- `f_1_telemetry_project_architecture.md` спирається на MVP-обмеження.
- `kafka_contracts_f_1_telemetry.md` є базовим комунікаційним контрактом.
- `state_machines_specification_f_1_telemetry.md` реалізує поведінку поверх Kafka контрактів.
- `telemetry_error_and_lifecycle_contract.md` уточнює та доповнює FSM.
- `rest_web_socket_api_contracts_f_1_telemetry.md` є read-model проєкцією внутрішнього стану.
- `react_spa_ui_architecture.md` описує реалізацію клієнта (React SPA) на основі REST/WS контрактів.

---

## react_spa_ui_architecture.md

**Призначення документа**  
Опис архітектури React SPA: місце UI в системі, сторінки (екрани), маршрути, layout (wireframe), потік даних (REST + WebSocket), компонентна структура та обробка помилок.

**Роль в архітектурі (glossary-aligned):**
- *UI architecture & UX reference*;
- single source of truth для зовнішнього вигляду та поведінки SPA;
- узгодження з REST/WS контрактами.

**Використовується для:**
- реалізації React SPA (сторінки, компоненти, хуки);
- планування UI-кроків у implementation_steps_plan;
- онбордингу фронтенд-розробників.

### 1. Місце UI в архітектурі системи
### 2. Технологічний стек та структура проєкту
### 3. Сторінки (екрани) та маршрути
### 4. Опис екранів та зовнішній вигляд (layout)
### 5. Потік даних (data flow)
### 6. Компонентна структура
### 7. Обробка помилок та крайні стани
### 8. Reconnect та узгодження з backend
### 9. Нефункціональні вимоги (UI)
### 10. Посилання на суміжні документи

---

## telemetry_error_and_lifecycle_contract.md

**Призначення документа**  
Контракт життєвого циклу сесії та помилок між ingest, processing, API та UI. Документ задає єдині правила інтерпретації станів, завершення сесій і сигналів.

**Використовується для:**
- реалізації session lifecycle у processing service;
- узгодження поведінки API та UI;
- формалізації timeout та error semantics.

### 1. Session Lifecycle Contract
#### 1.1 Source of Truth
#### 1.2 State Machine
#### 1.3 State Definitions
#### 1.4 Event Contract

### 2. Error Semantics
#### 2.1 Error Categories
#### 2.2 Error Event Contract
#### 2.3 Error Severity

### 3. Timeout → endReason Mapping
#### 3.1 Timeout Types
#### 3.2 endReason Enum
#### 3.3 Mapping Rules

### 4. Signals
#### 4.1 Kafka → API Signals
#### 4.2 API → UI Signals
#### 4.3 Monitoring Signals

### 5. Invariants

### 6. Versioning

---

## kafka_contracts_f_1_telemetry.md

**Призначення документа**  
Нормативний контракт Kafka-повідомлень між сервісами. Визначає envelope, payload-и, ordering, idempotency та еволюцію схем.

**Використовується для:**
- реалізації Kafka producers / consumers;
- контролю breaking / non-breaking змін;
- гарантії узгодженості даних між сервісами.

### 1. Purpose
#### 1.1 Scope
#### 1.2 Terminology

### 2. General Rules

### 3. Topics

### 4. Kafka Message Envelope
#### 4.1 Envelope Schema
#### 4.2 Field Description

### 5. Payload Contracts
#### 5.1 SessionEventDto
#### 5.2 LapDto
#### 5.3 CarTelemetryDto
#### 5.4 CarStatusDto

### 6. Session Lifecycle – Extended Contract
#### 6.1 Packets Before SSTA
#### 6.2 Packets During ACTIVE
#### 6.3 SEND / SSTA Semantics
#### 6.4 Session Timeout (Pseudo-event)

### 7. Flashback / Rewind Contract
#### 7.1 Detection
#### 7.2 Payload
#### 7.3 Processing Behavior

### 8. Schema Evolution Contract
#### 8.1 Non-breaking Changes
#### 8.2 Breaking Changes

### 9. Ordering & Idempotency
#### 9.1 Ordering Model
#### 9.2 Idempotency Rules

### 10. Versioning Strategy

### 11. Error Handling

### 12. Error Handling Contract
#### 12.1 Error / DLQ Topics
#### 12.2 Error Payload Schema
#### 12.3 Error Classification

### 13. Ordering & Out-of-Order Frames
#### 13.1 Ordering Model
#### 13.2 Reorder Window
#### 13.3 Watermark Semantics

### 14. Multi-Car Messaging (Future)
#### 14.1 Current State (MVP)
#### 14.2 Batch Payload

### 15. Compatibility Rules

### 16. Status

### 17. Future Extensions

---

## f_1_telemetry_project_architecture.md

**Призначення документа**  
Описує загальну архітектуру проєкту, data flow, сервіси, БД та інфраструктурні рішення.

**Використовується для:**
- розуміння high-level архітектури;
- онбордингу розробників;
- прийняття технічних рішень щодо масштабування.

### 1. General Overview

### 2. Architectural Principles
#### 2.1 Architectural Style
#### 2.2 Key Patterns

### 3. Service Decomposition
#### 3.1 UDP Ingest Service
#### 3.2 Telemetry Processing & API Service
#### 3.3 UI (Web)
#### 3.4 Service Layout (MVP)

### 4. Multi-module Maven Structure
#### 4.1 telemetry-contracts
#### 4.2 telemetry-parser-f125

### 5. Communication & Data Flow
#### 5.1 High-level Flow
#### 5.2 Session Start Sequence
#### 5.3 Live Telemetry Flow
#### 5.4 Lap Aggregation Flow
#### 5.5 Session Finish Flow

### 6. Kafka Design
#### 6.1 Topics
#### 6.2 Key Strategy
#### 6.3 Message Envelope

### 7. Batching & Performance
#### 7.1 Kafka Producer
#### 7.2 Database
#### 7.3 WebSocket

### 8. Session Lifecycle
#### 8.1 Main Flow
#### 8.2 Fallback (Timeout)
#### 8.3 Packets Before SSTA
#### 8.4 Packets During ACTIVE
#### 8.5 Packets After FINISHED

### 9. Data Storage & DDL
#### 9.1 TimescaleDB Initialization
#### 9.2 Schema
#### 9.3 Sessions
#### 9.4 Session Cars
#### 9.5 Idempotency Tables
#### 9.6 Raw Telemetry
#### 9.7 Aggregates
#### 9.8 Retention & Compression

### 10. Kafka Contracts (DTO)

### 11. REST + WebSocket API Contracts
#### 11.1 REST API
#### 11.2 WebSocket API

### 12. Internal State Machines
#### 12.1 Session State Machine
#### 12.2 Lap State Machine
#### 12.3 Aggregation Rules

### 13. API Overview

### 14. Single-car → Multi-car Evolution

### 15. Deployment

### 16. Database Schema

### 17. TimescaleDB Raw Telemetry

### 18. PostgreSQL Aggregates & Metadata

### 19. Indexes

### 20. Schema Evolution

### 21. Aggregation Contract
#### 21.1 Data Layers
#### 21.2 Finalization Rules

### 22. Reorder & Out-of-Order Frames

### 23. Flashback / Rewind

### 24. UDP Packet Loss Formalization

### 25. Observability Contract

---

## state_machines_specification_f_1_telemetry.md

**Призначення документа**  
Нормативна специфікація всіх state machines у системі. Формалізує поведінку processing layer у вигляді детермінованих FSM.

**Роль в архітектурі (glossary-aligned):**
- *Lifecycle authority* для session / lap / timeout / flashback;
- джерело істини для reducer‑логіки;
- основа для тестування та валідації переходів станів.

**Використовується для:**
- реалізації processing runtime state;
- синхронізації поведінки між Kafka, DB та API;
- перевірки edge‑cases (late packets, timeout, rewind).

### 0. Terminology & Inputs
#### 0.1 Identifiers
#### 0.2 Input Signals
#### 0.3 Runtime State

### 1. Session Lifecycle FSM
#### 1.1 States
#### 1.2 State Diagram
#### 1.3 Transitions & Actions
#### 1.4 Finish Priority Rules
#### 1.5 Data After Finish
#### 1.6 API Mapping

### 2. Timeout FSM
#### 2.1 Model
#### 2.2 Diagram
#### 2.3 SESSION_TIMEOUT Event

### 3. Lap Lifecycle FSM
#### 3.1 States
#### 3.2 Source of Truth
#### 3.3 Diagram
#### 3.4 Transitions & Actions
#### 3.5 Finalization Rules

### 4. Flashback / Rewind FSM
#### 4.1 Input
#### 4.2 Principles
#### 4.3 Diagram
#### 4.4 Invalidation Rules

### 5. Error → Lifecycle Mapping
#### 5.1 Error Categories
#### 5.2 Lifecycle Impact
#### 5.3 endReason Policy

### 6. Invariants

### 7. Reference Implementation (Java)
#### 7.1 Enums
#### 7.2 Session Reducer
#### 7.3 Watermark Policy
#### 7.4 Timeout Worker

### 8. Acceptance Scenarios

### 9. Synchronization Checklist

---

## rest_web_socket_api_contracts_f_1_telemetry.md

**Призначення документа**  
Контракт зовнішньої взаємодії з системою через REST та WebSocket. Формалізує read‑model та live‑stream інтерфейси.

**Роль в архітектурі (glossary-aligned):**
- *Read API contract* поверх агрегованих даних;
- *Live projection channel* для UI;
- проєкція processing state у клієнтські сигнали.

**Використовується для:**
- реалізації backend API;
- розробки React SPA;
- гарантії узгодженості між lifecycle state та UI.

### 1. API Overview

### 2. REST API

### 3. WebSocket API

### 4. Session Discovery

### 5. Session State Model

### 6. Reconnect Contract
#### 6.1 Snapshot on Connect
#### 6.2 Last Known State
#### 6.3 Mid-lap Reconnect

### 7. Unfinished / Timeout-ended Sessions

### 8. API vs Aggregates Consistency

### 9. Multi-car Support

---

## mvp-requirements.md

**Призначення документа**  
Фіксує scope MVP, функціональні та нефункціональні межі системи. Визначає, що система *робить* і що *свідомо не робить*.

**Роль в архітектурі (glossary-aligned):**
- *Scope boundary document*;
- джерело non‑goals та допущень;
- захист від архітектурного over‑engineering.

**Використовується для:**
- планування реалізації MVP;
- прийняття рішень щодо edge‑cases;
- комунікації очікувань між розробниками.

### 1. Project Goal

### 2. Technology Stack

### 3. Data Source & Protocol

### 4. High-level Architecture

### 5. MVP Functional Requirements
#### 5.1 Data Ingestion
#### 5.2 Kafka Topics
#### 5.3 Persistence
#### 5.4 Analytics
#### 5.5 API

### 6. Frontend (SPA)

### 7. Non-functional Requirements

### 8. MVP Readiness Criteria

### 9. Scope & Expectations
#### 9.1 Out of Scope
#### 9.2 Ignored Edge-cases

### 10. Packet Loss Constraints

### 11. Aggregation Accuracy Guarantees

### 12. Non-goals

### 13. Session Lifecycle Rules

### 14. Observability & Monitoring

