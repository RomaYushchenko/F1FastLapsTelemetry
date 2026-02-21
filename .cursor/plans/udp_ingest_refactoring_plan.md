# UDP Ingest Refactoring — Implementation Plan

Move **business logic** (packet handlers, parsing, Kafka publishing) from the library module `f1-telemetry-udp-spring` into the **core service** `udp-ingest-service`. Keep **infrastructure** (annotations, adapter, registry, dispatcher) in the library so the service is configured via annotations without duplicating UDP plumbing.

**Goals:**
- **udp-ingest-service** = core service that owns all packet **processing** logic (parse → publish to Kafka).
- **f1-telemetry-udp-spring** = infrastructure only: annotations, method adapter, post-processor, registry, dispatcher config, and the `TelemetryPublisher` **interface** (contract).
- No change to existing business behaviour: move code without altering parsing, event shapes, or Kafka contracts.
- Follow SOLID, OOP, and existing project patterns (logging, testing, docs in English).

---

## Documentation references

Use these documents when implementing and reviewing the refactor. Do not change Kafka or API contracts in this refactor.

### Project documentation (`.github/project/`)

| Document | Purpose |
|----------|---------|
| [documentation_index.md](../../.github/project/documentation_index.md) | Index of all project docs; recommended reading order |
| [f_1_telemetry_project_architecture.md](../../.github/project/f_1_telemetry_project_architecture.md) | Architecture, services, data flow — update after refactor to state that ingest business logic lives in udp-ingest-service |
| [ARCHITECTURE_CLARIFICATION.md](../../.github/project/ARCHITECTURE_CLARIFICATION.md) | Clarifies UDP library vs udp-ingest-service vs processing service |
| [kafka_contracts_f_1_telemetry.md](../../.github/project/kafka_contracts_f_1_telemetry.md) | **Kafka topics, envelope, payload, ordering — do not change** |
| [state_machines_specification_f_1_telemetry.md](../../.github/project/state_machines_specification_f_1_telemetry.md) | Session/lap lifecycle, FSM — source of truth for ingest behaviour |
| [telemetry_error_and_lifecycle_contract.md](../../.github/project/telemetry_error_and_lifecycle_contract.md) | Lifecycle and error semantics between layers |
| [rest_web_socket_api_contracts_f_1_telemetry.md](../../.github/project/rest_web_socket_api_contracts_f_1_telemetry.md) | REST/WebSocket API (downstream of ingest; no direct change) |
| [code_skeleton_java_packages_interfaces.md](../../.github/project/code_skeleton_java_packages_interfaces.md) | Packages and interfaces; [telemetry_processing_api_service.md](../../.github/project/telemetry_processing_api_service.md) for processing service |
| [mvp-requirements.md](../../.github/project/mvp-requirements.md) | MVP scope and constraints |
| [unit_testing_policy.md](../../.github/project/unit_testing_policy.md) | Unit testing policy (coverage, TestData, AAA, @DisplayName) |

### Cursor rules (`.cursor/rules/`)

| Rule | Applies to |
|------|------------|
| [english-documentation.mdc](../rules/english-documentation.mdc) | All docs and comments in code — English only |
| [logging-policy.mdc](../rules/logging-policy.mdc) | Log levels, parameterized messages, DEBUG at method entry |
| [unit-testing-policy.mdc](../rules/unit-testing-policy.mdc) | JUnit 5, Mockito, AssertJ, TestData, @DisplayName, 85% coverage |
| [java-rules.mdc](../rules/java-rules.mdc) | Java/Spring Boot style and conventions |

### Other plans (`.cursor/plans/`)

| Plan | Relation |
|------|----------|
| [implementation_phases.md](implementation_phases.md) | General implementation phases; build verification after each phase |
| [unit_test_coverage_plan.md](unit_test_coverage_plan.md) | Coverage plan by phase (e.g. for telemetry-processing-api-service) |

### Module READMEs

| Module | README |
|--------|--------|
| f1-telemetry-udp-spring | [f1-telemetry-udp-spring/README.md](../../f1-telemetry-udp-spring/README.md) — update after refactor to describe infrastructure-only role |
| udp-ingest-service | Add or update README to describe core ingest service and handler/publisher packages |

---

## 1. Principles and Constraints

### 1.1 Do Not Break Existing Behaviour
- **Parsing logic** (ByteBuffer → DTO) stays byte‑for‑byte; only package/class location changes.
- **Kafka topics, keys, event types** (e.g. `CarTelemetryEvent`, `SessionLifecycleEvent`) and contracts remain unchanged.
- **Configuration properties** (`f1.telemetry.udp.*`, `f1.telemetry.kafka.*`) keep the same names and semantics; only the owning module (service vs library) may change.

### 1.2 SOLID and Patterns
- **DIP (Dependency Inversion):** Handlers depend on the `TelemetryPublisher` **interface** (in the library). The service provides the implementation (Kafka + decorators).
- **SRP:** Library = “dispatch UDP to handler methods”; Service = “parse packets and publish telemetry”.
- **OCP:** New packet types = new handler classes in the service; no changes to library.
- **Decorator pattern:** Keep retry and throttling as decorators around the Kafka publisher in the service.
- **Single place for business rules:** All F1 packet layout constants, parsing, and topic names live in the service.

### 1.3 Project Conventions
- **Documentation and comments:** English (per `.cursor/rules/english-documentation.mdc`).
- **Logging:** Per `.cursor/rules/logging-policy.mdc` (SLF4J, parameterized messages, DEBUG at method entry, WARN before throw, ERROR with exception).
- **Tests:** JUnit 5, Mockito, AssertJ; use centralised test data where applicable; `@DisplayName` for class and methods (per unit testing policy).
- **Build:** After each phase, run `mvn clean compile` (or `mvn clean install -DskipTests`) from repo root and fix any failures before proceeding.

---

## 2. Target Architecture

### 2.1 Module Roles

| Module | Role | Contents after refactor |
|--------|------|-------------------------|
| **f1-telemetry-udp-core** | UDP socket, header decode, dispatcher | Unchanged. |
| **f1-telemetry-udp-spring** | Annotation-based wiring only | `@F1UdpListener`, `@F1PacketHandler`; `MethodPacketHandler`; `F1PacketHandlerPostProcessor`; `PacketHandlerRegistry`; `UdpDispatcherConfiguration`; **`TelemetryPublisher` interface**; **`PublishException`** (used by decorators). No handlers, no Kafka, no contracts. |
| **f1-telemetry-udp-starter** | Auto-config + lifecycle | Unchanged: imports `UdpDispatcherConfiguration`, scans `com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring`, provides `UdpTelemetryListener` bean and start/stop. Does **not** scan the service package; handlers are discovered via the main app’s component scan. |
| **udp-ingest-service** | Core ingest service (business logic) | All packet handlers (Session, CarTelemetry, CarStatus, CarDamage, LapData); Kafka publisher implementation and decorators (KafkaTelemetryPublisher, RetryingPublisher, ThrottlingPublisher, NoOpPublisher); publisher config and properties; `application.yml` for UDP and Kafka. |

### 2.2 Data Flow (Unchanged)
```
UDP packet → UdpTelemetryListener (core) → PacketHeader decode → UdpPacketDispatcher
  → MethodPacketHandler (library) → @F1PacketHandler method (service)
  → parse payload (service) → build event (service) → TelemetryPublisher.publish (interface)
  → Kafka impl + decorators (service) → Kafka
```

### 2.3 Package Layout After Refactor

**f1-telemetry-udp-spring (slimmed):**
```
com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring/
├── annotation/
│   ├── F1PacketHandler.java
│   └── F1UdpListener.java
├── adapter/
│   └── MethodPacketHandler.java
├── processor/
│   └── F1PacketHandlerPostProcessor.java
├── registry/
│   └── PacketHandlerRegistry.java
├── config/
│   └── UdpDispatcherConfiguration.java
└── publisher/
    ├── TelemetryPublisher.java   (interface only)
    └── PublishException.java
```

**udp-ingest-service (new/relocated):**
```
com.ua.yushchenko.f1.fastlaps.telemetry.ingest/
├── UdpIngestApplication.java
├── handler/
│   ├── SessionPacketHandler.java
│   ├── CarTelemetryPacketHandler.java
│   ├── CarStatusPacketHandler.java
│   ├── CarDamagePacketHandler.java
│   └── LapDataPacketHandler.java
├── publisher/
│   ├── KafkaTelemetryPublisher.java
│   ├── RetryingPublisher.java
│   ├── ThrottlingPublisher.java
│   └── NoOpPublisher.java
└── config/
    ├── TelemetryPublisherConfiguration.java
    └── TelemetryPublisherProperties.java
```

Handlers stay as Spring beans under a package below `UdpIngestApplication`, so the default `@SpringBootApplication` component scan picks them up. `F1PacketHandlerPostProcessor` (in the library) runs over all beans and registers any `@F1UdpListener` beans (including those in the service).

---

## 3. Dependency Changes

### 3.1 f1-telemetry-udp-spring (pom.xml)
- **Remove:** `telemetry-api-contracts`, `spring-kafka`, `guava`.
- **Keep:** `f1-telemetry-udp-core`, `spring-context`, `spring-boot`, `spring-boot-autoconfigure`, Lombok, test deps.

Rationale: No handlers or Kafka code remain; only the publisher **interface** and exception stay, so no contracts or Kafka/Guava are needed.

### 3.2 udp-ingest-service (pom.xml)
- **Keep:** `telemetry-api-contracts`, `f1-telemetry-udp-starter`, `spring-boot-starter`, `spring-kafka`, Lombok, Jackson.
- **Add:** `guava` (same version as currently in f1-telemetry-udp-spring, e.g. 33.0.0-jre) for `ThrottlingPublisher`.

Starter already brings in `f1-telemetry-udp-spring` (with `TelemetryPublisher` and annotations) and `f1-telemetry-udp-core`; no extra direct dependency on the spring module is required.

### 3.3 Interface Contract
- `TelemetryPublisher` remains in **f1-telemetry-udp-spring** so that:
  - Handlers in the service depend on the abstraction (DIP).
  - The library stays free of Kafka/contracts; the service owns the implementation and configuration.

---

## 4. Implementation Phases

### Phase 1: Add publisher contract and exception to the service’s classpath (no move yet)
**Goal:** Service can compile against `TelemetryPublisher` and `PublishException` from the library; no behaviour change.

1. Confirm that `udp-ingest-service` already has a transitive dependency on `f1-telemetry-udp-spring` via `f1-telemetry-udp-starter`.
2. No code changes in this phase; only verify that the service runs and tests pass (e.g. start the app, or run any existing tests). This establishes the baseline.

**Verification:** `mvn clean compile` and `mvn -pl udp-ingest-service test` (if any) from repo root.

---

### Phase 2: Create packages and move publisher implementation to the service
**Goal:** All publisher **implementations** and their configuration live in the service; library keeps only the interface and `PublishException`.

1. **In udp-ingest-service**, create packages:
   - `com.ua.yushchenko.f1.fastlaps.telemetry.ingest.publisher`
   - `com.ua.yushchenko.f1.fastlaps.telemetry.ingest.config`
2. **Copy** (do not yet delete from spring) into the service with **new package**:
   - `KafkaTelemetryPublisher.java` → `ingest.publisher`
   - `RetryingPublisher.java` → `ingest.publisher`
   - `ThrottlingPublisher.java` → `ingest.publisher`
   - `NoOpPublisher.java` → `ingest.publisher`
   - `TelemetryPublisherConfiguration.java` → `ingest.config`
   - `TelemetryPublisherProperties.java` → `ingest.config`
3. In each moved class, **change package** to `com.ua.yushchenko.f1.fastlaps.telemetry.ingest.publisher` or `...ingest.config`. Update imports:
   - References to `TelemetryPublisher` and `PublishException` must point to the **library** packages: `com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.publisher` (so the service depends on the interface, not the other way around).
4. In **TelemetryPublisherConfiguration** (service), ensure `@ConfigurationProperties` and property prefix `f1.telemetry.kafka` are unchanged so `application.yml` remains valid.
5. **Do not** remove the same classes from the library yet (avoids breaking the library build and any other consumers).

**Important:** After Phase 2, both the library and the service define `TelemetryPublisher` beans (library via its `TelemetryPublisherConfiguration`, service via the new config). Do **not** run the full application until Phase 3 is complete, or you will get a duplicate bean error. Either complete Phase 3 immediately after Phase 2 in the same change set, or run only `mvn clean compile` for verification in Phase 2.

**Verification:** `mvn clean compile`. Service and library both compile. After Phase 3, run `udp-ingest-service` and confirm the single `TelemetryPublisher` bean comes from the service.

---

### Phase 3: Remove publisher implementation from the library
**Goal:** Library contains only the `TelemetryPublisher` interface and `PublishException`; no Kafka or Guava.

1. From **f1-telemetry-udp-spring**, **delete**:
   - `publisher/KafkaTelemetryPublisher.java`
   - `publisher/RetryingPublisher.java`
   - `publisher/ThrottlingPublisher.java`
   - `publisher/NoOpPublisher.java`
   - `config/TelemetryPublisherConfiguration.java`
   - `config/TelemetryPublisherProperties.java`
2. In **f1-telemetry-udp-spring/pom.xml**, remove dependencies: `telemetry-api-contracts`, `spring-kafka`, `guava`.
3. Move **publisher tests** from the library to the service (see Phase 5). For this phase, **delete** from the library (or temporarily disable) the tests for the removed classes: `KafkaTelemetryPublisherTest`, `RetryingPublisherTest`, `ThrottlingPublisherTest`, `NoOpPublisherTest`, `TelemetryPublisherConfigurationTest`. They will be re-added under the service in Phase 5.

**Verification:** `mvn clean compile`. Run `udp-ingest-service`: it should start, and the only `TelemetryPublisher` bean should come from the service’s `TelemetryPublisherConfiguration`. No duplicate bean definition. If you deferred Phase 3, do it now so that the library no longer defines publisher beans.

---

### Phase 4: Move packet handlers to the service
**Goal:** All five handlers live in the service; library has no handler classes.

1. **In udp-ingest-service**, create package:
   - `com.ua.yushchenko.f1.fastlaps.telemetry.ingest.handler`
2. **Copy** each handler from the library to the service (new package), then **change package** and **imports**:
   - `SessionPacketHandler.java`
   - `CarTelemetryPacketHandler.java`
   - `CarStatusPacketHandler.java`
   - `CarDamagePacketHandler.java`
   - `LapDataPacketHandler.java`
3. In each moved handler:
   - Package: `com.ua.yushchenko.f1.fastlaps.telemetry.ingest.handler`
   - Imports for annotations and interface: keep `com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.annotation.*` and `com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.publisher.TelemetryPublisher`.
   - Imports for API contracts: `com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.*`
   - Imports for core: `com.ua.yushchenko.f1.fastlaps.telemetry.udp.core.packet.PacketHeader`
   - Do **not** change method bodies, parsing logic, topic names, or event building; only package and imports.
4. **Delete** the five handler classes from **f1-telemetry-udp-spring**.
5. Ensure the service’s `application.yml` (or equivalent) still contains the same `f1.telemetry.udp.handlers.*` toggles if you use them (`session.enabled`, `telemetry.enabled`, etc.).

**Verification:** `mvn clean compile`. Start `udp-ingest-service`; send a test UDP packet (or use existing test harness) and confirm that the appropriate handler runs and publishes to Kafka (same topics and payloads as before). No change in behaviour.

---

### Phase 5: Move and adapt tests
**Goal:** All tests for moved code run in the service; library tests only cover what remains (adapter, post-processor, registry, dispatcher config).

1. **Handler tests** (from f1-telemetry-udp-spring to udp-ingest-service):
   - Copy `SessionPacketHandlerTest`, `CarTelemetryPacketHandlerTest`, `CarStatusPacketHandlerTest`, `CarDamagePacketHandlerTest`, `LapDataPacketHandlerTest` into `udp-ingest-service/src/test/.../ingest/handler/`.
   - Update package to `com.ua.yushchenko.f1.fastlaps.telemetry.ingest.handler` and fix imports (handlers now in `ingest.handler`, `TelemetryPublisher` from spring, contracts from api). Mock `TelemetryPublisher` as before; assertions and test data stay the same.
2. **Publisher and config tests** (from f1-telemetry-udp-spring to udp-ingest-service):
   - Copy `KafkaTelemetryPublisherTest`, `RetryingPublisherTest`, `ThrottlingPublisherTest`, `NoOpPublisherTest`, `TelemetryPublisherConfigurationTest` into `udp-ingest-service/src/test/.../ingest/publisher/` or `.../ingest/config/`.
   - Update packages and imports; keep test logic and coverage (e.g. retry count, throttling, no-op behaviour).
3. **Remove** from f1-telemetry-udp-spring all tests that were moved (handlers + publisher/config tests). Keep in the library: `MethodPacketHandlerTest`, `F1PacketHandlerPostProcessorTest`, `DuplicatePacketIdValidationTest`, `UdpDispatcherConfigurationTest`.
4. If the project has a central **TestData** or similar (e.g. in telemetry-processing-api-service), reuse it in the service tests where applicable; otherwise keep test data local to the service tests to avoid cross-module test coupling.

**Verification:**  
- `mvn clean test` for the whole repo, or at least:
  - `mvn -pl f1-telemetry-udp-spring test`
  - `mvn -pl udp-ingest-service test`
- All tests pass; no tests reference deleted classes.

---

### Phase 6: Cleanup and documentation
**Goal:** Clear boundaries, no dead code, docs aligned with new layout.

1. **f1-telemetry-udp-spring/README.md**: Update to describe the module as **infrastructure only** (annotations, adapter, registry, dispatcher, `TelemetryPublisher` interface). Remove or shorten sections that describe handlers and Kafka; add a short note that business logic (handlers and publisher implementation) lives in **udp-ingest-service**.
2. **udp-ingest-service**: Add a short README (if missing) stating that this service is the **core UDP ingest service**: it receives F1 UDP packets, parses them via annotation-driven handlers, and publishes events to Kafka; other applications consume from Kafka. List the handlers and point to `f1.telemetry.udp` / `f1.telemetry.kafka` configuration.
3. Remove any leftover references in the library to the deleted packages (e.g. broken links in README or javadoc).
4. Optionally update **.github/project/f_1_telemetry_project_architecture.md** (or equivalent) to state that UDP ingest **business logic** lives in `udp-ingest-service` and the spring module provides only the annotation-based dispatch layer.

**Verification:** `mvn clean install -DskipTests` from repo root. Final manual check: run `udp-ingest-service`, send UDP traffic, confirm Kafka messages unchanged (topics, keys, payload structure).

---

## 5. Testing Strategy

- **Unit tests (service):** Handlers and publisher classes are unit-tested with mocks (e.g. `TelemetryPublisher`, `KafkaTemplate`). Use AssertJ and JUnit 5; follow the project’s unit testing policy (e.g. 85% coverage where required, TestData, `@DisplayName`).
- **Unit tests (library):** Keep and run tests for `MethodPacketHandler`, `F1PacketHandlerPostProcessor`, `DuplicatePacketIdValidation`, `UdpDispatcherConfiguration`. No dependency on contracts or Kafka.
- **Integration:** If the project has integration tests that start the ingest service and send UDP/Kafka, run them after the refactor to confirm end-to-end behaviour.
- **No behavioural change:** Parsing, event DTOs, and Kafka usage must match the pre-refactor behaviour; tests should catch any accidental change in parsing or event structure.

---

## 6. Rollback and Risk Mitigation

- **Incremental move:** Copy → adjust package/imports → verify → then delete from library. Avoid “big bang” delete before the service is proven to work.
- **Git:** Prefer one commit per phase (or small, logical commits) so each phase can be reverted independently if needed.
- **Duplicate beans:** After Phase 2, if both library and service defined `TelemetryPublisher` or publisher config, remove the library’s in Phase 3 so only the service provides the bean.
- **Contract stability:** Do not change `telemetry-api-contracts` (event types, topics) in this refactor; that would affect consumers (e.g. telemetry-processing-api-service).

---

## 7. Summary Checklist

- [x] Phase 1: Baseline verified (compile + run/tests).
- [x] Phase 2: Publisher impl + config copied to service; service compiles and runs; publisher bean from service.
- [x] Phase 3: Publisher impl and config removed from library; library pom cleaned; no duplicate beans.
- [x] Phase 4: Handlers moved to service; library handlers removed; behaviour unchanged (UDP → Kafka).
- [x] Phase 5: Tests moved/adapted; all tests pass in both modules.
- [x] Phase 6: READMEs and architecture docs updated; final full build and smoke test.

After completion, **udp-ingest-service** is the single place for UDP ingest business logic; **f1-telemetry-udp-spring** remains the thin annotation-based layer for “listen to UDP and call your handlers” without dictating what those handlers do.
