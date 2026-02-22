package com.ua.yushchenko.f1.fastlaps.telemetry.ingest.handler;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.EventCode;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.SessionLifecycleEvent;
import com.ua.yushchenko.f1.fastlaps.telemetry.ingest.parser.SessionDataPacketParser;
import com.ua.yushchenko.f1.fastlaps.telemetry.ingest.parser.SessionPacketParser;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.core.packet.PacketHeader;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.publisher.TelemetryPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SessionPacketHandler")
class SessionPacketHandlerTest {

    @Mock
    private TelemetryPublisher publisher;
    @Spy
    private SessionPacketParser sessionPacketParser = new SessionPacketParser();
    @Spy
    private SessionDataPacketParser sessionDataPacketParser = new SessionDataPacketParser();
    @InjectMocks
    private SessionPacketHandler handler;

    @Test
    @DisplayName("should parse and publish session start event")
    void shouldParseAndPublishSessionStartEvent() {
        // Arrange
        PacketHeader header = PacketHeader.builder()
                .packetFormat(2025)
                .gameYear((short) 25)
                .gameMajorVersion((short) 1)
                .gameMinorVersion((short) 0)
                .packetVersion((short) 1)
                .packetId((short) 1)
                .sessionUID(123456789L)
                .sessionTime(10.5f)
                .frameIdentifier(100L)
                .overallFrameIdentifier(100L)
                .playerCarIndex((short) 0)
                .secondaryPlayerCarIndex((short) 0)
                .build();

        ByteBuffer payload = createSessionStartPayload();

        // Act
        handler.handleSessionPacket(header, payload);

        // Assert
        ArgumentCaptor<SessionLifecycleEvent> eventCaptor = ArgumentCaptor.forClass(SessionLifecycleEvent.class);
        verify(publisher).publish(eq("telemetry.session"), anyString(), eventCaptor.capture());

        SessionLifecycleEvent event = eventCaptor.getValue();
        assertThat(event.getSessionUID()).isEqualTo(123456789L);
        assertThat(event.getPayload().getEventCode()).isEqualTo(EventCode.SSTA);
        assertThat(event.getPayload().getSessionTypeId()).isNotNull();
    }

    @Test
    @DisplayName("should parse and publish session end event")
    void shouldParseAndPublishSessionEndEvent() {
        // Arrange
        PacketHeader header = PacketHeader.builder()
                .packetFormat(2025)
                .gameYear((short) 25)
                .gameMajorVersion((short) 1)
                .gameMinorVersion((short) 0)
                .packetVersion((short) 1)
                .packetId((short) 1)
                .sessionUID(123456789L)
                .sessionTime(100.5f)
                .frameIdentifier(5000L)
                .overallFrameIdentifier(5000L)
                .playerCarIndex((short) 0)
                .secondaryPlayerCarIndex((short) 0)
                .build();

        ByteBuffer payload = createSessionEndPayload();

        // Act
        handler.handleSessionPacket(header, payload);

        // Assert
        ArgumentCaptor<SessionLifecycleEvent> eventCaptor = ArgumentCaptor.forClass(SessionLifecycleEvent.class);
        verify(publisher).publish(eq("telemetry.session"), anyString(), eventCaptor.capture());

        SessionLifecycleEvent event = eventCaptor.getValue();
        assertThat(event.getPayload().getEventCode()).isEqualTo(EventCode.SEND);
    }

    @Test
    @DisplayName("should not publish non start/end events")
    void shouldNotPublishNonStartEndEvents() {
        // Arrange
        PacketHeader header = PacketHeader.builder()
                .packetFormat(2025)
                .gameYear((short) 25)
                .gameMajorVersion((short) 1)
                .gameMinorVersion((short) 0)
                .packetVersion((short) 1)
                .packetId((short) 1)
                .sessionUID(123456789L)
                .sessionTime(50.5f)
                .frameIdentifier(2500L)
                .overallFrameIdentifier(2500L)
                .playerCarIndex((short) 0)
                .secondaryPlayerCarIndex((short) 0)
                .build();

        ByteBuffer payload = createFlashbackEventPayload();

        // Act
        handler.handleSessionPacket(header, payload);

        // Assert
        verify(publisher, never()).publish(anyString(), anyString(), any());
    }

    private ByteBuffer createSessionStartPayload() {
        ByteBuffer buffer = ByteBuffer.allocate(100).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put("SSTA".getBytes());  // Event code
        buffer.position(24);             // Skip to session fields
        buffer.put((byte) 10);           // Session type = RACE
        buffer.put((byte) 5);            // Track ID = 5
        buffer.put((byte) 50);           // Total laps = 50
        buffer.rewind();
        return buffer;
    }

    private ByteBuffer createSessionEndPayload() {
        ByteBuffer buffer = ByteBuffer.allocate(100).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put("SEND".getBytes());  // Event code
        buffer.position(24);
        buffer.put((byte) 10);           // Session type = RACE
        buffer.put((byte) 5);            // Track ID = 5
        buffer.put((byte) 50);           // Total laps = 50
        buffer.rewind();
        return buffer;
    }

    private ByteBuffer createFlashbackEventPayload() {
        ByteBuffer buffer = ByteBuffer.allocate(100).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put("FLBK".getBytes());  // Event code
        buffer.position(24);
        buffer.put((byte) 10);
        buffer.put((byte) 5);
        buffer.put((byte) 50);
        buffer.rewind();
        return buffer;
    }
}
