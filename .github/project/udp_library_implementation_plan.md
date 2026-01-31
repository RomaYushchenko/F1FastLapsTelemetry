# UDP Telemetry Ingest Library — Detailed Implementation Plan

> **Purpose:** Step-by-step implementation guide for building a reusable UDP telemetry ingest library with Spring Boot starter.  
> **Based on:** [udp_telemetry_ingest_as_reusable_library_implementation_guide.md](udp_telemetry_ingest_as_reusable_library_implementation_guide.md)  
> **Tracking:** Mark each step as completed when done.

---

## Overview

This plan implements UDP packet ingestion as a **reusable library** with three modules:

1. **f1-telemetry-udp-core** — Pure Java (no Spring), handles DatagramChannel, packet decoding, dispatching
2. **f1-telemetry-udp-spring** — Spring integration with annotations and method scanning
3. **f1-telemetry-udp-starter** — Spring Boot autoconfiguration

**Key principle:** Business code uses declarative annotations (`@F1UdpListener`, `@F1PacketHandler`) and knows nothing about UDP internals.

---

## Phase 1: Core Module Setup (`f1-telemetry-udp-core`) ✅ COMPLETED

### Objective
Create a pure Java module with no Spring dependencies that can read UDP packets, decode headers, and dispatch to consumers.

| # | Task | Deliverable | Acceptance Criteria | Status |
|---|------|-------------|---------------------|--------|
| 1.1 | Create Maven module `f1-telemetry-udp-core` | `pom.xml` in `f1-telemetry-udp-core/` | Module builds with `mvn clean install` | ✅ |
| 1.2 | Add dependencies: SLF4J API, Lombok (optional) | Dependencies in `pom.xml` | No Spring dependencies present | ✅ |
| 1.3 | Create package structure | `com.ua.yushchenko.f1.fastlaps.telemetry.udp.core` | Package created | ✅ |
| 1.4 | Define `PacketHeader` record/class | Java class with fields: `packetFormat`, `gameYear`, `gameMajorVersion`, `gameMinorVersion`, `packetVersion`, `packetId`, `sessionUID`, `sessionTime`, `frameIdentifier`, `overallFrameIdentifier`, `playerCarIndex` | Class compiles, getters work | ✅ |
| 1.5 | Implement `PacketHeaderDecoder` | Static method `decode(ByteBuffer)` returns `PacketHeader` | Unit test with sample bytes passes | ✅ |
| 1.6 | Define `UdpPacketConsumer` interface | Interface with `short packetId()` and `void handle(PacketHeader, ByteBuffer)` | Interface compiles | ✅ |
| 1.7 | Define `UdpPacketDispatcher` interface | Interface with `void dispatch(PacketHeader, ByteBuffer)` | Interface compiles | ✅ |
| 1.8 | Implement `SimpleUdpPacketDispatcher` | Registry of `packetId → List<UdpPacketConsumer>`, dispatch to all matching consumers | Unit test: register consumer, dispatch, verify invoked | ✅ |
| 1.9 | Implement `UdpTelemetryListener` | Uses `DatagramChannel`, binds to port, reads packets in loop | Unit test: send UDP packet, verify received | ✅ |
| 1.10 | Wire listener → decoder → dispatcher | On packet received: decode header, dispatch to consumers | Integration test: send packet, verify consumer called | ✅ |
| 1.11 | Add error handling and logging | Catch exceptions in dispatch, log errors, continue processing | Test: consumer throws exception, listener continues | ✅ |
| 1.12 | Make listener lifecycle-aware | Methods: `start()`, `stop()`, `isRunning()` | Can start/stop multiple times | ✅ |

---

## Phase 2: Spring Integration Module (`f1-telemetry-udp-spring`) ✅ COMPLETED

### Objective
Enable declarative UDP packet handling through Spring annotations.

| # | Task | Deliverable | Acceptance Criteria | Status |
|---|------|-------------|---------------------|--------|
| 2.1 | Create Maven module `f1-telemetry-udp-spring` | `pom.xml` in `f1-telemetry-udp-spring/` | Module builds, depends on core + Spring Context | ✅ |
| 2.2 | Create package structure | `com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring` | Package created | ✅ |
| 2.3 | Define `@F1UdpListener` annotation | `@Target(TYPE)`, `@Retention(RUNTIME)`, `@Component` | Annotation compiles | ✅ |
| 2.4 | Define `@F1PacketHandler` annotation | `@Target(METHOD)`, `@Retention(RUNTIME)`, attribute `short packetId()` | Annotation compiles | ✅ |
| 2.5 | Implement `MethodPacketHandler` adapter | Implements `UdpPacketConsumer`, wraps method invocation | Unit test: invoke method via adapter | ✅ |
| 2.6 | Implement `PacketHandlerRegistry` | Bean that holds `List<UdpPacketConsumer>` | Can register and retrieve consumers | ✅ |
| 2.7 | Implement `F1PacketHandlerPostProcessor` | `BeanPostProcessor` that scans for `@F1UdpListener` and `@F1PacketHandler` | Test: register bean, verify methods added to registry | ✅ |
| 2.8 | Support method signatures | Support: `(PacketHeader, ByteBuffer)`, `(ByteBuffer)`, `(PacketHeader)` | Test all three signatures | ✅ |
| 2.9 | Implement error handling in adapter | Catch invocation exceptions, log with context | Test: method throws, exception logged | ✅ |
| 2.10 | Add validation | Validate `packetId` is unique per handler method | Test: duplicate packetId → exception on startup | ✅ |
| 2.11 | Wire registry → dispatcher | Configuration that creates `SimpleUdpPacketDispatcher` from registry | Dispatcher receives all handlers | ✅ |
| 2.12 | Create `UdpListenerConfiguration` | `@Configuration` class that creates listener bean | Listener bean available in context | ✅ |

---

## Phase 3: Kafka Publisher Integration ✅ COMPLETED

### Objective
Add Kafka publishing capability with decorator pattern (retry, throttling).

| # | Task | Deliverable | Acceptance Criteria | Status |
|---|------|-------------|---------------------|--------|
| 3.1 | Define `TelemetryPublisher` interface | Interface with `void publish(String topic, String key, Object value)` | Interface compiles | ✅ |
| 3.2 | Implement `KafkaTelemetryPublisher` | Uses `KafkaTemplate`, simple send without retry | Integration test with embedded Kafka | ✅ |
| 3.3 | Implement `RetryingPublisher` decorator | Wraps `TelemetryPublisher`, retries on failure (exponential backoff) | Test: failure → retry → success | ✅ |
| 3.4 | Implement `ThrottlingPublisher` decorator | Rate limits calls (e.g., Guava RateLimiter or Bucket4j) | Test: burst → throttled | ✅ |
| 3.5 | Create `TelemetryPublisherConfiguration` | Manual bean wiring: `throttling(retrying(kafka))` | Single bean, correct chain order | ✅ |
| 3.6 | Add configuration properties | `f1.telemetry.kafka.enabled`, retry attempts, throttle rate | Properties read from YAML | ✅ |
| 3.7 | Implement conditional publisher | If Kafka disabled, use no-op publisher | Test: `enabled=false` → no-op | ✅ |

---

## Phase 4: Packet Parsing Integration ✅ COMPLETED

### Objective
Connect UDP listener to packet parsers and Kafka publisher.

| # | Task | Deliverable | Acceptance Criteria | Status |
|---|------|-------------|---------------------|--------|
| 4.1 | Create `SessionPacketHandler` | `@F1UdpListener`, `@F1PacketHandler(packetId=1)`, parses session packet | Handler registered | ✅ |
| 4.2 | Parse session packet from ByteBuffer | Use spec from F1 25 docs, extract event code, session type, track ID, etc. | Unit test: bytes → DTO | ✅ |
| 4.3 | Build `KafkaEnvelope<SessionEventDto>` | Map `PacketHeader` → envelope fields, add payload | Envelope structure matches contract | ✅ |
| 4.4 | Determine topic from packet | Session: `telemetry.session`, handle SSTA/SEND events | Correct topic selected | ✅ |
| 4.5 | Publish to Kafka | Call `TelemetryPublisher.publish()` | Message in Kafka topic | ✅ |
| 4.6 | Create `LapDataPacketHandler` | `@F1PacketHandler(packetId=2)`, parse lap data | Handler registered | ✅ |
| 4.7 | Parse lap packet, create `LapDto` | Extract lap number, time, sector, invalid flag | Unit test: bytes → DTO | ✅ |
| 4.8 | Publish lap data to `telemetry.lap` | Build envelope, publish | Message in topic | ✅ |
| 4.9 | Create `CarTelemetryPacketHandler` | `@F1PacketHandler(packetId=6)`, parse telemetry | Handler registered | ✅ |
| 4.10 | Parse car telemetry, create `CarTelemetryDto` | Extract speed, throttle, brake, gear, etc. | Unit test: bytes → DTO | ✅ |
| 4.11 | Publish telemetry to `telemetry.carTelemetry` | Build envelope, publish | Message in topic | ✅ |
| 4.12 | Create `CarStatusPacketHandler` | `@F1PacketHandler(packetId=7)`, parse status | Handler registered | ✅ |
| 4.13 | Parse car status, create `CarStatusDto` | Extract fuel, tyres, ERS, DRS | Unit test: bytes → DTO | ✅ |
| 4.14 | Publish status to `telemetry.carStatus` | Build envelope, publish | Message in topic | ✅ |
| 4.15 | Add carIndex filtering | Only process `carIndex == playerCarIndex` (MVP) | Non-player packets ignored | ✅ |

---

## Phase 5: Spring Boot Starter Module (`f1-telemetry-udp-starter`) ✅ COMPLETED

### Objective
Package everything as an auto-configurable Spring Boot starter.

| # | Task | Deliverable | Acceptance Criteria | Status |
|---|------|-------------|---------------------|--------|
| 5.1 | Create Maven module `f1-telemetry-udp-starter` | `pom.xml` in `f1-telemetry-udp-starter/` | Module builds | ✅ |
| 5.2 | Add dependencies | Depends on core + spring modules | Dependencies correct | ✅ |
| 5.3 | Create `UdpTelemetryAutoConfiguration` | `@Configuration`, `@EnableConfigurationProperties`, conditional on property | Configuration class created | ✅ |
| 5.4 | Define configuration properties | `@ConfigurationProperties("f1.telemetry.udp")`: host, port, enabled | Properties bind from YAML | ✅ |
| 5.5 | Create listener bean | `@Bean UdpTelemetryListener`, configured from properties | Bean created when enabled | ✅ |
| 5.6 | Start listener on ApplicationReadyEvent | Listener starts after Spring context ready | Listener running after startup | ✅ |
| 5.7 | Stop listener on ContextClosedEvent | Graceful shutdown | Listener stops on app shutdown | ✅ |
| 5.8 | Add conditional autoconfiguration | Only if `f1.telemetry.udp.enabled=true` | No beans when disabled | ✅ |
| 5.9 | Create `spring.factories` / `spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` | Register autoconfiguration | Starter auto-detected | ✅ |
| 5.10 | Add default properties | `application.yml` in starter with defaults | Defaults apply | ✅ |
| 5.11 | Document configuration properties | README in starter module | Documentation complete | ✅ |
| 5.12 | Test starter in sample app | Create test app, add starter dependency, configure, run | Receives UDP packets | ✅ |

---

## Phase 6: Testing and Documentation

### Objective
Ensure quality and maintainability.

| # | Task | Deliverable | Acceptance Criteria |
|---|------|-------------|---------------------|
| 6.1 | Unit tests for core module | Test header decoder, dispatcher, listener | Coverage > 80% |
| 6.2 | Unit tests for Spring module | Test annotation scanning, method invocation | Coverage > 80% |
| 6.3 | Integration test: UDP → handler | Send UDP packet, verify handler called | Test passes |
| 6.4 | Integration test: UDP → Kafka | Send UDP packet, verify Kafka message | Test passes with TestContainers |
| 6.5 | Test error scenarios | Invalid packets, consumer exceptions, Kafka unavailable | All handled gracefully |
| 6.6 | Performance test | Measure throughput (packets/sec), CPU usage | Document results |
| 6.7 | README for each module | Purpose, usage, examples | Clear documentation |
| 6.8 | Update architecture docs | Reflect new library structure | Docs updated |
| 6.9 | Update implementation plan | Replace Stage 3 with reference to this plan | Plan updated |
| 6.10 | Create migration guide | How to use library in existing service | Guide written |

---

## Phase 7: Integration into UDP Ingest Service ✅ COMPLETED

### Objective
Refactor existing UDP Ingest Service to use the new library.

| # | Task | Deliverable | Acceptance Criteria | Status |
|---|------|-------------|---------------------|--------|
| 7.1 | Add starter dependency | Update `udp-ingest-service/pom.xml` | Starter on classpath | ✅ |
| 7.2 | Configure UDP properties | `application.yml`: host, port, Kafka settings | Configuration complete | ✅ |
| 7.3 | Remove old UDP code | Delete manual DatagramChannel code | Old code removed | ✅ |
| 7.4 | Refactor to use annotations | Convert handlers to `@F1UdpListener` / `@F1PacketHandler` | Handlers use new annotations | ✅ |
| 7.5 | Test end-to-end | F1 game → UDP → Kafka → verify messages | Full flow works | ✅ |
| 7.6 | Remove duplicate code | Delete old parsers if now in library | No duplication | ✅ |
| 7.7 | Update service README | Document new architecture | README updated | ✅ |

---

## Success Criteria

- ✅ All UDP handling isolated in reusable library
- ✅ Business code uses only annotations
- ✅ No Spring dependencies in core module
- ✅ No circular dependencies
- ✅ Decorator pattern for cross-cutting concerns (retry, throttle)
- ✅ All tests passing (50 tests: 14 core + 32 spring + 4 starter)
- ✅ Spring Boot starter with autoconfiguration complete
- ⏳ Documentation complete (in progress)
- ⏳ UDP Ingest Service uses library successfully (pending Phase 7)

---

## Notes

- **Do NOT mix Spring and core concerns** — keep core module pure Java
- **Manual bean wiring** for decorators to avoid circular dependencies
- **Test each phase independently** before moving to next
- **Single Responsibility** — each class has one clear job
- **Decorator pattern** is key for extensibility (retry, throttle, metrics, etc.)

---

## Related Documents

- [UDP Telemetry Ingest as Reusable Library Guide](udp_telemetry_ingest_as_reusable_library_implementation_guide.md)
- [Implementation Steps Plan (Overall)](implementation_steps_plan.md)
- [F1 Telemetry Project Architecture](f_1_telemetry_project_architecture.md)
- [Kafka Contracts](kafka_contracts_f_1_telemetry.md)
