# F1 Telemetry UDP Core

Pure Java library for receiving and processing F1 UDP telemetry packets. **No Spring dependencies.**

## Purpose

Provides low-level UDP packet handling infrastructure:

- **DatagramChannel-based UDP listener** - Receives packets from F1 games
- **Packet header decoding** - Parses F1 2025 29-byte packet headers
- **Consumer-based dispatching** - Routes packets to registered handlers by packet type

## Architecture

```
UDP Socket → UdpTelemetryListener → PacketHeaderDecoder → UdpPacketDispatcher → UdpPacketConsumer(s)
```

### Key Components

| Component | Responsibility |
|-----------|---------------|
| `UdpTelemetryListener` | Binds to UDP port, reads packets, manages lifecycle |
| `PacketHeaderDecoder` | Decodes 29-byte F1 packet headers |
| `UdpPacketDispatcher` | Routes packets to consumers by `packetId` |
| `UdpPacketConsumer` | Interface for handling specific packet types |
| `SimpleUdpPacketDispatcher` | Thread-safe implementation with error isolation |

## Usage

### 1. Implement a Consumer

```java
public class TelemetryConsumer implements UdpPacketConsumer {
    
    @Override
    public short packetId() {
        return 6;  // Car telemetry packet
    }
    
    @Override
    public void handle(PacketHeader header, ByteBuffer payload) {
        // Parse payload bytes according to F1 spec
        float speed = payload.getFloat();
        System.out.println("Speed: " + speed);
    }
}
```

### 2. Configure and Start Listener

```java
// Create dispatcher and register consumers
UdpPacketDispatcher dispatcher = new SimpleUdpPacketDispatcher();
dispatcher.registerConsumer(new TelemetryConsumer());

// Configure listener
UdpListenerConfig config = UdpListenerConfig.builder()
    .host("0.0.0.0")
    .port(20777)
    .build();

UdpTelemetryListener listener = new UdpTelemetryListener(config, dispatcher);

// Start in separate thread (blocking call)
Thread listenerThread = new Thread(() -> {
    try {
        listener.start();
    } catch (IOException e) {
        e.printStackTrace();
    }
});
listenerThread.start();

// Later: stop listener
listener.stop();
```

## Packet Header Format

F1 2025 packet header (29 bytes, little-endian):

```
Offset | Type    | Field
-------|---------|----------------------------
0      | uint16  | packetFormat (2025)
2      | uint8   | gameYear (25)
3      | uint8   | gameMajorVersion
4      | uint8   | gameMinorVersion
5      | uint8   | packetVersion
6      | uint8   | packetId
7      | uint64  | sessionUID
15     | float   | sessionTime
19     | uint32  | frameIdentifier
23     | uint32  | overallFrameIdentifier
27     | uint8   | playerCarIndex
28     | uint8   | secondaryPlayerCarIndex
```

## Packet Types (packetId)

| ID | Type | Description |
|----|------|-------------|
| 0  | Motion | Car motion data |
| 1  | Session | Session metadata and events |
| 2  | Lap Data | Lap times, sectors, validity |
| 3  | Event | Session-wide events (SSTA, SEND, etc.) |
| 4  | Participants | Driver/team info |
| 5  | Car Setups | Car configuration |
| 6  | Car Telemetry | Speed, throttle, brake, gear, etc. |
| 7  | Car Status | Fuel, tyres, ERS, DRS, damage |
| 8  | Final Classification | Race results |
| 9  | Lobby Info | Pre-race lobby data |
| 10 | Car Damage | Detailed damage info |
| 11 | Session History | Historical lap data |
| 12 | Tyre Sets | Tyre compound history |
| 13 | Motion Ex | Extended motion data |
| 14 | Time Trial | Time trial specific data |

## Error Handling

- **Consumer exceptions are isolated** - If one consumer throws, others still process the packet
- **Invalid packets are logged and skipped** - Listener continues running
- **Thread-safe registration** - Consumers can be registered concurrently

## Lifecycle Management

```java
listener.start();      // Blocking - starts UDP receive loop
listener.stop();       // Non-blocking - signals shutdown
listener.isRunning();  // Check current state
```

**Important:** `start()` blocks the calling thread. Run in a separate thread for non-blocking operation.

## Testing

Run unit tests:

```bash
mvn test
```

Tests cover:
- Packet header decoding with various byte orders
- Dispatcher routing to multiple consumers
- Error isolation in consumer invocation
- UDP packet reception and lifecycle management

## Dependencies

- **SLF4J API** - Logging abstraction
- **Lombok** - Boilerplate reduction (optional, compile-time only)

**No Spring, No Kafka, No Jackson** - Pure Java for maximum portability.

## Thread Safety

- `SimpleUdpPacketDispatcher` is fully thread-safe
- `UdpTelemetryListener` uses single-threaded receive loop
- Buffer rewind ensures each consumer sees clean data

## Related Modules

- `f1-telemetry-udp-spring` - Spring Boot integration with `@F1PacketHandler` annotations
- `f1-telemetry-udp-starter` - Spring Boot auto-configuration
- `udp-ingest-service` - Business logic using the library

## References

- [F1 2025 UDP Specification](https://answers.ea.com/t5/General-Discussion/F1-25-UDP-Specification/td-p/14362164)
- [UDP Library Implementation Guide](../../.github/project/udp_telemetry_ingest_as_reusable_library_implementation_guide.md)
