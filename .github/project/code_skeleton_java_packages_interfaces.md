# Code Skeleton – Java Packages & Interfaces

Документ описує **каркас коду (code skeleton)** для реалізації проєкту **F1 Telemetry**, узгоджений з поточною архітектурою, Kafka/REST контрактами та DDL.

Фокус:
- практичний Spring Boot підхід;
- можливість логічно розбивати код на business layers;
- готовність до масштабування та розширення.

---

## 1. Загальна структура репозиторію (Maven multi-module)

```
f1-telemetry
├── telemetry-api-contracts
├── udp-ingest-service
├── telemetry-processing-api-service
└── infra
```
- Watermark logic:
    - `SessionStateManager`.
- Lap boundaries:
    - `LapAggregator`.
- Thread-safety:
    - `SessionRuntimeState` має бути thread-safe.
- Persistence:
    - async + batch дозволені.
- UDP packet parsing:
    - `switch(packetId)`.
- DTO changes:
    - дозволені без DB міграцій (nullable fields).
- `ts`:
- derived з `sessionTime`.
- Frame gaps:
    - допустимі.
- Retention:
    - raw не видаляється до FINISHED.
- Aggregates → raw linkage:
    - через `frameIdentifier`.
- Confidence flag:
    - зберігається у `laps` та `session_summary`.
---

## 2. telemetry-api-contracts (shared module)

**Призначення:**
- спільні DTO для Kafka / REST / WebSocket;
- enum-и, які є частиною контракту;
- не містить Spring, Kafka, DB залежностей.

```
telemetry-api-contracts
└── com.ua.yushchenko.f1.fastlaps.telemetry
    ├── api
    │   ├── kafka
    │   │   ├── KafkaEnvelope.java
    │   │   ├── PacketId.java
    │   │   └── EventCode.java
    │   ├── rest
    │   │   ├── SessionDto.java
    │   │   ├── LapDto.java
    │   │   └── SessionSummaryDto.java
    │   └── ws
    │       ├── WsSubscribeMessage.java
    │       ├── WsSnapshotMessage.java
    │       └── WsSessionEndedMessage.java
    └── common
        └── SchemaVersion.java
```

**Принципи:**
- DTO = прості POJO + Lombok (`@Data` / `@Value`);
- жодної бізнес-логіки.

---

## 3. udp-ingest-service

**Роль:** прийом UDP, парсинг, публікація в Kafka.

```
udp-ingest-service
└── com.ua.yushchenko.f1.fastlaps.telemetry.ingest
    ├── config
    │   ├── KafkaProducerConfig.java
    │   └── UdpServerConfig.java
    │
    ├── udp
    │   ├── UdpServer.java
    │   ├── UdpPacketListener.java
    │   └── UdpPacketDispatcher.java
    │
    ├── parser
    │   ├── PacketParser.java
    │   ├── CarTelemetryParser.java
    │   ├── LapDataParser.java
    │   └── SessionEventParser.java
    │
    ├── kafka
    │   ├── TelemetryKafkaProducer.java
    │   └── TopicResolver.java
    │
    └── mapper
        └── PacketToEnvelopeMapper.java
```

### Ключові інтерфейси

```java
public interface PacketParser<T> {
    boolean supports(int packetId);
    T parse(ByteBuffer buffer);
}
```

```java
public interface TelemetryKafkaProducer {
    void send(KafkaEnvelope<?> envelope);
}
```

---

## 4. telemetry-processing-api-service

Найбільший модуль. Поєднує Kafka consumers, агрегацію, persistence та API.

```
telemetry-processing-api-service
└── com.ua.yushchenko.f1.fastlaps.telemetry.processing
    ├── config
    │   ├── KafkaConsumerConfig.java
    │   ├── WebSocketConfig.java
    │   └── ExecutorConfig.java
    │
    ├── kafka
    │   ├── consumer
    │   │   ├── SessionEventConsumer.java
    │   │   ├── LapConsumer.java
    │   │   ├── CarTelemetryConsumer.java
    │   │   └── CarStatusConsumer.java
    │   └── dispatcher
    │       └── KafkaMessageDispatcher.java
    │
    ├── lifecycle
    │   ├── SessionLifecycleService.java
    │   └── NoDataTimeoutWorker.java
    │
    ├── state
    │   ├── SessionStateManager.java
    │   ├── SessionRuntimeState.java
    │   └── LapRuntimeState.java
    │
    ├── aggregation
    │   ├── TelemetryAggregator.java
    │   ├── LapAggregator.java
    │   └── SessionSummaryAggregator.java
    │
    ├── persistence
    │   ├── write
    │   │   ├── RawTelemetryWriter.java
    │   │   ├── LapWriteRepository.java
    │   │   └── SessionSummaryWriteRepository.java
    │   ├── read
    │   │   ├── SessionReadRepository.java
    │   │   └── LapReadRepository.java
    │   └── idempotency
    │       └── ProcessedPacketRepository.java
    │
    ├── api
    │   ├── rest
    │   │   ├── SessionController.java
    │   │   ├── LapController.java
    │   │   └── SessionSummaryController.java
    │   └── ws
    │       ├── LiveTelemetryWsController.java
    │       └── WsMessageRouter.java
    │
    └── mapper
        └── DomainToDtoMapper.java
```

---

## 5. CQRS розділення

### Write side
- Kafka consumers
- Aggregators
- `RawTelemetryWriter` (batch insert у Timescale)
- `LapWriteRepository`

### Read side
- REST controllers
- `SessionReadRepository`
- optimized SELECT-запити

---

## 6. Session lifecycle & state

```java
public interface SessionLifecycleService {
    void onSessionStarted(SessionEventDto event);
    void onSessionEnded(SessionEventDto event);
}
```

```java
public interface SessionStateManager {
    SessionRuntimeState getOrCreate(long sessionUid);
    void close(long sessionUid);
}
```

---

## 7. WebSocket live pipeline

Flow:
- Kafka consumer → Aggregator → SessionRuntimeState
- Scheduler (10 Hz) → SnapshotBuilder → STOMP broadcast

```java
public interface LiveSnapshotPublisher {
    void publish(SessionRuntimeState state);
}
```

---

## 8. Infra module

```
infra
├── docker-compose.yml
├── kafka
├── postgres
└── scripts
```

---

## 9. Що свідомо НЕ входить у skeleton

- тестова інфраструктура
- security / auth
- replay / flashback
- multi-user routing

---

## 10. Як використовувати цей skeleton

1. Реалізувати `telemetry-api-contracts`
2. Підняти `udp-ingest-service`
3. Реалізувати Kafka consumers + state
4. Додати persistence
5. Підключити UI

Skeleton є **blueprint**, а не обмеженням.

