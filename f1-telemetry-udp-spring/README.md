# F1 Telemetry UDP Spring Integration Module

Spring Boot integration module providing annotation-based UDP packet handling for F1 2025 game telemetry.

## Overview

This module provides a declarative, annotation-driven approach to handling F1 UDP telemetry packets in Spring applications. It eliminates boilerplate UDP code by using `@F1UdpListener` and `@F1PacketHandler` annotations.

## Features

- **Declarative packet handling** - Use annotations instead of manual UDP setup
- **Automatic method registration** - Spring automatically discovers and registers handler methods
- **Multiple method signatures** - Support for full, header-only, and payload-only handlers
- **Type-safe** - Compile-time validation of handler methods
- **Thread-safe** - Safe concurrent packet handling
- **Error isolation** - Exceptions in one handler don't affect others
- **Duplicate detection** - Validates that each packet ID has exactly one handler

## Dependencies

```xml
<dependency>
    <groupId>com.ua.yushchenko.f1.fastlaps.telemetry</groupId>
    <artifactId>f1-telemetry-udp-spring</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

This module depends on:
- `f1-telemetry-udp-core` - Core UDP handling and packet header decoding
- Spring Context - Bean lifecycle management
- Spring Boot (optional) - For auto-configuration support

## Quick Start

### 1. Create a Listener Bean

```java
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.annotation.F1PacketHandler;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.annotation.F1UdpListener;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;

@F1UdpListener
@Component
public class TelemetryListener {
    
    @F1PacketHandler(packetId = 6)  // Car Telemetry
    public void handleCarTelemetry(PacketHeader header, ByteBuffer payload) {
        // Parse and process car telemetry data
        System.out.println("Received telemetry from session: " + header.sessionUID());
    }
    
    @F1PacketHandler(packetId = 2)  // Session Data
    public void handleSessionData(ByteBuffer payload) {
        // Parse session data
        // Header not needed for this handler
    }
    
    @F1PacketHandler(packetId = 1)  // Session Events
    public void handleSessionEvent(PacketHeader header) {
        // Process event based on header only
        System.out.println("Event at frame " + header.frameIdentifier());
    }
}
```

### 2. Configure Spring Application

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TelemetryApplication {
    public static void main(String[] args) {
        SpringApplication.run(TelemetryApplication.class, args);
    }
}
```

That's it! The `F1PacketHandlerPostProcessor` automatically discovers your listener beans and registers their handler methods.

## Architecture

### Components

1. **@F1UdpListener** - Class-level annotation marking a bean as containing packet handlers
   - Meta-annotated with `@Component` for automatic Spring discovery
   - Applied to classes containing `@F1PacketHandler` methods

2. **@F1PacketHandler** - Method-level annotation defining packet handlers
   - `packetId()` attribute specifies which packet type to handle (0-255)
   - Method must have one of three supported signatures (see below)

3. **F1PacketHandlerPostProcessor** - `BeanPostProcessor` implementation
   - Scans Spring beans for `@F1UdpListener` annotation
   - Discovers `@F1PacketHandler` methods
   - Creates `MethodPacketHandler` adapters
   - Registers handlers with `PacketHandlerRegistry`
   - Validates no duplicate packet IDs

4. **MethodPacketHandler** - Adapter implementing `UdpPacketConsumer`
   - Wraps annotated methods to conform to core interface
   - Supports three method signatures
   - Handles reflection invocation
   - Logs errors without propagating exceptions

5. **PacketHandlerRegistry** - Registry of all packet handlers
   - Thread-safe `CopyOnWriteArrayList` storage
   - Registration API for Spring integration
   - Used by `UdpDispatcherConfiguration`

6. **UdpDispatcherConfiguration** - Spring configuration class
   - Creates `SimpleUdpPacketDispatcher` bean
   - Wires registered handlers from registry
   - Logs dispatcher statistics

### Data Flow

```
UDP Packet Arrives
       ↓
UdpTelemetryListener (core module)
       ↓
PacketHeaderDecoder (core module)
       ↓
SimpleUdpPacketDispatcher (core module)
       ↓
MethodPacketHandler (adapter)
       ↓
@F1PacketHandler method (your code)
```

## Handler Method Signatures

Your `@F1PacketHandler` methods can use any of these three signatures:

### 1. Full Signature (Header + Payload)
```java
@F1PacketHandler(packetId = 6)
public void handlePacket(PacketHeader header, ByteBuffer payload) {
    // Access both header metadata and packet payload
}
```

### 2. Payload Only
```java
@F1PacketHandler(packetId = 2)
public void handlePacket(ByteBuffer payload) {
    // Only need the packet data
}
```

### 3. Header Only
```java
@F1PacketHandler(packetId = 1)
public void handlePacket(PacketHeader header) {
    // Only need header metadata (frame, session UID, timestamp, etc.)
}
```

**Important:** The `ByteBuffer` parameter is a **view** of the original packet buffer. Its position is set to the start of the packet payload (after the 29-byte header). Always call `buffer.rewind()` if you need to re-read the data.

## Packet IDs

F1 2025 UDP telemetry uses the following packet IDs:

| Packet ID | Packet Type | Description |
|-----------|-------------|-------------|
| 0 | Motion | Player car motion data |
| 1 | Session | Session start/end events |
| 2 | Lap Data | Lap times and progress |
| 3 | Event | Session events (crashes, penalties, etc.) |
| 4 | Participants | Player and AI driver data |
| 5 | Car Setups | Vehicle setup parameters |
| 6 | Car Telemetry | Speed, throttle, brakes, gear, etc. |
| 7 | Car Status | Fuel, tyres, damage, ERS |
| 8 | Final Classification | End of session results |
| 9 | Lobby Info | Multiplayer lobby data |
| 10 | Car Damage | Detailed damage model |
| 11 | Session History | Historical lap data |
| 12 | Tyre Sets | Tyre compound history |
| 13 | Motion Ex | Extended motion data |
| 14 | Time Trial | Time Trial specific data |

## Error Handling

### Handler Method Exceptions

Exceptions thrown by your handler methods are **caught and logged** but **do not stop** packet processing:

```java
@F1PacketHandler(packetId = 6)
public void handleTelemetry(ByteBuffer payload) {
    throw new RuntimeException("Oops!");  // Logged, other handlers continue
}
```

**Log output:**
```
ERROR c.u.y.f.f.t.u.s.a.MethodPacketHandler -- Error invoking packet handler 
method YourClass.handleTelemetry for packetId=6: Oops!
```

This ensures one failing handler doesn't crash your entire telemetry processing pipeline.

### Duplicate Packet ID Detection

The framework validates that each packet ID has **exactly one handler** per application context:

```java
@F1UdpListener
@Component
public class BadListener {
    @F1PacketHandler(packetId = 6)
    public void handleFirst(ByteBuffer payload) { }
    
    @F1PacketHandler(packetId = 6)  // ❌ DUPLICATE!
    public void handleSecond(ByteBuffer payload) { }
}
```

**Result:** Application fails to start with:
```
BeanCreationException: Duplicate @F1PacketHandler for packetId=6. 
Each packet ID must have exactly one handler method.
```

## Thread Safety

- All components are **thread-safe**
- Multiple threads can process packets concurrently
- `PacketHandlerRegistry` uses `CopyOnWriteArrayList` (safe for concurrent reads)
- Handler methods may be invoked concurrently - ensure they are thread-safe

## Integration with Core Module

This module extends `f1-telemetry-udp-core` by providing Spring integration. You still need to:

1. **Configure UDP listener** - See core module README for `UdpListenerConfig` setup
2. **Start the listener** - Call `UdpTelemetryListener.start()` (can be done in `@PostConstruct` method)
3. **Stop the listener** - Call `UdpTelemetryListener.stop()` (can be done in `@PreDestroy` method)

Example configuration bean:

```java
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.core.UdpTelemetryListener;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.core.config.UdpListenerConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UdpListenerConfiguration {
    
    @Bean
    public UdpTelemetryListener udpTelemetryListener(UdpPacketDispatcher dispatcher) {
        UdpListenerConfig config = UdpListenerConfig.builder()
                .port(20777)  // F1 2025 default port
                .bufferSize(2048)
                .build();
        
        return new UdpTelemetryListener(config, dispatcher);
    }
}
```

Then start/stop in application lifecycle:

```java
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

@Component
public class TelemetryLifecycle {
    
    private final UdpTelemetryListener listener;
    
    public TelemetryLifecycle(UdpTelemetryListener listener) {
        this.listener = listener;
    }
    
    @PostConstruct
    public void startListener() {
        listener.start();
        System.out.println("UDP telemetry listener started on port 20777");
    }
    
    @PreDestroy
    public void stopListener() {
        listener.stop();
        System.out.println("UDP telemetry listener stopped");
    }
}
```

## Testing

All components have comprehensive unit tests:

```bash
mvn test
```

**Test coverage:**
- `MethodPacketHandlerTest` - All three method signatures, exception handling
- `F1PacketHandlerPostProcessorTest` - Bean scanning, method discovery, registration
- `DuplicatePacketIdValidationTest` - Duplicate detection
- `UdpDispatcherConfigurationTest` - Spring configuration wiring

## Example: Complete Application

```java
package com.example.f1telemetry;

import com.ua.yushchenko.f1.fastlaps.telemetry.udp.core.PacketHeader;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.core.UdpPacketDispatcher;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.core.UdpTelemetryListener;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.core.config.UdpListenerConfig;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.annotation.F1PacketHandler;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.annotation.F1UdpListener;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;

@SpringBootApplication
public class F1TelemetryApplication {
    public static void main(String[] args) {
        SpringApplication.run(F1TelemetryApplication.class, args);
    }
    
    @Bean
    public UdpTelemetryListener udpTelemetryListener(UdpPacketDispatcher dispatcher) {
        UdpListenerConfig config = UdpListenerConfig.builder()
                .port(20777)
                .bufferSize(2048)
                .build();
        return new UdpTelemetryListener(config, dispatcher);
    }
}

@Component
class TelemetryLifecycle {
    private final UdpTelemetryListener listener;
    
    TelemetryLifecycle(UdpTelemetryListener listener) {
        this.listener = listener;
    }
    
    @PostConstruct
    void start() {
        listener.start();
    }
    
    @PreDestroy
    void stop() {
        listener.stop();
    }
}

@F1UdpListener
@Component
class MyTelemetryHandler {
    
    @F1PacketHandler(packetId = 6)
    public void handleCarTelemetry(PacketHeader header, ByteBuffer payload) {
        System.out.printf("Frame %d: Car telemetry received%n", header.frameIdentifier());
        // Parse payload and process telemetry data
    }
    
    @F1PacketHandler(packetId = 2)
    public void handleLapData(ByteBuffer payload) {
        System.out.println("Lap data received");
        // Parse lap times and positions
    }
    
    @F1PacketHandler(packetId = 1)
    public void handleSessionEvent(PacketHeader header) {
        System.out.printf("Session event at frame %d%n", header.frameIdentifier());
    }
}
```

Run the application and it will automatically listen for F1 2025 UDP packets on port 20777.

## Module Structure

```
f1-telemetry-udp-spring/
├── pom.xml
├── README.md
└── src/
    ├── main/
    │   └── java/
    │       └── com/ua/yushchenko/f1/fastlaps/telemetry/udp/spring/
    │           ├── annotation/
    │           │   ├── F1PacketHandler.java        # Method-level annotation
    │           │   └── F1UdpListener.java          # Class-level annotation
    │           ├── adapter/
    │           │   └── MethodPacketHandler.java    # Method invocation adapter
    │           ├── registry/
    │           │   └── PacketHandlerRegistry.java  # Handler registration
    │           ├── processor/
    │           │   └── F1PacketHandlerPostProcessor.java  # Bean scanning
    │           └── config/
    │               └── UdpDispatcherConfiguration.java    # Spring config
    └── test/
        └── java/
            └── com/ua/yushchenko/f1/fastlaps/telemetry/udp/spring/
                ├── adapter/
                │   └── MethodPacketHandlerTest.java
                ├── config/
                │   └── UdpDispatcherConfigurationTest.java
                └── processor/
                    ├── F1PacketHandlerPostProcessorTest.java
                    └── DuplicatePacketIdValidationTest.java
```

## Next Steps

- **Phase 3**: Add Kafka publisher integration for forwarding packets to message brokers
- **Phase 4**: Implement packet parsing helpers for each packet type
- **Phase 5**: Create Spring Boot Starter for zero-configuration setup

## License

Part of the F1 FastLaps Telemetry project.
