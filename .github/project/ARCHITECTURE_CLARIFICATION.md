# Architecture Clarification: UDP Library vs Services

## The Confusion

**Question:** "Why do we need udp-ingest-service if the library already handles everything? Does telemetry-processing-api-service also catch events?"

**Answer:** The UDP library provides **infrastructure** (UDP listening, parsing, Kafka publishing), but **udp-ingest-service** is still needed as the **deployment unit** that uses this library. The **telemetry-processing-api-service** is a completely separate service that **consumes from Kafka** (not UDP).

---

## Clear Separation of Concerns

### 1. UDP Library (f1-telemetry-udp-*)
**What it is:** Reusable infrastructure library (like Spring Boot itself)

**What it provides:**
- UDP packet reception (DatagramChannel)
- Packet header parsing
- Annotation-based handler discovery (`@F1PacketHandler`)
- Built-in handlers for 4 packet types (Session, Lap, CarTelemetry, CarStatus)
- Kafka publishing with decorators (retry, throttling)

**What it does NOT do:**
- It's NOT a runnable application
- It does NOT contain business logic beyond parsing
- It does NOT aggregate data
- It does NOT store anything in database
- It does NOT provide REST/WebSocket APIs

**Built-in Handlers:**
The library includes 4 **reference implementations**:
```
SessionPacketHandler → Parse UDP packet → Publish to Kafka topic "telemetry.session"
LapDataPacketHandler → Parse UDP packet → Publish to Kafka topic "telemetry.lap"
CarTelemetryPacketHandler → Parse UDP packet → Publish to Kafka topic "telemetry.carTelemetry"
CarStatusPacketHandler → Parse UDP packet → Publish to Kafka topic "telemetry.carStatus"
```

These handlers do **ONLY**:
1. Parse binary UDP → DTO
2. Build KafkaEnvelope
3. Publish to Kafka
4. Log errors

**Analogy:** The UDP library is like `spring-boot-starter-web`. It provides infrastructure (embedded Tomcat, HTTP handling), but you still need to create a Spring Boot application with your controllers.

---

### 2. udp-ingest-service
**What it is:** Spring Boot application (microservice) that **uses** the UDP library

**What it does:**
- Main class with `@SpringBootApplication`
- Configuration (application.yml): UDP port, Kafka bootstrap servers
- **OPTIONAL:** Custom packet handlers (if you need packet types not in the library)
- **OPTIONAL:** Custom business logic (filtering, enrichment, custom metrics)
- Deployment artifact (Docker image, JAR file)

**Current Implementation:**
```java
@SpringBootApplication
public class UdpIngestApplication {
    public static void main(String[] args) {
        SpringApplication.run(UdpIngestApplication.class, args);
    }
}
```

**What happens at runtime:**
1. Spring Boot starts
2. UDP library autoconfiguration kicks in
3. UDP listener starts on port 20777
4. Built-in handlers from library are registered
5. When UDP packet arrives:
   - Library parses header
   - Library dispatches to appropriate handler
   - Handler parses payload
   - Handler publishes to Kafka
   - **That's it! No database, no aggregation, no REST API**

**Why we need it:**
- **Deployment boundary:** Separate microservice with its own lifecycle
- **Configuration:** Set UDP port, Kafka settings for production
- **Extensibility:** Add custom handlers if needed
- **Scalability:** Can deploy multiple instances (though MVP has one)
- **Isolation:** UDP concerns separated from processing logic

**Analogy:** If UDP library is `spring-boot-starter-web`, then udp-ingest-service is your actual web application (like a REST API microservice).

---

### 3. telemetry-processing-api-service
**What it is:** Completely separate Spring Boot application (microservice)

**What it does:**
- **Kafka Consumers:** Reads from 4 Kafka topics (telemetry.session, .lap, .carTelemetry, .carStatus)
- **State Management:** Session FSM (INIT → ACTIVE → ENDING → TERMINAL)
- **Aggregation:** Lap times, sector times, best laps, session summary
- **Persistence:** Writes to PostgreSQL/TimescaleDB
  - Raw telemetry (time-series data)
  - Aggregated data (laps, sessions, summary)
- **REST API:** Endpoints for historical data
  - `GET /api/sessions`
  - `GET /api/sessions/{uid}/laps`
  - `GET /api/sessions/{uid}/summary`
- **WebSocket:** Live streaming of current session data (10 Hz)

**Key Point:** This service **NEVER** receives UDP packets. It only consumes from Kafka.

**Why separate from udp-ingest-service:**
- **Different responsibilities:** Ingestion vs. processing/API
- **Different scaling needs:** UDP ingestion vs. API traffic
- **Different dependencies:** UDP library vs. JPA/WebSocket
- **Fault isolation:** If processing fails, ingestion continues (Kafka buffers)
- **Clean architecture:** Each service does one thing well

---

## Data Flow (Complete Picture)

```
┌─────────────┐
│  F1 Game    │
└──────┬──────┘
       │ UDP packets
       │ (60 Hz)
       ▼
┌────────────────────────────────────────────────────┐
│ udp-ingest-service                                 │
│ ┌────────────────────────────────────────────────┐ │
│ │ UDP Library (f1-telemetry-udp-starter)         │ │
│ │ ┌──────────────────────────────────────────┐   │ │
│ │ │ Built-in Handlers:                       │   │ │
│ │ │ - SessionPacketHandler                   │   │ │
│ │ │ - LapDataPacketHandler                   │   │ │
│ │ │ - CarTelemetryPacketHandler              │   │ │
│ │ │ - CarStatusPacketHandler                 │   │ │
│ │ │                                          │   │ │
│ │ │ Each handler:                            │   │ │
│ │ │ 1. Parse binary → DTO                    │   │ │
│ │ │ 2. Build KafkaEnvelope                   │   │ │
│ │ │ 3. Publish to Kafka topic                │   │ │
│ │ └──────────────────────────────────────────┘   │ │
│ └────────────────────────────────────────────────┘ │
└────────────────────┬───────────────────────────────┘
                     │ Kafka messages
                     │ (4 topics)
                     ▼
            ┌────────────────┐
            │     Kafka      │
            │                │
            │ Topics:        │
            │ - telemetry.session      │
            │ - telemetry.lap          │
            │ - telemetry.carTelemetry │
            │ - telemetry.carStatus    │
            └────────┬───────┘
                     │ Kafka consumers
                     │ read messages
                     ▼
┌──────────────────────────────────────────────────────┐
│ telemetry-processing-api-service                     │
│                                                      │
│ ┌──────────────────────────────────────────────┐    │
│ │ Kafka Consumers (4 listeners)                │    │
│ │ - SessionEventConsumer                       │    │
│ │ - LapDataConsumer                            │    │
│ │ - CarTelemetryConsumer                       │    │
│ │ - CarStatusConsumer                          │    │
│ └──────────┬───────────────────────────────────┘    │
│            ▼                                         │
│ ┌──────────────────────────────────────────────┐    │
│ │ Business Logic:                              │    │
│ │ - Idempotency check (processed_packets)     │    │
│ │ - Session FSM (SessionLifecycleService)     │    │
│ │ - Lap aggregation (LapAggregator)           │    │
│ │ - Summary calculation                       │    │
│ │ - Raw telemetry batching                    │    │
│ └──────────┬───────────────────────────────────┘    │
│            ▼                                         │
│ ┌──────────────────────────────────────────────┐    │
│ │ Persistence (JPA Repositories)               │    │
│ │ - SessionRepository                          │    │
│ │ - LapRepository                              │    │
│ │ - SessionSummaryRepository                   │    │
│ │ - RawTelemetryRepository                     │    │
│ └──────────┬───────────────────────────────────┘    │
│            ▼                                         │
│    ┌──────────────────────┐                         │
│    │ PostgreSQL +         │                         │
│    │ TimescaleDB          │                         │
│    └──────────┬───────────┘                         │
│               │                                      │
│    ┌──────────┴───────────┐                         │
│    ▼                      ▼                         │
│ REST API             WebSocket                      │
│ /api/sessions        /ws/live                       │
│ /api/sessions/{uid}  (10 Hz snapshots)              │
│ /api/.../laps                                       │
│ /api/.../summary                                    │
└──────┬───────────────────┬───────────────────────────┘
       │                   │
       ▼                   ▼
   ┌────────────────────────────┐
   │      React SPA             │
   │ - Live Dashboard           │
   │ - Session History          │
   │ - Lap Comparison           │
   └────────────────────────────┘
```

---

## Why This Architecture?

### Separation of Concerns
- **UDP Library:** Reusable infrastructure (parse UDP, publish Kafka)
- **udp-ingest-service:** Deployment wrapper + optional customization
- **telemetry-processing-api-service:** Business logic + persistence + API

### Benefits

1. **Reusability:** UDP library can be used in other projects
2. **Testability:** Each layer independently testable
3. **Scalability:** Services can scale independently
4. **Fault Isolation:** If processing fails, ingestion continues (Kafka buffers)
5. **Clear Boundaries:** Each service has one responsibility
6. **Technology Flexibility:** Could replace Kafka without changing UDP library

---

## What Each Service Actually Contains

### udp-ingest-service (MINIMAL CODE)
```
src/main/java/
  └── com.ua.yushchenko.f1.fastlaps.telemetry.ingest/
      └── UdpIngestApplication.java  (10 lines - just main method)

src/main/resources/
  └── application.yml  (UDP port, Kafka settings)

pom.xml
  └── Dependency on f1-telemetry-udp-starter
```

**Total lines of business code:** ~10 lines

### telemetry-processing-api-service (LOTS OF CODE)
```
src/main/java/
  └── com.ua.yushchenko.f1.fastlaps.telemetry.processing/
      ├── TelemetryProcessingApplication.java
      ├── consumer/
      │   ├── SessionEventConsumer.java
      │   ├── LapDataConsumer.java
      │   ├── CarTelemetryConsumer.java
      │   └── CarStatusConsumer.java
      ├── service/
      │   ├── SessionLifecycleService.java
      │   ├── SessionStateManager.java
      │   ├── LapAggregator.java
      │   ├── SessionSummaryAggregator.java
      │   └── RawTelemetryWriter.java
      ├── state/
      │   ├── SessionRuntimeState.java
      │   ├── LapRuntimeState.java
      │   └── SessionState.java (enum)
      ├── repository/
      │   ├── SessionRepository.java
      │   ├── LapRepository.java
      │   ├── SessionSummaryRepository.java
      │   └── ProcessedPacketRepository.java
      ├── entity/
      │   ├── SessionEntity.java
      │   ├── LapEntity.java
      │   ├── SessionSummaryEntity.java
      │   └── ProcessedPacketEntity.java
      ├── controller/
      │   ├── SessionController.java
      │   └── LiveTelemetryController.java (WebSocket)
      └── mapper/
          └── EntityMapper.java

src/main/resources/
  └── application.yml  (Kafka consumer, DB, REST port)

pom.xml
  └── Dependencies: Spring Web, Kafka, JPA, WebSocket
```

**Total lines of business code:** ~2000-3000 lines

---

## Summary

| Component | Role | Receives UDP? | Publishes to Kafka? | Consumes from Kafka? | Has Database? | Has REST API? |
|-----------|------|---------------|---------------------|---------------------|---------------|---------------|
| **UDP Library** | Infrastructure | ✅ Yes (internal) | ✅ Yes | ❌ No | ❌ No | ❌ No |
| **udp-ingest-service** | Deployment wrapper | ✅ Yes (via library) | ✅ Yes (via library) | ❌ No | ❌ No | ❌ No |
| **telemetry-processing-api-service** | Business logic + API | ❌ No | ❌ No | ✅ Yes | ✅ Yes | ✅ Yes |

**Key Insight:** The UDP library **is used by** udp-ingest-service. It's not a separate running process. Think of it like Spring Boot itself - you don't run Spring Boot separately; you use it in your application.

**The Answer:**
- **udp-ingest-service:** Needed as deployment unit, uses UDP library
- **telemetry-processing-api-service:** Completely separate service, consumes from Kafka (not UDP), has all the business logic
