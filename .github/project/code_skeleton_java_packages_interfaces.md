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
├── f1-telemetry-udp-core
├── f1-telemetry-udp-spring
├── f1-telemetry-udp-starter
├── udp-ingest-service
├── telemetry-processing-api-service
└── infra
```

**Ключові модулі:**

| Модуль | Призначення | Залежності |
|--------|-------------|-----------|
| **telemetry-api-contracts** | Спільні DTO, контракти | Lombok, validation |
| **f1-telemetry-udp-core** | UDP listener, dispatcher (pure Java) | SLF4J, Lombok |
| **f1-telemetry-udp-spring** | Spring integration для UDP | Spring Context, core |
| **f1-telemetry-udp-starter** | AutoConfiguration | Spring Boot, spring module |
| **udp-ingest-service** | Business handlers для UDP | UDP starter, contracts, Kafka |
| **telemetry-processing-api-service** | Kafka consumers, persistence, API | Spring Boot, Kafka, JPA |
| **infra** | Docker Compose, SQL scripts | N/A |

**Детальна архітектура UDP модулів:** [udp_telemetry_ingest_as_reusable_library_implementation_guide.md](udp_telemetry_ingest_as_reusable_library_implementation_guide.md)

---

## 1.1 Архітектурні правила
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

## 3. f1-telemetry-udp-core (pure Java library)

**Роль:** UDP packet reception, header decoding, dispatching (no Spring).

```
f1-telemetry-udp-core
└── com.ua.yushchenko.f1.fastlaps.telemetry.udp.core
    ├── listener
    │   ├── UdpTelemetryListener.java
    │   └── UdpListenerConfig.java
    │
    ├── packet
    │   ├── PacketHeader.java
    │   └── PacketHeaderDecoder.java
    │
    ├── dispatcher
    │   ├── UdpPacketDispatcher.java
    │   ├── SimpleUdpPacketDispatcher.java
    │   └── UdpPacketConsumer.java
    │
    └── exception
        └── PacketDecodingException.java
```

### Ключові інтерфейси

```java
public interface UdpPacketConsumer {
    short packetId();
    void handle(PacketHeader header, ByteBuffer payload);
}
```

```java
public interface UdpPacketDispatcher {
    void dispatch(PacketHeader header, ByteBuffer payload);
    void registerConsumer(UdpPacketConsumer consumer);
}
```

---

## 4. f1-telemetry-udp-spring (Spring integration)

**Роль:** Spring annotations, BeanPostProcessor, method adapter.

```
f1-telemetry-udp-spring
└── com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring
    ├── annotation
    │   ├── F1UdpListener.java
    │   └── F1PacketHandler.java
    │
    ├── adapter
    │   └── MethodPacketHandler.java
    │
    ├── processor
    │   └── F1PacketHandlerPostProcessor.java
    │
    ├── registry
    │   └── PacketHandlerRegistry.java
    │
    └── config
        └── UdpListenerConfiguration.java
```

### Ключові класи

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface F1UdpListener {}
```

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface F1PacketHandler {
    short packetId();
}
```

---

## 5. f1-telemetry-udp-starter (Spring Boot autoconfiguration)

**Роль:** автоконфігурація UDP listener.

```
f1-telemetry-udp-starter
└── com.ua.yushchenko.f1.fastlaps.telemetry.udp.starter
    ├── autoconfigure
    │   ├── UdpTelemetryAutoConfiguration.java
    │   └── UdpTelemetryProperties.java
    │
    └── lifecycle
        └── UdpListenerLifecycleManager.java
```

**spring.factories / AutoConfiguration.imports:**
```
com.ua.yushchenko.f1.fastlaps.telemetry.udp.starter.autoconfigure.UdpTelemetryAutoConfiguration
```

---

## 6. udp-ingest-service (business logic)

**Роль:** packet handlers, parsing, Kafka publishing.

```
udp-ingest-service
└── com.ua.yushchenko.f1.fastlaps.telemetry.ingest
    ├── config
    │   ├── KafkaProducerConfig.java
    │   └── TelemetryPublisherConfig.java
    │
    ├── parser
    │   ├── SessionPacketParser.java
    │   ├── LapDataPacketParser.java
    │   ├── CarTelemetryPacketParser.java
    │   └── CarStatusPacketParser.java
    │
    ├── handler
    │   ├── SessionPacketHandler.java       // @F1UdpListener + @F1PacketHandler
    │   ├── LapPacketHandler.java
    │   ├── TelemetryPacketHandler.java
    │   └── StatusPacketHandler.java
    │
    ├── publisher
    │   ├── TelemetryPublisher.java
    │   ├── KafkaTelemetryPublisher.java
    │   ├── RetryingPublisher.java          // Decorator
    │   └── ThrottlingPublisher.java        // Decorator
    │
    ├── mapper
    │   └── PacketToEnvelopeMapper.java
    │
    └── topic
        └── TopicResolver.java
```

### Приклад handler-класу

```java
@F1UdpListener
public class SessionPacketHandler {
    
    private final TelemetryPublisher publisher;
    private final PacketToEnvelopeMapper mapper;
    
    @F1PacketHandler(packetId = 1)  // Session packet
    public void onSessionPacket(PacketHeader header, ByteBuffer payload) {
        SessionEventDto event = SessionPacketParser.parse(payload);
        KafkaEnvelope<SessionEventDto> envelope = mapper.toEnvelope(header, event);
        String topic = TopicResolver.resolve(event);
        publisher.publish(topic, header.getSessionUID(), envelope);
    }
}
```

### Ключові інтерфейси

```java
public interface TelemetryPublisher {
    void publish(String topic, String key, Object value);
}
```

---

## 7. telemetry-processing-api-service

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

