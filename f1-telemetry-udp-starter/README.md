# F1 Telemetry UDP Spring Boot Starter

Spring Boot starter for F1 telemetry UDP packet ingestion.

## Features

- **Auto-configuration**: Automatically configures UDP listener when enabled
- **Declarative packet handling**: Use `@F1UdpListener` and `@F1PacketHandler` annotations
- **Lifecycle management**: Automatic start/stop with Spring application context
- **Configuration properties**: Easy configuration via `application.yml`

## Usage

### 1. Add Dependency

```xml
<dependency>
    <groupId>com.ua.yushchenko.f1.fastlaps.telemetry</groupId>
    <artifactId>f1-telemetry-udp-starter</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

### 2. Configure Properties

```yaml
f1:
  telemetry:
    udp:
      enabled: true        # Enable UDP listener (default: true)
      host: 0.0.0.0        # Bind address (default: 0.0.0.0)
      port: 20777          # UDP port (default: 20777)
      buffer-size: 2048    # Buffer size in bytes (default: 2048)
```

### 3. Create Packet Handlers

```java
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.annotation.F1PacketHandler;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.annotation.F1UdpListener;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.core.packet.PacketHeader;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;

@Component
@F1UdpListener
public class MyTelemetryHandler {

    @F1PacketHandler(packetId = 6) // Car telemetry
    public void handleCarTelemetry(PacketHeader header, ByteBuffer payload) {
        // Parse and process telemetry data
        System.out.println("Received telemetry packet: " + header.getSessionUID());
    }

    @F1PacketHandler(packetId = 2) // Lap data
    public void handleLapData(PacketHeader header, ByteBuffer payload) {
        // Parse and process lap data
        System.out.println("Received lap data: " + header.getFrameIdentifier());
    }
}
```

### 4. Run Application

The UDP listener will automatically:
- Start when Spring Boot application is ready
- Stop gracefully when application shuts down
- Route packets to registered handlers by packet ID

## Supported Method Signatures

Handler methods can use any of these signatures:

```java
// Full context
@F1PacketHandler(packetId = 1)
public void handle(PacketHeader header, ByteBuffer payload) { }

// Payload only
@F1PacketHandler(packetId = 1)
public void handle(ByteBuffer payload) { }

// Header only
@F1PacketHandler(packetId = 1)
public void handle(PacketHeader header) { }
```

## F1 2025 Packet IDs

| Packet ID | Description |
|-----------|-------------|
| 0 | Motion |
| 1 | Session |
| 2 | Lap Data |
| 3 | Event |
| 4 | Participants |
| 5 | Car Setups |
| 6 | Car Telemetry |
| 7 | Car Status |
| 8 | Final Classification |
| 9 | Lobby Info |
| 10 | Car Damage |
| 11 | Session History |
| 12 | Tyre Sets |
| 13 | Motion Ex |

## Architecture

The starter brings together three modules:

1. **f1-telemetry-udp-core**: Pure Java UDP handling (no Spring)
2. **f1-telemetry-udp-spring**: Spring integration with annotations
3. **f1-telemetry-udp-starter**: Auto-configuration

## Configuration Properties Reference

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `f1.telemetry.udp.enabled` | Boolean | `true` | Enable/disable UDP listener |
| `f1.telemetry.udp.host` | String | `0.0.0.0` | Host address to bind |
| `f1.telemetry.udp.port` | Integer | `20777` | UDP port to listen on |
| `f1.telemetry.udp.buffer-size` | Integer | `2048` | UDP packet buffer size |

## Disabling the Starter

To disable UDP telemetry ingestion:

```yaml
f1:
  telemetry:
    udp:
      enabled: false
```

When disabled, no UDP beans are created and no resources are consumed.

## Example Application

See the `udp-ingest-service` module for a complete example application using this starter.

## Requirements

- Java 17+
- Spring Boot 3.2.5+

## License

This project is part of the F1 FastLaps Telemetry system.
