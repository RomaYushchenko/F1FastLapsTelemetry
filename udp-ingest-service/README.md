# UDP Ingest Service

Core service for F1 25 UDP telemetry ingestion: receives UDP packets, parses them via annotation-driven handlers, and publishes events to Kafka.

## Role

- **Listens** to the F1 25 UDP port (default 20777).
- **Parses** binary packets using handler classes (Session, Lap Data, Car Telemetry, Car Status, Car Damage).
- **Publishes** events to Kafka topics (`telemetry.session`, `telemetry.lap`, `telemetry.carTelemetry`, `telemetry.carStatus`, `telemetry.carDamage`).

Downstream services (e.g. telemetry-processing-api-service) consume from Kafka. This service does not write to a database.

## Architecture

- **Handlers** live in `com.ua.yushchenko.f1.fastlaps.telemetry.ingest.handler` and are discovered by the **f1-telemetry-udp-spring** library via `@F1UdpListener` and `@F1PacketHandler`.
- **Publisher** implementation and decorators (Kafka, retry, throttling, no-op) live in `com.ua.yushchenko.f1.fastlaps.telemetry.ingest.publisher` and are configured in `ingest.config`.
- Handlers depend on the `TelemetryPublisher` **interface** from the library; this service provides the Kafka-backed implementation.

## Configuration

- **UDP:** `f1.telemetry.udp.*` (port, buffer size, handler toggles such as `f1.telemetry.udp.handlers.session.enabled`).
- **Kafka:** `f1.telemetry.kafka.*` (enabled, retry, throttle). See `application.yml` in this module.

## Package layout

```
com.ua.yushchenko.f1.fastlaps.telemetry.ingest/
├── UdpIngestApplication.java
├── handler/          # Thin handlers: dispatch → parser → builder → publish
├── parser/            # ByteBuffer → DTO (one per packet type)
├── builder/           # PacketHeader + DTO → Kafka event (one per event type)
├── publisher/         # Kafka impl and decorators (retry, throttle, no-op)
└── config/            # Publisher configuration and properties
```

Handlers depend on parsers and builders; event assembly is in builder classes, parsing in parser classes (aligned with telemetry-processing-api-service patterns).

## Build and run

```bash
mvn -pl udp-ingest-service clean package
java -jar udp-ingest-service/target/udp-ingest-service-*.jar
```

Requires Kafka (and optional UDP traffic from F1 25) as per project setup.
